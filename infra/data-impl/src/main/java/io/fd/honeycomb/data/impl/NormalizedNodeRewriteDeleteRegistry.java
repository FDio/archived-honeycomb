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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.model.api.CaseSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic logic for creating {@link NormalizedNodeUpdate} for changed that delete by rewrite
 */
final class NormalizedNodeRewriteDeleteRegistry implements RewriteDeleteProducer {

    private static final Logger LOG = LoggerFactory.getLogger(NormalizedNodeRewriteDeleteRegistry.class);

    private final RewriteDeleteProducer leafDeleteProducer;
    private final RewriteDeleteProducer leafListDeleteProducer;
    private final RewriteDeleteProducer listDeleteProducer;
    private final RewriteDeleteProducer containerDeleteProducer;
    private final RewriteDeleteProducer choiceDeleteProducer;
    private final RewriteDeleteProducer caseDeleteProducer;
    private final RewriteDeleteProducer augmentationDeleteProducer;

    NormalizedNodeRewriteDeleteRegistry(@Nonnull final SchemaContext ctx) {
        leafDeleteProducer = new LeafRewriteDeleteProducer();
        leafListDeleteProducer = new LeafListRewriteDeleteProducer();
        listDeleteProducer = new ListRewriteDeleteProducer();
        containerDeleteProducer = new ContainerRewriteDeleteProducer(this, ctx);
        choiceDeleteProducer = new ChoiceRewriteDeleteProducer(this);
        caseDeleteProducer = new CaseRewriteDeleteProducer(this);
        augmentationDeleteProducer = new AugmentationRewriteDeleteProducer(this);
    }

    @Override
    public Collection<NormalizedNodeUpdate> normalizedUpdates(@Nonnull final YangInstanceIdentifier topLevelIdentifier,
                                                              @Nonnull final Map.Entry<YangInstanceIdentifier.PathArgument, DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> entry) {
        if (entry.getValue() instanceof LeafNode) {
            LOG.debug("Processing leaf node {}", topLevelIdentifier);
            return leafDeleteProducer.normalizedUpdates(topLevelIdentifier, entry);
        }

        if (entry.getValue() instanceof LeafSetNode) {
            LOG.debug("Processing leaf-list node {}", topLevelIdentifier);
            return leafListDeleteProducer.normalizedUpdates(topLevelIdentifier, entry);
        }

        if (entry.getValue() instanceof MapNode) {
            LOG.debug("Processing list {}", topLevelIdentifier);
            return listDeleteProducer.normalizedUpdates(topLevelIdentifier, entry);
        }

        if (entry.getValue() instanceof ContainerNode) {
            LOG.debug("Processing container {}", topLevelIdentifier);
            return containerDeleteProducer.normalizedUpdates(topLevelIdentifier, entry);
        }

        if (entry.getValue() instanceof ChoiceNode) {
            LOG.debug("Processing choice {}", topLevelIdentifier);
            return choiceDeleteProducer.normalizedUpdates(topLevelIdentifier, entry);
        }

        if (entry.getValue() instanceof CaseSchemaNode) {
            LOG.debug("Processing case {}", topLevelIdentifier);
            return caseDeleteProducer.normalizedUpdates(topLevelIdentifier, entry);
        }

        if (entry.getValue() instanceof AugmentationNode) {
            LOG.debug("Processing augmentation {}", topLevelIdentifier);
            return augmentationDeleteProducer.normalizedUpdates(topLevelIdentifier, entry);
        }

        return Collections.emptyList();
    }
}
