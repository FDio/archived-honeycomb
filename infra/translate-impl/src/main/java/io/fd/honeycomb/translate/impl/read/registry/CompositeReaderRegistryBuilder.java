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

import com.google.common.collect.ImmutableMap;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.InitReader;
import io.fd.honeycomb.translate.read.Initializer;
import io.fd.honeycomb.translate.read.Reader;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.read.registry.ReaderRegistry;
import io.fd.honeycomb.translate.read.registry.ReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.AbstractSubtreeManagerRegistryBuilderBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

@NotThreadSafe
public final class CompositeReaderRegistryBuilder
        extends AbstractSubtreeManagerRegistryBuilderBuilder<Reader<? extends DataObject, ? extends Builder<?>>, ReaderRegistry>
        implements ModifiableReaderRegistryBuilder, ReaderRegistryBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeReaderRegistryBuilder.class);

    @Override
    protected Reader<? extends DataObject, ? extends Builder<?>> getSubtreeHandler(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                                                                   @Nonnull final Reader<? extends DataObject, ? extends Builder<?>> reader) {
        return reader instanceof Initializer
                ? InitSubtreeReader.createForReader(handledChildren, (InitReader<?, ?>) reader)
                : SubtreeReader.createForReader(handledChildren, reader);
    }

    @Override
    public <D extends DataObject> void addStructuralReader(@Nonnull InstanceIdentifier<D> id,
                                                           @Nonnull Class<? extends Builder<D>> builderType) {
        // prevent case to submit structural reader for list, which would cause fail because target setter consumes
        // List<TargetType>, not Target type. If node is static list, addStructuralListReader should be used
        checkArgument(!Identifiable.class.isAssignableFrom(id.getTargetType()), "Structural readers cannot be used for keyed nodes[node type %s], use addStructuralListReader()", id.getTargetType());
        add(GenericReader.createReflexive(id, builderType));
    }

    @Override
    public <D extends DataObject & Identifiable<K>, K extends Identifier<D>> void addStructuralListReader(@Nonnull InstanceIdentifier<D> id, @Nonnull Class<? extends Builder<D>> builderType, @Nonnull List<K> staticKeys) {
        checkArgument(Identifiable.class.isAssignableFrom(id.getTargetType()), "Node %s is not keyed, use addStructuralReader()", id.getTargetType());
        add(GenericListReader.createReflexive(id, builderType, staticKeys));
    }

    /**
     * Create {@link CompositeReaderRegistry} with Readers ordered according to submitted relationships.
     * <p/>
     * Note: The ordering only applies between nodes on the same level, inter-level and inter-subtree relationships are
     * ignored.
     */
    @Override
    public ReaderRegistry build() {
        ImmutableMap<InstanceIdentifier<?>, Reader<? extends DataObject, ? extends Builder<?>>> mappedReaders =
                getMappedHandlers();
        LOG.debug("Building Reader registry with Readers: {}",
                mappedReaders.keySet().stream()
                        .map(InstanceIdentifier::getTargetType)
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", ")));

        LOG.trace("Building Reader registry with Readers: {}", mappedReaders);
        final List<InstanceIdentifier<?>> readerOrder = new ArrayList<>(mappedReaders.keySet());

        // Wrap readers into composite readers recursively, collect roots and create registry
        final TypeHierarchy typeHierarchy = TypeHierarchy.create(mappedReaders.keySet());
        final List<Reader<? extends DataObject, ? extends Builder<?>>> orderedRootReaders =
                typeHierarchy.getRoots().stream()
                        .map(rootId -> toCompositeReader(rootId, mappedReaders, typeHierarchy))
                        .collect(Collectors.toList());

        // We are violating the ordering from mappedReaders, since we are forming a composite structure
        // but at least order root writers
        orderedRootReaders.sort((reader1, reader2) -> readerOrder.indexOf(reader1.getManagedDataObjectType())
                - readerOrder.indexOf(reader2.getManagedDataObjectType()));

        return new CompositeReaderRegistry(orderedRootReaders);
    }

    private Reader<? extends DataObject, ? extends Builder<?>> toCompositeReader(
            final InstanceIdentifier<?> instanceIdentifier,
            final ImmutableMap<InstanceIdentifier<?>, Reader<? extends DataObject, ? extends Builder<?>>> mappedReaders,
            final TypeHierarchy typeHierarchy) {

        // Order child readers according to the mappedReadersCollection
        final ImmutableMap.Builder<Class<?>, Reader<?, ? extends Builder<?>>> childReadersMapB = ImmutableMap.builder();
        for (InstanceIdentifier<?> childId : mappedReaders.keySet()) {
            if (typeHierarchy.getDirectChildren(instanceIdentifier).contains(childId)) {
                childReadersMapB.put(childId.getTargetType(), toCompositeReader(childId, mappedReaders, typeHierarchy));
            }
        }

        final ImmutableMap<Class<?>, Reader<?, ? extends Builder<?>>> childReadersMap = childReadersMapB.build();
        return childReadersMap.isEmpty()
                ? mappedReaders.get(instanceIdentifier)
                : CompositeReader.createForReader(mappedReaders.get(instanceIdentifier), childReadersMap);
    }
}
