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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.util.SchemaContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ContainerRewriteDeleteProducer extends DelegatingRewriteDeleteProducer implements RewriteDeleteProducer {

    private static final Logger LOG = LoggerFactory.getLogger(ContainerRewriteDeleteProducer.class);

    private final SchemaContext ctx;

    ContainerRewriteDeleteProducer(@Nonnull final NormalizedNodeRewriteDeleteRegistry baseRewriteProducer,
                                   @Nonnull final SchemaContext ctx) {
        super(baseRewriteProducer);
        this.ctx = ctx;
    }

    @Override
    public Collection<NormalizedNodeUpdate> normalizedUpdates(@Nonnull final YangInstanceIdentifier topLevelIdentifier,
                                                              @Nonnull final Map.Entry<YangInstanceIdentifier.PathArgument, DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> entry) {
        final ContainerSchemaNode containerSchemaNode =
                (ContainerSchemaNode) SchemaContextUtil
                        .findDataSchemaNode(ctx, getSchemaPath(topLevelIdentifier, entry));

        if (containerSchemaNode.isPresenceContainer()) {
            LOG.debug("Processing {} as presence container", topLevelIdentifier);
            // if presence container  - create delete right away
            return ((ContainerNode) entry.getValue()).getValue().stream()
                    .map(containerNode -> new NormalizedNodeUpdate(
                            YangInstanceIdentifier.builder(topLevelIdentifier)
                                    .node(containerNode.getIdentifier()).build(), containerNode, null))
                    .collect(Collectors.toList());
        } else {
            LOG.debug("Processing {} as non-presence container", topLevelIdentifier);
            // if non-presence - goes deep with base logic
            return super.normalizedUpdates(topLevelIdentifier, entry);
        }
    }

    private static SchemaPath getSchemaPath(final YangInstanceIdentifier topLevelIdentifier,
                                            final Map.Entry<YangInstanceIdentifier.PathArgument, DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> entry) {
        return SchemaPath.create(extractSchemaPathQNames(topLevelIdentifier, entry), true);
    }

    private static List<QName> extractSchemaPathQNames(final YangInstanceIdentifier topLevelIdentifier,
                                                       final Map.Entry<YangInstanceIdentifier.PathArgument, DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> entry) {
        // must be filtered out of augmentation and keyed NodeIdentifiers
        return Stream.concat(topLevelIdentifier.getPathArguments().stream(), Stream.of(entry.getKey()))
                .filter(pathArgument -> pathArgument instanceof YangInstanceIdentifier.NodeIdentifier)
                .map(YangInstanceIdentifier.PathArgument::getNodeType)
                .collect(Collectors.toList());
    }
}
