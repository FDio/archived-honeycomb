/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import io.fd.honeycomb.data.DataModification;
import io.fd.honeycomb.subtree.test.model.Ids;
import io.fd.honeycomb.translate.impl.write.registry.FlatWriterRegistryBuilder;
import io.fd.honeycomb.translate.util.YangDAG;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.Writer;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import java.util.Map;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.subtree.test.rev180116.C1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.subtree.test.rev180116.C1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.subtree.test.rev180116.c1.C2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.subtree.test.rev180116.c1.C2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.subtree.test.rev180116.c1.C3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.subtree.test.rev180116.c1.C3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.subtree.test.rev180116.c1.C4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.subtree.test.rev180116.c1.C4Builder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

/**
 * Testing honeycomb writes from data tree up to mocked writers.
 */
public final class SubtreeWriteTest extends AbstractInfraTest {

    private DataTree dataTree;

    @Mock
    private Writer<C1> c1Writer;

    @Override
    void postSetup() {
        dataTree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_CONFIGURATION);
        dataTree.setSchemaContext(schemaContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMissingWriter() throws Exception {
        when(c1Writer.getManagedDataObjectType()).thenReturn(Ids.C1_ID);

        final WriterRegistry writerRegistry = new FlatWriterRegistryBuilder(new YangDAG())
            .subtreeAdd(Sets.newHashSet(Ids.C2_ID, Ids.C3_ID), c1Writer)
            .build();

        // Prepare modification for C4
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
            new ModifiableDataTreeDelegator(serializer, dataTree, schemaContext, writerRegistry, contextBroker);
        final C4 c4 = new C4Builder().setLeaf4(4).build();
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> c4NormalizedNode =
            serializer.toNormalizedNode(Ids.C4_ID, c4);
        final DataModification dataModification = modifiableDataTreeDelegator.newModification();
        dataModification.write(c4NormalizedNode.getKey(), c4NormalizedNode.getValue());

        // Commit modification and fail with missing writer for C4
        dataModification.commit();
    }

    @Test
    public void testWrite() throws Exception {
        when(c1Writer.getManagedDataObjectType()).thenReturn(Ids.C1_ID);

        final WriterRegistry writerRegistry = new FlatWriterRegistryBuilder(new YangDAG())
            .subtreeAdd(Sets.newHashSet(Ids.C2_ID, Ids.C3_ID), c1Writer)
            .build();

        // Prepare modification for C1 and C2 containers
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
            new ModifiableDataTreeDelegator(serializer, dataTree, schemaContext, writerRegistry, contextBroker);
        final C2 c2 = new C2Builder().setLeaf2(2).build();
        final C3 c3 = new C3Builder().setLeaf3(3).build();
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> c2NormalizedNode =
            serializer.toNormalizedNode(Ids.C2_ID, c2);
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> c3NormalizedNode =
            serializer.toNormalizedNode(Ids.C3_ID, c3);

        // Create data
        final DataModification dataModification = modifiableDataTreeDelegator.newModification();
        dataModification.write(c2NormalizedNode.getKey(), c2NormalizedNode.getValue());
        dataModification.write(c3NormalizedNode.getKey(), c3NormalizedNode.getValue());
        dataModification.commit();

        // Check if updates for two child containers are wrapped into single update
        verify(c1Writer).processModification(any(), any(), any(), any());

        // Verify that writer received create modification for C1 with C2 and C3 included
        final C1 expectedC1 = new C1Builder().setC2(c2).setC3(c3).build();
        verify(c1Writer).processModification(eq(Ids.C1_ID), eq(null), eq(expectedC1), any(WriteContext.class));
    }

    @Test
    public void testDelete() throws Exception {
        when(c1Writer.getManagedDataObjectType()).thenReturn(Ids.C1_ID);

        final WriterRegistry writerRegistry = new FlatWriterRegistryBuilder(new YangDAG())
            .subtreeAdd(Sets.newHashSet(Ids.C2_ID, Ids.C3_ID), c1Writer)
            .build();

        // Prepare C1 with leaf1 and C1, C2 containers
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
            new ModifiableDataTreeDelegator(serializer, dataTree, schemaContext, writerRegistry, contextBroker);
        final C2 c2 = new C2Builder().setLeaf2(2).build();
        final C3 c3 = new C3Builder().setLeaf3(3).build();
        final C1 c1 = new C1Builder().setC2(c2).setC3(c3).setLeaf1("some-value").build();
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> c1NormalizedNode =
            serializer.toNormalizedNode(Ids.C1_ID, c1);

        // Create data
        DataModification dataModification = modifiableDataTreeDelegator.newModification();
        dataModification.write(c1NormalizedNode.getKey(), c1NormalizedNode.getValue());
        dataModification.commit();
        verify(c1Writer).processModification(eq(Ids.C1_ID), eq(null), eq(c1), any(WriteContext.class));

        // Now delete C1 and C2
        dataModification = modifiableDataTreeDelegator.newModification();
        dataModification.delete(serializer.toYangInstanceIdentifier(Ids.C2_ID));
        dataModification.delete(serializer.toYangInstanceIdentifier(Ids.C3_ID));
        dataModification.commit();

        // Check that in total, there were 2 invocations of processModification
        verify(c1Writer, times(2)).processModification(any(), any(), any(), any());

        // First create for C1
        final InOrder inOrder = Mockito.inOrder(c1Writer);
        inOrder.verify(c1Writer).processModification(eq(Ids.C1_ID), eq(null), eq(c1), any(WriteContext.class));

        // Then delete for C2 and C3, but wrapped in C1 update
        final C1 c1WithoutC2AndC3 = new C1Builder().setLeaf1("some-value").build();
        inOrder.verify(c1Writer)
            .processModification(eq(Ids.C1_ID), eq(c1), eq(c1WithoutC2AndC3), any(WriteContext.class));
    }

    @Test
    public void testUpdate() throws Exception {
        when(c1Writer.getManagedDataObjectType()).thenReturn(Ids.C1_ID);

        final WriterRegistry writerRegistry = new FlatWriterRegistryBuilder(new YangDAG())
            .subtreeAdd(Sets.newHashSet(Ids.C2_ID, Ids.C3_ID), c1Writer)
            .build();

        // Prepare C1 with leaf1 and C1, C2 containers
        final ModifiableDataTreeDelegator modifiableDataTreeDelegator =
            new ModifiableDataTreeDelegator(serializer, dataTree, schemaContext, writerRegistry, contextBroker);
        final C2 c2 = new C2Builder().setLeaf2(2).build();
        final C3 c3 = new C3Builder().setLeaf3(3).build();
        final C1 c1 = new C1Builder().setC2(c2).setC3(c3).build();
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> c1NormalizedNode =
            serializer.toNormalizedNode(Ids.C1_ID, c1);

        // Create data
        DataModification dataModification = modifiableDataTreeDelegator.newModification();
        dataModification.write(c1NormalizedNode.getKey(), c1NormalizedNode.getValue());
        dataModification.commit();
        verify(c1Writer).processModification(eq(Ids.C1_ID), eq(null), eq(c1), any(WriteContext.class));

        // Now change C2
        final C2 modifiedC2 = new C2Builder().setLeaf2(22).build();
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> c2NormalizedNode =
            serializer.toNormalizedNode(Ids.C2_ID, modifiedC2);
        dataModification = modifiableDataTreeDelegator.newModification();
        dataModification.write(c2NormalizedNode.getKey(), c2NormalizedNode.getValue());
        dataModification.commit();

        // Check that in total, there were 2 invocations of processModification
        // TODO (HONEYCOMB-422)
        // verify(c1Writer, times(2)).processModification(any(), any(), any(), any());
        verify(c1Writer, times(3)).processModification(any(), any(), any(), any());

        // First create for C1
        final InOrder inOrder = Mockito.inOrder(c1Writer);
        inOrder.verify(c1Writer).processModification(eq(Ids.C1_ID), eq(null), eq(c1), any(WriteContext.class));

        // Then delete and create for C2, because c1Writer does not support direct updates
        final C1 modifiedC1 = new C1Builder().setC2(modifiedC2).setC3(c3).build();
        // TODO (HONEYCOMB-422)
        // inOrder.verify(c1Writer).processModification(eq(Ids.C1_ID), eq(c1), eq(modifiedC1), any(WriteContext.class));
        inOrder.verify(c1Writer, times(2))
            .processModification(eq(Ids.C1_ID), eq(c1), eq(modifiedC1), any(WriteContext.class));
    }
}