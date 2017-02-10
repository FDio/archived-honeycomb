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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType.APPEARED;
import static org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType.DELETE;
import static org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType.DISAPPEARED;
import static org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType.WRITE;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.AugmentationTarget;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recursively collects and provides all unique and non-null modifications (modified normalized nodes).
 */
final class ModificationDiff {

    private static final Logger LOG = LoggerFactory.getLogger(ModificationDiff.class);

    private static final ModificationDiff EMPTY_DIFF = new ModificationDiff(Collections.emptyMap());
    private static final EnumSet VALID_MODIFICATIONS = EnumSet.of(WRITE, DELETE);
    private static final EnumSet IGNORED_MODIFICATIONS = EnumSet.of(APPEARED, DISAPPEARED);

    private final Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates;

    private ModificationDiff(@Nonnull Map<YangInstanceIdentifier, NormalizedNodeUpdate> updates) {
        this.updates = updates;
    }

    /**
     * Get processed modifications.
     *
     * @return mapped modifications, where key is keyed {@link YangInstanceIdentifier}.
     */
    Map<YangInstanceIdentifier, NormalizedNodeUpdate> getUpdates() {
        return updates;
    }

    private ModificationDiff merge(final ModificationDiff other) {
        if (this == EMPTY_DIFF) {
            return other;
        }

        if (other == EMPTY_DIFF) {
            return this;
        }

        return new ModificationDiff(join(updates, other.updates));
    }

    private static Map<YangInstanceIdentifier, NormalizedNodeUpdate> join(
            Map<YangInstanceIdentifier, NormalizedNodeUpdate> first,
            Map<YangInstanceIdentifier, NormalizedNodeUpdate> second) {
        final Map<YangInstanceIdentifier, NormalizedNodeUpdate> merged = new HashMap<>();
        merged.putAll(first);
        merged.putAll(second);
        return merged;
    }

    private static ModificationDiff create(Modification modification) {
        return new ModificationDiff(ImmutableMap.of(modification.getId(), NormalizedNodeUpdate.create(modification)));
    }

    /**
     * Produce an aggregated diff from a candidate node recursively. MixinNodes are ignored as modifications and so
     * are complex nodes which direct leaves were not modified.
     */
    @Nonnull
    static ModificationDiff recursivelyFromCandidate(@Nonnull final Modification modification) {
        // recursively process child nodes for exact modifications
        return recursivelyChildrenFromCandidate(modification)
                // also add modification on current level, if elligible
                .merge(isModification(modification)
                        ? ModificationDiff.create(modification)
                        : EMPTY_DIFF);
    }

    /**
     * Same as {@link #recursivelyFromCandidate(Modification)} but does
     * not process the root node for modifications, since it's the artificial data root, that has no child leaves but
     * always is marked as SUBTREE_MODIFIED.
     */
    @Nonnull
    static ModificationDiff recursivelyFromCandidateRoot(@Nonnull final DataTreeCandidateNode currentCandidate,
                                                         @Nonnull final SchemaContext ctx) {
        return recursivelyChildrenFromCandidate(new Modification(YangInstanceIdentifier.EMPTY, currentCandidate, ctx));
    }

    /**
     * Check whether current node was modified. {@link MixinNode}s are ignored
     * and only nodes which direct leaves(or choices) are modified are considered a modification.
     */
    private static Boolean isModification(@Nonnull final Modification modification) {
        // Disappear is not a modification
        if (IGNORED_MODIFICATIONS.contains(modification.getModificationType())) {
            return false;
        // Mixin nodes are not considered modifications
        } else if (modification.isMixin() && !modification.is(AugmentationSchema.class)) {
            return false;
        } else {
            return isCurrentModified(modification);
        }
    }

    private static Boolean isCurrentModified(@Nonnull final Modification modification) {
        // First check if it's an empty presence node
        final boolean emptyPresenceNode = isEmptyPresenceNode(modification);

        // Check if there are any modified leaves and if so, consider current node as modified
        final Boolean directLeavesModified = emptyPresenceNode
                || modification.streamChildren()
                // Checking leaf or leaf-lists children for direct modification, which means that leafs of leaf lists
                // trigger a modification on parent node
                .filter(child -> child.is(LeafSchemaNode.class) || child.is(LeafListSchemaNode.class))
                // For some reason, we get modifications on unmodified list keys
                // and that messes up our modifications collection here, so we need to skip
                .filter(Modification::isBeforeAndAfterDifferent)
                .filter(child -> VALID_MODIFICATIONS.contains(child.getModificationType()))
                .findFirst()
                .isPresent();

        // Also as fallback check choices (choices do not exist in BA world and if anything within a choice was modified,
        // consider its parent as being modified)
        final boolean modified = directLeavesModified
                || modification.streamChildren()
                .filter(child -> child.is(ChoiceSchemaNode.class))
                // Recursively check each choice if there was any change to it
                .filter(ModificationDiff::isCurrentModified)
                .findFirst()
                .isPresent();

        if (modified) {
            LOG.debug("Modification detected as {} at {}",
                    modification.getModificationType(), modification.getId());
        }

        return modified;
    }

