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

package io.fd.honeycomb.translate.impl.read.registry;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import io.fd.honeycomb.translate.read.InitFailedException;
import io.fd.honeycomb.translate.read.InitListReader;
import io.fd.honeycomb.translate.read.InitReader;
import io.fd.honeycomb.translate.read.Initializer;
import io.fd.honeycomb.translate.read.ListReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.read.Reader;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.AbstractGenericReader;
import io.fd.honeycomb.translate.util.read.DelegatingReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CompositeReader<D extends DataObject, B extends Builder<D>>
        extends AbstractGenericReader<D, B>
        implements InitReader<D, B>, DelegatingReader<D, B> {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeReader.class);

    private final Reader<D, B> delegate;
    private final ImmutableMap<Class<?>, Reader<? extends DataObject, ? extends Builder<?>>> childReaders;

    private CompositeReader(final Reader<D, B> reader,
                            final ImmutableMap<Class<?>, Reader<? extends DataObject, ? extends Builder<?>>> childReaders) {
        super(reader.getManagedDataObjectType());
        this.delegate = reader;
        this.childReaders = childReaders;
    }

    @VisibleForTesting
    ImmutableMap<Class<?>, Reader<? extends DataObject, ? extends Builder<?>>> getChildReaders() {
        return childReaders;
    }

    @SuppressWarnings("unchecked")
    public static <D extends DataObject> InstanceIdentifier<D> appendTypeToId(
        final InstanceIdentifier<? extends DataObject> parentId, final InstanceIdentifier<D> type) {
        final InstanceIdentifier.PathArgument t = InstanceIdentifier.Item.of(type.getTargetType());
        return (InstanceIdentifier<D>) InstanceIdentifier.create(Iterables.concat(
            parentId.getPathArguments(), Collections.singleton(t)));
    }

    @Nonnull
    @Override
    public Optional<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                               @Nonnull final ReadContext ctx) throws ReadFailedException {
        if (shouldReadCurrent(id)) {
            LOG.trace("{}: Reading current: {}", this, id);
            return readCurrent((InstanceIdentifier<D>) id, ctx);
        } else if (shouldDelegateToChild(id)) {
            LOG.trace("{}: Reading child: {}", this, id);
            return readSubtree(id, ctx);
        } else {
            // Fallback
            LOG.trace("{}: Delegating read: {}", this, id);
            return delegate.read(id, ctx);
        }
    }

    private boolean shouldReadCurrent(@Nonnull final InstanceIdentifier<? extends DataObject> id) {
        return id.getTargetType().equals(getManagedDataObjectType().getTargetType());
    }

    private boolean shouldDelegateToChild(@Nonnull final InstanceIdentifier<? extends DataObject> id) {
        return childReaders.containsKey(RWUtils.getNextId(id, getManagedDataObjectType()).getType());
    }

    private Optional<? extends DataObject> readSubtree(final InstanceIdentifier<? extends DataObject> id,
                                                       final ReadContext ctx) throws ReadFailedException {
        final InstanceIdentifier.PathArgument nextId = RWUtils.getNextId(id, getManagedDataObjectType());
        final Reader<?, ? extends Builder<?>> nextReader = childReaders.get(nextId.getType());
        checkArgument(nextReader != null, "Unable to read: %s. No delegate present, available readers at next level: %s",
                id, childReaders.keySet());
        return nextReader.read(id, ctx);
    }

    @SuppressWarnings("unchecked")
    private void readChildren(final InstanceIdentifier<D> id, @Nonnull final ReadContext ctx, final B builder)
            throws ReadFailedException {
        LOG.debug("{}: Reading children: {}", this, childReaders.keySet());
        for (Reader child : childReaders.values()) {
            final InstanceIdentifier childId = appendTypeToId(id, child.getManagedDataObjectType());

            LOG.debug("{}: Reading child from: {}", this, child);
            if (child instanceof ListReader) {
                final List<? extends DataObject> list = ((ListReader) child).readList(childId, ctx);
                // Dont set empty lists
                if (!list.isEmpty()) {
                    ((ListReader) child).merge(builder, list);
                }
            } else {
                final Optional<? extends DataObject> read = child.read(childId, ctx);
                if (read.isPresent()) {
                    child.merge(builder, read.get());
                }
            }
        }
    }

    @Override
    public Reader<D, B> getDelegate() {
        return delegate;
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final B builder,
                                      @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        delegate.readCurrentAttributes(id, builder, ctx);
        readChildren(id, ctx, builder);
    }

    /**
     * Wrap a Reader as a Composite Reader.
     */
    static <D extends DataObject, B extends Builder<D>> Reader<D, B> createForReader(
            @Nonnull final Reader<D, B> reader,
            @Nonnull final ImmutableMap<Class<?>, Reader<?, ? extends Builder<?>>> childReaders) {

        return (reader instanceof ListReader)
                ? new CompositeListReader<>((ListReader) reader, childReaders)
                : new CompositeReader<>(reader, childReaders);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init(final DataBroker broker, final InstanceIdentifier<D> id, final ReadContext ctx) throws InitFailedException {
        if (delegate instanceof Initializer) {
            LOG.trace("{}: Initializing current: {}", this, id);
            ((Initializer<D>) delegate).init(broker, id, ctx);
        }

        for (Reader child : childReaders.values()) {
            final InstanceIdentifier childId = appendTypeToId(id, child.getManagedDataObjectType());

            if (child instanceof Initializer) {
                LOG.trace("{}: Initializing child: {}", this, childId);
                ((Initializer) child).init(broker, childId, ctx);
            }
        }
    }

    private static class CompositeListReader<D extends DataObject & Identifiable<K>, B extends Builder<D>, K extends Identifier<D>>
            extends CompositeReader<D, B>
            implements DelegatingListReader<D, K, B>, InitListReader<D, K, B> {

        private final ListReader<D, K, B> delegate;

        private CompositeListReader(final ListReader<D, K, B> reader,
                                    final ImmutableMap<Class<?>, Reader<? extends DataObject, ? extends Builder<?>>> childReaders) {
            super(reader, childReaders);
            this.delegate = reader;
        }

        @Override
        public ListReader<D, K, B> getDelegate() {
            return delegate;
        }

        @Nonnull
        @Override
        public List<D> readList(@Nonnull final InstanceIdentifier<D> id, @Nonnull final ReadContext ctx)
                throws ReadFailedException {
            LOG.trace("{}: Reading all list entries", this);
            final List<K> allIds = delegate.getAllIds(id, ctx);
            LOG.debug("{}: Reading list entries for: {}", this, allIds);

            // Override read list in order to perform readCurrent + readChildren here
            final ArrayList<D> allEntries = new ArrayList<>(allIds.size());
            for (K key : allIds) {
                final InstanceIdentifier.IdentifiableItem<D, K> currentBdItem = RWUtils.getCurrentIdItem(id, key);
                final InstanceIdentifier<D> keyedId = RWUtils.replaceLastInId(id, currentBdItem);
                final Optional<D> read = readCurrent(keyedId, ctx);
                if (read.isPresent()) {
                    final DataObject singleItem = read.get();
                    checkArgument(getManagedDataObjectType().getTargetType().isAssignableFrom(singleItem.getClass()));
                    allEntries.add(getManagedDataObjectType().getTargetType().cast(singleItem));
                }
            }
            return allEntries;
        }

        @Override
        public void init(final DataBroker broker, final InstanceIdentifier<D> id, final ReadContext ctx)
                throws InitFailedException {
            try {
                final List<K> allIds = delegate.getAllIds(id, ctx);
                for (K key : allIds) {
                    super.init(broker, RWUtils.replaceLastInId(id, RWUtils.getCurrentIdItem(id, key)), ctx);
                }
            } catch (ReadFailedException e) {
                throw new InitFailedException(id, e);
            }
        }
    }
}
