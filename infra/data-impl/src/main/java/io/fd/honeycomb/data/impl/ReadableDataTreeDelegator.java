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

package io.fd.honeycomb.data.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.FluentFuture;
import io.fd.honeycomb.data.ReadableDataManager;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.read.registry.ReaderRegistry;
import io.fd.honeycomb.translate.util.TransactionMappingContext;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.UnkeyedListNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ReadableDataTree implementation for operational data.
 */
public final class ReadableDataTreeDelegator implements ReadableDataManager {
    private static final Logger LOG = LoggerFactory.getLogger(ReadableDataTreeDelegator.class);

    private final BindingNormalizedNodeSerializer serializer;
    private final ReaderRegistry readerRegistry;
    private final SchemaContext globalContext;
    private final org.opendaylight.mdsal.binding.api.DataBroker contextBroker;

    /**
     * Creates operational data tree instance.
     * @param serializer     service for serialization between Java Binding Data representation and NormalizedNode
     *                       representation.
     * @param globalContext  service for obtaining top level context data from all yang modules.
     * @param readerRegistry service responsible for translation between DataObjects and data provider.
     * @param contextBroker BA broker for context data
     */
    public ReadableDataTreeDelegator(@Nonnull BindingNormalizedNodeSerializer serializer,
                                     @Nonnull final SchemaContext globalContext,
                                     @Nonnull final ReaderRegistry readerRegistry,
                                     @Nonnull final org.opendaylight.mdsal.binding.api.DataBroker contextBroker) {
        this.contextBroker = checkNotNull(contextBroker, "contextBroker should not be null");
        this.globalContext = checkNotNull(globalContext, "globalContext should not be null");
        this.serializer = checkNotNull(serializer, "serializer should not be null");
        this.readerRegistry = checkNotNull(readerRegistry, "reader should not be null");
    }

    @Override
    public FluentFuture<Optional<NormalizedNode<?, ?>>> read(
            @Nonnull final YangInstanceIdentifier yangInstanceIdentifier) {

        try (TransactionMappingContext mappingContext = new TransactionMappingContext(
                contextBroker.newReadWriteTransaction());
             ReadContext ctx = new ReadContextImpl(mappingContext)) {

            final Optional<NormalizedNode<?, ?>> value;
            if (checkNotNull(yangInstanceIdentifier).equals(YangInstanceIdentifier.EMPTY)) {
                value = readRoot(ctx);
            } else {
                value = readNode(yangInstanceIdentifier, ctx);
            }

            // Submit context mapping updates
            final FluentFuture<? extends CommitInfo> contextUpdateResult =
                    ((TransactionMappingContext) ctx.getMappingContext()).commit();
            // Blocking on context data update
            contextUpdateResult.get();

            return FluentFutures.immediateFluentFuture(value);

        } catch (InterruptedException | ExecutionException | ReadFailedException ex) {
            return FluentFutures.immediateFailedFluentFuture(
                    new org.opendaylight.controller.md.sal.common.api.data.ReadFailedException("Failed to read data",
                            ex));
        }
    }

    private Optional<NormalizedNode<?, ?>> readNode(final YangInstanceIdentifier yangInstanceIdentifier,
                                                    final ReadContext ctx) throws ReadFailedException {
        LOG.debug("OperationalDataTree.readNode(), yangInstanceIdentifier={}", yangInstanceIdentifier);
        final InstanceIdentifier<?> path = serializer.fromYangInstanceIdentifier(yangInstanceIdentifier);
        checkNotNull(path, "Invalid instance identifier %s. Cannot create BA equivalent.", yangInstanceIdentifier);
        LOG.debug("OperationalDataTree.readNode(), path={}", path);

        final Optional<? extends DataObject> dataObject = readerRegistry.read(path, ctx);

        // Modification cache should not be used after DOs are read, so we can clear it now to reduce peak footprint.
        ctx.getModificationCache().close();

        if (dataObject.isPresent()) {
            final NormalizedNode<?, ?> value = toNormalizedNodeFunction(path).apply(dataObject.get());
            return Optional.ofNullable(value);
        } else {
            return Optional.empty();
        }
    }

    private Optional<NormalizedNode<?, ?>> readRoot(final ReadContext ctx) throws ReadFailedException {
        LOG.debug("OperationalDataTree.readRoot()");

        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> dataNodeBuilder =
                Builders.containerBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(SchemaContext.NAME));