    /**
     * Check if new data are empty but still to be considered as a modification, meaning it's presence has a meaning
     * e.g. containers with presence statement.
     */
    private static boolean isEmptyPresenceNode(@Nonnull final Modification modification) {
        return modification.is(ContainerSchemaNode.class)
                && ((ContainerSchemaNode) modification.getSchemaNode()).isPresenceContainer()
                && modification.getChildNodes().isEmpty()
                && VALID_MODIFICATIONS.contains(modification.getModificationType());
    }

    /**
     * Process all non-leaf child nodes recursively, creating aggregated {@link ModificationDiff}.
     */
    private static ModificationDiff recursivelyChildrenFromCandidate(@Nonnull final Modification modification) {
        // recursively process child nodes for specific modifications
        return modification.streamChildren()
                .filter(child -> !child.is(LeafSchemaNode.class))
                .map(ModificationDiff::recursivelyFromCandidate)
                .reduce(ModificationDiff::merge)
                .orElse(EMPTY_DIFF);
    }

    @Override
    public String toString() {
        return "ModificationDiff{updates=" + updates + '}';
    }

    /**
     * Update to a normalized node identifiable by its {@link YangInstanceIdentifier}.
     */
    static final class NormalizedNodeUpdate {

        @Nonnull
        private final YangInstanceIdentifier id;
        @Nullable
        private final NormalizedNode<?, ?> dataBefore;
        @Nullable
        private final NormalizedNode<?, ?> dataAfter;

        private NormalizedNodeUpdate(@Nonnull final YangInstanceIdentifier id,
                                     @Nullable final NormalizedNode<?, ?> dataBefore,
                                     @Nullable final NormalizedNode<?, ?> dataAfter) {
            this.id = checkNotNull(id);
            this.dataAfter = dataAfter;
            this.dataBefore = dataBefore;
        }

        @Nullable
        public NormalizedNode<?, ?> getDataBefore() {
            return dataBefore;
        }

        @Nullable
        public NormalizedNode<?, ?> getDataAfter() {
            return dataAfter;
        }

        @Nonnull
        public YangInstanceIdentifier getId() {
            return id;
        }

        static NormalizedNodeUpdate create(@Nonnull final Modification modification) {
            final com.google.common.base.Optional<NormalizedNode<?, ?>> beforeData =
                    modification.getDataBefore();
            final com.google.common.base.Optional<NormalizedNode<?, ?>> afterData =
                    modification.getDataAfter();
            checkArgument(beforeData.isPresent() || afterData.isPresent(),
                    "Both before and after data are null for %s", modification.getId());
            return NormalizedNodeUpdate.create(modification.getId(), beforeData.orNull(), afterData.orNull());
        }

        static NormalizedNodeUpdate create(@Nonnull final YangInstanceIdentifier id,
                                           @Nullable final NormalizedNode<?, ?> dataBefore,
                                           @Nullable final NormalizedNode<?, ?> dataAfter) {
            return new NormalizedNodeUpdate(id, dataBefore, dataAfter);
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            final NormalizedNodeUpdate that = (NormalizedNodeUpdate) other;

            return id.equals(that.id);

        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return "NormalizedNodeUpdate{" + "id=" + id
                    + ", dataBefore=" + dataBefore
                    + ", dataAfter=" + dataAfter
                    + '}';
        }
    }

    /**
     * Intermediate representation of a modification + its schema.
     */
    private static final class Modification {
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
            return dataCandidate.getDataBefore();
        }

        com.google.common.base.Optional<NormalizedNode<?, ?>> getDataAfter() {
            return dataCandidate.getDataAfter();
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
            return dataCandidate.getDataBefore().orNull() instanceof MixinNode ||
                    dataCandidate.getDataAfter().orNull() instanceof MixinNode;
        }

        private boolean isBeforeAndAfterDifferent() {
            if (dataCandidate.getDataBefore().isPresent()) {
                return !dataCandidate.getDataBefore().get().equals(dataCandidate.getDataAfter().orNull());
            }

            // considering not a modification if data after is also null
            return dataCandidate.getDataAfter().isPresent();
        }

        private AugmentationSchema findAugmentation(Object currentNode,
                                                    final YangInstanceIdentifier.AugmentationIdentifier identifier) {
            if (currentNode != null) {
                // check if identifier points to some augmentation of currentNode
                if (currentNode instanceof AugmentationTarget) {
                    Optional<AugmentationSchema> augmentationSchema =
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
                    childNodes = ((ChoiceSchemaNode) currentNode).getCases().stream()
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
                        if (schemaNode instanceof AugmentationSchema) {
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
                final Optional<DataSchemaNode> maybeChild = ((ChoiceSchemaNode) schemaNode).getCases().stream()
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
}
