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

package io.fd.honeycomb.data.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.fd.honeycomb.translate.util.RWUtils.makeIidWildcarded;

import com.google.common.annotations.Beta;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.translate.TranslationException;
import io.fd.honeycomb.translate.write.DataObjectUpdate;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.registry.UpdateFailedException;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes list of changes, creates revert operations and writes them using writer registry
 */
final class Reverter {

    private static final Logger LOG = LoggerFactory.getLogger(Reverter.class);

    private final List<DataObjectUpdate> toBeReverted;
    private final WriterRegistry writerRegistry;

    /**
     * @param toBeReverted   - list of changes in order they were processed, that should be reverted. Reverting order
     *                       and data inside operations will be done by this reverter.
     * @param writerRegistry - registry able to handle all nodes that should be reverted
     */
    Reverter(final List<DataObjectUpdate> toBeReverted,
             final WriterRegistry writerRegistry) {
        this.toBeReverted = toBeReverted;
        this.writerRegistry = writerRegistry;
    }

    /**
     * Reverts changes that were successfully applied during update before failure occurred. Changes are reverted in
     * reverse order they were applied. Used {@code WriteContext} needs to be in non-closed state, creating fresh one
     * for revert is recommended, same way as for write, to allow {@code Reverter} use same logic as write.
     *
     * @param writeContext Non-closed {@code WriteContext} to be used by reverting logic
     * @throws RevertFailedException if not all of applied changes were successfully reverted
     */
    void revert(@Nonnull final WriteContext writeContext) throws RevertFailedException {
        checkNotNull(writeContext, "Cannot revert changes for null context");

        // create list of changes in revert order, and than switch data inside these chagnes to create opposite operations
        final WriterRegistry.DataObjectUpdates revertedAndMapped = revertAndMapProcessed(revertOrder(toBeReverted));

        LOG.info("Attempting revert for changes: {}", revertedAndMapped);
        try {
            // Perform reversed bulk update without revert attempt
            writerRegistry.processModifications(revertedAndMapped, writeContext);
            LOG.info("Revert successful");
        } catch (UpdateFailedException e) {
            // some of revert operations failed
            // throws exception with all revert operations that failed
            LOG.error("Revert failed", e);
            final Set<DataObjectUpdate> nonReverted = revertedAndMapped.getAllModifications();
            nonReverted.removeAll(e.getProcessed());
            throw new RevertFailedException(e.getFailed(), nonReverted, e);
        } catch (Exception e) {
            // any other unexpected error
            LOG.error("Revert failed with unexpected error");
            throw new RevertFailedException(e);
        }
    }

    /**
     * Switching before and after data for each update.
     */
    private WriterRegistry.DataObjectUpdates revertAndMapProcessed(final List<DataObjectUpdate> updates) {
        // uses linked maps to preserve order of insertion
        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updatesMap = LinkedHashMultimap.create();
        final Multimap<InstanceIdentifier<?>, DataObjectUpdate.DataObjectDelete> deleteMap =
                LinkedHashMultimap.create();

        updates.stream()
                .map(DataObjectUpdate::reverse)
                .forEach(reversed -> {
                    // putting under unkeyed identifier, to prevent failing of checkAllTypesCanBeHandled
                    final InstanceIdentifier<?> wildcardedIid = makeIidWildcarded(reversed.getId());
                    if (reversed.getDataAfter() == null) {
                        deleteMap.put(wildcardedIid, DataObjectUpdate.DataObjectDelete.class.cast(reversed));
                    } else {
                        updatesMap.put(wildcardedIid, reversed);
                    }
                });
        return new WriterRegistry.DataObjectUpdates(updatesMap, deleteMap);
    }

    private List<DataObjectUpdate> revertOrder(final List<DataObjectUpdate> processingOrdered) {
        final List<DataObjectUpdate> copy = new ArrayList<>(processingOrdered);
        Collections.reverse(copy);
        return copy;
    }

    /**
     * Thrown when some of the changes applied during update were not reverted.
     */
    @Beta
    static class RevertFailedException extends TranslationException {

        /**
         * Constructs a RevertFailedException with the list of changes that were not reverted.
         *
         * @param cause      the cause of revert failure
         * @param failed     node that failed to revert
         * @param unreverted unreverted changes
         */
        RevertFailedException(@Nonnull final DataObjectUpdate failed,
                              @Nonnull final Set<DataObjectUpdate> unreverted,
                              @Nonnull final Exception cause) {
            super("Unable to revert changes after failure. Revert failed for "
                    + failed + " unreverted subtrees: " + unreverted, cause);
        }

        RevertFailedException(@Nonnull final Exception cause) {
            super("Unexpected error while reverting", cause);
        }
    }

    /**
     * Thrown after bulk operation was successfully reverted, to maintain marking of transaction as failed,without
     * double logging of cause of update fail(its logged before reverting in ModifiableDataTreeDelegator
     */
    @Beta
    static class RevertSuccessException extends TranslationException {
        private final Set<InstanceIdentifier<?>> failedIds;

        /**
         * Constructs an RevertSuccessException.
         *
         * @param failedIds instance identifiers of the data objects that were not processed during bulk update.
         */
        public RevertSuccessException(@Nonnull final Set<InstanceIdentifier<?>> failedIds) {
            super("Bulk update failed for: " + failedIds);
            this.failedIds = failedIds;
        }

        public Set<InstanceIdentifier<?>> getFailedIds() {
            return failedIds;
        }
    }
}
