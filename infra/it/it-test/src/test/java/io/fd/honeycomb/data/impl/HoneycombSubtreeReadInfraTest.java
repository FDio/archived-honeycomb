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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FluentFuture;
import io.fd.honeycomb.test.model.Ids;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.registry.CompositeReaderRegistryBuilder;
import io.fd.honeycomb.translate.read.ListReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.read.Reader;
import io.fd.honeycomb.translate.read.registry.ReaderRegistry;
import io.fd.honeycomb.translate.util.YangDAG;
import io.fd.honeycomb.translate.util.read.ReflexiveListReaderCustomizer;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.ContainerInList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.ContainerInListBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class HoneycombSubtreeReadInfraTest extends AbstractInfraTest {

    @Mock
    private ReadContext ctx;
    private ReaderRegistry registry;

    private Reader<ContainerWithList, ContainerWithListBuilder> containerWithListReader =
            HoneycombReadInfraTest.mockReader(Ids.CONTAINER_WITH_LIST_ID, this::readSubtree, ContainerWithListBuilder.class);

    private ListReader<ListInContainer, ListInContainerKey, ListInContainerBuilder> listInContainerReader =
            new GenericListReader<>(Ids.LIST_IN_CONTAINER_ID,
                    new ReflexiveListReaderCustomizer<ListInContainer, ListInContainerKey, ListInContainerBuilder>(Ids.LIST_IN_CONTAINER_ID.getTargetType(), ListInContainerBuilder.class,
                            Lists.newArrayList(new ListInContainerKey(1L), new ListInContainerKey(2L))) {
                        @Override
                        public void readCurrentAttributes(final InstanceIdentifier<ListInContainer> id,
                                                          final ListInContainerBuilder builder,
                                                          final ReadContext context) throws ReadFailedException {
                            super.readCurrentAttributes(id, builder, context);
                            builder.withKey(id.firstKeyOf(ListInContainer.class));
                        }
                    });

    private Reader<ContainerInList, ContainerInListBuilder> containerInListReader =
            HoneycombReadInfraTest.mockReader(Ids.CONTAINER_IN_LIST_ID, this::readContainerInList, ContainerInListBuilder.class);

    // TODO HONEYCOMB-178 Test subtree readers especially composite structure where readers are under subtree reader

    @Override
    void postSetup() {
        initReaderRegistry();
    }

    private void initReaderRegistry() {
        registry = new CompositeReaderRegistryBuilder(new YangDAG())
                // Subtree reader handling its child list
                .subtreeAdd(Sets.newHashSet(Ids.LIST_IN_CONTAINER_ID), containerWithListReader)
                // Reflexive
                .add(listInContainerReader)
                .add(containerInListReader)
        .build();
    }

    @Test
    public void testReadAll() throws Exception {
        final ReadableDataTreeDelegator readableDataTreeDelegator =
                new ReadableDataTreeDelegator(serializer, schemaContext, registry, contextBroker);
        final FluentFuture<Optional<NormalizedNode<?, ?>>>
                read = readableDataTreeDelegator.read(YangInstanceIdentifier.EMPTY);

        final Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> readAll =
                toBinding(read.get().get());
        assertThat(readAll.size(), is(1));
        assertEquals(readEntireSubtree(), readAll.get(Ids.CONTAINER_WITH_LIST_ID).stream().findFirst().get());
    }

    private void readSubtree(final InstanceIdentifier<ContainerWithList> id,
                             final ContainerWithListBuilder b,
                             final ReadContext readContext) {
        b.setListInContainer(Lists.newArrayList(1L, 2L).stream()
                .map(l -> new ListInContainerBuilder().setId(l).build())
                .collect(Collectors.toList()));
    }

    private ContainerWithList readEntireSubtree() {
        final ContainerWithListBuilder b = new ContainerWithListBuilder();
        b.setListInContainer(Lists.newArrayList(1L, 2L).stream()
                .map(l -> {
                    final ContainerInListBuilder containerInListBuilder = new ContainerInListBuilder();
                    readContainerInList(
                            Ids.CONTAINER_WITH_LIST_ID.child(ListInContainer.class, new ListInContainerKey(l)).child(ContainerInList.class),
                            containerInListBuilder,
                            ctx);
                    return new ListInContainerBuilder().setId(l).setContainerInList(containerInListBuilder.build()).build();
                })
                .collect(Collectors.toList()));
        return b.build();
    }

    private void readContainerInList(final InstanceIdentifier<ContainerInList> id,
                                     final ContainerInListBuilder b,
                                     final ReadContext readContext) {
        b.setName(id.firstKeyOf(ListInContainer.class).getId().toString());
    }
}
