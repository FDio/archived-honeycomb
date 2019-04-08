/*
 * Copyright (c) 2016, 2017 Cisco and/or its affiliates.
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
import static io.fd.honeycomb.data.impl.ModifiableDataTreeDelegator.DataTreeWriteContextFactory.DataTreeWriteContext;
import static io.fd.honeycomb.data.impl.ModifiableDataTreeManager.DataTreeContextFactory.DataTreeContext;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.FluentFuture;
import io.fd.honeycomb.data.DataModification;
import io.fd.honeycomb.data.ReadableDataManager;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.TranslationException;
import io.fd.honeycomb.translate.ValidationFailedException;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.TransactionMappingContext;
import io.fd.honeycomb.translate.util.write.TransactionWriteContext;
import io.fd.honeycomb.translate.write.DataObjectUpdate;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.registry.UpdateFailedException;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import io.fd.honeycomb.translate.write.registry.WriterRegistry.DataObjectUpdates;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of {@link ModifiableDataTreeManager} that propagates data changes to underlying writer layer before they
 * are fully committed in the backing data tree. Data changes are propagated in BA format.
 */
public final class ModifiableDataTreeDelegator extends ModifiableDataTreeManager {

    private static final Logger LOG = LoggerFactory.getLogger(ModifiableDataTreeDelegator.class);
    private static final ReadableDataManager EMPTY_OPERATIONAL =
            p -> FluentFutures.immediateFluentFuture(Optional.empty());

    private final WriterRegistry writerRegistry;
    private final DataBroker contextBroker;
    private final BindingNormalizedNodeSerializer serializer;
    private final SchemaContext schema;

    /**
     * Creates configuration data tree instance.
     *  @param serializer     service for serialization between Java Binding Data representation and NormalizedNode
     *                       representation.
     * @param dataTree       data tree for configuration data representation
     * @param writerRegistry service for translation between Java Binding Data and data provider, capable of performing
     * @param contextBroker BA broker providing full access to mapping context data
     */
    public ModifiableDataTreeDelegator(@Nonnull final BindingNormalizedNodeSerializer serializer,
                                       @Nonnull final DataTree dataTree,
                                       @Nonnull final SchemaContext schema,
                                       @Nonnull final WriterRegistry writerRegistry,
                                       @Nonnull final DataBroker contextBroker) {
        super(dataTree, new DataTreeWriteContextFactory());
        this.contextBroker = checkNotNull(contextBroker, "contextBroker should not be null");
        this.serializer = checkNotNull(serializer, "serializer should not be null");
        this.writerRegistry = checkNotNull(writerRegistry, "writerRegistry should not be null");
        this.schema = checkNotNull(schema, "schema should not be null");
    }

    @Override
    public DataModification newModification() {
        return new DelegatingConfigSnapshot(super.newModification());
    }

    private final class DelegatingConfigSnapshot extends ModifiableDataTreeManager.ConfigSnapshot {

        private final DataModification untouchedModification;

        /**
         * @param untouchedModification DataModification captured while this modification/snapshot was created.
         *                              To be used later while invoking writers to provide them with before state
         *                              (state without current modifications).
         *                              It must be captured as close as possible to when current modification started.
         */
        DelegatingConfigSnapshot(final DataModification untouchedModification) {
            this.untouchedModification = untouchedModification;
        }

        @Override
        protected void validateCandidate(final DataTreeContext dataTreeContext) throws ValidationFailedException {
            final DataObjectUpdates baUpdates =
                getUpdates((DataTreeWriteContextFactory.DataTreeWriteContext) dataTreeContext);
            LOG.debug("ModifiableDataTreeDelegator.validateCandidate() extracted updates={}", baUpdates);

            try (final WriteContext ctx = getTransactionWriteContext()) {
                writerRegistry.validateModifications(baUpdates, ctx);
            }
        }

        private DataObjectUpdates getUpdates(final DataTreeWriteContext dataTreeContext) {
            DataObjectUpdates updates = dataTreeContext.getUpdates();
            if (updates != null) {
                return updates;
            } else {
                final DataTreeCandidate candidate = dataTreeContext.getCandidate();
                final DataTreeCandidateNode rootNode = candidate.getRootNode();
                final YangInstanceIdentifier rootPath = candidate.getRootPath();
                LOG.trace("ConfigDataTree.getUpdates() rootPath={}, rootNode={}, dataBefore={}, dataAfter={}",
                    rootPath, rootNode, rootNode.getDataBefore(), rootNode.getDataAfter());

                final ModificationDiff modificationDiff = new ModificationDiff.ModificationDiffBuilder()
                    .setCtx(schema).build(rootNode);
                LOG.debug("ConfigDataTree.getUpdates() diff: {}", modificationDiff);

                // Distinguish between updates (create + update) and deletes
                updates = toBindingAware(writerRegistry, modificationDiff.getUpdates());
                dataTreeContext.setUpdates(updates);
                return updates;
            }
        }

