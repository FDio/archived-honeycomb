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

package io.fd.honeycomb.translate.write;

import com.google.common.annotations.Beta;
import io.fd.honeycomb.translate.SubtreeManager;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Base writer, responsible for translation between DataObjects and any other side. Handling all update operations(create,
 * update, delete)
 *
 * @param <D> Specific DataObject derived type, that is handled by this writer
 */
@Beta
public interface Writer<D extends DataObject> extends SubtreeManager<D> {

    /**
     * Validates data modification.
     *
     * @param id Identifier of data being validated
     * @param dataBefore Old data
     * @param dataAfter New, updated data
     * @param ctx Write context enabling writer to get information about candidate data as well as current data
     */
    default void validate(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                          @Nullable final DataObject dataBefore,
                          @Nullable final DataObject dataAfter,
                          @Nonnull final WriteContext ctx) throws DataValidationFailedException {
    }

    /**
     * Process modifications and translate them as create/update/delete operations to lower level
     *
     * @param id         Identifier of data being written
     * @param dataBefore Old data
     * @param dataAfter  New, updated data
     * @param ctx        Write context enabling writer to get information about candidate data as well as current data
     * @throws WriteFailedException if update failed
     */
    void processModification(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                             @Nullable final DataObject dataBefore,
                             @Nullable final DataObject dataAfter,
                             @Nonnull final WriteContext ctx) throws WriteFailedException;

    /**
     * Indicates whether there is direct support for updating nodes handled by writer,
     * or they must be broken up to individual deletes and creates.
     */
    boolean supportsDirectUpdate();

    /**
     * Returns true if node identified by this identifier can be processes by this writer
     *
     * @param instanceIdentifier identifier to be checked
     */
    default boolean canProcess(@Nonnull final InstanceIdentifier<? extends DataObject> instanceIdentifier) {
        return getManagedDataObjectType().equals(instanceIdentifier);
    }
}
