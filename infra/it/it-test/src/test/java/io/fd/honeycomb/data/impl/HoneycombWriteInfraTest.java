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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.fd.honeycomb.data.DataModification;
import io.fd.honeycomb.test.model.Ids;
import io.fd.honeycomb.translate.impl.write.registry.FlatWriterRegistryBuilder;
import io.fd.honeycomb.translate.util.YangDAG;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.translate.write.Writer;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.Test;
import org.mockito.InOrder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ComplexAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ComplexAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithChoiceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.choice.Choice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.choice.choice.c3.C3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.choice.choice.c3.C3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.ContainerInList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.ContainerInListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.container.in.list.NestedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.container.in.list.NestedListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.container.in.list.NestedListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.simple.container.ComplexAugmentContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.simple.container.ComplexAugmentContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.some.attributes.ContainerFromGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.some.attributes.ContainerFromGroupingBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

/**
 * Testing honeycomb writes from data tree up to mocked writers.
 */
public class HoneycombWriteInfraTest extends AbstractInfraTest {

    private DataTree dataTree;
    private WriterRegistry writerRegistry;

    Writer<SimpleContainer> simpleContainerWriter = mockWriter(Ids.SIMPLE_CONTAINER_ID);
    Writer<ComplexAugment> complexAugmentWriter = mockWriter(Ids.COMPLEX_AUGMENT_ID);
    Writer<ComplexAugmentContainer> complexAugmentContainerWriter = mockWriter(Ids.COMPLEX_AUGMENT_CONTAINER_ID);
    Writer<SimpleAugment> simpleAugmentWriter = mockWriter(Ids.SIMPLE_AUGMENT_ID);

    Writer<ContainerWithList> containerWithListWriter = mockWriter(Ids.CONTAINER_WITH_LIST_ID);
    Writer<ListInContainer> listInContainerWriter = mockWriter(Ids.LIST_IN_CONTAINER_ID);
    Writer<ContainerInList> containerInListWriter = mockWriter(Ids.CONTAINER_IN_LIST_ID);
    Writer<NestedList> nestedListWriter = mockWriter(Ids.NESTED_LIST_ID);

    Writer<ContainerWithChoice> containerWithChoiceWriter = mockWriter(Ids.CONTAINER_WITH_CHOICE_ID);
    Writer<ContainerFromGrouping> containerFromGroupingWriter = mockWriter(Ids.CONTAINER_FROM_GROUPING_ID);
    Writer<C3> c3Writer = mockWriter(Ids.C3_ID);


    private static <D extends DataObject> Writer<D> mockWriter(final InstanceIdentifier<D> id) {
        final Writer<D> mock = (Writer<D>) mock(Writer.class);
        when(mock.getManagedDataObjectType()).thenReturn(id);
        //TODO - HONEYCOMB-412 - to call default impl of canProcess()
        when(mock.canProcess(any())).thenAnswer(invocationOnMock -> {
            final Writer writer = Writer.class.cast(invocationOnMock.getMock());
            final Writer delegatingWriter = new Writer() {
                @Nonnull
                @Override
                public InstanceIdentifier getManagedDataObjectType() {
                    return writer.getManagedDataObjectType();
                }

                @Override
                public boolean supportsDirectUpdate() {
                    return writer.supportsDirectUpdate();
                }

                @Override
                public void processModification(@Nonnull final InstanceIdentifier id,
                                                @Nullable final DataObject dataBefore,
                                                @Nullable final DataObject dataAfter, @Nonnull final WriteContext ctx)
                        throws WriteFailedException {
                    writer.processModification(id, dataBefore, dataAfter, ctx);
                }
            };
            return delegatingWriter.canProcess(InstanceIdentifier.class.cast(invocationOnMock.getArguments()[0]));
        });
        return mock;
    }

    @Override
    void postSetup() {
        initDataTree();
        initWriterRegistry();
    }

