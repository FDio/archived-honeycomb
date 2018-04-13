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

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.ValidationFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Thrown when a writer fails to validate data update.
 */
public class DataValidationFailedException extends ValidationFailedException {
    private static final long serialVersionUID = 1;

    private final InstanceIdentifier<?> failedId;

    /**
     * Constructs an ValidationFailedException given data id, exception detail message and exception cause.
     *
     * @param failedId instance identifier of the data object that could not be validated
     * @param cause    the cause of validation failure
     * @param message  the exception detail message
     */
    public DataValidationFailedException(@Nonnull final InstanceIdentifier<?> failedId,
                                         @Nonnull final String message,
                                         @Nonnull final Throwable cause) {
        super(message, cause);
        this.failedId = checkNotNull(failedId, "failedId should not be null");
    }

    /**
     * Constructs an ValidationFailedException given data id.
     *
     * @param failedId instance identifier of the data object that could not be validated
     */
    public DataValidationFailedException(@Nonnull final InstanceIdentifier<?> failedId,
                                         @Nonnull final String message) {
        super(message);
        this.failedId = checkNotNull(failedId, "failedId should not be null");
    }

    /**
     * Constructs an ValidationFailedException given data id and exception cause.
     *
     * @param failedId instance identifier of the data object that could not be validated
     * @param cause    the cause of validated failure
     */
    public DataValidationFailedException(@Nonnull final InstanceIdentifier<?> failedId,
                                         @Nonnull final Throwable cause) {
        super(cause);
        this.failedId = checkNotNull(failedId, "failedId should not be null");
    }

    /**
     * Returns id of the data object that could not be validated.
     *
     * @return data object instance identifier
     */
    @Nonnull
    public InstanceIdentifier<?> getFailedId() {
        return failedId;
    }

    /**
     * Create specific validated failed exception.
     */
    public static class CreateValidationFailedException extends DataValidationFailedException {
        private static final long serialVersionUID = 1;

        private final transient DataObject data;

        public CreateValidationFailedException(@Nonnull final InstanceIdentifier<?> failedId,
                                               @Nonnull final DataObject data,
                                               @Nonnull final Throwable cause) {
            super(failedId, getMsg(failedId, data), cause);
            this.data = checkNotNull(data, "data");
        }

        private static String getMsg(final @Nonnull InstanceIdentifier<?> failedId,
                                     final @Nonnull DataObject data) {
            return String.format("Failed to validate create request for: %s at: %s.", data, failedId);
        }

        public DataObject getData() {
            return data;
        }
    }

    /**
     * Update specific validated failed exception.
     */
    public static class UpdateValidationFailedException extends DataValidationFailedException {
        private static final long serialVersionUID = 1;

        private final transient DataObject dataBefore;
        private final transient DataObject dataAfter;

        public UpdateValidationFailedException(@Nonnull final InstanceIdentifier<?> failedId,
                                               @Nonnull final DataObject dataBefore,
                                               @Nonnull final DataObject dataAfter,
                                               @Nonnull final Throwable cause) {
            super(failedId, getMsg(failedId, dataBefore, dataAfter), cause);
            this.dataBefore = checkNotNull(dataBefore, "dataBefore");
            this.dataAfter = checkNotNull(dataAfter, "dataAfter");
        }

        private static String getMsg(final @Nonnull InstanceIdentifier<?> failedId, final DataObject dataBefore,
                                     final @Nonnull DataObject dataAfter) {
            return String
                .format("Failed to validate update request from: %s to: %s, at: %s.", dataBefore, dataAfter, failedId);
        }

        public DataObject getDataBefore() {
            return dataBefore;
        }

        public DataObject getDataAfter() {
            return dataAfter;
        }
    }

    /**
     * Delete specific validated failed exception.
     */
    public static class DeleteValidationFailedException extends DataValidationFailedException {
        private static final long serialVersionUID = 1;

        public DeleteValidationFailedException(@Nonnull final InstanceIdentifier<?> failedId, @Nonnull final Throwable cause) {
            super(failedId, getMsg(failedId), cause);
        }

        private static String getMsg(@Nonnull final InstanceIdentifier<?> failedId) {
            return String.format("Failed to validate delete request: %s.", failedId);
        }
    }
}
