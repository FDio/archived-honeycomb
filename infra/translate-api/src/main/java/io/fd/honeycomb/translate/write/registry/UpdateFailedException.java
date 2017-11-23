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

package io.fd.honeycomb.translate.write.registry;

import io.fd.honeycomb.translate.TranslationException;
import io.fd.honeycomb.translate.write.DataObjectUpdate;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Thrown when CRUD operation fails.
 *
 * Serialization/deserialization of this exception would cause
 * {@link #getProcessed()} and {@link #getFailed()} to return null.
 */
public class UpdateFailedException extends TranslationException {

    private static final long serialVersionUID = 896331856485410043L;
    private final transient  List<DataObjectUpdate> processed;
    private final transient DataObjectUpdate failed;

    /**
     * @param cause     original cause of failure
     * @param processed updates that were processed up until the point of failure
     * @param failed    update that cause the failure
     */
    public UpdateFailedException(@Nonnull final Throwable cause,
                                 @Nonnull final List<DataObjectUpdate> processed,
                                 @Nonnull final DataObjectUpdate failed) {
        super(cause);
        this.processed = processed;
        this.failed = failed;
    }

    /**
     * Returns set of nodes that has been processed by this operation till the failure happened, in execution order
     */
    public List<DataObjectUpdate> getProcessed() {
        return processed;
    }

    /**
     * Returns update that caused failure
     */
    public DataObjectUpdate getFailed() {
        return failed;
    }
}
