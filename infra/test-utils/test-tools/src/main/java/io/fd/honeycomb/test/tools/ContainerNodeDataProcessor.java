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
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkState;
import static io.fd.honeycomb.translate.util.JsonUtils.readContainerEntryJson;
import static io.fd.honeycomb.translate.util.JsonUtils.readJson;

final class ContainerNodeDataProcessor extends AbstractYangContextHolder implements YangDataProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ContainerNodeDataProcessor.class);

    ContainerNodeDataProcessor(@Nonnull SchemaContext schemaContext, @Nonnull BindingToNormalizedNodeCodec serializer) {
        super(schemaContext, serializer);
    }

    @Nonnull
    @Override
    public DataObject getNodeData(@Nonnull YangInstanceIdentifier yangInstanceIdentifier, @Nonnull String resourcePath) {

        final InputStream resourceStream = this.getClass().getResourceAsStream(resourcePath);
        final YangInstanceIdentifier nodeParent = getNodeParent(yangInstanceIdentifier).orElse(null);
        final SchemaNode parentSchema = parentSchema(schemaContext(), serializer(), nodeParent, () -> LOG);

        // to be able to process containers in root of model
        if (isRoot(yangInstanceIdentifier)) {
            // if root ,read as root
            final ContainerNode data = readJson(schemaContext(), resourceStream, parentSchema);
            checkState(data.getValue().size() == 1, "Single root expected in %s", resourcePath);
            //then extracts first child
            final QName rootNodeType = data.getValue().iterator().next().getNodeType();
            final YangInstanceIdentifier realIdentifier = YangInstanceIdentifier.of(rootNodeType);
            return nodeBinding(serializer(), realIdentifier, data).getValue();
        } else {
            // reads just container
            final YangInstanceIdentifier.NodeIdentifier nodeIdentifier = containerNodeIdentifier(yangInstanceIdentifier);
            final ContainerNode data = readContainerEntryJson(schemaContext(), resourceStream, parentSchema, nodeIdentifier);
            return nodeBinding(serializer(), yangInstanceIdentifier, data.getValue().iterator().next()).getValue();
        }
    }

    @Override
    public boolean canProcess(@Nonnull YangInstanceIdentifier identifier) {
        return isRoot(identifier) ||
                isContainer(identifierBinding(serializer(), identifier).getTargetType());
    }

    private static boolean isContainer(final Class targetType) {
        return !Identifiable.class.isAssignableFrom(targetType)
                && !Augmentation.class.isAssignableFrom(targetType);
    }

    private static YangInstanceIdentifier.NodeIdentifier containerNodeIdentifier(@Nonnull final YangInstanceIdentifier nodeIdentifier) {
        return java.util.Optional.ofNullable(nodeIdentifier.getLastPathArgument())
                .filter(pathArgument -> pathArgument instanceof YangInstanceIdentifier.NodeIdentifier)
                .map(YangInstanceIdentifier.NodeIdentifier.class::cast)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Unable to create container node identifier from %s", nodeIdentifier)));
    }
}
