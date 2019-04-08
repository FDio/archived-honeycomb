/*
 * Copyright (c) 2019 Cisco and/or its affiliates.
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

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

import com.google.common.util.concurrent.FluentFuture;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.translate.write.Writer;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Simple DataBroker backed writer allowing to delegate writes to different brokers.
 */
public final class LegacyBindingBrokerWriter<D extends DataObject> implements Writer<D> {
    private final InstanceIdentifier<D> instanceIdentifier;
    private final DataBroker dataBroker;

    public LegacyBindingBrokerWriter(final InstanceIdentifier<D> instanceIdentifier, final DataBroker dataBroker) {
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
        final FluentFuture<? extends CommitInfo> result = writeTransaction.commit();
        try {
            result.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new WriteFailedException(id, ex);
        }
    }

    @Override
    public boolean supportsDirectUpdate() {
        return false;
    }
}
