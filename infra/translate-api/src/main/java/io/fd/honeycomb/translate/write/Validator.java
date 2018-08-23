/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.UpdateValidationFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Responsible for validation of DataObjects.
 * Handles all update operations (create, update, delete).
 *
 * @param <D> Specific DataObject derived type, that is handled by this writer
 * @see Writer#validate(InstanceIdentifier, DataObject, DataObject, WriteContext)
 */
@Beta
public interface Validator<D extends DataObject> {
    /**
     * Validates write operation.
     *
     * @param id           Identifier(from root) of data being written
     * @param dataAfter    New data to be written
     * @param writeContext Write context that provides information about current state of DataTree.
     * @throws CreateValidationFailedException if write validation failed
     */
    default void validateWrite(@Nonnull final InstanceIdentifier<D> id,
                               @Nonnull final D dataAfter,
                               @Nonnull final WriteContext writeContext) throws CreateValidationFailedException {
        // Validation on write is optional
    }

    /**
     * Validates update operation.
     *
     * @param id           Identifier(from root) of data being updated
     * @param dataBefore   Old data
     * @param dataAfter    New, updated data
     * @param writeContext Write context that provides information about current state of DataTree.
     * @throws UpdateValidationFailedException if update validation failed
     */
    default void validateUpdate(@Nonnull InstanceIdentifier<D> id,
                                @Nonnull D dataBefore,
                                @Nonnull D dataAfter,
                                @Nonnull WriteContext writeContext) throws UpdateValidationFailedException {
        // Validation on update is optional
    }

    /**
     * Validates delete operation.
     *
     * @param id           Identifier(from root) of data being written
     * @param dataBefore   Old data being deleted
     * @param writeContext Write context that provides information about current state of DataTree.
     * @throws DeleteValidationFailedException if delete validation failed
     */
    default void validateDelete(@Nonnull InstanceIdentifier<D> id,
                                @Nonnull D dataBefore,
                                @Nonnull WriteContext writeContext) throws DeleteValidationFailedException {
        // Validation on delete is optional
    }
}
