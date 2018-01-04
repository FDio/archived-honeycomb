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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

/**
 * Intermediate representation of a modification + its schema.
 */
final class Modification {
    private final YangInstanceIdentifier id;
    private final DataTreeCandidateNode dataCandidate;
    // Using Object as type for schema node since it's the only type that's a parent to all schema node types from
    // yangtools. The hierarchy does not use e.g. SchemaNode class for all types
    private final Object parentNode;
    private final Object schemaNode;
    private final boolean updateParentNode;

    private Modification(final YangInstanceIdentifier id,
                         final DataTreeCandidateNode dataCandidate,
                         final Object parentNode,
                         final Object schemaNode,
                         final boolean updateParentNode) {
        this.id = id;
        this.dataCandidate = dataCandidate;
        this.parentNode = parentNode;
        this.schemaNode = schemaNode;
        // controls process of updating parent node while moving down the schema tree:
        this.updateParentNode = updateParentNode;
    }

    Modification(final YangInstanceIdentifier id,
                 final DataTreeCandidateNode dataCandidate,
                 final Object parentNode,
                 final Object schemaNode) {
        this(id, dataCandidate, parentNode, schemaNode, true);
    }

    Modification(final YangInstanceIdentifier id,
                 final DataTreeCandidateNode dataCandidate,
                 final Object schemaNode) {
        this(id, dataCandidate, schemaNode, schemaNode);
    }

    List<Modification> getChildNodes() {
        return streamChildren().collect(Collectors.toList());
    }

    YangInstanceIdentifier getId() {
        return id;
    }

    ModificationType getModificationType() {
        return dataCandidate.getModificationType();
    }

    com.google.common.base.Optional<NormalizedNode<?, ?>> getDataBefore() {
        // FIXME switch to java.util.Optional when rest of ODL infra does
        return com.google.common.base.Optional.fromNullable(dataCandidate.getDataBefore().orElse(null));
    }

    com.google.common.base.Optional<NormalizedNode<?, ?>> getDataAfter() {
        // FIXME switch to java.util.Optional when rest of ODL infra does
        return com.google.common.base.Optional.fromNullable(dataCandidate.getDataAfter().orElse(null));
    }

    Object getSchemaNode() {
        return schemaNode;
    }

    boolean is(final Class<?> schemaType) {
        return schemaType.isAssignableFrom(schemaNode.getClass());
    }

    boolean isMixin() {
        // Checking whether node is a mixin is not performed on schema, but on data since mixin is
        // only a NormalizedNode concept, not a schema concept
        return dataCandidate.getDataBefore().orElse(null) instanceof MixinNode ||
                dataCandidate.getDataAfter().orElse(null) instanceof MixinNode;
    }

    boolean isBeforeAndAfterDifferent() {
        if (dataCandidate.getDataBefore().isPresent()) {
            return !dataCandidate.getDataBefore().get().equals(dataCandidate.getDataAfter().orElse(null));
        }

        // considering not a modification if data after is also null
        return dataCandidate.getDataAfter().isPresent();
    }

    private AugmentationSchemaNode findAugmentation(Object currentNode,
                                                    final YangInstanceIdentifier.AugmentationIdentifier identifier) {
        if (currentNode != null) {
            // check if identifier points to some augmentation of currentNode
            if (currentNode instanceof AugmentationTarget) {
                Optional<AugmentationSchemaNode> augmentationSchema =
                    ((AugmentationTarget) currentNode).getAvailableAugmentations().stream()
                        .filter(aug -> identifier.equals(new YangInstanceIdentifier.AugmentationIdentifier(
                            aug.getChildNodes().stream()
                                .map(SchemaNode::getQName)
                                .collect(Collectors.toSet()))))
                        .findFirst();
                if (augmentationSchema.isPresent()) {
                    return augmentationSchema.get();
                }
            }

            // continue search:
            Collection<DataSchemaNode> childNodes = Collections.emptyList();
            if (currentNode instanceof DataNodeContainer) {
                childNodes = ((DataNodeContainer) currentNode).getChildNodes();
            } else if (currentNode instanceof ChoiceSchemaNode) {
                childNodes = ((ChoiceSchemaNode) currentNode).getCases().values().stream()
                    .flatMap(cas -> cas.getChildNodes().stream()).collect(Collectors.toList());
            }
            return childNodes.stream().map(n -> findAugmentation(n, identifier)).filter(n -> n != null).findFirst()
                .orElse(null);
        } else {
            return null;
        }
    }

