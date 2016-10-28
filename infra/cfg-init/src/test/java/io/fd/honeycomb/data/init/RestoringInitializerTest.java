/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.data.init;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Futures;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestoringInitializerTest {

    @Mock
    private SchemaService schemaService;
    @Mock
    private SchemaContext schemaContext;
    @Mock
    private DOMDataBroker dataTree;
    @Mock
    private RestoringInitializer.JsonReader jsonReader;
    @Mock
    private ContainerNode data;
    @Mock
    private DOMDataWriteTransaction writeTx;
    private Path path;
    private YangInstanceIdentifier.NodeIdentifier nodeId =
            new YangInstanceIdentifier.NodeIdentifier(QName.create("namespace", "data"));

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        path = Files.createTempFile("hc", "restoretest");
        when(schemaService.getGlobalContext()).thenReturn(schemaContext);
        when(jsonReader.readData(schemaContext, path)).thenReturn(data);
        when(dataTree.newWriteOnlyTransaction()).thenReturn(writeTx);
        when(writeTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
        when(data.getValue()).thenReturn(Collections.singleton(data));
        when(data.getIdentifier()).thenReturn(nodeId);
    }

    @After
    public void tearDown() throws Exception {
        try {
            Files.delete(path);
        } catch (NoSuchFileException e) {
            // ignoring, if the file does not exist already, never mind
        }
    }

    @Test
    public void testPutOper() throws Exception {
        final RestoringInitializer init =
                new RestoringInitializer(schemaService, path, dataTree,
                        RestoringInitializer.RestorationType.Put, LogicalDatastoreType.OPERATIONAL, jsonReader);

        init.initialize();

        verify(schemaService).getGlobalContext();
        verify(jsonReader).readData(schemaContext, path);

        verify(dataTree).newWriteOnlyTransaction();
        verify(writeTx).put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.create(nodeId), data);
        verify(writeTx).submit();
    }

    @Test
    public void testMergeConfig() throws Exception {
        final RestoringInitializer init =
                new RestoringInitializer(schemaService, path, dataTree,
                        RestoringInitializer.RestorationType.Merge, LogicalDatastoreType.CONFIGURATION, jsonReader);

        init.initialize();

        verify(writeTx).merge(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.create(nodeId), data);
    }

    @Test
    public void testNoRestore() throws Exception {
        Files.delete(path);
        final RestoringInitializer init =
                new RestoringInitializer(schemaService, path, dataTree,
                        RestoringInitializer.RestorationType.Merge, LogicalDatastoreType.CONFIGURATION, jsonReader);

        init.initialize();

        verifyZeroInteractions(writeTx);
    }

    @Test(expected = DataTreeInitializer.InitializeException.class)
    public void testFail() throws Exception {
        when(jsonReader.readData(schemaContext, path)).thenThrow(new IOException("t"));

        final RestoringInitializer init =
                new RestoringInitializer(schemaService, path, dataTree,
                        RestoringInitializer.RestorationType.Merge, LogicalDatastoreType.CONFIGURATION, jsonReader);

        init.initialize();
    }
}