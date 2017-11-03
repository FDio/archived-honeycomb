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

package io.fd.honeycomb.translate.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.fd.honeycomb.translate.ModifiableSubtreeManagerRegistryBuilder;
import io.fd.honeycomb.translate.SubtreeManager;
import io.fd.honeycomb.translate.SubtreeManagerRegistryBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class AbstractSubtreeManagerRegistryBuilderBuilder<S extends SubtreeManager<? extends DataObject>, R>
        implements ModifiableSubtreeManagerRegistryBuilder<S>, SubtreeManagerRegistryBuilder<R> {

    private final Map<InstanceIdentifier<?>, S> handlersMap = new HashMap<>();
    private final YangDAG dag;

    protected AbstractSubtreeManagerRegistryBuilderBuilder(@Nonnull final YangDAG yangDAG) {
        this.dag = Preconditions.checkNotNull(yangDAG, "yangDAG should not be null");
    }

    /**
     * Add handler without any special relationship to any other type.
     */
    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> add(@Nonnull final S handler) {
        // Make IID wildcarded just in case
        // + the way InstanceIdentifier.create + equals work for Identifiable items is unexpected, meaning updates would
        // not be matched to writers in registry
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(handler.getManagedDataObjectType());
        checkWriterNotPresentYet(targetType);
        dag.addVertex(targetType);
        handlersMap.put(targetType, handler);
        return this;
    }

    /**
     * Add handler without any special relationship to any other type.
     */
    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> subtreeAdd(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                                                         @Nonnull final S handler) {
        add(getSubtreeHandler(handledChildren, handler));
        return this;
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<S> wildcardedSubtreeAdd(@Nonnull S handler) {
        add(getWildcardedSubtreeHandler(handler));
        return this;
    }

    private void checkWriterNotPresentYet(final InstanceIdentifier<?> targetType) {
        Preconditions.checkArgument(!handlersMap.containsKey(targetType),
                "Writer for type: %s already present: %s", targetType, handlersMap.get(targetType));
    }

    /**
     * Add handler with relationship: to be executed before handler handling relatedType.
     */
    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> addBefore(@Nonnull final S handler,
                                                                        @Nonnull final InstanceIdentifier<?> relatedType) {
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(handler.getManagedDataObjectType());
        final InstanceIdentifier<?> wildcardedRelatedType = RWUtils.makeIidWildcarded(relatedType);
        checkWriterNotPresentYet(targetType);
        dag.addVertex(targetType);
        dag.addVertex(wildcardedRelatedType);
        dag.addEdge(targetType, wildcardedRelatedType);
        handlersMap.put(targetType, handler);
        return this;
    }

    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> addBefore(@Nonnull final S handler,
                                                                        @Nonnull final Collection<InstanceIdentifier<?>> relatedTypes) {
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(handler.getManagedDataObjectType());
        checkWriterNotPresentYet(targetType);
        dag.addVertex(targetType);
        relatedTypes.stream()
                .map(RWUtils::makeIidWildcarded)
                .forEach(dag::addVertex);
        relatedTypes.stream()
                .map(RWUtils::makeIidWildcarded)
                .forEach(type -> dag.addEdge(targetType, type));
        handlersMap.put(targetType, handler);
        return this;
    }

    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> subtreeAddBefore(
            @Nonnull final Set<InstanceIdentifier<?>> handledChildren,
            @Nonnull final S handler,
            @Nonnull final InstanceIdentifier<?> relatedType) {
        return addBefore(getSubtreeHandler(handledChildren, handler), relatedType);
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<S> wildcardedSubtreeAddBefore(@Nonnull final S handler,
                                                                                 @Nonnull final InstanceIdentifier<?> relatedType) {
        return addBefore(getWildcardedSubtreeHandler(handler), relatedType);
    }

    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> subtreeAddBefore(
            @Nonnull final Set<InstanceIdentifier<?>> handledChildren,
            @Nonnull final S handler,
            @Nonnull final Collection<InstanceIdentifier<?>> relatedTypes) {
        return addBefore(getSubtreeHandler(handledChildren, handler), relatedTypes);
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<S> wildcardedSubtreeAddBefore(@Nonnull final S handler,
                                                                                 @Nonnull final Collection<InstanceIdentifier<?>> relatedTypes) {
        return addBefore(getWildcardedSubtreeHandler(handler), relatedTypes);
    }

    protected abstract S getSubtreeHandler(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                           @Nonnull final S handler);

    protected abstract S getWildcardedSubtreeHandler(@Nonnull final S handler);

    /**
     * Add handler with relationship: to be executed after handler handling relatedType.
     */
    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> addAfter(@Nonnull final S handler,
                                                                       @Nonnull final InstanceIdentifier<?> relatedType) {
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(handler.getManagedDataObjectType());
        final InstanceIdentifier<?> wildcardedRelatedType = RWUtils.makeIidWildcarded(relatedType);
        checkWriterNotPresentYet(targetType);
        dag.addVertex(targetType);
        dag.addVertex(wildcardedRelatedType);
        // set edge to indicate before relationship, just reversed
        dag.addEdge(wildcardedRelatedType, targetType);
        handlersMap.put(targetType, handler);
        return this;
    }

    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> addAfter(@Nonnull final S handler,
                                                                       @Nonnull final Collection<InstanceIdentifier<?>> relatedTypes) {
        final InstanceIdentifier<?> targetType = RWUtils.makeIidWildcarded(handler.getManagedDataObjectType());
        checkWriterNotPresentYet(targetType);
        dag.addVertex(targetType);
        relatedTypes.stream()
                .map(RWUtils::makeIidWildcarded)
                .forEach(dag::addVertex);
        // set edge to indicate before relationship, just reversed
        relatedTypes.stream()
                .map(RWUtils::makeIidWildcarded)
                .forEach(type -> dag.addEdge(type, targetType));
        handlersMap.put(targetType, handler);
        return this;
    }

    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> subtreeAddAfter(
            @Nonnull final Set<InstanceIdentifier<?>> handledChildren,
            @Nonnull final S handler,
            @Nonnull final InstanceIdentifier<?> relatedType) {
        return addAfter(getSubtreeHandler(handledChildren, handler), relatedType);
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<S> wildcardedSubtreeAddAfter(@Nonnull final S handler,
                                                                                @Nonnull final InstanceIdentifier<?> relatedType) {
        return addAfter(getWildcardedSubtreeHandler(handler), relatedType);
    }

    @Override
    public AbstractSubtreeManagerRegistryBuilderBuilder<S, R> subtreeAddAfter(
            @Nonnull final Set<InstanceIdentifier<?>> handledChildren,
            @Nonnull final S handler,
            @Nonnull final Collection<InstanceIdentifier<?>> relatedTypes) {
        return addAfter(getSubtreeHandler(handledChildren, handler), relatedTypes);
    }

    @Override
    public ModifiableSubtreeManagerRegistryBuilder<S> wildcardedSubtreeAddAfter(@Nonnull S handler, @Nonnull Collection<InstanceIdentifier<?>> relatedTypes) {
        return addAfter(getWildcardedSubtreeHandler(handler), relatedTypes);
    }

    protected ImmutableMap<InstanceIdentifier<?>, S> getMappedHandlers() {
        final ImmutableMap.Builder<InstanceIdentifier<?>, S> builder = ImmutableMap.builder();
        // Iterate writer types according to their relationships from graph
        dag.iterator().forEachRemaining(handlerType -> {
                    // There might be types stored just for relationship sake, no real writer, ignoring those
                    if (handlersMap.containsKey(handlerType)) {
                        builder.put(handlerType, handlersMap.get(handlerType));
                    }
                });

        // TODO HONEYCOMB-171 we could optimize subtree handlers, if there is a dedicated handler for a node managed by a subtree
        // handler, recreate the subtree handler with a subset of handled child nodes
        // This way it is not necessary to change the configuration of subtree writer, just to add a dedicated child
        // writer

        return builder.build();
    }
}
