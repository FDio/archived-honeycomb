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

package io.fd.honeycomb.data;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Facade over data tree that allows reading tree nodes.
 */
@Beta
@FunctionalInterface
public interface ReadableDataManager {

    /**
     * Reads a particular node from the data tree.
     *
     * @param path Path of the node
     * @return a CheckFuture containing the result of the read.
     */
    FluentFuture<Optional<NormalizedNode<?, ?>>> read(@Nonnull final YangInstanceIdentifier path);
}
