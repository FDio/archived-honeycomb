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

package io.fd.honeycomb.notification.impl;

import java.io.IOException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.netconf.notifications.NetconfNotification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class TranslationUtil {

    public TranslationUtil() {}

    private static final Logger LOG = LoggerFactory.getLogger(TranslationUtil.class);
    private static final XMLOutputFactory XML_FACTORY;

    static {
        XML_FACTORY = XMLOutputFactory.newFactory();
        XML_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, false);
    }

    /**
     * Transform {@link org.opendaylight.mdsal.dom.api.DOMNotification} into an XML based {@link NetconfNotification}.
     */
    public static NetconfNotification notificationToXml(final DOMNotification domNotification, final SchemaContext ctx) {

        LOG.trace("Transforming notification: {} into XML", domNotification.getType());

        final SchemaPath type = domNotification.getType();
        final QName notificationQName = type.getLastComponent();
        final DOMResult result = prepareDomResultForRpcRequest(notificationQName);

        try {
            writeNormalizedRpc(domNotification, result, type, ctx);
        } catch (final XMLStreamException | IOException | IllegalStateException e) {
            LOG.warn("Unable to transform notification: {} into XML", domNotification.getType(), e);
            throw new IllegalArgumentException("Unable to serialize " + type, e);
        }

        final Document node = result.getNode().getOwnerDocument();
        return new NetconfNotification(node);
    }

    private static DOMResult prepareDomResultForRpcRequest(final QName notificationQName) {
        final Document document = XmlUtil.newDocument();
        final Element notificationElement =
            document.createElementNS(notificationQName.getNamespace().toString(), notificationQName.getLocalName());
        document.appendChild(notificationElement);
        return new DOMResult(notificationElement);
    }

    private static void writeNormalizedRpc(final DOMNotification normalized, final DOMResult result,
                                           final SchemaPath schemaPath, final SchemaContext baseNetconfCtx)
        throws IOException, XMLStreamException {
        final XMLStreamWriter writer = XML_FACTORY.createXMLStreamWriter(result);
        try {
            try (final NormalizedNodeStreamWriter normalizedNodeStreamWriter =
                     XMLStreamNormalizedNodeStreamWriter.create(writer, baseNetconfCtx, schemaPath)) {
                try (final NormalizedNodeWriter normalizedNodeWriter =
                         NormalizedNodeWriter.forStreamWriter(normalizedNodeStreamWriter)) {
                    for (DataContainerChild<?, ?> dataContainerChild : normalized.getBody().getValue()) {
                        normalizedNodeWriter.write(dataContainerChild);
                    }
                    normalizedNodeWriter.flush();
                }
            }
        } finally {
            try {
                writer.close();
            } catch (final Exception e) {
                LOG.warn("Unable to close resource properly. Ignoring", e);
            }
        }
    }
}
