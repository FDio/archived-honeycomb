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

package io.fd.honeycomb.translate.impl.write.registry;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toMap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import io.fd.honeycomb.translate.TranslationException;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.write.DataObjectUpdate;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.Writer;
import io.fd.honeycomb.translate.write.registry.UpdateFailedException;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flat writer registry, delegating updates to writers in the order writers were submitted.
 */
@ThreadSafe
final class FlatWriterRegistry implements WriterRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(FlatWriterRegistry.class);

    private final Set<InstanceIdentifier<?>> writersOrderReversed;
    private final Set<InstanceIdentifier<?>> writersOrder;
    private final Map<InstanceIdentifier<?>, Writer<?>> writersById;
    private final Set<? extends Writer<?>> writers;

    /**
     * Create flat registry instance.
     *
     * @param writersById immutable, ordered map of writers to use to process updates. Order of the writers has to be one in
     *                which create and update operations should be handled. Deletes will be handled in reversed order.
     *                All deletes are handled before handling all the updates.
     */
    FlatWriterRegistry(@Nonnull final ImmutableMap<InstanceIdentifier<?>, Writer<?>> writersById) {
        this.writersById = writersById;
        this.writersOrderReversed = Sets.newLinkedHashSet(Lists.reverse(Lists.newArrayList(writersById.keySet())));
        this.writersOrder = writersById.keySet();
        this.writers = writersById.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toSet());
    }

    @Override
    public void validateModifications(@Nonnull final DataObjectUpdates updates, @Nonnull final WriteContext ctx)
        throws DataValidationFailedException {
        // Optimization: validation order is not relevant, so do not merge deletes and updates.
        validateModifications(updates.getDeletes(), ctx);
        validateModifications(updates.getUpdates(), ctx);
    }

    private void validateModifications(
        @Nonnull final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates,
        @Nonnull final WriteContext ctx) throws DataValidationFailedException {
        if (updates.isEmpty()) {
            return;
        }
        // Fail early if some handlers are missing.
        checkAllTypesCanBeHandled(updates);

        // Validators do not modify anything, so order of validators is not important.
        // We iterate over writersOrder instead of modifications for consistent handling of subtree writers case.
        for (InstanceIdentifier<?> writerType : writersOrder) {
            final Writer<?> writer = getWriter(writerType);
            for (DataObjectUpdate singleUpdate : getWritersData(updates, writer, writerType, ctx)) {
                writer.validate(singleUpdate.getId(), singleUpdate.getDataBefore(), singleUpdate.getDataAfter(), ctx);
            }
        }
    }

    @Override
    public void processModifications(@Nonnull final DataObjectUpdates updates,
                                     @Nonnull final WriteContext ctx) throws TranslationException {
        if (updates.isEmpty()) {
            return;
        }

        // ordered set of already processed nodes
        final List<DataObjectUpdate> alreadyProcessed = new LinkedList<>();

        // Optimization for single type updates, less consuming for pairing update with responsible writer,etc
        if (updates.containsOnlySingleType()) {
            // First process delete
            singleUpdate(updates.getDeletes(), alreadyProcessed, ctx);

            // Next is update
            singleUpdate(updates.getUpdates(), alreadyProcessed, ctx);
        } else {
            // First process deletes
            bulkUpdate(updates.getDeletes(), alreadyProcessed, ctx, writersOrderReversed);

            // Next are updates
            bulkUpdate(updates.getUpdates(), alreadyProcessed, ctx, writersOrder);
        }

        LOG.debug("Update successful for types: {}", updates.getTypeIntersection());
        LOG.trace("Update successful for: {}", updates);
    }

    @Override
    public boolean writerSupportsUpdate(@Nonnull final InstanceIdentifier<?> type) {
        Writer writer = getWriter(type);

        if (writer == null) {
            writer = getSubtreeWriterResponsible(type);
        }

        return checkNotNull(writer, "Unable to find writer for %s", type).supportsDirectUpdate();
    }

    private void singleUpdate(
            @Nonnull final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates,
            @Nonnull final List<DataObjectUpdate> alreadyProcessed,
            @Nonnull final WriteContext ctx) throws UpdateFailedException {
        if (updates.isEmpty()) {
            return;
        }

        DataObjectUpdate current = null;
        final InstanceIdentifier<?> singleType = updates.keySet().iterator().next();
        LOG.debug("Performing single type update for: {}", singleType);
        Collection<? extends DataObjectUpdate> singleTypeUpdates = updates.get(singleType);
        Writer<?> writer = getWriter(singleType);

        if (writer == null) {
            // This node must be handled by a subtree writer, find it and call it or else fail
            writer = getSubtreeWriterResponsible(singleType);
            checkArgument(writer != null, "Unable to process update. Missing writers for: %s",
                    singleType);
            singleTypeUpdates = getParentDataObjectUpdate(ctx, updates, writer);
        }

        try {
            LOG.trace("Performing single type update with writer: {}", writer);

            for (DataObjectUpdate singleUpdate : singleTypeUpdates) {
                current = singleUpdate;
                writer.processModification(singleUpdate.getId(), singleUpdate.getDataBefore(),
                        singleUpdate.getDataAfter(),
                        ctx);
                alreadyProcessed.add(singleUpdate);
            }
        } catch (Exception e) {
            throw new UpdateFailedException(e, alreadyProcessed, current);
        }
    }

    @Nullable
    private Writer<?> getSubtreeWriterResponsible(final InstanceIdentifier<?> singleType) {
        return writersById.values().stream()
                .filter(w -> w instanceof SubtreeWriter)
                .filter(w -> w.canProcess(singleType))
                .findFirst()
                .orElse(null);
    }

    static Collection<DataObjectUpdate> getParentDataObjectUpdate(final WriteContext ctx,
                                                                   final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates,
                                                                   final Writer<?> writer) {
        // Now read data for subtree reader root, but first keyed ID is needed and that ID can be cut from updates
        return ((SubtreeWriter<?>) writer).getHandledChildTypes().stream()
                .filter(updates::containsKey)
                .map(unkeyedId -> updates.get(unkeyedId))
                .flatMap(doUpdates -> doUpdates.stream())
                .map(DataObjectUpdate::getId)
                .map(id -> getSingleParentDataObjectUpdate(ctx, (Multimap<InstanceIdentifier<?>, DataObjectUpdate>) updates, writer, id))
                // Reduce the list of updates by putting them to a map. If a subtree writer for container gets 2 children updated, it will
                // get only a single update, however if its a registered on a listand 2 different list items get their children updated,
                // both updates should be preserved.
                // Essentially, only group child updates in case the ID from root to writer is identical
                .collect(toMap(update -> RWUtils.cutId(update.getId(), writer.getManagedDataObjectType()), Function.identity(), (u1, u2) -> u1))
                .values();
    }

    private static DataObjectUpdate getSingleParentDataObjectUpdate(WriteContext ctx, Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates, Writer<?> writer, InstanceIdentifier<?> firstAffectedChildId) {
        final InstanceIdentifier<?> parentKeyedId =
                RWUtils.cutId(firstAffectedChildId, writer.getManagedDataObjectType());

        final Optional<? extends DataObject> parentBefore = ctx.readBefore(parentKeyedId);
        final Optional<? extends DataObject> parentAfter = ctx.readAfter(parentKeyedId);

        // Put the parent update data into updates map so that revert can also access the state
        DataObjectUpdate parentUpdate =
                DataObjectUpdate.create(parentKeyedId, parentBefore.orElse(null), parentAfter.orElse(null));
        updates.put(RWUtils.makeIidWildcarded(parentKeyedId), parentUpdate);
        return parentUpdate;
    }

    private void bulkUpdate(
            @Nonnull final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates,
            @Nonnull final List<DataObjectUpdate> alreadyProcessed,
            @Nonnull final WriteContext ctx,
            @Nonnull final Set<InstanceIdentifier<?>> writersOrder) throws UpdateFailedException {
        if (updates.isEmpty()) {
            return;
        }

        // Check that all updates can be handled
        checkAllTypesCanBeHandled(updates);

        LOG.debug("Performing bulk update for: {}", updates.keySet());
        // Iterate over all writers and call update if there are any related updates
        for (InstanceIdentifier<?> writerType : writersOrder) {
            final Writer<?> writer = getWriter(writerType);
            LOG.debug("Performing update for: {}", writerType);
            LOG.trace("Performing update with writer: {}", writer);

            for (DataObjectUpdate singleUpdate : getWritersData(updates, writer, writerType, ctx)) {
                try {
                    writer.processModification(singleUpdate.getId(), singleUpdate.getDataBefore(),
                            singleUpdate.getDataAfter(), ctx);
                } catch (Exception e) {
                    throw new UpdateFailedException(e, alreadyProcessed, singleUpdate);
                }
                alreadyProcessed.add(singleUpdate);
                LOG.trace("Update successful for type: {}", writerType);
                LOG.debug("Update successful for: {}", singleUpdate);
            }
        }
    }

    private Collection<? extends DataObjectUpdate> getWritersData(
        final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates, final Writer<?> writer,
        final InstanceIdentifier<?> writerType, final WriteContext ctx) {
        Collection<? extends DataObjectUpdate> writersData = updates.get(writerType);

        if (writersData.isEmpty()) {
            // If there are no data for current writer, but it is a SubtreeWriter and there are updates to
            // its children, still invoke it with its root data.
            // Notice that child updates will be ignored if the set of all modifications
            // contain both parent update and child updates. But this is ok, since all changes are already expressed in
            // the parent update.
            if (writer instanceof SubtreeWriter<?> && isAffected((SubtreeWriter<?>) writer, updates)) {
                // Provide parent data for SubtreeWriter for further processing
                writersData = getParentDataObjectUpdate(ctx, updates, writer);
            } else {
                // Skipping unaffected writer
                // Alternative to this would be modification sort according to the order of writers
                return Collections.emptyList();
            }
        }
        return writersData;
    }

    private void checkAllTypesCanBeHandled(
            @Nonnull final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates) {

        List<InstanceIdentifier<?>> noWriterNodes = new ArrayList<>();
        for (InstanceIdentifier<?> id : updates.keySet()) {
            // either there is direct writer for the iid
            // or subtree one
            if (writersById.containsKey(id) || writers.stream().anyMatch(o -> o.canProcess(id))) {
                    continue;
            }
            noWriterNodes.add(id);
        }

        if (!noWriterNodes.isEmpty()) {
            throw new IllegalArgumentException("Unable to process update. Missing writers for: " + noWriterNodes);
        }
    }

    /**
     * Check whether {@link SubtreeWriter} is affected by the updates.
     *
     * @return true if there are any updates to SubtreeWriter's child nodes (those marked by SubtreeWriter as being
     * taken care of)
     */
    private static boolean isAffected(final SubtreeWriter<?> writer,
                                      final Multimap<InstanceIdentifier<?>, ? extends DataObjectUpdate> updates) {
        return !Sets.intersection(writer.getHandledChildTypes(), updates.keySet()).isEmpty();
    }

    @Nullable
    private Writer<?> getWriter(@Nonnull final InstanceIdentifier<?> singleType) {
        return writersById.get(singleType);
    }

}
