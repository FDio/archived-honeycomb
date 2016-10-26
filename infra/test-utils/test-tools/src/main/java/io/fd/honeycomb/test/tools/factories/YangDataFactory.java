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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import io.fd.honeycomb.translate.util.JsonUtils;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.data.util.AbstractModuleStringInstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

/**
 * Common logic for reading of yang data
 */
abstract class YangDataFactory {

    final SchemaContext schemaContext;
    final BindingToNormalizedNodeCodec serializer;
    final AbstractModuleStringInstanceIdentifierCodec iidParser;

    YangDataFactory(@Nonnull final SchemaContext schemaContext,
                    @Nonnull final BindingToNormalizedNodeCodec serializer,
                    @Nonnull final AbstractModuleStringInstanceIdentifierCodec iidParser) {
        this.schemaContext = checkNotNull(schemaContext, "SchemaContext cannot be null");
        this.serializer = checkNotNull(serializer, "Serializer cannot be null");
        this.iidParser = checkNotNull(iidParser, "Instance identifier parser cannot be null");
    }

    DataObject getDataForNode(final YangInstanceIdentifier nodeYangIdentifier,
                              final String resourcePath,
                              final DataNodeContainer parentSchema)
            throws DeserializationException, IOException {

        // Reads resources from provided resource path
        final ContainerNode rootData = getCheckedRootData(resourcePath, parentSchema);

        // Now transform the single child from JSON into BA format
        final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> actualData =
                extractCheckedSingleChild(rootData);

        return getCheckedBinding(nodeYangIdentifier, actualData).getValue();
    }

    private ContainerNode getCheckedRootData(final String resourcePath, final DataNodeContainer parentSchema)
            throws IOException {
        // TODO the cast to SchemaNode is dangerous and would not work for Augments, Choices and some other nodes maybe. At least check
        // TODO not sure if this is true, while testing this code was working fine event while processing choices/cases,
        // TODO only problem is to find suitable codec that can process cases,etc
        // Transform JSON into NormalizedNode

        final ContainerNode rootData = JsonUtils.readJson(schemaContext,
                checkNotNull(this.getClass().getResource(resourcePath), "Unable to find resource %s", resourcePath)
                        .openStream(), ((SchemaNode) parentSchema));

        checkArgument(rootData.getValue().size() == 1, "Only a single data node is expected in %s, but there were: %s",
                resourcePath, rootData.getValue());
        return rootData;
    }

    private DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> extractCheckedSingleChild(
            final ContainerNode rootData) {
        // Now transform the single child from JSON into BA format
        final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> actualData =
                Iterables.getFirst(rootData.getValue(), null);

        checkNotNull(actualData, "Unable to extract single child from %s", rootData);
        return actualData;
    }

    private Map.Entry<InstanceIdentifier<? extends DataObject>, DataObject> getCheckedBinding(
            final YangInstanceIdentifier nodeYangIdentifier,
            final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> actualData)
            throws DeserializationException {
        final Optional<Map.Entry<InstanceIdentifier<? extends DataObject>, DataObject>> ba =
                serializer.toBinding(new AbstractMap.SimpleImmutableEntry<>(nodeYangIdentifier, actualData));

        checkArgument(ba.isPresent(), "Unable to convert to binding %s", nodeYangIdentifier);
        return ba.get();
    }
}