    private void initDataTree() {
        dataTree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_CONFIGURATION);
        dataTree.setSchemaContext(schemaContext);
    }

    private void initWriterRegistry() {
        writerRegistry = new FlatWriterRegistryBuilder(new YangDAG())
                .add(complexAugmentWriter) // unordered
                .add(nestedListWriter) // 6
                .addAfter(listInContainerWriter, Ids.NESTED_LIST_ID) // 7
                .addAfter(containerInListWriter, Ids.LIST_IN_CONTAINER_ID) // 8
                .addAfter(containerWithListWriter, Ids.CONTAINER_IN_LIST_ID) // 9
                .addBefore(containerFromGroupingWriter, Ids.NESTED_LIST_ID) // 5
                .addBefore(containerWithChoiceWriter, Ids.CONTAINER_FROM_GROUPING_ID) // 4
                .addBefore(simpleContainerWriter, Ids.CONTAINER_WITH_CHOICE_ID) // 3
                .addBefore(c3Writer, Ids.SIMPLE_CONTAINER_ID) // 2
                .addBefore(simpleAugmentWriter, Ids.SIMPLE_CONTAINER_ID) // 2
                .addBefore(complexAugmentContainerWriter, Sets.newHashSet(Ids.C3_ID, Ids.SIMPLE_AUGMENT_ID)) // 1
                .build();
    }

    @Test
    public void testWriteEmptyNonPresenceContainer() throws Exception {
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
                new ModifiableDataTreeDelegator(serializer, dataTree, schemaContext, writerRegistry, contextBroker);

        final DataModification dataModification = modifiableDataTreeDelegator.newModification();
        final SimpleContainer data = new SimpleContainerBuilder()
                .build();

        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
                serializer.toNormalizedNode(Ids.SIMPLE_CONTAINER_ID, data);
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());

        dataModification.commit();

        verify(simpleContainerWriter).getManagedDataObjectType();
        verifyNoMoreInteractions(simpleContainerWriter);
    }

    @Test
    public void testWriteEverything() throws Exception {
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
                new ModifiableDataTreeDelegator(serializer, dataTree, schemaContext, writerRegistry, contextBroker);

        final DataModification dataModification = modifiableDataTreeDelegator.newModification();
        // Now write everything we can
        writeSimpleContainer(dataModification);
        writeContainerWithChoice(dataModification);
        writeContainerWithList(dataModification);
        dataModification.commit();

        final Writer<?>[] orderedWriters = getOrderedWriters();
        final InOrder inOrder = inOrder(orderedWriters);
        verifyOrderedWrites(orderedWriters, inOrder);
    }

    private void verifyOrderedWrites(final Writer<?>[] orderedWriters, final InOrder inOrder)
            throws WriteFailedException {
        // Modifications are not produced for nodes that do not contain any actual leaves (except when choice is a child)
        // Unordered
        // verify(complexAugmentWriter).update(eq(COMPLEX_AUGMENT_ID), eq(null), eq(getComplexAugment()), any(WriteContext.class));
        // 1
        inOrder.verify(complexAugmentContainerWriter)
                .processModification(eq(Ids.COMPLEX_AUGMENT_CONTAINER_ID), eq(null), eq(getComplexAugmentContainer()), any(WriteContext.class));
        // 2
        inOrder.verify(c3Writer)
                .processModification(eq(Ids.C3_ID), eq(null), eq(getC3()), any(WriteContext.class));
        // 2
        verify(simpleAugmentWriter)
                .processModification(eq(Ids.SIMPLE_AUGMENT_ID), eq(null), eq(getSimpleAugment()), any(WriteContext.class));
        // 3
        inOrder.verify(simpleContainerWriter)
                .processModification(eq(Ids.SIMPLE_CONTAINER_ID), eq(null), eq(getSimpleContainer()), any(WriteContext.class));
        // 4
        inOrder.verify(containerWithChoiceWriter)
                .processModification(eq(Ids.CONTAINER_WITH_CHOICE_ID), eq(null), eq(getContainerWithChoiceWithComplexCase()), any(WriteContext.class));
        // 5
        inOrder.verify(containerFromGroupingWriter)
                .processModification(eq(Ids.CONTAINER_FROM_GROUPING_ID), eq(null), eq(getContainerFromGrouping()), any(WriteContext.class));

        final KeyedInstanceIdentifier<ListInContainer, ListInContainerKey> keyedListInContainer1 =
                Ids.CONTAINER_WITH_LIST_ID.child(ListInContainer.class, new ListInContainerKey((long) 1));
        final KeyedInstanceIdentifier<NestedList, NestedListKey> keyedNestedList1 =
                keyedListInContainer1.child(ContainerInList.class).child(NestedList.class, new NestedListKey("1"));
        final KeyedInstanceIdentifier<ListInContainer, ListInContainerKey> keyedListInContainer2 =
                Ids.CONTAINER_WITH_LIST_ID.child(ListInContainer.class, new ListInContainerKey((long) 2));
        final KeyedInstanceIdentifier<NestedList, NestedListKey> keyedNestedList2 =
                keyedListInContainer2.child(ContainerInList.class).child(NestedList.class, new NestedListKey("2"));

        // 6 - two items
        inOrder.verify(nestedListWriter)
                .processModification(eq(keyedNestedList1), eq(null), eq(getSingleNestedList("1")), any(WriteContext.class));
        verify(nestedListWriter)
                .processModification(eq(keyedNestedList2), eq(null), eq(getSingleNestedList("2")), any(WriteContext.class));

        // 7 - two items
        inOrder.verify(listInContainerWriter)
                .processModification(eq(keyedListInContainer1), eq(null), eq(getSingleListInContainer((long)1)), any(WriteContext.class));
        verify(listInContainerWriter)
                .processModification(eq(keyedListInContainer2), eq(null), eq(getSingleListInContainer((long)2)), any(WriteContext.class));

        // 8
        inOrder.verify(containerInListWriter)
                .processModification(eq(keyedListInContainer1.child(ContainerInList.class)), eq(null), eq(getContainerInList("1")), any(WriteContext.class));
        verify(containerInListWriter)
                .processModification(eq(keyedListInContainer2.child(ContainerInList.class)), eq(null), eq(getContainerInList("2")), any(WriteContext.class));

        // 9 - Ignored because the container has no leaves, only complex child nodes
        // inOrder.verify(containerWithListWriter)
        // .update(eq(CONTAINER_WITH_LIST_ID), eq(null), eq(getContainerWithList()), any(WriteContext.class));

        for (Writer<?> orderedWriter : orderedWriters) {
            verify(orderedWriter).getManagedDataObjectType();
            //TODO - HONEYCOMB-412
            //verifyNoMoreInteractions(orderedWriter);
        }

        verify(complexAugmentContainerWriter, times(1)).processModification(any(), any(), any(), any());
        verify(c3Writer, times(1)).processModification(any(), any(), any(), any());
        verify(simpleAugmentWriter, times(1)).processModification(any(), any(), any(), any());
        verify(simpleContainerWriter, times(1)).processModification(any(), any(), any(), any());
        verify(containerWithChoiceWriter, times(1)).processModification(any(), any(), any(), any());
        verify(containerFromGroupingWriter, times(1)).processModification(any(), any(), any(), any());
        verify(nestedListWriter, times(2)).processModification(any(), any(), any(), any());
        verify(listInContainerWriter, times(2)).processModification(any(), any(), any(), any());
        verify(containerInListWriter, times(2)).processModification(any(), any(), any(), any());
    }

    private Writer<?>[] getOrderedWriters() {
        return new Writer<?>[]{complexAugmentWriter, // Unordered
                    complexAugmentContainerWriter, // 1
                    c3Writer, // 2
                    simpleAugmentWriter, // 2
                    simpleContainerWriter, // 3
                    containerWithChoiceWriter, // 4
                    containerFromGroupingWriter, // 5
                    nestedListWriter, // 6
                    listInContainerWriter, // 7
                    containerInListWriter, // 8
                    containerWithListWriter};
    }

    @Test
    public void testDeletes() throws Exception {
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
                new ModifiableDataTreeDelegator(serializer, dataTree, schemaContext, writerRegistry, contextBroker);

        DataModification dataModification = modifiableDataTreeDelegator.newModification();
        // Now write everything we can
        writeSimpleContainer(dataModification);
        writeContainerWithChoice(dataModification);
        writeContainerWithList(dataModification);
        dataModification.commit();
        // Verify writes to be able to verifyNoMore interactions at the end
        verifyOrderedWrites(getOrderedWriters(), inOrder(getOrderedWriters()));

        dataModification = modifiableDataTreeDelegator.newModification();
        deleteSimpleContainer(dataModification);
        deleteContainerWithChoice(dataModification);
        deleteContainerWithList(dataModification);
        dataModification.commit();

        final Writer<?>[] orderedWriters = getOrderedWriters();
        Collections.reverse(Arrays.asList(orderedWriters));
        final InOrder inOrder = inOrder(orderedWriters);

        final KeyedInstanceIdentifier<ListInContainer, ListInContainerKey> keyedListInContainer1 =
                Ids.CONTAINER_WITH_LIST_ID.child(ListInContainer.class, new ListInContainerKey((long) 1));
        final KeyedInstanceIdentifier<NestedList, NestedListKey> keyedNestedList1 =
                keyedListInContainer1.child(ContainerInList.class).child(NestedList.class, new NestedListKey("1"));
        final KeyedInstanceIdentifier<ListInContainer, ListInContainerKey> keyedListInContainer2 =
                Ids.CONTAINER_WITH_LIST_ID.child(ListInContainer.class, new ListInContainerKey((long) 2));
        final KeyedInstanceIdentifier<NestedList, NestedListKey> keyedNestedList2 =
                keyedListInContainer2.child(ContainerInList.class).child(NestedList.class, new NestedListKey("2"));

        // Deletes are handled in reverse order
        // 1
        inOrder.verify(containerInListWriter)
                .processModification(eq(keyedListInContainer1.child(ContainerInList.class)), eq(getContainerInList("1")), eq(null), any(WriteContext.class));
        verify(containerInListWriter)
                .processModification(eq(keyedListInContainer2.child(ContainerInList.class)), eq(getContainerInList("2")), eq(null), any(WriteContext.class));

        // 2
        inOrder.verify(listInContainerWriter)
                .processModification(eq(keyedListInContainer1), eq(getSingleListInContainer((long)1)), eq(null),  any(WriteContext.class));
        verify(listInContainerWriter)
                .processModification(eq(keyedListInContainer2), eq(getSingleListInContainer((long)2)), eq(null), any(WriteContext.class));

        // 3
        inOrder.verify(nestedListWriter)
                .processModification(eq(keyedNestedList1), eq(getSingleNestedList("1")), eq(null), any(WriteContext.class));
        verify(nestedListWriter)
                .processModification(eq(keyedNestedList2), eq(getSingleNestedList("2")), eq(null), any(WriteContext.class));
        // 4
        inOrder.verify(containerFromGroupingWriter)
                .processModification(eq(Ids.CONTAINER_FROM_GROUPING_ID), eq(getContainerFromGrouping()), eq(null), any(WriteContext.class));
        // 5
        inOrder.verify(containerWithChoiceWriter)
                .processModification(eq(Ids.CONTAINER_WITH_CHOICE_ID), eq(getContainerWithChoiceWithComplexCase()), eq(null), any(WriteContext.class));
        // 6
        inOrder.verify(simpleContainerWriter)
                .processModification(eq(Ids.SIMPLE_CONTAINER_ID), eq(getSimpleContainer()), eq(null), any(WriteContext.class));
        // 7
        verify(simpleAugmentWriter)
                .processModification(eq(Ids.SIMPLE_AUGMENT_ID), eq(getSimpleAugment()), eq(null), any(WriteContext.class));
        // 8
        inOrder.verify(c3Writer)
                .processModification(eq(Ids.C3_ID), eq(getC3()), eq(null), any(WriteContext.class));
        // 9
        inOrder.verify(complexAugmentContainerWriter)
                .processModification(eq(Ids.COMPLEX_AUGMENT_CONTAINER_ID), eq(getComplexAugmentContainer()), eq(null), any(WriteContext.class));

        for (Writer<?> orderedWriter : orderedWriters) {
            verify(orderedWriter).getManagedDataObjectType();
            //TODO - HONEYCOMB-412
            // verifyNoMoreInteractions(orderedWriter);
        }

        verify(complexAugmentContainerWriter, times(2)).processModification(any(), any(), any(), any());
        verify(c3Writer, times(2)).processModification(any(), any(), any(), any());
        verify(simpleAugmentWriter, times(2)).processModification(any(), any(), any(), any());
        verify(simpleContainerWriter, times(2)).processModification(any(), any(), any(), any());
        verify(containerWithChoiceWriter, times(2)).processModification(any(), any(), any(), any());
        verify(containerFromGroupingWriter, times(2)).processModification(any(), any(), any(), any());
        verify(nestedListWriter, times(4)).processModification(any(), any(), any(), any());
        verify(listInContainerWriter, times(4)).processModification(any(), any(), any(), any());
        verify(containerInListWriter, times(4)).processModification(any(), any(), any(), any());
    }

    private void writeContainerWithList(final DataModification dataModification) {
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
                serializer.toNormalizedNode(Ids.CONTAINER_WITH_LIST_ID, getContainerWithList());
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());
    }

    private void deleteContainerWithList(final DataModification dataModification) {
        dataModification.delete(serializer.toYangInstanceIdentifier(Ids.CONTAINER_WITH_LIST_ID));
    }

    private ContainerWithList getContainerWithList() {
        return new ContainerWithListBuilder()
                .setListInContainer(getListInContainer((long)1, (long)2))
                .build();
    }

    private List<ListInContainer> getListInContainer(final Long... keys) {
        final ArrayList<ListInContainer> objects = Lists.newArrayList();
        for (Long key : keys) {
            objects.add(getSingleListInContainer(key));
        }
        return objects;
    }

    private ListInContainer getSingleListInContainer(final Long key) {
        return new ListInContainerBuilder()
                .setId(key)
                .setContainerInList(getContainerInList(Long.toString(key)))
                .build();
    }

    private ContainerInList getContainerInList(String... nestedKeys) {
        return new ContainerInListBuilder()
                .setName("inlist")
                .setNestedList(getNestedList(nestedKeys))
                .build();
    }

    private List<NestedList> getNestedList(String... keys) {
        final ArrayList<NestedList> nestedList = new ArrayList<>();
        for (String key : keys) {
            nestedList.add(getSingleNestedList(key));
        }
        return nestedList;
    }

    private NestedList getSingleNestedList(final String key) {
        return new NestedListBuilder()
                .setNestedId(key)
                .setNestedName(key)
                .build();
    }

    private void writeContainerWithChoice(final DataModification dataModification) {
        writeContainerWithChoice(dataModification, getContainerWithChoiceWithComplexCase());
    }


    private void deleteContainerWithChoice(final DataModification dataModification) {
        dataModification.delete(serializer.toYangInstanceIdentifier(Ids.CONTAINER_WITH_CHOICE_ID));
    }

    private void writeContainerWithChoice(final DataModification dataModification,
                                          final ContainerWithChoice containerWithChoice) {
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
                serializer.toNormalizedNode(Ids.CONTAINER_WITH_CHOICE_ID, containerWithChoice);
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());
    }

    private ContainerWithChoice getContainerWithChoiceWithComplexCase() {
        return new ContainerWithChoiceBuilder()
                .setLeafFromGrouping("fromG")
                .setName("name")
                .setContainerFromGrouping(getContainerFromGrouping())
                .setChoice(getComplexCase())
                .build();
    }

    private Choice getComplexCase() {
        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.choice.choice.C3Builder()
                .setC3(getC3())
                .build();
    }

    private C3 getC3() {
        return new C3Builder()
                .setName("c3")
                .build();
    }

    private void writeContainerFromGrouping(final DataModification dataModification) {
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
                serializer.toNormalizedNode(Ids.CONTAINER_FROM_GROUPING_ID, getContainerFromGrouping());
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());
    }

    private ContainerFromGrouping getContainerFromGrouping() {
        return new ContainerFromGroupingBuilder()
                .setLeafInContainerFromGrouping(111)
                .build();
    }


    private void writeSimpleContainer(final DataModification dataModification) {
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
                serializer.toNormalizedNode(Ids.SIMPLE_CONTAINER_ID, getSimpleContainer());
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());
    }

    private void deleteSimpleContainer(final DataModification dataModification) {
        final YangInstanceIdentifier yangId =
                serializer.toYangInstanceIdentifier(Ids.SIMPLE_CONTAINER_ID);
        dataModification.delete(yangId);
    }

    private SimpleContainer getSimpleContainer() {
        return new SimpleContainerBuilder()
                    .setSimpleContainerName("n")
                    .addAugmentation(SimpleAugment.class, getSimpleAugment())
                    .addAugmentation(ComplexAugment.class, getComplexAugment())
                    .build();
    }

    private ComplexAugment getComplexAugment() {
        return new ComplexAugmentBuilder()
                .setComplexAugmentContainer(getComplexAugmentContainer())
                .build();
    }

    private ComplexAugmentContainer getComplexAugmentContainer() {
        return new ComplexAugmentContainerBuilder().setSomeLeaf("s").build();
    }

    private SimpleAugment getSimpleAugment() {
        return new SimpleAugmentBuilder().setSimpleAugmentLeaf("a").build();
    }

    @Test
    public void testWriteAndDeleteInTx() throws Exception {
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
                new ModifiableDataTreeDelegator(serializer, dataTree, schemaContext, writerRegistry, contextBroker);

        final DataModification dataModification = modifiableDataTreeDelegator.newModification();
        // Now write everything we can
        writeSimpleContainer(dataModification);
        deleteSimpleContainer(dataModification);
        dataModification.commit();

        verify(simpleContainerWriter).getManagedDataObjectType();
        // No modification
        verifyNoMoreInteractions(simpleContainerWriter);
    }
}