        /**
         * Pass the changes to underlying writer layer. Transform from BI to BA. Revert(Write data before to subtrees
         * that have been successfully modified before failure) in case of failure.
         */
        @Override
        protected void processCandidate(final DataTreeContext dataTreeContext) throws TranslationException {
            final DataTreeWriteContextFactory.DataTreeWriteContext dataTreeWriteContext =
                (DataTreeWriteContextFactory.DataTreeWriteContext) dataTreeContext;

            final DataObjectUpdates baUpdates = dataTreeWriteContext.getUpdates();
            LOG.debug("ModifiableDataTreeDelegator.processCandidate() extracted updates={}", baUpdates);

            final WriteContext ctx = getTransactionWriteContext();
            final MappingContext mappingContext = ctx.getMappingContext();
            try {
                writerRegistry.processModifications(baUpdates, ctx);

                final FluentFuture<? extends CommitInfo> contextUpdateResult =
                        ((TransactionMappingContext) mappingContext).commit();
                // Blocking on context data update
                contextUpdateResult.get();
            } catch (UpdateFailedException e) {
                // TODO - HONEYCOMB-411
                LOG.warn("Failed to apply all changes", e);
                final List<DataObjectUpdate> processed = e.getProcessed();
                if (processed.isEmpty()) {
                    // nothing was processed, which means either very first operation failed, or it was single operation
                    // update. In both cases, no revert is needed
                    LOG.info("Nothing to revert");
                    throw e;
                } else {
                    LOG.info("Trying to revert successful changes for current transaction");
                    // attempt revert with fresh context, to allow write logic used context-binded data
                    try (final TransactionWriteContext txContext = getRevertTransactionContext(mappingContext)) {
                        new Reverter(processed, writerRegistry).revert(txContext);
                        LOG.info("Changes successfully reverted");
                    } catch (Reverter.RevertFailedException revertFailedException) {
                        // fail with failed revert
                        LOG.error("Failed to revert successful(comitted) changes", revertFailedException);
                        throw revertFailedException;
                    }
                }
                // fail with success revert
                // not passing the cause,its logged above and it would be logged after transaction
                // ended again(prevent double logging of same error
                throw new Reverter.RevertSuccessException(getNonProcessedNodes(baUpdates, processed));
            } catch (InterruptedException | ExecutionException ex) {
                // TODO HONEYCOMB-162 revert should probably occur when context is not written successfully
                final String msg = "Error while updating mapping context data";
                LOG.error(msg, ex);
                throw new TranslationException(msg, ex);
            } finally {
                // Using finally instead of try-with-resources in order to leave ctx open for BulkUpdateException catch
                // block. The context is needed there, but try-with-resources closes the resource before handling ex.
                LOG.debug("Closing write context {}", ctx);
                ctx.close();
            }
        }

        /**
         * Creates inverted transaction context for reverting of proceeded changes.
         * Invert before/after transaction and reuse affected mapping context created by previous updates
         * to access all data created by previous updates
         * */
        // Sonar reports unclosed resources, but everything is closed by TransactionWriteContext.close()
        // This is known SonarJava issue
        // https://jira.sonarsource.com/browse/SONARJAVA-1670
        // Fixed in SonarJava 4.0 (requires SonarCube 5.6).
        // TODO(HONEYCOMB-419): remove after LF upgrades SonarCube
        @SuppressWarnings("squid:S2095")
        private TransactionWriteContext getRevertTransactionContext(final MappingContext affectedMappingContext) {
            // Before Tx == after partial update
            final DOMDataTreeReadTransaction beforeTx = ReadOnlyTransaction.create(this, EMPTY_OPERATIONAL);
            // After Tx == before partial update
            final DOMDataTreeReadTransaction afterTx =
                    ReadOnlyTransaction.create(untouchedModification, EMPTY_OPERATIONAL);
            return new TransactionWriteContext(serializer, beforeTx, afterTx, affectedMappingContext);
        }

        // Sonar reports unclosed resources, but everything is closed by TransactionWriteContext.close()
        // TODO(HONEYCOMB-419): remove after LF upgrades SonarCube
        @SuppressWarnings("squid:S2095")
        private TransactionWriteContext getTransactionWriteContext() {
            // Before Tx must use modification
            final DOMDataTreeReadTransaction
                    beforeTx = ReadOnlyTransaction.create(untouchedModification, EMPTY_OPERATIONAL);
            // After Tx must use current modification
            final DOMDataTreeReadTransaction afterTx = ReadOnlyTransaction.create(this, EMPTY_OPERATIONAL);
            final TransactionMappingContext mappingContext = new TransactionMappingContext(
                contextBroker.newReadWriteTransaction());
            return new TransactionWriteContext(serializer, beforeTx, afterTx, mappingContext);
        }

        private WriterRegistry.DataObjectUpdates toBindingAware(final WriterRegistry registry,
                final Map<YangInstanceIdentifier, NormalizedNodeUpdate> biNodes) {
            return ModifiableDataTreeDelegator.toBindingAware(registry, biNodes, serializer);
        }
    }