        final Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> dataObjects =
                readerRegistry.readAll(ctx);

        // Modification cache should not be used after DOs are read, so we can clear it now to reduce peak footprint.
        // Even greater reduction can be achieved with HONEYCOMB-361.
        ctx.getModificationCache().close();

        for (final InstanceIdentifier<? extends DataObject> instanceIdentifier : dataObjects.keySet()) {
            final YangInstanceIdentifier rootElementId = serializer.toYangInstanceIdentifier(instanceIdentifier);
            final NormalizedNode<?, ?> node =
                    wrapDataObjects(rootElementId, instanceIdentifier, dataObjects.get(instanceIdentifier));
            dataNodeBuilder.withChild((DataContainerChild<?, ?>) node);
        }
        return Optional.<NormalizedNode<?, ?>>of(dataNodeBuilder.build());
    }

    private NormalizedNode<?, ?> wrapDataObjects(final YangInstanceIdentifier yangInstanceIdentifier,
                                                 final InstanceIdentifier<? extends DataObject> instanceIdentifier,
                                                 final Collection<? extends DataObject> dataObjects) {
        final Collection<NormalizedNode<?, ?>> normalizedRootElements = Collections2
                .transform(dataObjects, toNormalizedNodeFunction(instanceIdentifier));

        final DataSchemaNode schemaNode =
                globalContext.getDataChildByName(yangInstanceIdentifier.getLastPathArgument().getNodeType());
        if (schemaNode instanceof ListSchemaNode) {
            // In case of a list, wrap all the values in a Mixin parent node
            final ListSchemaNode listSchema = (ListSchemaNode) schemaNode;
            return wrapListIntoMixinNode(normalizedRootElements, listSchema);
        } else {
            Preconditions.checkState(dataObjects.size() == 1, "Singleton list was expected");
            return getOnlyElement(normalizedRootElements);
        }
    }

    @VisibleForTesting
    static DataContainerChild<?, ?> wrapListIntoMixinNode(
            final Collection<NormalizedNode<?, ?>> normalizedRootElements, final ListSchemaNode listSchema) {
        if (listSchema.getKeyDefinition().isEmpty()) {
            final CollectionNodeBuilder<UnkeyedListEntryNode, UnkeyedListNode> listBuilder =
                    Builders.unkeyedListBuilder();
            listBuilder.withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(listSchema.getQName()));

            for (NormalizedNode<?, ?> normalizedRootElement : normalizedRootElements) {
                listBuilder.withChild((UnkeyedListEntryNode) normalizedRootElement);
            }
            return listBuilder.build();
        } else {
            final CollectionNodeBuilder<MapEntryNode, ? extends MapNode> listBuilder =
                    listSchema.isUserOrdered()
                            ? Builders.orderedMapBuilder()
                            : Builders.mapBuilder();
            listBuilder.withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(listSchema.getQName()));

            for (NormalizedNode<?, ?> normalizedRootElement : normalizedRootElements) {
                listBuilder.withChild((MapEntryNode) normalizedRootElement);
            }
            return listBuilder.build();
        }
    }

    @SuppressWarnings("unchecked")
    private Function<DataObject, NormalizedNode<?, ?>> toNormalizedNodeFunction(final InstanceIdentifier path) {
        return dataObject -> {
            LOG.trace("OperationalDataTree.toNormalizedNode(), path={}, dataObject={}", path, dataObject);
            final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry =
                    serializer.toNormalizedNode(path, dataObject);

            LOG.trace("OperationalDataTree.toNormalizedNode(), normalizedNodeEntry={}", entry);
            return entry.getValue();
        };
    }

    private static final class ReadContextImpl implements ReadContext {

        private final ModificationCache ctx = new ModificationCache();
        private final MappingContext mappingContext;

        private ReadContextImpl(final MappingContext mappingContext) {
            this.mappingContext = mappingContext;
        }

        @Nonnull
        @Override
        public ModificationCache getModificationCache() {
            return ctx;
        }

        @Nonnull
        @Override
        public MappingContext getMappingContext() {
            return mappingContext;
        }

        @Override
        public void close() {
            // Make sure to clear the storage in case some customizer stored a reference to it to prevent memory leaks
            ctx.close();
        }
    }
}
