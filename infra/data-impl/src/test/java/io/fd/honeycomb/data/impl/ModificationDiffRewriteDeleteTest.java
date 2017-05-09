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
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModificationDiffRewriteDeleteTest extends ModificationBaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(ModificationDiffRewriteDeleteTest.class);

    /**
     * Covers case when non-presence container was already filled with some data,
     * and modification removes data by overriding by empty list
     */
    @Test
    public void testWriteNonPresenceNonEmptyContainerPreviousDataOverrideByEmpty() throws Exception {
        final MapEntryNode alreadyPresent = getNestedListEntry("value", "txt");
        final TipProducingDataTree dataTree = prepareStateBeforeWithTopContainer(Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .withChild(getNestedList(alreadyPresent))
                .build());
        // now we have state with some data

        // just empty non presence container
        final DataTreeCandidateTip prepareModified = prepareStateAfterEmpty(dataTree);

        final ModificationDiff modificationDiff = getModificationDiff(prepareModified);
        // should detect one new entry
        LOG.debug(modificationDiff.toString());
        assertThat(modificationDiff.getUpdates().size(), is(1));
        // is delete
        assertCollectionContainsOnlyDeletes(modificationDiff);
        assertNodeModificationPresent(modificationDiff, ImmutableSet.of(NESTED_LIST_QNAME));
    }

    /**
     * Covers case when non-presence container was already filled with some data,
     * and modification removes data by overriding by empty list. This case tests
     * when there are multiple nested non-presence containers
     */
    @Test
    public void testWriteNonPresenceMultipleNonEmptyContainerPreviousDataOverrideByEmpty() throws Exception {
        final MapEntryNode alreadyPresent = getNestedListInContainerEntry("key");
        final TipProducingDataTree dataTree = prepareStateBeforeWithTopContainer(Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                // another non-presence container with list entry
                .withChild(Builders.containerBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_QNAME))
                        .withChild(getNestedListInContainer(alreadyPresent))
                        .build())
                // direct list entry
                .withChild(getNestedList(alreadyPresent))
                .build());
        // now we have state with some data

        // just empty non presence container
        final DataTreeCandidateTip prepareModified = prepareStateAfterEmpty(dataTree);

        final ModificationDiff modificationDiff = getModificationDiff(prepareModified);
        // should detect two new entries
        LOG.debug(modificationDiff.toString());
        assertThat(modificationDiff.getUpdates().size(), is(2));
        // is delete
        assertCollectionContainsOnlyDeletes(modificationDiff);
        assertNodeModificationPresent(modificationDiff,
                ImmutableSet.of(NESTED_LIST_IN_CONTAINER_QNAME, NESTED_LIST_QNAME));
    }

    /**
     * Covers case when non-presence container was already filled with some data,
     * and modification removes data by overriding. Tests case with leaf
     */
    @Test
    public void testWriteNonPresenceNonEmptyContainerLeaf() throws Exception {
        final TipProducingDataTree dataTree = prepareStateBeforeWithTopContainer(Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                // another non-presence container with leaf
                .withChild(Builders.containerBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_QNAME))
                        .withChild(Builders.leafBuilder()
                                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NESTED_CONTAINER_VAL))
                                .withValue("val").build())
                        .build())
                .build());
        // now we have state with some data

        // just empty non presence container
        final DataTreeCandidateTip prepareModified = prepareStateAfterEmpty(dataTree);

        final ModificationDiff modificationDiff = getModificationDiff(prepareModified);
        // should detect one new entry
        LOG.debug(modificationDiff.toString());
        assertThat(modificationDiff.getUpdates().size(), is(1));
        // is delete
        assertCollectionContainsOnlyDeletes(modificationDiff);
        assertNodeModificationPresent(modificationDiff, ImmutableSet.of(NESTED_CONTAINER_VAL));
    }

    /**
     * Covers case when non-presence container was already filled with some data,
     * and modification removes data by overriding. Tests case with leaf-list
     */
    @Test
    public void testWriteNonPresenceNonEmptyContainerLeafList() throws Exception {
        final TipProducingDataTree dataTree = prepareStateBeforeWithTopContainer(Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                // another non-presence container with leaf
                .withChild(getNestedContainerWithLeafList())
                .build());
        // now we have state with some data

        // just empty non presence container
        final DataTreeCandidateTip prepareModified = prepareStateAfterEmpty(dataTree);

        final ModificationDiff modificationDiff = getModificationDiff(prepareModified);
        // should detect one new entry
        LOG.debug(modificationDiff.toString());
        assertThat(modificationDiff.getUpdates().size(), is(1));
        // is delete
        assertCollectionContainsOnlyDeletes(modificationDiff);
        assertNodeModificationPresent(modificationDiff, ImmutableSet.of(NESTED_CONTAINER_LEAF_LIST));
    }

    /**
     * Covers case when non-presence container was already filled with some data,
     * and modification removes data by overriding. Tests case with choice/case
     */
    @Test
    public void testWriteNonPresenceNonEmptyContainerWithChoice() throws Exception {
        final TipProducingDataTree dataTree = prepareStateBeforeWithTopContainer(Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                // another non-presence container with leaf
                .withChild(getNestedContainerWithChoice("val"))
                .build());
        // now we have state with some data

        // just empty non presence container
        final DataTreeCandidateTip prepareModified = prepareStateAfterEmpty(dataTree);

        final ModificationDiff modificationDiff = getModificationDiff(prepareModified);
        // should detect one new entry
        LOG.debug(modificationDiff.toString());
        assertThat(modificationDiff.getUpdates().size(), is(1));
        // is delete
        assertCollectionContainsOnlyDeletes(modificationDiff);
        assertNodeModificationPresent(modificationDiff, ImmutableSet.of(UNDER_NESTED_CASE));
    }
}