    private static Set<InstanceIdentifier<?>> getNonProcessedNodes(final WriterRegistry.DataObjectUpdates allUpdates,
                                                                   final List<DataObjectUpdate> alreadyProcessed) {
        return allUpdates.getAllModifications().stream()
                .filter(update -> !alreadyProcessed.contains(update))
                .map(DataObjectUpdate::getId)
                .collect(Collectors.toSet());
    }

    @VisibleForTesting
    static WriterRegistry.DataObjectUpdates toBindingAware(
            final WriterRegistry registry,
            final Map<YangInstanceIdentifier, NormalizedNodeUpdate> biNodes,
            final BindingNormalizedNodeSerializer serializer) {

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> dataObjectUpdates = HashMultimap.create();
        final Multimap<InstanceIdentifier<?>, DataObjectUpdate.DataObjectDelete> dataObjectDeletes =
                HashMultimap.create();

        for (Map.Entry<YangInstanceIdentifier, NormalizedNodeUpdate> biEntry : biNodes.entrySet()) {
            final InstanceIdentifier<?> keyedId = serializer.fromYangInstanceIdentifier(biEntry.getKey());
            final InstanceIdentifier<?> unkeyedIid =
                    RWUtils.makeIidWildcarded(keyedId);

            NormalizedNodeUpdate normalizedNodeUpdate = biEntry.getValue();
            final DataObjectUpdate dataObjectUpdate = toDataObjectUpdate(normalizedNodeUpdate, serializer);
            if (dataObjectUpdate != null) {
                if (dataObjectUpdate instanceof DataObjectUpdate.DataObjectDelete) {
                    // is delete
                    dataObjectDeletes.put(unkeyedIid, (DataObjectUpdate.DataObjectDelete) dataObjectUpdate);
                } else if (dataObjectUpdate.getDataBefore() != null && !registry.writerSupportsUpdate(unkeyedIid)) {
                    // is update and direct update operation is not supported
                    // breaks update to delete + create pair

                    dataObjectDeletes.put(unkeyedIid,
                            (DataObjectUpdate.DataObjectDelete) DataObjectUpdate.DataObjectDelete
                                    .create(keyedId, dataObjectUpdate.getDataBefore(), null));
                    dataObjectUpdates
                            .put(unkeyedIid, DataObjectUpdate.create(keyedId, null, dataObjectUpdate.getDataAfter()));
                } else {
                    // is create
                    dataObjectUpdates.put(unkeyedIid, dataObjectUpdate);
                }
            }
        }
        return new WriterRegistry.DataObjectUpdates(dataObjectUpdates, dataObjectDeletes);
    }

    @Nullable
    private static DataObjectUpdate toDataObjectUpdate(
            final NormalizedNodeUpdate normalizedNodeUpdate,
            final BindingNormalizedNodeSerializer serializer) {

        InstanceIdentifier<?> baId = serializer.fromYangInstanceIdentifier(normalizedNodeUpdate.getId());
        checkNotNull(baId, "Unable to transform instance identifier: %s into BA", normalizedNodeUpdate.getId());

        DataObject dataObjectBefore = getDataObject(serializer,
                normalizedNodeUpdate.getDataBefore(), normalizedNodeUpdate.getId());
        DataObject dataObjectAfter =
                getDataObject(serializer, normalizedNodeUpdate.getDataAfter(), normalizedNodeUpdate.getId());

        return dataObjectBefore == null && dataObjectAfter == null
                ? null
                : DataObjectUpdate.create(baId, dataObjectBefore, dataObjectAfter);
    }

    @Nullable
    private static DataObject getDataObject(@Nonnull final BindingNormalizedNodeSerializer serializer,
                                            @Nullable final NormalizedNode<?, ?> data,
                                            @Nonnull final YangInstanceIdentifier id) {
        DataObject dataObject = null;
        if (data != null) {
            final Map.Entry<InstanceIdentifier<?>, DataObject> dataObjectEntry =
                    serializer.fromNormalizedNode(id, data);
            if (dataObjectEntry != null) {
                dataObject = dataObjectEntry.getValue();
            }
        }
        return dataObject;
    }

    static final class DataTreeWriteContextFactory implements ModifiableDataTreeManager.DataTreeContextFactory {
        @Nonnull
        @Override
        public DataTreeWriteContext create(@Nonnull final DataTreeCandidate candidate) {
            return new DataTreeWriteContext(candidate);
        }

        /**
         * Implementation of {@link DataTreeContext} that, in addition to {@link DataTreeCandidate}, stores also
         * {@DataObjectUpdates}. The {@link DataObjectUpdates} are shared by the validation and translation process.
         */
        static final class DataTreeWriteContext implements DataTreeContext {
            private final DataTreeCandidate candidate;
            private DataObjectUpdates updates;

            DataTreeWriteContext(@Nonnull final DataTreeCandidate candidate) {
                this.candidate = candidate;
            }

            @Nonnull
            @Override
            public DataTreeCandidate getCandidate() {
                return candidate;
            }

            public void setUpdates(@Nonnull final DataObjectUpdates updates) {
                this.updates = updates;
            }

            DataObjectUpdates getUpdates() {
                return updates;
            }
        }
    }
}



