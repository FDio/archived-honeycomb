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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.data.DataModification;
import io.fd.honeycomb.translate.impl.write.registry.FlatWriterRegistryBuilder;
import io.fd.honeycomb.translate.util.YangDAG;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.Writer;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.AugTarget;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.AugTargetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.FromAugment2Augment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.FromAugment2AugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.FromAugmentAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.FromAugmentAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.FromAugmentListAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.FromAugmentListAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.ListAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.ListAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.ListFromAugmentAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.ListFromAugmentAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.SimpleAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.SimpleAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.SimpleNestedAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.SimpleNestedAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.aug.target.FromAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.aug.target.FromAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.aug.target.ListFromAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.aug.target.ListFromAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.aug.target.ListFromAugmentKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.aug.target.from.augment.FromAugment2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.aug.target.from.augment.FromAugment2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.aug.target.from.augment.FromAugmentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.aug.target.from.augment.FromAugmentEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.aug.target.from.augment.FromAugmentEntryKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

/**
 * Testing write for model with nested augmentation.
 * See <a href="https://jira.fd.io/browse/HONEYCOMB-302">HONEYCOMB-302</a>} for more details.
 */
public class NestedAugmentationWriteTest extends AbstractInfraTest {

    private static final InstanceIdentifier<AugTarget> AUG_TARGET_ID = InstanceIdentifier.create(AugTarget.class);
    private static final InstanceIdentifier<FromAugmentAugment> FROM_AUGMENT_AUGMENT_ID =
        AUG_TARGET_ID.augmentation(FromAugmentAugment.class);
    private static final InstanceIdentifier<FromAugment> FROM_AUGMENT_ID =
        FROM_AUGMENT_AUGMENT_ID.child(FromAugment.class);
    private static final InstanceIdentifier<ListAugment> LIST_AUGMENT_ID =
        AUG_TARGET_ID.augmentation(ListAugment.class);
    private static final InstanceIdentifier<ListFromAugment> LIST_FROM_AUGMENT_ID =
        LIST_AUGMENT_ID.child(ListFromAugment.class);
    private static final InstanceIdentifier<ListFromAugmentAugment> LIST_FROM_AUGMENT_AUGMENT_ID =
        LIST_FROM_AUGMENT_ID.augmentation(ListFromAugmentAugment.class);
    private static final InstanceIdentifier<SimpleAugment> SIMPLE_AUGMENT_ID =
        AUG_TARGET_ID.augmentation(SimpleAugment.class);
    private static final InstanceIdentifier<FromAugment2Augment> FROM_AUGMENT2_AUGMENT_ID =
        FROM_AUGMENT_ID.augmentation(FromAugment2Augment.class);
    private static final InstanceIdentifier<FromAugment2> FROM_AUGMENT2_ID =
        FROM_AUGMENT2_AUGMENT_ID.child(FromAugment2.class);
    private static final InstanceIdentifier<SimpleNestedAugment> SIMPLE_NESTED_AUGMENT_ID =
        FROM_AUGMENT_ID.augmentation(SimpleNestedAugment.class);
    private static final InstanceIdentifier<FromAugmentListAugment> FROM_AUGMENT_LIST_AUGMENT_ID =
        FROM_AUGMENT_ID.augmentation(FromAugmentListAugment.class);
    private static final InstanceIdentifier<FromAugmentEntry> FROM_AUGMENT_ENTRY_ID =
        FROM_AUGMENT_LIST_AUGMENT_ID.child(FromAugmentEntry.class);

    private TipProducingDataTree dataTree;
    private WriterRegistry writerRegistry;

    private final Writer<AugTarget> augTargetWriter = mockWriter(AUG_TARGET_ID);
    private final Writer<FromAugment> fromAugmentWriter = mockWriter(FROM_AUGMENT_ID);
    private final Writer<ListFromAugment> listFromAugmentWriter = mockWriter(LIST_FROM_AUGMENT_ID);
    private final Writer<ListFromAugmentAugment> listFromAugmentAugmentWriter =
        mockWriter(LIST_FROM_AUGMENT_AUGMENT_ID);
    private final Writer<FromAugment2> fromAugment2Writer = mockWriter(FROM_AUGMENT2_ID);
    private final Writer<FromAugmentEntry> fromAugmentListWriter = mockWriter(FROM_AUGMENT_ENTRY_ID);

    private final Writer<SimpleAugment> simpleAugmentWriter = mockWriter(SIMPLE_AUGMENT_ID);
    private final Writer<SimpleNestedAugment> simpleNestedAugmentWriter = mockWriter(SIMPLE_NESTED_AUGMENT_ID);

    private static <D extends DataObject> Writer<D> mockWriter(final InstanceIdentifier<D> id) {
        final Writer<D> mock = (Writer<D>) mock(Writer.class);
        when(mock.getManagedDataObjectType()).thenReturn(id);
        return mock;
    }

    @Override
    void postSetup() {
        initDataTree();
        initWriterRegistry();
    }

