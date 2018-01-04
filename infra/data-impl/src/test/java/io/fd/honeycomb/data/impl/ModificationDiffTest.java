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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModificationDiffTest extends ModificationBaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(ModificationDiffTest.class);

    @Test
    public void testInitialWrite() throws Exception {
        final DataTree dataTree = getDataTree();
        final DataTreeModification dataTreeModification = getModification(dataTree);
        final NormalizedNode<?, ?> topContainer = getTopContainer("string1");
        final YangInstanceIdentifier TOP_CONTAINER_ID = YangInstanceIdentifier.of(TOP_CONTAINER_QNAME);
        dataTreeModification.write(TOP_CONTAINER_ID, topContainer);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertThat(modificationDiff.getUpdates().size(), is(1));
        assertThat(modificationDiff.getUpdates().values().size(), is(1));
        assertUpdate(modificationDiff.getUpdates().values().iterator().next(), TOP_CONTAINER_ID, null, topContainer);
    }

    @Test
    public void testLeafList() throws Exception {
        final DataTree dataTree = getDataTree();
        final DataTreeModification dataTreeModification = getModification(dataTree);
        final ContainerNode topContainer = getTopContainerWithLeafList("string1", "string2");
        final YangInstanceIdentifier TOP_CONTAINER_ID = YangInstanceIdentifier.of(TOP_CONTAINER_QNAME);
        dataTreeModification.write(TOP_CONTAINER_ID, topContainer);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertThat(modificationDiff.getUpdates().size(), is(1));
        assertThat(modificationDiff.getUpdates().values().size(), is(1));
        assertUpdate(modificationDiff.getUpdates().values().iterator().next(),
                TOP_CONTAINER_ID.node(FOR_LEAF_LIST_QNAME), null,
                topContainer.getChild(new YangInstanceIdentifier.NodeIdentifier(FOR_LEAF_LIST_QNAME)).get());
    }

    @Test
    public void testWritePresenceEmptyContainer() throws Exception {
        final DataTree dataTree = getDataTree();
        final DataTreeModification dataTreeModification = getModification(dataTree);
        final NormalizedNode<?, ?> presenceContainer = Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(PRESENCE_CONTAINER_QNAME))
                .build();
        final YangInstanceIdentifier PRESENCE_CONTAINER_ID = YangInstanceIdentifier.of(PRESENCE_CONTAINER_QNAME);
        dataTreeModification.write(PRESENCE_CONTAINER_ID, presenceContainer);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final ModificationDiff modificationDiff = getModificationDiff(prepare);

        dataTree.commit(prepare);

        final Optional<NormalizedNode<?, ?>> presenceAfter = getModification(dataTree).readNode(PRESENCE_CONTAINER_ID);
        assertTrue(presenceAfter.isPresent());
        assertThat(presenceAfter.get(), equalTo(presenceContainer));

        assertThat(modificationDiff.getUpdates().size(), is(1));
        assertThat(modificationDiff.getUpdates().values().size(), is(1));
        assertUpdate(modificationDiff.getUpdates().values().iterator().next(), PRESENCE_CONTAINER_ID, null, presenceContainer);
    }

    @Test
    public void testInitialWriteForContainerWithChoice() throws Exception {
        final DataTree dataTree = getDataTree();
        final DataTreeModification dataTreeModification = getModification(dataTree);
        final ContainerNode containerWithChoice = Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(WITH_CHOICE_CONTAINER_QNAME))
                .withChild(Builders.choiceBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(CHOICE_QNAME))
                        .withChild(ImmutableNodes.leafNode(IN_CASE1_LEAF_QNAME, "withinCase1"))
                        .build())
                .build();
        final YangInstanceIdentifier WITH_CHOICE_CONTAINER_ID = YangInstanceIdentifier.of(WITH_CHOICE_CONTAINER_QNAME);
        dataTreeModification.write(WITH_CHOICE_CONTAINER_ID, containerWithChoice);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();

        assertThat(updates.size(), is(1));
        assertUpdate(getNormalizedNodeUpdateForAfterType(updates, ContainerNode.class),
                WITH_CHOICE_CONTAINER_ID, null, containerWithChoice);
    }



    @Test
    public void testWriteNonPresenceEmptyContainer() throws Exception {
        final DataTree dataTree = getDataTree();
        final DataTreeModification dataTreeModification = getModification(dataTree);
        final NormalizedNode<?, ?> topContainer = ImmutableNodes.containerNode(TOP_CONTAINER_QNAME);
        dataTreeModification.write(TOP_CONTAINER_ID, topContainer);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertThat(modificationDiff.getUpdates().size(), is(0));
    }

    /**
     * Covers case when both non-presence and nested is not present
     */
    @Test
    public void testWriteNonPresenceNonEmptyContainer() throws Exception {
        final DataTree dataTree = getDataTree();
        final DataTreeModification dataTreeModification = getModification(dataTree);
        // non presence ,but with valid child list
        final NormalizedNode<?, ?> topContainer = Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .withChild(getNestedList("value","txt"))
                .build();

        dataTreeModification.write(TOP_CONTAINER_ID, topContainer);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final ModificationDiff modificationDiff = getModificationDiff(prepare);

        assertThat(modificationDiff.getUpdates().size(), is(1));
        assertNodeModificationPresent(modificationDiff, ImmutableSet.of(NESTED_LIST_QNAME));
    }

    /**
     * Covers case when non-presence container was already filled with some data,
     * and modification just ads some other nested data
     */
    @Test
    public void testWriteNonPresenceNonEmptyContainerPreviousData() throws Exception {
        final DataTree dataTree = getDataTree();
        final DataTreeModification dataTreeModificationOriginal = getModification(dataTree);
        // non presence, but with valid child list
        final MapEntryNode alreadyPresent = getNestedListEntry("value", "txt");
        final NormalizedNode<?, ?> topContainerOriginal = Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .withChild(getNestedList(alreadyPresent))
                .build();

        dataTreeModificationOriginal.write(TOP_CONTAINER_ID, topContainerOriginal);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModificationOriginal);
        dataTree.commit(prepare);
        // now we have state with some data

        final MapEntryNode newEntry = getNestedListEntry("value2", "txt2");
        final NormalizedNode<?, ?> topContainerModified = Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .withChild(getNestedList(alreadyPresent, newEntry))
                .build();
        final DataTreeModification dataTreeModificationModified = getModification(dataTree);
        dataTreeModificationModified.write(TOP_CONTAINER_ID, topContainerModified);
        final DataTreeCandidateTip prepareModified = prepareModification(dataTree, dataTreeModificationModified);

        final ModificationDiff modificationDiff = getModificationDiff(prepareModified);
        // should detect one new entry
        LOG.debug(modificationDiff.toString());
        assertThat(modificationDiff.getUpdates().size(), is(1));
        assertNodeModificationPresent(modificationDiff, ImmutableSet.of(NESTED_LIST_QNAME));
    }


    @Test
    public void testWriteNonPresenceEmptyNestedContainer() throws Exception {
        final DataTree dataTree = getDataTree();
        final DataTreeModification dataTreeModification = getModification(dataTree);
        final NormalizedNode<?, ?> topContainer = Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .withChild(ImmutableNodes.containerNode(EMPTY_QNAME))
                .withChild(ImmutableNodes.leafNode(STRING_LEAF_QNAME, "1"))
                .build();
        dataTreeModification.write(TOP_CONTAINER_ID, topContainer);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final ModificationDiff modificationDiff = getModificationDiff(prepare);
        dataTree.commit(prepare);

        // Only the STRING_LEAF_QNAME is considered a modification, the EMPTY_QNAME container is ignored since it is
        // not a presence container
        assertThat(modificationDiff.getUpdates().size(), is(1));
    }

    @Test
    public void testUpdateWrite() throws Exception {
        final DataTree dataTree = getDataTree();
        final ContainerNode topContainer = getTopContainer("string1");
        addNodeToTree(dataTree, topContainer, TOP_CONTAINER_ID);

        final DataTreeModification dataTreeModification = getModification(dataTree);
        final NormalizedNode<?, ?> topContainerAfter = getTopContainer("string2");
        dataTreeModification.write(TOP_CONTAINER_ID, topContainerAfter);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();

        assertThat(updates.size(), is(1));
        assertThat(updates.values().size(), is(1));
        assertUpdate(updates.values().iterator().next(), TOP_CONTAINER_ID, topContainer, topContainerAfter);
    }


    @Test
    public void testUpdateMerge() throws Exception {
        final DataTree dataTree = getDataTree();
        final ContainerNode topContainer = getTopContainer("string1");
        addNodeToTree(dataTree, topContainer, TOP_CONTAINER_ID);

        final DataTreeModification dataTreeModification = getModification(dataTree);
        final NormalizedNode<?, ?> topContainerAfter = getTopContainer("string2");
        dataTreeModification.merge(TOP_CONTAINER_ID, topContainerAfter);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();
        assertThat(updates.size(), is(1));
        assertThat(updates.values().size(), is(1));
        assertUpdate(updates.values().iterator().next(), TOP_CONTAINER_ID, topContainer, topContainerAfter);
    }

    @Test
    public void testUpdateDelete() throws Exception {
        final DataTree dataTree = getDataTree();
        final ContainerNode topContainer = getTopContainer("string1");
        addNodeToTree(dataTree, topContainer, TOP_CONTAINER_ID);

        final DataTreeModification dataTreeModification = getModification(dataTree);
        dataTreeModification.delete(TOP_CONTAINER_ID);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();
        assertThat(updates.size(), is(1));
        assertThat(updates.values().size(), is(1));
        assertUpdate(updates.values().iterator().next(), TOP_CONTAINER_ID, topContainer, null);
    }

    @Test
    public void testWriteAndUpdateInnerList() throws Exception {
        final DataTree dataTree = getDataTree();

        DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        DataTreeModification dataTreeModification = dataTreeSnapshot.newModification();
        final YangInstanceIdentifier listId =
                YangInstanceIdentifier.create(
                        new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME),
                        new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME));

        final MapNode mapNode = getNestedList("name1", "text");
        final YangInstanceIdentifier listEntryId = listId.node(mapNode.getValue().iterator().next().getIdentifier());
        dataTreeModification.write(listId, mapNode);
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        DataTreeCandidateTip prepare = dataTree.prepare(dataTreeModification);

        Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();

        assertThat(updates.size(), is(1));
        assertUpdate(getNormalizedNodeUpdateForAfterType(updates, MapEntryNode.class),
                listEntryId, null, mapNode.getValue().iterator().next());

        // Commit so that update can be tested next
        dataTree.commit(prepare);

        YangInstanceIdentifier listItemId = listId.node(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(NESTED_LIST_QNAME, NAME_LEAF_QNAME, "name1"));
        MapEntryNode mapEntryNode =
                getNestedList("name1", "text-update").getValue().iterator().next();

        dataTreeSnapshot = dataTree.takeSnapshot();
        dataTreeModification = dataTreeSnapshot.newModification();
        dataTreeModification.write(listItemId, mapEntryNode);
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        prepare = dataTree.prepare(dataTreeModification);

        updates = getModificationDiff(prepare).getUpdates();
        assertThat(updates.size(), is(1 /*Actual list entry*/));
    }
