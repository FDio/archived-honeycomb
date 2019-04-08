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

package io.fd.honeycomb.test.tools;

import static com.google.common.base.Preconditions.checkState;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Process yang data from json to BA Objects
 */
final class YangDataProcessorRegistry {

    private final List<YangDataProcessor> processors;

    private YangDataProcessorRegistry(@Nonnull final SchemaContext context,
                                      @Nonnull final BindingToNormalizedNodeCodec codec) {
        // linked should be faster for iteration
        processors = new LinkedList<>();
        processors.add(new ListNodeDataProcessor(context, codec));
        processors.add(new ContainerNodeDataProcessor(context, codec));
    }

    static YangDataProcessorRegistry create(@Nonnull final SchemaContext context,
                                                   @Nonnull final BindingToNormalizedNodeCodec codec) {
        return new YangDataProcessorRegistry(context, codec);
    }

    @Nonnull
    DataObject getNodeData(@Nonnull final YangInstanceIdentifier yangInstanceIdentifier,
                                  @Nonnull final String resourcePath) {
        return pickProcessor(yangInstanceIdentifier).getNodeData(yangInstanceIdentifier, resourcePath);
    }

    private YangDataProcessor pickProcessor(final YangInstanceIdentifier yangInstanceIdentifier) {
        final List<YangDataProcessor> eligibleProcessors = processors.stream()
                .filter(processor -> processor.canProcess(yangInstanceIdentifier))
                .collect(Collectors.toList());

        // canProcess should be exclusive for node type, but just in case
        checkState(eligibleProcessors.size() == 1,
                "No single eligible processor found, matches=[%s]", eligibleProcessors);

        return eligibleProcessors.get(0);
    }
}
