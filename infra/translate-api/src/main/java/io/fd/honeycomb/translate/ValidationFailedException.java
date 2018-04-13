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

package io.fd.honeycomb.translate;

import javax.annotation.Nonnull;

/**
 * Thrown when validation fails at the translation layer.
 */
public class ValidationFailedException extends TranslationException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an {@link ValidationFailedException} given exception detail message.
     *
     * @param message the exception detail message
     */
    public ValidationFailedException(@Nonnull final String message) {
        super(message);
    }

    /**
     * Constructs an {@link ValidationFailedException} given exception detail message and exception cause.
     *
     * @param cause   the cause of validation failure
     * @param message the exception detail message
     */
    public ValidationFailedException(@Nonnull final String message, @Nonnull final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an {@link ValidationFailedException} given exception cause.
     *
     * @param cause the cause of validated failure
     */
    public ValidationFailedException(@Nonnull final Throwable cause) {
        super(cause);
    }
}
