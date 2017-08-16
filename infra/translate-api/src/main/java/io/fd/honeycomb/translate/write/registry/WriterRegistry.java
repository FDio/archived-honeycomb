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

package io.fd.honeycomb.translate.write.registry;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import io.fd.honeycomb.translate.TranslationException;
import io.fd.honeycomb.translate.write.DataObjectUpdate;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.Writer;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Special {@link Writer} capable of performing bulk updates.
 */
@Beta
public interface WriterRegistry {

    /**
     * Performs bulk update.
     *
     * @throws BulkUpdateException in case bulk update fails
     * @throws TranslationException in case some other error occurs while processing update request
     */
    void processModifications(@Nonnull DataObjectUpdates updates,
                              @Nonnull WriteContext ctx) throws TranslationException;

    /**
     * Indicates direct support for update operation on provided type
     *
     * @param type data object type
     */
    boolean writerSupportsUpdate(@Nonnull InstanceIdentifier<?> type);

    /**
     * Simple DTO containing updates for {@link WriterRegistry}. Currently only deletes and updates (create +
     * update) are distinguished.
     */
    @Beta
    final class DataObjectUpdates {

        private final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates;
        private final Multimap<InstanceIdentifier<?>, DataObjectUpdate.DataObjectDelete> deletes;

        /**
         * Create new instance.
         *
         * @param updates All updates indexed by their unkeyed {@link InstanceIdentifier}
         * @param deletes All deletes indexed by their unkeyed {@link InstanceIdentifier}
         */
        public DataObjectUpdates(@Nonnull final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates,
                                 @Nonnull final Multimap<InstanceIdentifier<?>, DataObjectUpdate.DataObjectDelete> deletes) {
            this.deletes = deletes;
            this.updates = updates;
        }

        public Multimap<InstanceIdentifier<?>, DataObjectUpdate> getUpdates() {
            return updates;
        }

        public Multimap<InstanceIdentifier<?>, DataObjectUpdate.DataObjectDelete> getDeletes() {
            return deletes;
        }

        public boolean isEmpty() {
            return updates.isEmpty() && deletes.isEmpty();
        }

        @Override
        public String toString() {
            return "DataObjectUpdates{" + "updates=" + updates + ", deletes=" + deletes + '}';
        }

        /**
         * Get a {@link Set} containing all update types from both updates as well as deletes.
         */
        public Set<InstanceIdentifier<?>> getTypeIntersection() {
            return Sets.union(deletes.keySet(), updates.keySet());
        }

        /**
         * Check whether there is only a single type of data object to be updated.
         *
         * @return true if there is only a single type of updates (update + delete)
         */
        public boolean containsOnlySingleType() {
            return getTypeIntersection().size() == 1;
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            final DataObjectUpdates that = (DataObjectUpdates) other;

            if (!updates.equals(that.updates)) {
                return false;
            }
            return deletes.equals(that.deletes);

        }

        @Override
        public int hashCode() {
            int result = updates.hashCode();
            result = 31 * result + deletes.hashCode();
            return result;
        }

    }

    /**
     * Thrown when bulk update failed.
     */
    @Beta
    class BulkUpdateException extends TranslationException {

        private final transient Reverter reverter;
        private final InstanceIdentifier<?> failedSubtree;
        private final DataObjectUpdate failedData;
        private final Set<InstanceIdentifier<?>> unrevertedSubtrees;

        /**
         * Constructs an BulkUpdateException.
         * @param unhandledSubtrees instance identifiers of the data objects that were not processed during bulk update.
         * @param cause the cause of bulk update failure
         */
        public BulkUpdateException(@Nonnull final InstanceIdentifier<?> failedSubtree,
                                   @Nonnull final DataObjectUpdate failedData,
                                   @Nonnull final Set<InstanceIdentifier<?>> unhandledSubtrees,
                                   @Nonnull final Reverter reverter,
                                   @Nonnull final Throwable cause) {
            super("Bulk update failed at: " + failedSubtree + " ignored updates: " + unhandledSubtrees, cause);
            this.failedSubtree = failedSubtree;
            this.failedData = failedData;
            this.unrevertedSubtrees = unhandledSubtrees;
            this.reverter = checkNotNull(reverter, "reverter should not be null");
        }

        /**
         * Reverts changes that were successfully applied during bulk update before failure occurred.
         *
         * @param writeContext Non-closed {@code WriteContext} to be used by reverting logic.<br> <b>Do not use same
         *                     write context as was used in previous write</b>
         * @throws Reverter.RevertFailedException if revert fails
         */
        public void revertChanges(@Nonnull final WriteContext writeContext) throws Reverter.RevertFailedException {
            reverter.revert(writeContext);
        }

        public Set<InstanceIdentifier<?>> getUnrevertedSubtrees() {
            return unrevertedSubtrees;
        }

        public InstanceIdentifier<?> getFailedSubtree() {
            return failedSubtree;
        }

        public DataObjectUpdate getFailedData() {
            return failedData;
        }
    }

    /**
     * Abstraction over revert mechanism in case of a bulk update failure.
     */
    @Beta
    interface Reverter {

        /**
         * Reverts changes that were successfully applied during bulk update before failure occurred. Changes are
         * reverted in reverse order they were applied.
         * Used {@code WriteContext} needs to be in non-closed state, creating fresh one for revert
         * is recommended, same way as for write, to allow {@code Reverter} use same logic as write.
         *
         * @param writeContext Non-closed {@code WriteContext} to be used by reverting logic
         * @throws RevertFailedException if not all of applied changes were successfully reverted
         */
        void revert(@Nonnull final WriteContext writeContext) throws RevertFailedException;

        /**
         * Thrown when some of the changes applied during bulk update were not reverted.
         */
        @Beta
        class RevertFailedException extends TranslationException {

            /**
             * Constructs a RevertFailedException with the list of changes that were not reverted.
             *
             * @param cause              the cause of revert failure
             */
            public RevertFailedException(@Nonnull final BulkUpdateException cause) {
                super("Unable to revert changes after failure. Revert failed for "
                        + cause.getFailedSubtree() + " unreverted subtrees: " + cause.getUnrevertedSubtrees(), cause);
            }

            /**
             * Returns the list of changes that were not reverted.
             *
             * @return list of changes that were not reverted
             */
            @Nonnull
            public Set<InstanceIdentifier<?>> getNotRevertedChanges() {
                return ((BulkUpdateException) getCause()).getUnrevertedSubtrees();
            }

            /**
             * Returns the update that caused the failure.
             *
             * @return update that caused the failure
             */
            @Nonnull
            public DataObjectUpdate getFailedUpdate() {
                return ((BulkUpdateException) getCause()).getFailedData();
            }
        }

        /**
         * Thrown after bulk operation was successfully reverted,
         * to maintain marking of transaction as failed,without double logging of
         * cause of update fail(its logged before reverting in ModifiableDataTreeDelegator
         */
        @Beta
        class RevertSuccessException extends TranslationException {
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
}