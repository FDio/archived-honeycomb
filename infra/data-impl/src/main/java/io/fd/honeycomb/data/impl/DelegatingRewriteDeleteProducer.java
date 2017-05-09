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

import static com.google.common.base.Preconditions.checkState;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;

/**
 * Basic implementation for nodes that are not considered modifications but their children could be
 */
abstract class DelegatingRewriteDeleteProducer implements RewriteDeleteProducer {

    private final NormalizedNodeRewriteDeleteRegistry baseRewriteProducer;

    DelegatingRewriteDeleteProducer(@Nonnull final NormalizedNodeRewriteDeleteRegistry baseRewriteProducer) {
        this.baseRewriteProducer = baseRewriteProducer;
    }

    @Override
    public Collection<NormalizedNodeUpdate> normalizedUpdates(@Nonnull final YangInstanceIdentifier topLevelIdentifier,
                                                              @Nonnull final Map.Entry<YangInstanceIdentifier.PathArgument, DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> entry) {
        // just delegates to lower level
        checkState(entry.getValue() instanceof DataContainerNode, "Unable to extract children");
        checkState(entry.getValue() instanceof DataContainerChild, "Unable to extract identifier");
        final Collection<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> value =
                DataContainerNode.class.cast(entry.getValue()).getValue();
        return value.stream()
                .map(DataContainerChild.class::cast)
                .flatMap(node -> baseRewriteProducer
                        .normalizedUpdates(childYangId(topLevelIdentifier, entry), childMapEntry(node)).stream())
                .collect(Collectors.toList());
    }

    private static AbstractMap.SimpleEntry<YangInstanceIdentifier.PathArgument, DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> childMapEntry(
            final DataContainerChild node) {
        return new HashMap.SimpleEntry<YangInstanceIdentifier.PathArgument, DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>>(
                node.getIdentifier(), node);
    }

    private static YangInstanceIdentifier childYangId(final @Nonnull YangInstanceIdentifier topLevelIdentifier,
                                                      final @Nonnull Map.Entry<YangInstanceIdentifier.PathArgument, DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> entry) {
        return YangInstanceIdentifier.builder(topLevelIdentifier)
                .node(entry.getKey()).build();
    }
}
