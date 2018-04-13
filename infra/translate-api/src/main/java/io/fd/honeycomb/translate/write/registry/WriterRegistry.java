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

import com.google.common.annotations.Beta;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import io.fd.honeycomb.translate.TranslationException;
import io.fd.honeycomb.translate.write.DataObjectUpdate;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.Writer;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Special {@link Writer} capable of performing bulk updates.
 */
@Beta
public interface WriterRegistry {
    /**
     * Validates provided DataObject updates.
     *
     * @param updates Updates to be validated
     * @param ctx Write context that provides information about current state of DataTree.
     * @throws DataValidationFailedException if validation failed.
     */
    default void validateModifications(@Nonnull DataObjectUpdates updates, @Nonnull WriteContext ctx) throws
        DataValidationFailedException {
    }

    /**
     * Performs bulk update.
     *
     * @throws TranslationException in case update fails or there was some other problem while processing
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
     * Simple DTO containing updates for {@link WriterRegistry}. Currently only deletes and updates (create + update)
     * are distinguished.
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

        public Set<DataObjectUpdate> getAllModifications() {
            return Stream.concat(updates.values().stream(), deletes.values().stream())
                    .collect(Collectors.toSet());
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
}