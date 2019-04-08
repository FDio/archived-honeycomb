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
import com.google.common.util.concurrent.FluentFuture;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.netconf.mdsal.connector.DOMDataTransactionValidator;

/**
 * An {@link DOMDataTreeWriteTransaction} than can be validated.
 * @see DOMDataTransactionValidator
 */
@Beta
interface ValidableTransaction extends DOMDataTreeWriteTransaction {
    /**
     * Validates state of the data tree associated with the provided {@link DOMDataTreeWriteTransaction}.
     *
     * <p>The operation should not have any side-effects on the transaction state.
     *
     * <p>It can be executed many times, providing the same results
     * if the state of the transaction has not been changed.
     *
     * @return
     *     a FluentFuture containing the result of the validate operation. The future blocks until the validation
     *     operation is complete. A successful validate returns nothing. On failure, the Future will fail.
     */
    FluentFuture<Void> validate();
}
