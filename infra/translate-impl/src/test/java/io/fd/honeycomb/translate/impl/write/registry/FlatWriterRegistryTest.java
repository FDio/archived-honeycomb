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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.translate.util.DataObjects;
import io.fd.honeycomb.translate.util.DataObjects.DataObject1;
import io.fd.honeycomb.translate.util.DataObjects.DataObject2;
import io.fd.honeycomb.translate.write.DataObjectUpdate;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.translate.write.Writer;
import io.fd.honeycomb.translate.write.registry.UpdateFailedException;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FlatWriterRegistryTest {

    @Mock
    private Writer<DataObject1> writer1;
    @Mock
    private Writer<DataObject2> writer2;
    @Mock
    private Writer<DataObjects.DataObject3> writer3;
    @Mock
    private Writer<DataObjects.DataObject1ChildK> writer4;
    @Mock
    private WriteContext ctx;
    @Mock
    private WriteContext revertWriteContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(writer1.getManagedDataObjectType()).thenReturn(DataObject1.IID);
        when(writer2.getManagedDataObjectType()).thenReturn(DataObject2.IID);
        when(writer3.getManagedDataObjectType()).thenReturn(DataObjects.DataObject3.IID);
        when(writer4.getManagedDataObjectType()).thenReturn(DataObjects.DataObject1ChildK.IID);
        // TODO - HONEYCOMB-412 - thenCallRealMethod doest work with default methods
        // https://stackoverflow.com/questions/27663252/can-you-make-mockito-1-10-17-work-with-default-methods-in-interfaces
        when(writer1.canProcess(any())).thenAnswer(answerWithImpl());
        when(writer2.canProcess(any())).thenAnswer(answerWithImpl());
        when(writer3.canProcess(any())).thenAnswer(answerWithImpl());
        when(writer4.canProcess(any())).thenAnswer(answerWithImpl());
    }

    private static Answer<Object> answerWithImpl() {
        return invocationOnMock -> new CheckedMockWriter(Writer.class.cast(invocationOnMock.getMock())).canProcess(
                InstanceIdentifier.class.cast(invocationOnMock.getArguments()[0]));
    }

    @Test
    public void testMultipleUpdatesForSingleWriter() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        final InstanceIdentifier<DataObject1> iid = InstanceIdentifier.create(DataObject1.class);
        final InstanceIdentifier<DataObject1> iid2 = InstanceIdentifier.create(DataObject1.class);
        final DataObject1 dataObject = mock(DataObject1.class);
        updates.put(DataObject1.IID, DataObjectUpdate.create(iid, dataObject, dataObject));
        updates.put(DataObject1.IID, DataObjectUpdate.create(iid2, dataObject, dataObject));
        flatWriterRegistry.processModifications(new WriterRegistry.DataObjectUpdates(updates, ImmutableMultimap.of()), ctx);

        verify(writer1).processModification(iid, dataObject, dataObject, ctx);
        verify(writer1).processModification(iid2, dataObject, dataObject, ctx);
        // Invoked when registry is being created
        verifyNoMoreInteractions(writer1);
        verifyZeroInteractions(writer2);
    }

    @Test
    public void testMultipleUpdatesForMultipleWriters() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        final InstanceIdentifier<DataObject1> iid = InstanceIdentifier.create(DataObject1.class);
        final DataObject1 dataObject = mock(DataObject1.class);
        updates.put(DataObject1.IID, DataObjectUpdate.create(iid, dataObject, dataObject));
        final InstanceIdentifier<DataObject2> iid2 = InstanceIdentifier.create(DataObject2.class);
        final DataObject2 dataObject2 = mock(DataObject2.class);
        updates.put(DataObject2.IID, DataObjectUpdate.create(iid2, dataObject2, dataObject2));
        flatWriterRegistry.processModifications(new WriterRegistry.DataObjectUpdates(updates, ImmutableMultimap.of()), ctx);

        final InOrder inOrder = inOrder(writer1, writer2);
        inOrder.verify(writer1).processModification(iid, dataObject, dataObject, ctx);
        inOrder.verify(writer2).processModification(iid2, dataObject2, dataObject2, ctx);

        // TODO - HONEYCOMB-412 -reintroduce verifyNoMoreInteractions and remove manual verify
        // we are really interested just in invocations of processModification(),so adding specific verify to check that
        verify(writer1,times(1)).processModification(any(),any(),any(),any());
        verify(writer2,times(1)).processModification(any(),any(),any(),any());
        //verifyNoMoreInteractions(writer1);
        //verifyNoMoreInteractions(writer2);
    }

    @Test
    public void testMultipleDeletesForMultipleWriters() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate.DataObjectDelete> deletes = HashMultimap.create();
        final InstanceIdentifier<DataObject1> iid = InstanceIdentifier.create(DataObject1.class);
        final DataObject1 dataObject = mock(DataObject1.class);
        deletes.put(DataObject1.IID, ((DataObjectUpdate.DataObjectDelete) DataObjectUpdate.create(iid, dataObject, null)));
        final InstanceIdentifier<DataObject2> iid2 = InstanceIdentifier.create(DataObject2.class);
        final DataObject2 dataObject2 = mock(DataObject2.class);
        deletes.put(
                DataObject2.IID, ((DataObjectUpdate.DataObjectDelete) DataObjectUpdate.create(iid2, dataObject2, null)));
        flatWriterRegistry.processModifications(new WriterRegistry.DataObjectUpdates(ImmutableMultimap.of(), deletes), ctx);

        final InOrder inOrder = inOrder(writer1, writer2);
        // Reversed order of invocation, first writer2 and then writer1
        inOrder.verify(writer2).processModification(iid2, dataObject2, null, ctx);
        inOrder.verify(writer1).processModification(iid, dataObject, null, ctx);

        // TODO - HONEYCOMB-412 -reintroduce verifyNoMoreInteractions and remove manual verify
        // we are really interested just in invocations of processModification(),so adding specific verify to check that
        verify(writer1,times(1)).processModification(any(),any(),any(),any());
        verify(writer2,times(1)).processModification(any(),any(),any(),any());
        //verifyNoMoreInteractions(writer1);
        //verifyNoMoreInteractions(writer2);
    }

    @Test
    public void testMultipleUpdatesAndDeletesForMultipleWriters() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate.DataObjectDelete> deletes = HashMultimap.create();
        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        final InstanceIdentifier<DataObject1> iid = InstanceIdentifier.create(DataObject1.class);
        final DataObject1 dataObject = mock(DataObject1.class);
        // Writer 1 delete
        deletes.put(DataObject1.IID, ((DataObjectUpdate.DataObjectDelete) DataObjectUpdate.create(iid, dataObject, null)));
        // Writer 1 update
        updates.put(DataObject1.IID, DataObjectUpdate.create(iid, dataObject, dataObject));
        final InstanceIdentifier<DataObject2> iid2 = InstanceIdentifier.create(DataObject2.class);
        final DataObject2 dataObject2 = mock(DataObject2.class);
        // Writer 2 delete
        deletes.put(
                DataObject2.IID, ((DataObjectUpdate.DataObjectDelete) DataObjectUpdate.create(iid2, dataObject2, null)));
        // Writer 2 update
        updates.put(DataObject2.IID, DataObjectUpdate.create(iid2, dataObject2, dataObject2));
        flatWriterRegistry.processModifications(new WriterRegistry.DataObjectUpdates(updates, deletes), ctx);

        final InOrder inOrder = inOrder(writer1, writer2);
        // Reversed order of invocation, first writer2 and then writer1 for deletes
        inOrder.verify(writer2).processModification(iid2, dataObject2, null, ctx);
        inOrder.verify(writer1).processModification(iid, dataObject, null, ctx);
        // Then also updates are processed
        inOrder.verify(writer1).processModification(iid, dataObject, dataObject, ctx);
        inOrder.verify(writer2).processModification(iid2, dataObject2, dataObject2, ctx);

        // TODO - HONEYCOMB-412 -reintroduce verifyNoMoreInteractions and remove manual verify
        // we are really interested just in invocations of processModification(),so adding specific verify to check that
        verify(writer1,times(2)).processModification(any(),any(),any(),any());
        verify(writer2,times(2)).processModification(any(),any(),any(),any());
        //verifyNoMoreInteractions(writer1);
        //verifyNoMoreInteractions(writer2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultipleUpdatesOneMissing() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        addUpdate(updates, DataObject1.class);
        addUpdate(updates, DataObject2.class);
        flatWriterRegistry.processModifications(new WriterRegistry.DataObjectUpdates(updates, ImmutableMultimap.of()), ctx);
    }

    @Test
    public void testMultipleUpdatesFirstFailing() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2));

        // Writer1 always fails
        doThrow(new RuntimeException()).when(writer1)
                .processModification(any(InstanceIdentifier.class), any(DataObject.class), any(DataObject.class), any(WriteContext.class));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        addUpdate(updates, DataObject1.class);
        addUpdate(updates, DataObject2.class);

        try {
            flatWriterRegistry.processModifications(new WriterRegistry.DataObjectUpdates(updates, ImmutableMultimap.of()), ctx);
            fail("Bulk update should have failed on writer1 with UpdateFailedException");
        } catch (UpdateFailedException e) {
            assertThat(e.getProcessed(), hasSize(0));// very first update failed
        }
    }

    @Test
    public void testMultipleUpdatesSecondFailing() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2));

        // Writer2 always fails
        doThrow(new RuntimeException()).when(writer2)
                .processModification(any(InstanceIdentifier.class), any(DataObject.class), any(DataObject.class), any(WriteContext.class));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        addUpdate(updates, DataObject1.class);
        addUpdate(updates, DataObject2.class);

        try {
            flatWriterRegistry.processModifications(new WriterRegistry.DataObjectUpdates(updates, ImmutableMultimap.of()), ctx);
            fail("Bulk update should have failed on writer1 with UpdateFailedException");
        } catch (UpdateFailedException e) {
            final List<DataObjectUpdate> alreadyProcessed = e.getProcessed();
            assertThat(alreadyProcessed, hasSize(1));// very first update failed
            assertEquals(updateData(DataObject1.class, DataObject1.IID),
                    e.getProcessed().iterator().next());
        }
    }

    @Test
    public void testMultipleUpdatesLastFailing() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(
                        ImmutableMap.of(DataObject1.IID, writer1, DataObject2.IID, writer2, DataObjects.DataObject3.IID, writer3));

        // Writer1 always fails
        doThrow(new RuntimeException()).when(writer3)
                .processModification(any(InstanceIdentifier.class), any(DataObject.class), any(DataObject.class), any(WriteContext.class));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        addUpdate(updates, DataObject1.class);
        addUpdate(updates, DataObject2.class);
        addUpdate(updates, DataObjects.DataObject3.class);

        try {
            flatWriterRegistry.processModifications(new WriterRegistry.DataObjectUpdates(updates, ImmutableMultimap.of()), ctx);
            fail("Bulk update should have failed on writer1 with UpdateFailedException");
        } catch (UpdateFailedException e) {
            final List<DataObjectUpdate> alreadyProcessed = e.getProcessed();
            assertEquals(2, alreadyProcessed.size());
            assertTrue(alreadyProcessed.contains(updateData(DataObject1.class, DataObject1.IID)));
            assertTrue(alreadyProcessed.contains(updateData(DataObject2.class, DataObject2.IID)));
        }
    }

    @Test
    public void testMutlipleUpdatesWithOneKeyedContainer() throws Exception {
        final FlatWriterRegistry flatWriterRegistry =
                new FlatWriterRegistry(
                        ImmutableMap.of(DataObject1.IID, writer1, DataObjects.DataObject1ChildK.IID, writer4));

        // Writer1 always fails
        doThrow(new RuntimeException()).when(writer1)
                .processModification(any(InstanceIdentifier.class), any(DataObject.class), any(DataObject.class),
                        any(WriteContext.class));

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates = HashMultimap.create();
        addKeyedUpdate(updates, DataObjects.DataObject1ChildK.class);
        addUpdate(updates, DataObject1.class);
        try {
            flatWriterRegistry.processModifications(new WriterRegistry.DataObjectUpdates(updates, ImmutableMultimap.of()), ctx);
            fail("Bulk update should have failed on writer1 with UpdateFailedException");
        } catch (UpdateFailedException e) {
            assertTrue(e.getProcessed().isEmpty());
        }
    }

    private <D extends DataObject> void addKeyedUpdate(final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates,
                                                       final Class<D> type) throws Exception {
        final InstanceIdentifier<D> iid = (InstanceIdentifier<D>) type.getDeclaredField("IID").get(null);
        final InstanceIdentifier<D> keyedIid = (InstanceIdentifier<D>) type.getDeclaredField("INTERNALLY_KEYED_IID").get(null);
        updates.put(iid, DataObjectUpdate.create(keyedIid, mock(type), mock(type)));
    }

    private <D extends DataObject> void addUpdate(final Multimap<InstanceIdentifier<?>, DataObjectUpdate> updates,
                                                  final Class<D> type) throws Exception {
        final InstanceIdentifier<D> iid = (InstanceIdentifier<D>) type.getDeclaredField("IID").get(null);
        updates.put(iid, updateData(type, iid));
    }

    private static <D extends DataObject> DataObjectUpdate updateData(final Class<D> type,
                                                                      final InstanceIdentifier<D> iid) {
        return DataObjectUpdate.create(iid, mock(type), mock(type));
    }

    //TODO - HONEYCOMB-412 - remove after
    /**
     * Used to utilize default implementation of canProcess()
     * */
    static class CheckedMockWriter implements Writer{

        private final Writer mockedWriter;

        CheckedMockWriter(final Writer mockedWriter) {
            this.mockedWriter = mockedWriter;
        }

        @Override
        public void processModification(@Nonnull final InstanceIdentifier id, @Nullable final DataObject dataBefore,
                                        @Nullable final DataObject dataAfter, @Nonnull final WriteContext ctx)
                throws WriteFailedException {
            mockedWriter.processModification(id,dataBefore,dataAfter,ctx);
        }

        @Override
        public boolean supportsDirectUpdate() {
            return mockedWriter.supportsDirectUpdate();
        }

        @Nonnull
        @Override
        public InstanceIdentifier getManagedDataObjectType() {
            return mockedWriter.getManagedDataObjectType();
        }
    }
}