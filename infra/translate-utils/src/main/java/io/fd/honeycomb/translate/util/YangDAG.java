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

import java.util.Iterator;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Directed acyclic graph representing ordering relationships between schema nodes represented as wildcarded
 * InstanceIdentifiers. Maintains topological order of vertices. Feature is used by translation layer if more nodes are
 * affected in single read/write transaction.
 */
public final class YangDAG {

    private final DirectedAcyclicGraph<InstanceIdentifier<?>, Edge>
        dag = new DirectedAcyclicGraph<>((sourceVertex, targetVertex) -> new Edge());

    /**
     * Adds the vertex if it wasn't already in the graph.
     *
     * @param vertex vertex to be added
     */
    public void addVertex(final InstanceIdentifier<?> vertex) {
        dag.addVertex(vertex);
    }

    /**
     * Adds edge between source and target vertices.
     *
     * @param source source vertex of the edge
     * @param target target vertex of the edge
     * @throws IllegalArgumentException if the edge would induce a cycle in the graph
     */
    public void addEdge(final InstanceIdentifier<?> source, final InstanceIdentifier<?> target) {
        try {
            dag.addDagEdge(source, target);
        } catch (DirectedAcyclicGraph.CycleFoundException e) {
            throw new IllegalArgumentException(String.format(
                "Unable to add writer with relation: %s -> %s. Loop detected", source, target), e);
        }
    }

    /**
     * Traverses schema nodes in topological order.
     *
     * @return an iterator that will traverse the graph in topological order.
     */
    public Iterator<InstanceIdentifier<?>> iterator() {
        return dag.iterator();
    }

    private static final class Edge {
    }
}
