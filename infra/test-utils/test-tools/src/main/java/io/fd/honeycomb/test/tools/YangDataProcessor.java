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

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.generator.impl.BindingSchemaContextUtils;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.codec.DeserializationException;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;

interface YangDataProcessor {

    /**
     * Attempts to find data in file specified by <b>resourcePath</b>,<br>
     * and translate it to BA object
     *
     * @param yangInstanceIdentifier identifier of path to read
     * @param resourcePath           path of resource file to load
     */
    @Nonnull
    DataObject getNodeData(@Nonnull final YangInstanceIdentifier yangInstanceIdentifier,
                           @Nonnull final String resourcePath);

    /**
     * Verifies if provided identifier is identifying node processed by this processor
     *
     * @param identifier node identifier
     */
    boolean canProcess(@Nonnull final YangInstanceIdentifier identifier);

    default boolean isRoot(@Nonnull final YangInstanceIdentifier identifier) {
        return identifier.getPathArguments().isEmpty();
    }

    @Nonnull
    default Optional<YangInstanceIdentifier> getNodeParent(@Nonnull final YangInstanceIdentifier identifier) {
        return Optional.ofNullable(identifier.getParent());
    }

    @Nonnull
    default SchemaNode parentSchema(@Nonnull final SchemaContext schemaContext,
                                    @Nonnull final BindingToNormalizedNodeCodec serializer,
                                    @Nullable final YangInstanceIdentifier parentYangId,
                                    @Nonnull final Logger logger) {
        // null or root
        if (parentYangId == null || parentYangId.getPathArguments().size() == 0) {
            // no parent == use schema context as root context
            logger.info("Parent is null, providing schema context as parent node");
            return schemaContext;
        }

        final Optional<InstanceIdentifier<? extends DataObject>> parentInstanceId;
        try {
            parentInstanceId = serializer.toBinding(parentYangId);
        } catch (DeserializationException e) {
            throw new IllegalArgumentException(String.format("Unable to deserialize %s", parentYangId), e);
        }

        if (!parentInstanceId.isPresent()) {
            throw new IllegalStateException(String.format("Unable to resolve %s to instance identifier", parentYangId));
        }

        final Optional<DataNodeContainer> dataNodeContainerOptional =
                BindingSchemaContextUtils.findDataNodeContainer(schemaContext, parentInstanceId.get());


        if (!dataNodeContainerOptional.isPresent()) {
            throw new IllegalArgumentException(
                    String.format("Error finding DataNodeContainer for %s", parentInstanceId.get()));
        }

        final DataNodeContainer parentNode = dataNodeContainerOptional.get();
        logger.info("Parent schema node resolved as {}", parentNode);
        return (SchemaNode) parentNode;
    }

    @Nonnull
    default Map.Entry<InstanceIdentifier<? extends DataObject>, DataObject> nodeBinding(
            @Nonnull final BindingToNormalizedNodeCodec serializer,
            @Nonnull final YangInstanceIdentifier identifier,
            @Nonnull final NormalizedNode<?, ?> data) throws IllegalArgumentException {
        try {
            return serializer.toBinding(new AbstractMap.SimpleImmutableEntry<>(identifier, data))
                    .orElseThrow(new Supplier<RuntimeException>() {
                        @Override
                        public RuntimeException get() {
                            throw new IllegalArgumentException(
                                    String.format("Unable to create node binding  for %s|%s", identifier, data));
                        }
                    });
        } catch (DeserializationException e) {
            throw new IllegalArgumentException(String.format("Unable to deserialize node %s|%s", identifier, data), e);
        }
    }

    @Nonnull
    default InstanceIdentifier<? extends DataObject> identifierBinding(
            @Nonnull final BindingToNormalizedNodeCodec serializer,
            @Nonnull final YangInstanceIdentifier identifier) throws IllegalArgumentException{
        try {
            return serializer.toBinding(identifier)
                    .orElseThrow(new Supplier<RuntimeException>() {
                        @Override
                        public RuntimeException get() {
                            throw new IllegalArgumentException(
                                    String.format("Unable convert %s to binding", identifier));
                        }
                    });
        } catch (DeserializationException e) {
            throw new IllegalArgumentException(String.format("Unable to deserialize %s", identifier), e);
        }
    }
}
