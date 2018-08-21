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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.data.DataModification;
import io.fd.honeycomb.translate.write.DataObjectUpdate;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ModifiableDataTreeDelegatorTest extends ModifiableDataTreeDelegatorBaseTest {

    @Test
    public void testRead() throws Exception {
        final ContainerNode topContainer = getTopContainer("topContainer");
        addNodeToTree(dataTree, topContainer, TOP_CONTAINER_ID);
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read =
                configDataTree.read(TOP_CONTAINER_ID);
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read2 =
                configDataTree.newModification().read(TOP_CONTAINER_ID);
        final Optional<NormalizedNode<?, ?>> normalizedNodeOptional = read.get();
        final Optional<NormalizedNode<?, ?>> normalizedNodeOptional2 = read2.get();

        assertEquals(normalizedNodeOptional, normalizedNodeOptional2);
        assertTrue(normalizedNodeOptional.isPresent());
        assertEquals(topContainer, normalizedNodeOptional.get());
        assertEquals(dataTree.takeSnapshot().readNode(TOP_CONTAINER_ID), Optional.toJavaUtil(normalizedNodeOptional));
    }

    @Test
    public void testValidateTwice() throws Exception {
        final MapNode nestedList = getNestedList("listEntry", "listValue");

        final DataModification dataModification = configDataTree.newModification();
        dataModification.write(NESTED_LIST_ID, nestedList);
        dataModification.validate();
        dataModification.validate();

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> map = HashMultimap.create();
        map.put(DEFAULT_ID, DataObjectUpdate.create(DEFAULT_ID, null, DEFAULT_DATA_OBJECT));
        final WriterRegistry.DataObjectUpdates updates =
            new WriterRegistry.DataObjectUpdates(map, ImmutableMultimap.of());
        verify(writer, times(2)).validateModifications(eq(updates), any(WriteContext.class));
    }

    @Test
    public void testCommitSuccessful() throws Exception {
        final MapNode nestedList = getNestedList("listEntry", "listValue");

        final DataModification dataModification = configDataTree.newModification();
        dataModification.write(NESTED_LIST_ID, nestedList);
        dataModification.validate();
        dataModification.commit();

        final Multimap<InstanceIdentifier<?>, DataObjectUpdate> map = HashMultimap.create();
        // data before should be null as it is create
        map.put(DEFAULT_ID, DataObjectUpdate.create(DEFAULT_ID, null, DEFAULT_DATA_OBJECT));
        verify(writer).processModifications(eq(new WriterRegistry.DataObjectUpdates(map, ImmutableMultimap.of())), any(WriteContext.class));
        assertEquals(nestedList, dataTree.takeSnapshot().readNode(NESTED_LIST_ID).get());
    }

    @Test
    public void testToBindingAware() throws Exception {
        when(serializer.fromNormalizedNode(any(YangInstanceIdentifier.class), eq(null))).thenReturn(null);
        when(writer.writerSupportsUpdate(any())).thenReturn(true);
        final Map<YangInstanceIdentifier, NormalizedNodeUpdate> biNodes = new HashMap<>();
        // delete
        final QName nn1 = QName.create("namespace", "nn1");
        final YangInstanceIdentifier yid1 = mockYid(nn1);
        final InstanceIdentifier iid1 = mockIid(yid1, DataObject1.class);
        final NormalizedNode nn1B = mockNormalizedNode(nn1);
        final DataObject1 do1B = mockDataObject(yid1, iid1, nn1B, DataObject1.class);
        biNodes.put(yid1, NormalizedNodeUpdate.create(yid1, nn1B, null));

        // create
        final QName nn2 = QName.create("namespace", "nn1");
        final YangInstanceIdentifier yid2 = mockYid(nn2);
        final InstanceIdentifier iid2 = mockIid(yid2, DataObject2.class);
        final NormalizedNode nn2A = mockNormalizedNode(nn2);
        final DataObject2 do2A = mockDataObject(yid2, iid2, nn2A, DataObject2.class);
        biNodes.put(yid2, NormalizedNodeUpdate.create(yid2, null, nn2A));

        // update
        final QName nn3 = QName.create("namespace", "nn1");
        final YangInstanceIdentifier yid3 = mockYid(nn3);
        final InstanceIdentifier iid3 = mockIid(yid3, DataObject3.class);
        final NormalizedNode nn3B = mockNormalizedNode(nn3);
        final DataObject3 do3B = mockDataObject(yid3, iid3, nn3B, DataObject3.class);
        final NormalizedNode nn3A = mockNormalizedNode(nn3);
        final DataObject3 do3A = mockDataObject(yid3, iid3, nn3A, DataObject3.class);
        biNodes.put(yid3, NormalizedNodeUpdate.create(yid3, nn3B, nn3A));

        final WriterRegistry.DataObjectUpdates dataObjectUpdates =
                ModifiableDataTreeDelegator.toBindingAware(writer, biNodes, serializer);

        assertThat(dataObjectUpdates.getDeletes().size(), is(1));
        assertThat(dataObjectUpdates.getDeletes().keySet(), hasItem(((InstanceIdentifier<?>) iid1)));
        assertThat(dataObjectUpdates.getDeletes().values(), hasItem(
                ((DataObjectUpdate.DataObjectDelete) DataObjectUpdate.create(iid1, do1B, null))));

        assertThat(dataObjectUpdates.getUpdates().size(), is(2));
        assertThat(dataObjectUpdates.getUpdates().keySet(), hasItems((InstanceIdentifier<?>) iid2, (InstanceIdentifier<?>) iid3));
        assertThat(dataObjectUpdates.getUpdates().values(), hasItems(
                DataObjectUpdate.create(iid2, null, do2A),
                DataObjectUpdate.create(iid3, do3B, do3A)));

        assertThat(dataObjectUpdates.getTypeIntersection().size(), is(3));
    }

    @Test
    public void testToBindingAwareUpdateNotSupported() throws Exception {
        when(serializer.fromNormalizedNode(any(YangInstanceIdentifier.class), eq(null))).thenReturn(null);
        when(writer.writerSupportsUpdate(any())).thenReturn(false);
        final Map<YangInstanceIdentifier, NormalizedNodeUpdate> biNodes = new HashMap<>();
        // delete
        final QName nn1 = QName.create("namespace", "nn1");
        final YangInstanceIdentifier yid1 = mockYid(nn1);
        final InstanceIdentifier iid1 = mockIid(yid1, DataObject1.class);
        final NormalizedNode nn1B = mockNormalizedNode(nn1);
        final DataObject1 do1B = mockDataObject(yid1, iid1, nn1B, DataObject1.class);
        biNodes.put(yid1, NormalizedNodeUpdate.create(yid1, nn1B, null));

        // create
        final QName nn2 = QName.create("namespace", "nn1");
        final YangInstanceIdentifier yid2 = mockYid(nn2);
        final InstanceIdentifier iid2 = mockIid(yid2, DataObject2.class);
        final NormalizedNode nn2A = mockNormalizedNode(nn2);
        final DataObject2 do2A = mockDataObject(yid2, iid2, nn2A, DataObject2.class);
        biNodes.put(yid2, NormalizedNodeUpdate.create(yid2, null, nn2A));

        // processModifications
        final QName nn3 = QName.create("namespace", "nn1");
        final YangInstanceIdentifier yid3 = mockYid(nn3);
        final InstanceIdentifier iid3 = mockIid(yid3, DataObject3.class);
        final NormalizedNode nn3B = mockNormalizedNode(nn3);
        final DataObject3 do3B = mockDataObject(yid3, iid3, nn3B, DataObject3.class);
        final NormalizedNode nn3A = mockNormalizedNode(nn3);
        final DataObject3 do3A = mockDataObject(yid3, iid3, nn3A, DataObject3.class);
        biNodes.put(yid3, NormalizedNodeUpdate.create(yid3, nn3B, nn3A));

        final WriterRegistry.DataObjectUpdates dataObjectUpdates =
                ModifiableDataTreeDelegator.toBindingAware(writer, biNodes, serializer);

        // should have also id and data for delete as delete + create pair was created
        assertThat(dataObjectUpdates.getDeletes().size(), is(2));
        assertThat(dataObjectUpdates.getDeletes().keySet(),
                hasItems(((InstanceIdentifier<?>) iid1), (InstanceIdentifier<?>) iid3));
        assertThat(dataObjectUpdates.getDeletes().values(), hasItems(
                ((DataObjectUpdate.DataObjectDelete) DataObjectUpdate.create(iid1, do1B, null)),
                ((DataObjectUpdate.DataObjectDelete) DataObjectUpdate.create(iid3, do3B, null))));

        assertThat(dataObjectUpdates.getUpdates().size(), is(2));
        assertThat(dataObjectUpdates.getUpdates().keySet(), hasItems((InstanceIdentifier<?>) iid2, (InstanceIdentifier<?>) iid3));
        assertThat(dataObjectUpdates.getUpdates().values(), hasItems(
                DataObjectUpdate.create(iid2, null, do2A),
                DataObjectUpdate.create(iid3, null, do3A)));

        assertThat(dataObjectUpdates.getTypeIntersection().size(), is(3));
    }
}
