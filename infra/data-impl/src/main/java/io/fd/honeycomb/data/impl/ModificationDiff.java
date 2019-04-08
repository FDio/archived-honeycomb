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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType.DELETE;
import static org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType.SUBTREE_MODIFIED;
import static org.opendaylight.yangtools.yang.data.api.schema.tree.ModificationType.WRITE;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateNode;
import org.opendaylight.yangtools.yang.data.impl.schema.nodes.AbstractImmutableDataContainerNode;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recursively collects and provides all unique and non-null modifications (modified normalized nodes).
 */
final class ModificationDiff {

    private static final Logger LOG = LoggerFactory.getLogger(ModificationDiff.class);

    private static final ModificationDiff EMPTY_DIFF = new ModificationDiff(Collections.emptyMap());
    private static final EnumSet VALID_MODIFICATIONS = EnumSet.of(WRITE, DELETE);
    private static final EnumSet LEAF_VALID_MODIFICATIONS = EnumSet.of(WRITE, DELETE, SUBTREE_MODIFIED);

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

    @Override
    public String toString() {
        return "ModificationDiff{updates=" + updates + '}';
    }

    static final class ModificationDiffBuilder {
        private NormalizedNodeRewriteDeleteRegistry registry;
        private SchemaContext ctx;

        ModificationDiffBuilder setCtx(final SchemaContext ctx) {
            this.ctx = ctx;
            registry = new NormalizedNodeRewriteDeleteRegistry(ctx);
            return this;
        }

        ModificationDiff build(@Nonnull final DataTreeCandidateNode currentCandidate) {
            checkNotNull(currentCandidate, "Data tree candidate cannot be null");
            checkNotNull(ctx, "Schema ctx cannot be null");

            return recursivelyFromCandidateRoot(currentCandidate, ctx);
        }

        /**
         * Produce an aggregated diff from a candidate node recursively. MixinNodes are ignored as modifications and so
         * are complex nodes which direct leaves were not modified.
         */
        @Nonnull
        ModificationDiff recursivelyFromCandidate(@Nonnull final Modification modification) {
            // recursively process child nodes for exact modifications
            return recursivelyChildrenFromCandidate(modification)
                    // also add modification on current level, if elligible
                    .merge(isModification(modification)
                            ? ModificationDiff.create(modification)
                            // Modification that writes only non-presence container to override nested nodes wont have
                            // child nodes(in data tree candidate) so logic before will not detected such change, so checking directly
                            : isNonPresenceOverride(modification)
                                    ? detectUnderDisappearedNonPresenceContainer(modification)
                                    : EMPTY_DIFF);
        }

        private ModificationDiff detectUnderDisappearedNonPresenceContainer(
                @Nonnull final Modification modification) {
            final java.util.Optional<NormalizedNode<?, ?>> dataBefore = modification.getDataBefore();

            // is disappear case
            if (dataBefore.isPresent()) {
                final NormalizedNode<?, ?> parentData = dataBefore.get();

                // have potential to extract children
                if (parentData instanceof AbstractImmutableDataContainerNode) {
                    final AbstractImmutableDataContainerNode<YangInstanceIdentifier.PathArgument> parentContainerNode =
                            (AbstractImmutableDataContainerNode) parentData;

                    final Map<YangInstanceIdentifier, NormalizedNodeUpdate> childUpdates =
                            parentContainerNode.getChildren().entrySet().stream()
                                    .flatMap(entry -> registry.normalizedUpdates(modification.getId(), entry).stream())
                                    .collect(Collectors.toMap(NormalizedNodeUpdate::getId, update -> update));

                    return new ModificationDiff(childUpdates);
                }
            }
            return EMPTY_DIFF;
        }

        /**
         * Same as {@link #recursivelyFromCandidate(Modification)} but does not process the root node for modifications,
         * since it's the artificial data root, that has no child leaves but always is marked as SUBTREE_MODIFIED.
         */
        @Nonnull
        ModificationDiff recursivelyFromCandidateRoot(@Nonnull final DataTreeCandidateNode currentCandidate,
                                                      @Nonnull final SchemaContext ctx) {
            return recursivelyChildrenFromCandidate(
                    new Modification(YangInstanceIdentifier.EMPTY, currentCandidate, ctx));
        }

        /**
         * Check whether current node was modified. {@link MixinNode}s are ignored
         * and only nodes which direct leaves(or choices) are modified are considered a modification.
         */
        private Boolean isModification(@Nonnull final Modification modification) {
            // APPEAR/DISAPPEAR are not valid modifications, but some of the children can be modified
            // aka. list entry added to nested list under non-presence container, which would be resolved as APPEAR for
            // that container, but MERGE for nested list
            if (modification.isMixin() && !modification.is(AugmentationSchemaNode.class)) {
                return false;
            } else {
                return isCurrentModified(modification);
            }
        }

        private Boolean isCurrentModified(@Nonnull final Modification modification) {
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
                    .filter(child -> LEAF_VALID_MODIFICATIONS.contains(child.getModificationType()))
                    .findFirst()
                    .isPresent();

            // Also as fallback check choices (choices do not exist in BA world and if anything within a choice was modified,
            // consider its parent as being modified)
            final boolean modified = directLeavesModified
                    || modification.streamChildren()
                    .filter(child -> child.is(ChoiceSchemaNode.class))
                    // Recursively check each choice if there was any change to it
                    .filter(this::isCurrentModified)
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
         * Checks whether node is non-presence container but with changed nested data
         */
        private static boolean isNonPresenceOverride(@Nonnull final Modification modification) {
            return modification.is(ContainerSchemaNode.class)// must be container
                    && !((ContainerSchemaNode) modification.getSchemaNode()).isPresenceContainer()
                    // must be non-presence
                    && modification.getChildNodes().isEmpty() // is override to empty
                    && modification.isBeforeAndAfterDifferent()// to detect that it is modification
                    &&
                    modification.getDataBefore().isPresent(); // to ensure the case when overriding previously existing
        }

        /**
         * Process all non-leaf child nodes recursively, creating aggregated {@link ModificationDiff}.
         */
        private ModificationDiff recursivelyChildrenFromCandidate(@Nonnull final Modification modification) {
            // recursively process child nodes for specific modifications
            return modification.streamChildren()
                    .filter(child -> !child.is(LeafSchemaNode.class))
                    .map(this::recursivelyFromCandidate)
                    .reduce(ModificationDiff::merge)
                    .orElse(EMPTY_DIFF);
        }
    }

}
