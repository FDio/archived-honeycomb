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

package io.fd.honeycomb.translate.read.registry;

import com.google.common.annotations.Beta;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Simple delegating reader suitable as a holder for all other root readers, providing readAll feature.
 */
@Beta
public interface ReaderRegistry extends InitRegistry {

    /**
     * Performs read on all registered root readers and merges the results into a Multimap. Keys represent identifiers
     * for root DataObjects from the data tree modeled by YANG.
     *
     * @param ctx Read context
     *
     * @return multimap that preserves deterministic iteration order across non-distinct key values
     * @throws ReadFailedException if read was unsuccessful
     */
    @Nonnull
    Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> readAll(@Nonnull final ReadContext ctx)
            throws ReadFailedException;

    /**
     * Reads data identified by id.
     *
     * @param id unique identifier of subtree to be read. The subtree must contain managed data object type. For
     *           identifiers pointing below node managed by this reader, it's reader's responsibility to filter out the
     *           right node or to delegate the read to a child reader.
     * @param ctx Read context
     *
     * @return List of DataObjects identified by id. If the ID points to a single node, it will be wrapped in a list
     * @throws ReadFailedException if read was unsuccessful
     */
    @Nonnull
    Optional<? extends DataObject> read(@Nonnull InstanceIdentifier<? extends DataObject> id, @Nonnull ReadContext ctx)
            throws ReadFailedException;
}
