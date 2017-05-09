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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TipProducingDataTree;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModificationDiffNestedAugRewriteDeleteTest extends ModificationBaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(ModificationDiffNestedAugRewriteDeleteTest.class);

    @Test
    public void testWriteNonPresenceNonEmptyContainerNestedAugWithContainer()
            throws DataValidationFailedException, ReactorException {
        final TipProducingDataTree dataTree = prepareStateBeforeWithTopContainer(Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .withChild(getNestedContWithContainerUnderNestedAug("val"))
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
        assertNodeModificationPresent(modificationDiff, ImmutableSet.of(NESTED_AUG_CONTAINER_LEAF));
    }

    @Test
    public void testWriteNonPresenceNonEmptyContainerNestedAugWithLeafList()
            throws DataValidationFailedException, ReactorException {
        final TipProducingDataTree dataTree = prepareStateBeforeWithTopContainer(Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .withChild(getNestedContWithLeafListUnderNestedAug("val"))
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
        assertNodeModificationPresent(modificationDiff, ImmutableSet.of(NESTED_AUG_LEAF_LIST));
    }

    @Test
    public void testWriteNonPresenceNonEmptyContainerNestedAugWithList()
            throws DataValidationFailedException, ReactorException {
        final TipProducingDataTree dataTree = prepareStateBeforeWithTopContainer(Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .withChild(getNestedContWithListUnderNestedAug("val"))
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
        assertNodeModificationPresent(modificationDiff, ImmutableSet.of(NESTED_AUG_LIST));
    }

    @Test
    public void testWriteNonPresenceNonEmptyContainerNestedAugWithLeaf()
            throws DataValidationFailedException, ReactorException {
        final TipProducingDataTree dataTree = prepareStateBeforeWithTopContainer(Builders.containerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TOP_CONTAINER_QNAME))
                .withChild(getNestedContWithLeafUnderNestedAug("val"))
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
        assertNodeModificationPresent(modificationDiff, ImmutableSet.of(NESTED_AUG_LEAF));
    }
}
