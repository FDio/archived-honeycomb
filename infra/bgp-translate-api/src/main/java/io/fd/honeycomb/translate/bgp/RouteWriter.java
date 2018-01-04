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

package io.fd.honeycomb.translate.bgp;

import com.google.common.annotations.Beta;
import io.fd.honeycomb.translate.SubtreeManager;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Route;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Responsible for translation of Routes from local RIB and any other side.
 * Handling all update operations(create, update, delete).
 * Can be used for RIB to FIB translation.
 *
 * @param <R> Specific Route derived type, that is handled by this writer
 */
@Beta
public interface RouteWriter<R extends Route> extends SubtreeManager<R> {
    /**
     * Handles create operation.
     * @param id identifier(from root) of data being written
     * @param dataAfter new data to be written
     * @throws WriteFailedException.CreateFailedException if create was unsuccessful
     */
    void create(@Nonnull final InstanceIdentifier<R> id,
                @Nullable final R dataAfter) throws WriteFailedException.CreateFailedException;

    /**
     * Handles delete operation.
     * @param id identifier(from root) of data being deleted
     * @param dataBefore old data being deleted
     * @throws WriteFailedException.DeleteFailedException if delete was unsuccessful
     */
    void delete(@Nonnull final InstanceIdentifier<R> id,
                @Nullable final R dataBefore) throws WriteFailedException.DeleteFailedException;

    /**
     * Handles update operation.
     * @param id identifier(from root) of data being updated
     * @param dataBefore old data
     * @param dataAfter new, updated data
     * @throws WriteFailedException.UpdateFailedException if update was unsuccessful
     */
    void update(@Nonnull final InstanceIdentifier<R> id,
                @Nullable final R dataBefore,
                @Nullable final R dataAfter) throws WriteFailedException.UpdateFailedException;
}
