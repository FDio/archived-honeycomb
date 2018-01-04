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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.test.util.YangParserTestUtils;

abstract class ModificationBaseTest extends ModificationMetadata {

    void addNodeToTree(final DataTree dataTree, final NormalizedNode<?, ?> node,
                       final YangInstanceIdentifier id)
            throws DataValidationFailedException {
        DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        DataTreeModification dataTreeModification = dataTreeSnapshot.newModification();
        dataTreeModification.write(id, node);
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        DataTreeCandidate prepare = dataTree.prepare(dataTreeModification);
        dataTree.commit(prepare);
    }

    protected DataTree getDataTree() throws ReactorException {
        final DataTree dataTree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_CONFIGURATION);
        dataTree.setSchemaContext(getSchemaCtx());
        return dataTree;
    }

    ContainerNode getTopContainer(final String stringValue) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .withChild(ImmutableNodes.leafNode(STRING_LEAF_QNAME, stringValue))
                .build();
    }

    ContainerNode getTopContainerWithLeafList(final String... stringValue) {
        final ListNodeBuilder<String, LeafSetEntryNode<String>> leafSetBuilder = Builders.leafSetBuilder();
        for (final String value : stringValue) {
            leafSetBuilder.withChild(Builders.<String>leafSetEntryBuilder()
                    .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue<>(NESTED_LEAF_LIST_QNAME, value))
                    .withValue(value)
                    .build());
        }

        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .withChild(Builders.containerBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(FOR_LEAF_LIST_QNAME))
                        .withChild(leafSetBuilder
                                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_LEAF_LIST_QNAME))
                                .build())
                        .build())
                .build();
    }

    MapNode getNestedList(final String listItemName, final String text) {
        return Builders.mapBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME))
                .withChild(
                        Builders.mapEntryBuilder()
                                .withNodeIdentifier(
                                        new YangInstanceIdentifier.NodeIdentifierWithPredicates(NESTED_LIST_QNAME,
                                                NAME_LEAF_QNAME, listItemName))
                                .withChild(ImmutableNodes.leafNode(NAME_LEAF_QNAME, listItemName))
                                .withChild(ImmutableNodes.leafNode(TEXT_LEAF_QNAME, text))
                                .build()
                )
                .build();
    }

    MapNode getDeepList(final String listItemName) {
        return Builders.mapBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(DEEP_LIST_QNAME))
                .withChild(
                        Builders.mapEntryBuilder()
                                .withNodeIdentifier(
                                        new YangInstanceIdentifier.NodeIdentifierWithPredicates(DEEP_LIST_QNAME,
                                                NAME_LEAF_QNAME, listItemName))
                                .withChild(ImmutableNodes.leafNode(NAME_LEAF_QNAME, listItemName))
                                .build()
                )
                .build();
    }

    SchemaContext getSchemaCtx() throws ReactorException {
        return YangParserTestUtils.parseYangResource("/test-diff.yang");
    }

    DataTreeModification getModification(final DataTree dataTree) {
        final DataTreeSnapshot dataTreeSnapshot = dataTree.takeSnapshot();
        return dataTreeSnapshot.newModification();
    }


    DataTreeCandidateTip prepareModification(final DataTree dataTree,
                                             final DataTreeModification dataTreeModification)
            throws DataValidationFailedException {
        dataTreeModification.ready();
        dataTree.validate(dataTreeModification);
        return dataTree.prepare(dataTreeModification);
    }

    ModificationDiff getModificationDiff(final DataTreeCandidateTip prepare) throws ReactorException {
        return new ModificationDiff.ModificationDiffBuilder()
                .setCtx(getSchemaCtx())
                .build(prepare.getRootNode());
    }

    NormalizedNodeUpdate getNormalizedNodeUpdateForAfterType(
            final Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates,
            final Class<? extends NormalizedNode<?, ?>> containerNodeClass) {
        return updates.values().stream()
                .filter(update -> containerNodeClass.isAssignableFrom(update.getDataAfter().getClass()))
                .findFirst().get();
    }

    NormalizedNodeUpdate getNormalizedNodeUpdateForBeforeType(
            final Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates,
            final Class<? extends NormalizedNode<?, ?>> containerNodeClass) {
        return updates.values().stream()
                .filter(update -> containerNodeClass.isAssignableFrom(update.getDataBefore().getClass()))
                .findFirst().get();
    }

    void assertUpdate(final NormalizedNodeUpdate update,
                      final YangInstanceIdentifier idExpected,
                      final NormalizedNode<?, ?> beforeExpected,
                      final NormalizedNode<?, ?> afterExpected) {
        assertThat(update.getId(), is(idExpected));
        assertThat(update.getDataBefore(), is(beforeExpected));
        assertThat(update.getDataAfter(), is(afterExpected));
    }


    ContainerNode getNestedContainerWithLeafList(final String... stringValue) {
        final ListNodeBuilder<String, LeafSetEntryNode<String>> leafSetBuilder = Builders.leafSetBuilder();
        for (final String value : stringValue) {
            leafSetBuilder.withChild(Builders.<String>leafSetEntryBuilder()
                    .withNodeIdentifier(new YangInstanceIdentifier.NodeWithValue<>(NESTED_CONTAINER_LEAF_LIST, value))
                    .withValue(value)
                    .build());
        }

        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_QNAME))
                .withChild(leafSetBuilder
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_LEAF_LIST))
                        .build())
                .build();
    }

    ContainerNode getNestedContainerWithChoice(final String caseLeafValue) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_QNAME))
                .withChild(Builders.choiceBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CHOICE))
                        .withChild(Builders.leafBuilder()
                                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(UNDER_NESTED_CASE))
                                .withValue(caseLeafValue)
                                .build()).build())
                .build();
    }

    MapEntryNode getNestedListEntry(final String listItemName, final String text) {
        return Builders.mapEntryBuilder()
                .withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifierWithPredicates(NESTED_LIST_QNAME,
                                NAME_LEAF_QNAME, listItemName))
                .withChild(ImmutableNodes.leafNode(NAME_LEAF_QNAME, listItemName))
                .withChild(ImmutableNodes.leafNode(TEXT_LEAF_QNAME, text))
                .build();
    }

    MapEntryNode getNestedListInContainerEntry(final String listItemName) {
        return Builders.mapEntryBuilder()
                .withNodeIdentifier(
                        new YangInstanceIdentifier.NodeIdentifierWithPredicates(NESTED_LIST_IN_CONTAINER_QNAME,
                                IN_CONTAINER_NAME_LEAF_QNAME, listItemName))
                .withChild(ImmutableNodes.leafNode(IN_CONTAINER_NAME_LEAF_QNAME, listItemName))
                .build();
    }

    MapNode getNestedList(MapEntryNode... entries) {
        return Builders.mapBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_QNAME))
                .withValue(Arrays.asList(entries))
                .build();
    }

    MapNode getNestedListInContainer(MapEntryNode... entries) {
        return Builders.mapBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_LIST_IN_CONTAINER_QNAME))
                .withValue(Arrays.asList(entries))
                .build();
    }

    ContainerNode getNestedContWithLeafUnderAug(String value) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_QNAME))
                .withChild(Builders.leafBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(AUG_LEAF))
                        .withValue(value).build()).build();
    }

    ContainerNode getNestedContWithListUnderAug(String value) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_QNAME))
                .withChild(Builders.mapBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(AUG_LIST))
                        .withChild(Builders.mapEntryBuilder()
                                .withNodeIdentifier(
                                        new YangInstanceIdentifier.NodeIdentifierWithPredicates(AUG_LIST, AUG_LIST_KEY,
                                                value))
                                .build())
                        .build()).build();
    }

    ContainerNode getNestedContWithContUnderAug(String value) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_QNAME))
                .withChild(Builders.containerBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(AUG_CONTAINER))
                        .withChild(Builders.leafBuilder()
                                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(AUG_CONTAINER_LEAF))
                                .withValue(value)
                                .build())
                        .build()).build();
    }

    ContainerNode getNestedContWithLeafListUnderAug(String value) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_QNAME))
                .withChild(Builders.leafSetBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(AUG_LEAFLIST))
                        .withChildValue(value)
                        .build()).build();
    }

    ContainerNode getNestedContWithContainerUnderNestedAug(String value) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_QNAME))
                .withChild(Builders.containerBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(AUG_CONTAINER))
                        .withChild(Builders.containerBuilder()
                                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_AUG_CONTAINER))
                                .withChild(Builders.leafBuilder()
                                        .withNodeIdentifier(
                                                new YangInstanceIdentifier.NodeIdentifier(NESTED_AUG_CONTAINER_LEAF))
                                        .withValue(value)
                                        .build()).build()).build()).build();
    }

    ContainerNode getNestedContWithLeafListUnderNestedAug(String value) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_QNAME))
                .withChild(Builders.containerBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(AUG_CONTAINER))
                        .withChild(Builders.leafSetBuilder()
                                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_AUG_LEAF_LIST))
                                .withChildValue(value)
                                .build()).build()).build();
    }

    ContainerNode getNestedContWithLeafUnderNestedAug(String value) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_QNAME))
                .withChild(Builders.containerBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(AUG_CONTAINER))
                        .withChild(Builders.leafBuilder()
                                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_AUG_LEAF))
                                .withValue(value)
                                .build()).build()).build();
    }

    ContainerNode getNestedContWithListUnderNestedAug(String value) {
        return Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_QNAME))
                .withChild(Builders.containerBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(AUG_CONTAINER))
                        .withChild(Builders.mapBuilder()
                                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_AUG_LIST))
                                .withChild(Builders.mapEntryBuilder()
                                        .withNodeIdentifier(
                                                new YangInstanceIdentifier.NodeIdentifierWithPredicates(NESTED_AUG_LIST,
                                                        NESTED_AUG_LIST_KEY, value)).build())
                                .build()).build()).build();
    }

    DataTree prepareStateBeforeWithTopContainer(final NormalizedNode<?, ?> topContainerData)
            throws ReactorException, DataValidationFailedException {
        final DataTree dataTree = getDataTree();
        final DataTreeModification dataTreeModificationOriginal = getModification(dataTree);
        // non presence, but with valid child list
        dataTreeModificationOriginal.write(TOP_CONTAINER_ID, topContainerData);
        final DataTreeCandidateTip prepare = prepareModification(dataTree, dataTreeModificationOriginal);
        dataTree.commit(prepare);
        return dataTree;
    }

    DataTreeCandidateTip prepareStateAfterEmpty(final DataTree dataTree)
            throws DataValidationFailedException {
        final NormalizedNode<?, ?> topContainerModified = Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .build();
        final DataTreeModification dataTreeModificationModified = getModification(dataTree);
        dataTreeModificationModified.write(TOP_CONTAINER_ID, topContainerModified);
        return prepareModification(dataTree, dataTreeModificationModified);
    }

    void assertCollectionContainsOnlyDeletes(final ModificationDiff modificationDiff) {
        assertTrue(modificationDiff.getUpdates().entrySet().stream().map(Map.Entry::getValue)
                .map(NormalizedNodeUpdate::getDataAfter)
                .filter(Objects::nonNull).collect(Collectors.toList()).isEmpty());
    }

    void assertNodeModificationPresent(final ModificationDiff modificationDiff, final Set<QName> expected) {
        assertTrue(modificationDiff.getUpdates().values().stream()
                .allMatch(update -> expected.contains(update.getId().getLastPathArgument().getNodeType())));
    }
}
