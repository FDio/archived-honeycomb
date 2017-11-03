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

package io.fd.honeycomb.translate.util.write;

import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.translate.write.Writer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

/**
 * Simple DataBroker backed writer allowing to delegate writes to different brokers.
 */
public final class BindingBrokerWriter<D extends DataObject> implements Writer<D> {
    private final InstanceIdentifier<D> instanceIdentifier;
    private final DataBroker dataBroker;

    public BindingBrokerWriter(final InstanceIdentifier<D> instanceIdentifier, final DataBroker dataBroker) {
        this.instanceIdentifier = instanceIdentifier;
        this.dataBroker = dataBroker;
    }

    @Nonnull
    @Override
    public InstanceIdentifier<D> getManagedDataObjectType() {
        return instanceIdentifier;
    }

    @Override
    public void processModification(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                    @Nullable final DataObject dataBefore, @Nullable final DataObject dataAfter,
                                    @Nonnull final WriteContext ctx) throws WriteFailedException {
        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.put(CONFIGURATION, (InstanceIdentifier<DataObject>) id, dataAfter);
        final CheckedFuture<Void, TransactionCommitFailedException> result = writeTransaction.submit();
        try {
            result.checkedGet();
        } catch (TransactionCommitFailedException e) {
            throw new WriteFailedException(id, e);
        }
    }

    @Override
    public boolean supportsDirectUpdate() {
        return false;
    }
}
