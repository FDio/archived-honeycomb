/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.data.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeConfiguration.DEFAULT_CONFIGURATION;

import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.Futures;
import io.fd.honeycomb.data.ModifiableDataManager;
import io.fd.honeycomb.data.ReadableDataManager;
import java.io.StringWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.netconf.api.DocumentedException;
import org.opendaylight.netconf.api.xml.XmlUtil;
import org.opendaylight.netconf.mapping.api.NetconfOperation;
import org.opendaylight.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.netconf.mdsal.connector.CurrentSchemaContext;
import org.opendaylight.netconf.mdsal.connector.TransactionProvider;
import org.opendaylight.netconf.mdsal.connector.ops.Commit;
import org.opendaylight.netconf.mdsal.connector.ops.EditConfig;
import org.opendaylight.netconf.mdsal.connector.ops.Validate;
import org.opendaylight.netconf.util.test.NetconfXmlUnitRecursiveQualifier;
import org.opendaylight.netconf.util.test.XmlFileLoader;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class ValidateTest {
    private static final Logger LOG = LoggerFactory.getLogger(ValidateTest.class);
    private static final String SESSION_ID_FOR_REPORTING = "netconf-test-session";
    private static final Document RPC_REPLY_OK = getReplyOk();

    @Mock
    private DOMSchemaService schemaService;
    @Mock
    private ReadableDataManager operationalData;

    private ModifiableDataManager configData;
    private CurrentSchemaContext currentSchemaContext;
    private DataBroker dataBroker;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        final DataTree dataTree = new InMemoryDataTreeFactory().create(DEFAULT_CONFIGURATION);
        final SchemaContext schemaContext = YangParserTestUtils.parseYangResources(ValidateTest.class,
            "/test-validate.yang");
        when(schemaService.registerSchemaContextListener(any())).thenAnswer(invocation -> {
            SchemaContextListener listener = invocation.getArgument(0);
            listener.onGlobalContextUpdated(schemaContext);
            return new ListenerRegistration<SchemaContextListener>() {
                @Override
                public void close() {
                }

                @Override
                public SchemaContextListener getInstance() {
                    return listener;
                }
            };
        });
        currentSchemaContext = new CurrentSchemaContext(schemaService, sourceIdentifier -> {
            final YangTextSchemaSource yangTextSchemaSource =
                YangTextSchemaSource.delegateForByteSource(sourceIdentifier, ByteSource.wrap("module test".getBytes()));
            return Futures.immediateCheckedFuture(yangTextSchemaSource);
        });
        dataTree.setSchemaContext(schemaContext);
        configData = new ModifiableDataTreeManager(dataTree);
        dataBroker = DataBroker.create(configData, operationalData);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
    }

    @Test
    public void testValidate() throws Exception {
        verifyResponse(validate("messages/validate/validate.xml"));
    }

    @Test
    public void testValidateMissingMandatoryNode() throws Exception {
        final TransactionProvider transactionProvider = new TransactionProvider(dataBroker, SESSION_ID_FOR_REPORTING);
        verifyResponse(edit("messages/validate/edit-config-missing-mandatory-node.xml", transactionProvider));
        try {
            verifyResponse(commit(transactionProvider));
            fail("Should have failed due to missing mandatory node");
        } catch (DocumentedException e) {
            assertTrue(e.getMessage().contains("missing mandatory descendant"));
        }
    }

    @Test
    public void testValidateAndCommit() throws Exception {
        final TransactionProvider transactionProvider = new TransactionProvider(dataBroker, SESSION_ID_FOR_REPORTING);
        verifyResponse(edit("messages/validate/edit-config-create-top-container.xml", transactionProvider));
        verifyResponse(validate("messages/validate/validate.xml", transactionProvider));
        verifyResponse(commit(transactionProvider));
    }

    @Test
    public void testValidateTwice() throws Exception {
        final TransactionProvider transactionProvider = initCandidateTransaction();
        verifyResponse(validate("messages/validate/validate.xml", transactionProvider));
        verifyResponse(validate("messages/validate/validate.xml", transactionProvider));
    }

    @SuppressWarnings("illegalCatch")
    private static Document getReplyOk() {
        Document doc;
        try {
            doc = XmlFileLoader.xmlFileToDocument("messages/rpc-reply_ok.xml");
        } catch (final Exception e) {
            LOG.debug("unable to load rpc reply ok.", e);
            doc = XmlUtil.newDocument();
        }
        return doc;
    }

    private static void verifyResponse(final Document actual) throws Exception {
        final DetailedDiff dd = new DetailedDiff(new Diff(RPC_REPLY_OK, actual));
        dd.overrideElementQualifier(new NetconfXmlUnitRecursiveQualifier());
        if (!dd.similar()) {
            LOG.warn("Actual response:");
            printDocument(actual);
            LOG.warn("Expected response:");
            printDocument(RPC_REPLY_OK);
            fail("Differences found: " + dd.toString());
        }
    }

    private static void printDocument(final Document doc) throws Exception {
        final TransformerFactory tf = TransformerFactory.newInstance();
        final Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        final StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc),
            new StreamResult(writer));
        LOG.warn(writer.getBuffer().toString());
    }

    private Document validate(final String resource) throws Exception {
        final TransactionProvider transactionProvider = initCandidateTransaction();
        return validate(resource, transactionProvider);
    }

    private Document validate(final String resource, final TransactionProvider transactionProvider) throws Exception {
        final Validate validate = new Validate(SESSION_ID_FOR_REPORTING, transactionProvider);
        return executeOperation(validate, resource);
    }

    private Document edit(final String resource, final TransactionProvider transactionProvider) throws Exception {
        final EditConfig editConfig = new EditConfig(SESSION_ID_FOR_REPORTING, currentSchemaContext,
            transactionProvider);
        return executeOperation(editConfig, resource);
    }

    private Document commit(final TransactionProvider transactionProvider) throws Exception {
        final Commit commit = new Commit(SESSION_ID_FOR_REPORTING, transactionProvider);
        return executeOperation(commit, "messages/validate/commit.xml");
    }

    private TransactionProvider initCandidateTransaction() {
        final TransactionProvider transactionProvider = new TransactionProvider(dataBroker, SESSION_ID_FOR_REPORTING);
        // Emulate non empty <candidate> DS to make sure validate is not skipped:
        transactionProvider.getOrCreateTransaction();
        return transactionProvider;
    }

    private static Document executeOperation(final NetconfOperation op, final String filename) throws Exception {
        final Document request = XmlFileLoader.xmlFileToDocument(filename);
        final Document response = op.handle(request, NetconfOperationChainedExecution.EXECUTION_TERMINATION_POINT);

        LOG.debug("Got response {}", response);
        return response;
    }
}
