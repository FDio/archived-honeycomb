/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.honeycomb.test.tools;

import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static io.fd.honeycomb.translate.util.JsonUtils.readListEntryFromJson;

/**
 * json --> BA processor for list entry data
 */
final class ListNodeDataProcessor extends AbstractYangContextHolder implements YangDataProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ListNodeDataProcessor.class);

    ListNodeDataProcessor(@Nonnull final SchemaContext schemaContext,
                          @Nonnull final BindingToNormalizedNodeCodec serializer) {
        super(schemaContext, serializer);
    }

    @Nonnull
    @Override
    public DataObject getNodeData(@Nonnull final YangInstanceIdentifier nodeIdentifier,
                                  @Nonnull final String resourcePath) {
        checkArgument(canProcess(nodeIdentifier), "Cannot process identifier %s", nodeIdentifier);
        final YangInstanceIdentifier listParent = listNodeParent(nodeIdentifier);
        final YangInstanceIdentifier.NodeIdentifierWithPredicates keyedNodeIdentifier = listNodeIdentifier(nodeIdentifier);
        final InputStream resourceStream = this.getClass().getResourceAsStream(resourcePath);
        checkState(resourceStream != null, "Resource %s not found", resourcePath);

        final SchemaNode parentSchemaNode = parentSchema(schemaContext(), serializer(), listParent, () -> LOG);
        final MapEntryNode data = readListEntryFromJson(schemaContext(), resourceStream, parentSchemaNode, keyedNodeIdentifier);

        return nodeBinding(serializer(), nodeIdentifier, data).getValue();
    }

    @Override
    public boolean canProcess(@Nonnull final YangInstanceIdentifier identifier) {
        return !isRoot(identifier) &&
                Identifiable.class.isAssignableFrom(identifierBinding(serializer(), identifier).getTargetType());
    }

    private YangInstanceIdentifier listNodeParent(@Nonnull final YangInstanceIdentifier nodeIdentifier) {
        // if it is list, real parent is second to last
        return getNodeParent(nodeIdentifier).map(parent -> getNodeParent(parent).orElse(null))
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Unable to get parent for list node from %s", nodeIdentifier)));
    }

    private static YangInstanceIdentifier.NodeIdentifierWithPredicates listNodeIdentifier(@Nonnull final YangInstanceIdentifier nodeIdentifier) {
        return java.util.Optional.ofNullable(nodeIdentifier.getLastPathArgument())
                .filter(pathArgument -> pathArgument instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates)
                .map(YangInstanceIdentifier.NodeIdentifierWithPredicates.class::cast)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Unable to create list node identifier from %s", nodeIdentifier)));
    }
}



