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

package io.fd.honeycomb.translate.impl.write.registry;

import io.fd.honeycomb.translate.write.Writer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.AugTarget;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.FromAugmentAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.FromAugmentListAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.SimpleNestedAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.aug.target.FromAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev161222.aug.target.from.augment.FromAugmentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.choice.choice.c3.C3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.ContainerInList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.container.in.list.NestedList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WildcardedSubtreeWriterTest {

    private static final InstanceIdentifier<ContainerWithList> C_WITH_LIST = InstanceIdentifier.create(ContainerWithList.class);
    private static final InstanceIdentifier<ContainerWithChoice> C_WITH_CHOICE = InstanceIdentifier.create(ContainerWithChoice.class);
    private static final InstanceIdentifier<AugTarget> C_AUG = InstanceIdentifier.create(AugTarget.class);

    private static final InstanceIdentifier<ListInContainer> L_IN_CONTAINER = C_WITH_LIST.child(ListInContainer.class);
    private static final InstanceIdentifier<ContainerInList> C_IN_LIST = L_IN_CONTAINER.child(ContainerInList.class);

    private static final InstanceIdentifier<NestedList> N_LIST = C_IN_LIST.child(NestedList.class);

    private Writer subtreeContainerWithList;
    private Writer subtreeContainerWithChoice;
    private Writer subtreeAugTarget;


    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        Writer<ContainerWithList> writerContainerWithList = mock(Writer.class);
        Writer<ContainerWithChoice> writerContainerWithChoice = mock(Writer.class);
        Writer<AugTarget> writerAugTarget = mock(Writer.class);
        when(writerContainerWithList.getManagedDataObjectType()).thenReturn(C_WITH_LIST);
        when(writerContainerWithChoice.getManagedDataObjectType()).thenReturn(C_WITH_CHOICE);
        when(writerAugTarget.getManagedDataObjectType()).thenReturn(C_AUG);
        subtreeContainerWithList = SubtreeWriter.createWildcardedForWriter(writerContainerWithList);
        subtreeContainerWithChoice = SubtreeWriter.createWildcardedForWriter(writerContainerWithChoice);
        subtreeAugTarget = SubtreeWriter.createWildcardedForWriter(writerAugTarget);
    }

    @Test
    public void testParent() {
        assertTrue(subtreeContainerWithList.canProcess(C_WITH_LIST));
        assertFalse(subtreeContainerWithList.canProcess(C_WITH_CHOICE));

        assertTrue(subtreeContainerWithChoice.canProcess(C_WITH_CHOICE));
        assertFalse(subtreeContainerWithChoice.canProcess(C_WITH_LIST));

        assertTrue(subtreeAugTarget.canProcess(C_AUG));
        assertFalse(subtreeAugTarget.canProcess(C_WITH_LIST));
    }

    @Test
    public void testDirectChild() {
        assertTrue(subtreeContainerWithList.canProcess(L_IN_CONTAINER));
        assertFalse(subtreeContainerWithList.canProcess(C_WITH_CHOICE.child(C3.class)));

        assertTrue(subtreeContainerWithChoice.canProcess(C_WITH_CHOICE.child(C3.class)));
        assertFalse(subtreeContainerWithChoice.canProcess(L_IN_CONTAINER));
    }

    @Test
    public void testIndirectChild() {
        assertTrue(subtreeContainerWithList.canProcess(C_IN_LIST));
        assertTrue(subtreeContainerWithList.canProcess(N_LIST));
    }

    @Test
    public void testAugDirectChild() {
        assertTrue(subtreeAugTarget.canProcess(C_AUG.augmentation(FromAugmentAugment.class).child(FromAugment.class)));
        assertFalse(subtreeContainerWithList.canProcess(C_AUG.augmentation(FromAugmentAugment.class).child(FromAugment.class)));
    }

    @Test
    public void testAugIndirectChild() {
        assertTrue(subtreeAugTarget.canProcess(C_AUG.augmentation(FromAugmentAugment.class)
                .child(FromAugment.class)
                .augmentation(SimpleNestedAugment.class)));
        assertFalse(subtreeContainerWithList.canProcess(C_AUG.augmentation(FromAugmentAugment.class)
                .child(FromAugment.class)
                .augmentation(FromAugmentListAugment.class)
                .child(FromAugmentEntry.class)));
    }
}