//

    @Test
    public void testWriteTopContainerAndInnerList() throws Exception {
        final DataTree dataTree = getDataTree();

        DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        DataTreeModification dataTreeModification = dataTreeSnapshot.newModification();

        final ContainerNode topContainer = getTopContainer("string1");
        dataTreeModification.write(TOP_CONTAINER_ID, topContainer);

        final YangInstanceIdentifier listId =
                YangInstanceIdentifier.create(
                        new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME),
                        new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME));

        final MapNode mapNode = getNestedList("name1", "text");
        final YangInstanceIdentifier listEntryId = listId.node(mapNode.getValue().iterator().next().getIdentifier());

        dataTreeModification.write(listId, mapNode);

        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModification);

        final Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();

        assertThat(updates.size(), is(2));
        assertThat(updates.values().size(), is(2));
        assertUpdate(getNormalizedNodeUpdateForAfterType(updates, ContainerNode.class), TOP_CONTAINER_ID, null,
                Builders.containerBuilder(topContainer).withChild(mapNode).build());
        assertUpdate(getNormalizedNodeUpdateForAfterType(updates, MapEntryNode.class), listEntryId, null, mapNode.getValue().iterator().next());
        // Assert that keys of the updates map are not wildcarded YID
        assertThat(updates.keySet(), hasItems(
                TOP_CONTAINER_ID,
                listEntryId));
    }



    @Test
    public void testWriteDeepList() throws Exception {
        final DataTree dataTree = getDataTree();

        DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        DataTreeModification dataTreeModification = dataTreeSnapshot.newModification();

        YangInstanceIdentifier listId =
                YangInstanceIdentifier.create(
                        new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME),
                        new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME));

        MapNode mapNode = getNestedList("name1", "text");
        dataTreeModification.write(listId, mapNode);

        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        DataTreeCandidateTip prepare = dataTree.prepare(dataTreeModification);
        dataTree.commit(prepare);

        dataTreeSnapshot = dataTree.takeSnapshot();
        dataTreeModification = dataTreeSnapshot.newModification();

        final YangInstanceIdentifier.NodeIdentifierWithPredicates nestedListNodeId =
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(NESTED_LIST_QNAME, NAME_LEAF_QNAME, "name1");
        listId = YangInstanceIdentifier.create(
                new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME),
                new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME),
                nestedListNodeId);
        final YangInstanceIdentifier deepListId =
                listId.node(new YangInstanceIdentifier.NodeIdentifier(DEEP_LIST_QNAME));
        final YangInstanceIdentifier deepListEntryId = deepListId.node(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(DEEP_LIST_QNAME, NAME_LEAF_QNAME, "name1"));

        final MapEntryNode deepListEntry = getDeepList("name1").getValue().iterator().next();
        // Merge parent list, just to see no modifications on it
        dataTreeModification.merge(
                listId,
                Builders.mapEntryBuilder().withNodeIdentifier(nestedListNodeId)
                        .withChild(ImmutableNodes.leafNode(NAME_LEAF_QNAME, "name1")).build());
        dataTreeModification.merge(
                deepListId,
                Builders.mapBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(DEEP_LIST_QNAME))
                        .build());
        dataTreeModification.merge(
                deepListEntryId,
                deepListEntry);

        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        prepare = dataTree.prepare(dataTreeModification);
        dataTree.commit(prepare);

        final Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();
        assertThat(updates.size(), is(1));
        assertUpdate(getNormalizedNodeUpdateForAfterType(updates, MapEntryNode.class), deepListEntryId, null, deepListEntry);
    }

    @Test
    public void testDeleteInnerListItem() throws Exception {
        final DataTree dataTree = getDataTree();

        DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        DataTreeModification dataTreeModification = dataTreeSnapshot.newModification();
        final YangInstanceIdentifier listId =
                YangInstanceIdentifier.create(
                        new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME),
                        new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME));

        final MapNode mapNode = getNestedList("name1", "text");
        dataTreeModification.write(listId, mapNode);
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        DataTreeCandidateTip prepare = dataTree.prepare(dataTreeModification);

        // Commit so that update can be tested next
        dataTree.commit(prepare);

        YangInstanceIdentifier listItemId = listId.node(
                new YangInstanceIdentifier.NodeIdentifierWithPredicates(NESTED_LIST_QNAME, NAME_LEAF_QNAME, "name1"));

        dataTreeSnapshot = dataTree.takeSnapshot();
        dataTreeModification = dataTreeSnapshot.newModification();
        dataTreeModification.delete(listItemId);
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        prepare = dataTree.prepare(dataTreeModification);

        final Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates = getModificationDiff(prepare).getUpdates();
        assertThat(updates.size(), is(1));
        assertUpdate(getNormalizedNodeUpdateForBeforeType(updates, MapEntryNode.class), listItemId, mapNode.getValue().iterator().next(), null);
    }


}