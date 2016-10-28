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

package io.fd.honeycomb.translate.util.write.registry;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import io.fd.honeycomb.translate.util.DataObjects;
import io.fd.honeycomb.translate.write.DataObjectUpdate;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.Writer;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FlatWriterRegistryBuilderTest {

    @Test
    public void testRelationsBefore() throws Exception {
        final FlatWriterRegistryBuilder flatWriterRegistryBuilder = new FlatWriterRegistryBuilder();
        /*
            1   ->  2   ->  3
                        ->  4
         */
        flatWriterRegistryBuilder.add(mockWriter(DataObjects.DataObject3.class));
        flatWriterRegistryBuilder.add(mockWriter(DataObjects.DataObject4.class));
        flatWriterRegistryBuilder.addBefore(mockWriter(DataObjects.DataObject2.class),
                Lists.newArrayList(DataObjects.DataObject3.IID, DataObjects.DataObject4.IID));
        flatWriterRegistryBuilder.addBefore(mockWriter(DataObjects.DataObject1.class), DataObjects.DataObject2.IID);
        final ImmutableMap<InstanceIdentifier<?>, Writer<?>> mappedWriters =
                flatWriterRegistryBuilder.getMappedHandlers();

        final ArrayList<InstanceIdentifier<?>> typesInList = Lists.newArrayList(mappedWriters.keySet());
        assertEquals(DataObjects.DataObject1.IID, typesInList.get(0));
        assertEquals(DataObjects.DataObject2.IID, typesInList.get(1));
        assertThat(typesInList.get(2), anyOf(equalTo(DataObjects.DataObject3.IID), equalTo(DataObjects.DataObject4.IID)));
        assertThat(typesInList.get(3), anyOf(equalTo(DataObjects.DataObject3.IID), equalTo(DataObjects.DataObject4.IID)));
    }

    @Test
    public void testBuild() throws Exception {
        final FlatWriterRegistryBuilder flatWriterRegistryBuilder = new FlatWriterRegistryBuilder();
        final Writer<? extends DataObject> writer = mockWriter(DataObjects.DataObject3.class);
        flatWriterRegistryBuilder.add(writer);
        final WriterRegistry build = flatWriterRegistryBuilder.build();

        final InstanceIdentifier<DataObjects.DataObject3> id = InstanceIdentifier.create(DataObjects.DataObject3.class);
        final DataObjectUpdate update = mock(DataObjectUpdate.class);
        doReturn(id).when(update).getId();
        final DataObjects.DataObject3 before = mock(DataObjects.DataObject3.class);
        final DataObjects.DataObject3 after = mock(DataObjects.DataObject3.class);
        when(update.getDataBefore()).thenReturn(before);
        when(update.getDataAfter()).thenReturn(after);

        WriterRegistry.DataObjectUpdates updates = new WriterRegistry.DataObjectUpdates(
                Multimaps.forMap(Collections.singletonMap(id, update)),
                Multimaps.forMap(Collections.emptyMap()));
        final WriteContext ctx = mock(WriteContext.class);
        build.update(updates, ctx);

        verify(writer).update(id, before, after, ctx);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildUnknownWriter() throws Exception {
        final FlatWriterRegistryBuilder flatWriterRegistryBuilder = new FlatWriterRegistryBuilder();
        final Writer<? extends DataObject> writer = mockWriter(DataObjects.DataObject3.class);
        flatWriterRegistryBuilder.add(writer);
        final WriterRegistry build = flatWriterRegistryBuilder.build();

        final InstanceIdentifier<DataObjects.DataObject1> id2 = InstanceIdentifier.create(DataObjects.DataObject1.class);
        final DataObjectUpdate update2 = mock(DataObjectUpdate.class);
        final WriterRegistry.DataObjectUpdates updates = new WriterRegistry.DataObjectUpdates(
                Multimaps.forMap(Collections.singletonMap(id2, update2)),
                Multimaps.forMap(Collections.emptyMap()));
        build.update(updates, mock(WriteContext.class));
    }

    @Test
    public void testRelationsAfter() throws Exception {
        final FlatWriterRegistryBuilder flatWriterRegistryBuilder = new FlatWriterRegistryBuilder();
        /*
            1   ->  2   ->  3
                        ->  4
         */
        flatWriterRegistryBuilder.add(mockWriter(DataObjects.DataObject1.class));
        flatWriterRegistryBuilder.addAfter(mockWriter(DataObjects.DataObject2.class), DataObjects.DataObject1.IID);
        flatWriterRegistryBuilder.addAfter(mockWriter(DataObjects.DataObject3.class), DataObjects.DataObject2.IID);
        flatWriterRegistryBuilder.addAfter(mockWriter(DataObjects.DataObject4.class),
                Lists.newArrayList(DataObjects.DataObject2.IID, DataObjects.DataObject3.IID));
        final ImmutableMap<InstanceIdentifier<?>, Writer<?>> mappedWriters =
                flatWriterRegistryBuilder.getMappedHandlers();

        final List<InstanceIdentifier<?>> typesInList = Lists.newArrayList(mappedWriters.keySet());
        assertEquals(DataObjects.DataObject1.IID, typesInList.get(0));
        assertEquals(DataObjects.DataObject2.IID, typesInList.get(1));
        assertThat(typesInList.get(2), anyOf(equalTo(DataObjects.DataObject3.IID), equalTo(DataObjects.DataObject4.IID)));
        assertThat(typesInList.get(3), anyOf(equalTo(DataObjects.DataObject3.IID), equalTo(DataObjects.DataObject4.IID)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRelationsLoop() throws Exception {
        final FlatWriterRegistryBuilder flatWriterRegistryBuilder = new FlatWriterRegistryBuilder();
        /*
            1   ->  2   ->  1
         */
        flatWriterRegistryBuilder.add(mockWriter(DataObjects.DataObject1.class));
        flatWriterRegistryBuilder.addAfter(mockWriter(DataObjects.DataObject2.class), DataObjects.DataObject1.IID);
        flatWriterRegistryBuilder.addAfter(mockWriter(DataObjects.DataObject1.class), DataObjects.DataObject2.IID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddWriterTwice() throws Exception {
        final FlatWriterRegistryBuilder flatWriterRegistryBuilder = new FlatWriterRegistryBuilder();
        flatWriterRegistryBuilder.add(mockWriter(DataObjects.DataObject1.class));
        flatWriterRegistryBuilder.add(mockWriter(DataObjects.DataObject1.class));
    }

    @Test
    public void testAddSubtreeWriter() throws Exception {
        final FlatWriterRegistryBuilder flatWriterRegistryBuilder = new FlatWriterRegistryBuilder();
        flatWriterRegistryBuilder.subtreeAdd(
                Sets.newHashSet(DataObjects.DataObject4.DataObject41.IID,
                                DataObjects.DataObject4.DataObject41.IID),
                mockWriter(DataObjects.DataObject4.class));
        final ImmutableMap<InstanceIdentifier<?>, Writer<?>> mappedWriters =
                flatWriterRegistryBuilder.getMappedHandlers();
        final ArrayList<InstanceIdentifier<?>> typesInList = Lists.newArrayList(mappedWriters.keySet());

        assertEquals(DataObjects.DataObject4.IID, typesInList.get(0));
        assertEquals(1, typesInList.size());
    }

    @Test
    public void testCreateSubtreeWriter() throws Exception {
        final Writer<?> forWriter = SubtreeWriter.createForWriter(Sets.newHashSet(
                DataObjects.DataObject4.DataObject41.IID,
                DataObjects.DataObject4.DataObject41.DataObject411.IID,
                DataObjects.DataObject4.DataObject42.IID),
                mockWriter(DataObjects.DataObject4.class));
        assertThat(forWriter, instanceOf(SubtreeWriter.class));
        assertThat(((SubtreeWriter<?>) forWriter).getHandledChildTypes().size(), is(3));
        assertThat(((SubtreeWriter<?>) forWriter).getHandledChildTypes(), hasItems(DataObjects.DataObject4.DataObject41.IID,
                DataObjects.DataObject4.DataObject42.IID, DataObjects.DataObject4.DataObject41.DataObject411.IID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateInvalidSubtreeWriter() throws Exception {
        SubtreeWriter.createForWriter(Sets.newHashSet(
                InstanceIdentifier.create(DataObjects.DataObject3.class).child(DataObjects.DataObject3.DataObject31.class)),
                mockWriter(DataObjects.DataObject4.class));
    }

    @SuppressWarnings("unchecked")
    private Writer<? extends DataObject> mockWriter(final Class<? extends DataObject> doClass)
            throws NoSuchFieldException, IllegalAccessException {
        final Writer mock = mock(Writer.class);
        when(mock.getManagedDataObjectType()).thenReturn((InstanceIdentifier<?>) doClass.getDeclaredField("IID").get(null));
        return mock;
    }

}