    Stream<Modification> streamChildren() {
        return dataCandidate.getChildNodes().stream()
            .map(child -> {
                final YangInstanceIdentifier childId = id.node(child.getIdentifier());
                final Object schemaChild = schemaChild(schemaNode, child.getIdentifier());

                // An augment cannot change other augment, so we do not update parent node if we are streaming
                // children of AugmentationSchema (otherwise we would fail to find schema for nested augmentations):
                if (updateParentNode) {
                    if (schemaNode instanceof AugmentationSchemaNode) {
                        // child nodes would not have nested augmentations, so we stop moving parentNode:
                        return new Modification(childId, child, parentNode, schemaChild, false);
                    } else {
                        // update parent node:
                        return new Modification(childId, child, schemaNode, schemaChild, true);
                    }
                }
                return new Modification(childId, child, parentNode, schemaChild, updateParentNode);
            });
    }

    /**
     * Find next schema node in hierarchy.
     */
    private Object schemaChild(final Object schemaNode, final YangInstanceIdentifier.PathArgument identifier) {
        Object found = null;

        if (identifier instanceof YangInstanceIdentifier.AugmentationIdentifier) {
            if (schemaNode instanceof AugmentationTarget) {
                // Find matching augmentation
                found = ((AugmentationTarget) schemaNode).getAvailableAugmentations().stream()
                    .filter(aug -> identifier.equals(new YangInstanceIdentifier.AugmentationIdentifier(
                        aug.getChildNodes().stream()
                            .map(SchemaNode::getQName)
                            .collect(Collectors.toSet()))))
                    .findFirst()
                    .orElse(null);

                if (found == null) {
                    // An augment cannot change other augment, but all augments only change their targets (data nodes).
                    //
                    // As a consequence, if nested augmentations are present,
                    // AugmentationSchema might reference child schema node instances that do not include changes
                    // from nested augments.
                    //
                    // But schemaNode, as mentioned earlier, contains all the changes introduced by augments.
                    //
                    // On the other hand, in case of augments which introduce leaves,
                    // we need to address AugmentationSchema node directly so we can't simply do
                    // found = schemaNode;
                    //
                    found =
                        findAugmentation(parentNode, (YangInstanceIdentifier.AugmentationIdentifier) identifier);
                }
            }
        } else if (schemaNode instanceof DataNodeContainer) {
            // Special handling for list aggregator nodes. If we are at list aggregator node e.g. MapNode and
            // we are searching for schema for a list entry e.g. MapEntryNode just return the same schema
            if (schemaNode instanceof ListSchemaNode &&
                ((SchemaNode) schemaNode).getQName().equals(identifier.getNodeType())) {
                found = schemaNode;
            } else {
                found = ((DataNodeContainer) schemaNode).getDataChildByName(identifier.getNodeType());
            }
        } else if (schemaNode instanceof ChoiceSchemaNode) {
            // For choices, iterate through all the cases
            final Optional<DataSchemaNode> maybeChild = ((ChoiceSchemaNode) schemaNode).getCases().values().stream()
                    .flatMap(cas -> cas.getChildNodes().stream())
                    .filter(child -> child.getQName().equals(identifier.getNodeType()))
                    .findFirst();
            if (maybeChild.isPresent()) {
                found = maybeChild.get();
            }
            // Special handling for leaf-list nodes. Basically the same as is for list mixin nodes
        } else if (schemaNode instanceof LeafListSchemaNode &&
            ((SchemaNode) schemaNode).getQName().equals(identifier.getNodeType())) {
            found = schemaNode;
        }

        return checkNotNull(found, "Unable to find child node in: %s identifiable by: %s", schemaNode, identifier);
    }

    @Override
    public String toString() {
        return "Modification{" +
                "id=" + id +
                '}';
    }
}
