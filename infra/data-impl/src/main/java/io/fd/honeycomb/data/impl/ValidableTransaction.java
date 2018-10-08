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

package io.fd.honeycomb.data.impl;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.netconf.mdsal.connector.DOMDataTransactionValidator;
import org.opendaylight.netconf.mdsal.connector.DOMDataTransactionValidator.ValidationFailedException;

/**
 * An {@link DOMDataWriteTransaction} than can be validated.
 * @see DOMDataTransactionValidator
 */
@Beta
interface ValidableTransaction extends DOMDataWriteTransaction {
    /**
     * Validates state of the data tree associated with the provided {@link DOMDataWriteTransaction}.
     *
     * <p>The operation should not have any side-effects on the transaction state.
     *
     * <p>It can be executed many times, providing the same results
     * if the state of the transaction has not been changed.
     *
     * @return
     *     a CheckedFuture containing the result of the validate operation. The future blocks until the validation
     *     operation is complete. A successful validate returns nothing. On failure, the Future will fail
     *     with a {@link ValidationFailedException} or an exception derived from ValidationFailedException.
     */
    CheckedFuture<Void, ValidationFailedException> validate();
}
