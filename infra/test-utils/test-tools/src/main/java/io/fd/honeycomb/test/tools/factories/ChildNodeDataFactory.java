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

package io.fd.honeycomb.test.tools.factories;


import com.google.common.base.Optional;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.yangtools.sal.binding.generator.impl.BindingSchemaContextUtils;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.data.util.AbstractModuleStringInstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChildNodeDataFactory extends YangDataFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ChildNodeDataFactory.class);

    public ChildNodeDataFactory(@Nonnull final SchemaContext schemaContext,
                                @Nonnull final BindingToNormalizedNodeCodec serializer,
                                @Nonnull final AbstractModuleStringInstanceIdentifierCodec iidParser) {
        super(schemaContext, serializer, iidParser);
    }

    public DataObject getChildNodeData(final String instanceIdentifier,
                                       final String resourcePath) throws DeserializationException, IOException {
        // Parse string ID into YangId
        final YangInstanceIdentifier nodeYid = iidParser.deserialize(instanceIdentifier);
        // Look for parent YangId
        final YangInstanceIdentifier parentYid = nodeYid.getParent();

        if (parentYid.isEmpty()) {
            throw new IllegalArgumentException(
                    "Attempt to process root node as children has been detected,to process root nodes just don't use id in @InjectTestData");
        }
        // Find Schema node for parent of data that's currently being parsed (needed when parsing the data into NormalizedNodes)
        return getDataForNode(nodeYid, resourcePath, getNonRootParentSchema(parentYid));
    }

    private DataNodeContainer getNonRootParentSchema(final YangInstanceIdentifier parentYangId)
            throws DeserializationException {
        LOG.debug("Processing parent identifier {}", parentYangId);
        final Optional<InstanceIdentifier<? extends DataObject>> parentInstanceId = serializer.toBinding(parentYangId);
        if (!parentInstanceId.isPresent()) {
            throw new IllegalStateException("Unable to resolve " + parentYangId + " to instance identifier");
        }

        final Optional<DataNodeContainer> dataNodeContainerOptional =
                BindingSchemaContextUtils.findDataNodeContainer(schemaContext, parentInstanceId.get());

        if (!dataNodeContainerOptional.isPresent()) {
            throw new IllegalArgumentException("Error finding DataNodeContainer for " + parentInstanceId.get());
        }

        return dataNodeContainerOptional.get();
    }
}