    private void initDataTree() {
        dataTree = InMemoryDataTreeFactory.getInstance().create(TreeType.CONFIGURATION);
        dataTree.setSchemaContext(schemaContext);
    }

    private void initWriterRegistry() {
        writerRegistry = new FlatWriterRegistryBuilder(new YangDAG())
            .add(augTargetWriter)
            .add(fromAugmentWriter)
            .add(listFromAugmentWriter)
            .add(listFromAugmentAugmentWriter)
            .add(simpleAugmentWriter)
            .add(fromAugment2Writer)
            .add(simpleNestedAugmentWriter)
            .add(fromAugmentListWriter)
            .build();
    }

    @Test
    public void testSimpleAugmentationWrite() throws Exception {
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
            new ModifiableDataTreeDelegator(serializer, dataTree, schemaContext, writerRegistry, contextBroker);

        final DataModification dataModification = modifiableDataTreeDelegator.newModification();
        final AugTarget data = new AugTargetBuilder()
            .setSomeLeaf("aug-target-leaf-val")
            .addAugmentation(SimpleAugment.class, simpleAugment())
            .build();

        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
            serializer.toNormalizedNode(AUG_TARGET_ID, data);
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());

        dataModification.commit();

        verify(simpleAugmentWriter).update(eq(SIMPLE_AUGMENT_ID), eq(null), eq(simpleAugment()), any(WriteContext.class));
    }

    @Test
    public void testSimpleNestedAugmentationWrite() throws Exception {
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
            new ModifiableDataTreeDelegator(serializer, dataTree, schemaContext, writerRegistry, contextBroker);

        final DataModification dataModification = modifiableDataTreeDelegator.newModification();

        final SimpleNestedAugment augData = simpleNestedAugment();
        final AugTarget data = augTarget(fromAugmentSimple(augData));

        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
            serializer.toNormalizedNode(AUG_TARGET_ID, data);
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());

        dataModification.commit();

        verify(augTargetWriter).update(eq(AUG_TARGET_ID), eq(null), eq(data), any(WriteContext.class));
        verify(fromAugmentWriter).update(eq(FROM_AUGMENT_ID), eq(null), eq(fromAugmentSimple(augData)), any(WriteContext.class));
        verify(simpleNestedAugmentWriter).update(eq(SIMPLE_NESTED_AUGMENT_ID), eq(null), eq(augData), any(WriteContext.class));
    }

    private SimpleAugment simpleAugment() {
        return new SimpleAugmentBuilder().setSimpleAugmentLeaf("val").build();
    }

    @Test
    public void testNestedAugmentationWrite() throws Exception {
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
            new ModifiableDataTreeDelegator(serializer, dataTree, schemaContext, writerRegistry, contextBroker);

        final DataModification dataModification = modifiableDataTreeDelegator.newModification();
        final AugTarget data = augTarget(fromAugment(FromAugment2Augment.class, fromAugment2Augment(fromAugment2())));

        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
            serializer.toNormalizedNode(AUG_TARGET_ID, data);
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());

        dataModification.commit();

        verify(augTargetWriter).update(eq(AUG_TARGET_ID), eq(null), eq(data), any(WriteContext.class));
        verify(fromAugmentWriter).update(eq(FROM_AUGMENT_ID), eq(null), eq(fromAugment()), any(WriteContext.class));
        verify(fromAugment2Writer).update(eq(FROM_AUGMENT2_ID), eq(null), eq(fromAugment2()), any(WriteContext.class));
    }

    @Test
    public void testNestedAugmentationListWrite() throws Exception {
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
            new ModifiableDataTreeDelegator(serializer, dataTree, schemaContext, writerRegistry, contextBroker);

        final DataModification dataModification = modifiableDataTreeDelegator.newModification();
        final List<FromAugmentEntry> entries = Arrays.asList(
            new FromAugmentEntryBuilder().setSomeLeaf("1").build(),
            new FromAugmentEntryBuilder().setSomeLeaf("2").build()
        );
        final FromAugment fromAugment = fromAugment(FromAugmentListAugment.class, fromAugmentList(entries));
        final AugTarget data = augTarget(fromAugment);

        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
            serializer.toNormalizedNode(AUG_TARGET_ID, data);
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());
        dataModification.commit();

        final ArgumentCaptor<DataObject> doCaptor = ArgumentCaptor.forClass(DataObject.class);
        verify(augTargetWriter).update(eq(AUG_TARGET_ID), eq(null), doCaptor.capture(), any(WriteContext.class));
        assertEquals(data.getSomeLeaf(), ((AugTarget)doCaptor.getValue()).getSomeLeaf());

        verify(fromAugmentWriter).update(eq(FROM_AUGMENT_ID), eq(null), doCaptor.capture(), any(WriteContext.class));
        assertEquals(fromAugment.getSomeLeaf(), ((FromAugment)doCaptor.getValue()).getSomeLeaf());


        final KeyedInstanceIdentifier<FromAugmentEntry, FromAugmentEntryKey> keyedNestedList1 =
            FROM_AUGMENT_LIST_AUGMENT_ID.child(FromAugmentEntry.class, new FromAugmentEntryKey("1"));
        final KeyedInstanceIdentifier<FromAugmentEntry, FromAugmentEntryKey> keyedNestedList2 =
            FROM_AUGMENT_LIST_AUGMENT_ID.child(FromAugmentEntry.class, new FromAugmentEntryKey("2"));

        verify(fromAugmentListWriter)
            .update(eq(keyedNestedList1), eq(null), eq(entries.get(0)), any(WriteContext.class));
        verify(fromAugmentListWriter)
            .update(eq(keyedNestedList2), eq(null), eq(entries.get(1)), any(WriteContext.class));
    }

    @Test
    public void testListNestedAugmentationWrite() throws Exception {
        // tests augmenting list that already comes from augment
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
            new ModifiableDataTreeDelegator(serializer, dataTree, schemaContext, writerRegistry, contextBroker);

        final DataModification dataModification = modifiableDataTreeDelegator.newModification();

        final ListFromAugmentAugment listAugmentation = new ListFromAugmentAugmentBuilder()
            .setNewLeaf("new-leaf-val").build();
        final List<ListFromAugment> list = Collections.singletonList(
            new ListFromAugmentBuilder()
                .setSomeLeaf("some-leaf-val")
                .addAugmentation(ListFromAugmentAugment.class, listAugmentation).build());
        final AugTarget data = new AugTargetBuilder()
            .setSomeLeaf("aug-target-leaf-val")
            .addAugmentation(ListAugment.class, new ListAugmentBuilder().setListFromAugment(list).build()).build();

        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
            serializer.toNormalizedNode(AUG_TARGET_ID, data);
        dataModification.write(normalizedNode.getKey(), normalizedNode.getValue());
        dataModification.commit();

        // verify aug target update:
        verify(augTargetWriter).update(eq(AUG_TARGET_ID), eq(null), eq(data), any(WriteContext.class));

        // verify list customizer update:
        final KeyedInstanceIdentifier<ListFromAugment, ListFromAugmentKey> keyedNestedList =
            LIST_AUGMENT_ID.child(ListFromAugment.class, new ListFromAugmentKey("some-leaf-val"));
        final ArgumentCaptor<DataObject> doCaptor = ArgumentCaptor.forClass(DataObject.class);
        verify(listFromAugmentWriter)
            .update(eq(keyedNestedList), eq(null), doCaptor.capture(), any(WriteContext.class));
        assertEquals(list.get(0).getSomeLeaf(), ((ListFromAugment) doCaptor.getValue()).getSomeLeaf());

        // verify list augmentation customizer update:
        verify(listFromAugmentAugmentWriter)
            .update(eq(keyedNestedList.augmentation(ListFromAugmentAugment.class)), eq(null), doCaptor.capture(),
                any(WriteContext.class));
        assertEquals(listAugmentation.getNewLeaf(), ((ListFromAugmentAugment) doCaptor.getValue()).getNewLeaf());
    }

    private AugTarget augTarget(FromAugment fromAugment) {
        return new AugTargetBuilder()
            .setSomeLeaf("aug-target-leaf-val")
            .addAugmentation(FromAugmentAugment.class,
                new FromAugmentAugmentBuilder().setFromAugment(fromAugment).build())
            .build();
    }

    private FromAugment fromAugment() {
        return new FromAugmentBuilder()
            .setSomeLeaf("from-augment-leaf-val")
            .addAugmentation(FromAugment2Augment.class, new FromAugment2AugmentBuilder()
                .setFromAugment2(fromAugment2()).build())
            .build();
    }

    private FromAugment fromAugmentSimple(SimpleNestedAugment simpleNestedAugment) {
        return new FromAugmentBuilder()
            .setSomeLeaf("from-augment-leaf-val")
            .addAugmentation(SimpleNestedAugment.class, simpleNestedAugment)
            .build();
    }

    private SimpleNestedAugment simpleNestedAugment() {
        return new SimpleNestedAugmentBuilder()
            .setSimpleNestedAugmentLeaf("simple-nested-augment-leaf-val").build();
    }

    private FromAugment fromAugment(final Class<? extends Augmentation<FromAugment>> augmentationClass,
                                    final Augmentation<FromAugment> augmentation) {
        return new FromAugmentBuilder()
            .setSomeLeaf("from-augment-leaf-val")
            .addAugmentation(augmentationClass, augmentation)
            .build();
    }

    private FromAugment2Augment fromAugment2Augment(FromAugment2 fromAugment2) {
        return new FromAugment2AugmentBuilder().setFromAugment2(fromAugment2).build();
    }

    private FromAugment2 fromAugment2() {
        return new FromAugment2Builder()
            .setNewLeaf("new-leaf-val")
            .build();
    }

    private FromAugmentListAugment fromAugmentList(final List<FromAugmentEntry> entries) {
        return new FromAugmentListAugmentBuilder()
            .setFromAugmentEntry(entries)
            .build();
    }
}
