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
import io.fd.honeycomb.translate.TranslationException;
import io.fd.honeycomb.translate.ValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Modification of a {@link ModifiableDataManager}.
 */
@Beta
public interface DataModification extends ReadableDataManager, AutoCloseable {

    /**
     * Delete the node at specified path.
     *
     * @param path Node path
     */
    void delete(YangInstanceIdentifier path);

    /**
     * Merge the specified data with the currently-present data
     * at specified path.
     *
     * @param path Node path
     * @param data Data to be merged
     */
    void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

    /**
     * Replace the data at specified path with supplied data.
     *
     * @param path Node path
     * @param data New node data
     */
    void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

    /**
     * Alters data tree using this modification.
     *
     * <p>Modification is validated before application.
     *
     * @throws TranslationException if commit failed while updating data tree state
     */
    void commit() throws TranslationException;

    /**
     * Validates state of the {@link DataModification}.
     *
     * <p>The operation does not have any side-effects on the modification state.
     *
     * <p>It can be executed many times, providing the same results
     * if the state of the modification has not been changed.
     *
     * @throws ValidationFailedException if modification data is not valid
     */
    void validate() throws ValidationFailedException;

    /**
     * Perform cleanup if necessary.
     */
    @Override
    default void close() {
        // by default, no cleanup is required
    }

}
