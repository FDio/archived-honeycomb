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

package io.fd.honeycomb.translate.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class YangDAGTest {

    private static final InstanceIdentifier<DataObjects.DataObject1> VERTEX_A = DataObjects.DataObject1.IID;
    private static final InstanceIdentifier<DataObjects.DataObject2> VERTEX_B = DataObjects.DataObject2.IID;

    private YangDAG dag;

    @Before
    public void setUp() {
        dag = new YangDAG();
    }
    @Test
    public void testAddVertex() {
        dag.addVertex(VERTEX_A);
        final Iterator<InstanceIdentifier<?>> it = dag.iterator();
        assertEquals(VERTEX_A, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testAddEdge() {
        dag.addVertex(VERTEX_A);
        dag.addVertex(VERTEX_B);
        dag.addEdge(VERTEX_A, VERTEX_B);
        final Iterator<InstanceIdentifier<?>> it = dag.iterator();
        assertEquals(VERTEX_A, it.next());
        assertEquals(VERTEX_B, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testAddCycleFails() {
        dag.addVertex(VERTEX_A);
        dag.addVertex(VERTEX_B);
        dag.addEdge(VERTEX_A, VERTEX_B);
        try {
            dag.addEdge(VERTEX_B, VERTEX_A);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getCause() instanceof DirectedAcyclicGraph.CycleFoundException);
        }
    }
}