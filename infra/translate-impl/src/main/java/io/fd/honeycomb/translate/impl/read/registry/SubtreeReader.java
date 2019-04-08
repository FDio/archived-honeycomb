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

import com.google.common.collect.Iterables;
import io.fd.honeycomb.translate.read.ListReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.read.Reader;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.ReflectionUtils;
import io.fd.honeycomb.translate.util.read.DelegatingReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Reader delegate for subtree Readers (Readers handling also children nodes) providing a list of all the
 * children nodes being handled.
 */
class SubtreeReader<D extends DataObject, B extends Builder<D>> implements DelegatingReader<D, B> {

    private static final Logger LOG = LoggerFactory.getLogger(SubtreeReader.class);

    protected final Reader<D, B> delegate;
    private final Set<InstanceIdentifier<?>> handledChildTypes = new HashSet<>();

    SubtreeReader(final Reader<D, B> delegate, Set<InstanceIdentifier<?>> handledTypes) {
        this.delegate = delegate;
        for (InstanceIdentifier<?> handledType : handledTypes) {
            // Iid has to start with Reader's handled root type
            checkArgument(delegate.getManagedDataObjectType().getTargetType().equals(
                    handledType.getPathArguments().iterator().next().getType()),
                    "Handled node from subtree has to be identified by an instance identifier starting from: %s."
                            + "Instance identifier was: %s", getManagedDataObjectType().getTargetType(), handledType);
            checkArgument(Iterables.size(handledType.getPathArguments()) > 1,
                    "Handled node from subtree identifier too short: %s", handledType);
            handledChildTypes.add(InstanceIdentifier.create(Iterables.concat(
                    getManagedDataObjectType().getPathArguments(), Iterables.skip(handledType.getPathArguments(), 1))));
        }
    }

    /**
     * Return set of types also handled by this Reader. All of the types are children of the type managed by this Reader
     * excluding the type of this Reader.
     */
    Set<InstanceIdentifier<?>> getHandledChildTypes() {
        return handledChildTypes;
    }

    @Override
    @Nonnull
    public Optional<? extends DataObject> read(
            @Nonnull final InstanceIdentifier<? extends DataObject> id,
            @Nonnull final ReadContext ctx) throws ReadFailedException {
        final InstanceIdentifier<?> wildcarded = RWUtils.makeIidWildcarded(id);

        // Reading entire subtree and filtering if is current reader responsible
        if (getHandledChildTypes().contains(wildcarded)) {
            LOG.debug("{}: Subtree node managed by this writer requested: {}. Reading current and filtering", this, id);
            // If there's no dedicated reader, use read current
            final InstanceIdentifier<D> currentId = RWUtils.cutId(id, getManagedDataObjectType());
            final Optional<? extends DataObject> current = delegate.read(currentId, ctx);
            // then perform post-reading filtering (return only requested sub-node)
            final Optional<? extends DataObject> readSubtree = current.isPresent()
                ? filterSubtree(current.get(), id, getManagedDataObjectType().getTargetType())
                : current;

            LOG.debug("{}: Subtree: {} read successfully. Result: {}", this, id, readSubtree);
            return readSubtree;

        // If child that's handled here is not requested, then delegate should be able to handle the read
        } else {
            return delegate.read(id, ctx);
        }
    }

    @Override
    public Reader<D, B> getDelegate() {
        return delegate;
    }

    @Nonnull
    private static Optional<? extends DataObject> filterSubtree(@Nonnull final DataObject parent,
                                                                @Nonnull final InstanceIdentifier<? extends DataObject> absolutPath,
                                                                @Nonnull final Class<?> managedType) {
        final InstanceIdentifier.PathArgument nextId =
                RWUtils.getNextId(absolutPath, InstanceIdentifier.create(parent.getClass()));

        final Optional<? extends DataObject> nextParent = findNextParent(parent, nextId, managedType);

        if (Iterables.getLast(absolutPath.getPathArguments()).equals(nextId)) {
            return nextParent; // we found the dataObject identified by absolutePath
        } else if (nextParent.isPresent()) {
            return filterSubtree(nextParent.get(), absolutPath, nextId.getType());
        } else {
            return nextParent; // we can't go further, return Optional.absent()
        }
    }

    private static Optional<? extends DataObject> findNextParent(@Nonnull final DataObject parent,
                                                                 @Nonnull final InstanceIdentifier.PathArgument nextId,
                                                                 @Nonnull final Class<?> managedType) {
        Optional<Method> method = ReflectionUtils.findMethodReflex(managedType, "get",
                Collections.emptyList(), nextId.getType());

        if (method.isPresent()) {
            return Optional.ofNullable(filterSingle(parent, nextId, method.get()));
        } else {
            // List child nodes
            method = ReflectionUtils.findMethodReflex(managedType,
                    "get" + nextId.getType().getSimpleName(), Collections.emptyList(), List.class);

            if (method.isPresent()) {
                return filterList(parent, nextId, method.get());
            } else {
                throw new IllegalStateException(
                        "Unable to filter " + nextId + " from " + parent + " getters not found using reflexion");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<? extends DataObject> filterList(final DataObject parent,
                                                             final InstanceIdentifier.PathArgument nextId,
                                                             final Method method) {
        final List<? extends DataObject> invoke = (List<? extends DataObject>) invoke(method, nextId, parent);

        checkArgument(nextId instanceof InstanceIdentifier.IdentifiableItem<?, ?>,
                "Unable to perform wildcarded read for %s", nextId);
        final Identifier key = ((InstanceIdentifier.IdentifiableItem) nextId).getKey();

        final Method keyGetter = ReflectionUtils.findMethodReflex(nextId.getType(), "get",
                        Collections.emptyList(), key.getClass()).get();

        return Optional.ofNullable(invoke.stream()
                .filter(item -> key.equals(invoke(keyGetter, nextId, item)))
                .findFirst().orElse(null));
    }

    private static DataObject filterSingle(final DataObject parent,
                                           final InstanceIdentifier.PathArgument nextId, final Method method) {
        return nextId.getType().cast(invoke(method, nextId, parent));
    }

    private static Object invoke(final Method method,
                                 final InstanceIdentifier.PathArgument nextId, final DataObject parent) {
        try {
            return method.invoke(parent);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to get " + nextId + " from " + parent, e);
        }
    }

    @Override
    @Nonnull
    public InstanceIdentifier<D> getManagedDataObjectType() {
        return delegate.getManagedDataObjectType();
    }

    /**
     * Wrap a Reader as a subtree Reader.
     */
    static <D extends DataObject, B extends Builder<D>> Reader<D, B> createForReader(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                                                                     @Nonnull final Reader<D, B> reader) {
        return (reader instanceof ListReader)
                ? new SubtreeListReader<>((ListReader) reader, handledChildren)
                : new SubtreeReader<>(reader, handledChildren);
    }

    static class SubtreeListReader<D extends DataObject & Identifiable<K>, B extends Builder<D>, K extends Identifier<D>>
            extends SubtreeReader<D, B> implements DelegatingListReader<D, K, B>, ListReader<D, K, B> {

        final ListReader<D, K, B> delegate;

        SubtreeListReader(final ListReader<D, K, B> delegate,
                                  final Set<InstanceIdentifier<?>> handledTypes) {
            super(delegate, handledTypes);
            this.delegate = delegate;
        }

        @Override
        public ListReader<D, K, B> getDelegate() {
            return delegate;
        }
    }

}
