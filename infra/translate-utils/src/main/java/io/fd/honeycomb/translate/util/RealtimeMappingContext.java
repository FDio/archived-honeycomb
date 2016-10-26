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

package io.fd.honeycomb.translate.util;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.MappingContext;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class RealtimeMappingContext implements MappingContext {

    private DataBroker contextBindingBrokerDependency;

    public RealtimeMappingContext(final DataBroker contextBindingBrokerDependency) {
        this.contextBindingBrokerDependency = contextBindingBrokerDependency;
    }

    @Override
    public <T extends DataObject> Optional<T> read(@Nonnull final InstanceIdentifier<T> currentId) {
        try (ReadOnlyTransaction tx = contextBindingBrokerDependency.newReadOnlyTransaction()) {
            try {
                return tx.read(LogicalDatastoreType.OPERATIONAL, currentId).checkedGet();
            } catch (ReadFailedException e) {
                throw new IllegalStateException("Unable to perform read of " + currentId, e);
            }
        }
    }

    @Override
    public void delete(final InstanceIdentifier<?> path) {
        final WriteTransaction writeTx = contextBindingBrokerDependency.newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.OPERATIONAL, path);
        try {
            writeTx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            throw new IllegalStateException("Unable to perform delete of " + path, e);
        }
    }

    @Override
    public <T extends DataObject> void merge(final InstanceIdentifier<T> path, final T data) {
        final WriteTransaction writeTx = contextBindingBrokerDependency.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.OPERATIONAL, path, data, true);
        try {
            writeTx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            throw new IllegalStateException("Unable to perform merge of " + path, e);
        }
    }

    @Override
    public <T extends DataObject> void put(final InstanceIdentifier<T> path, final T data) {
        final WriteTransaction writeTx = contextBindingBrokerDependency.newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, path, data, true);
        try {
            writeTx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            throw new IllegalStateException("Unable to perform put of " + path, e);
        }
    }

    @Override
    public void close() {
        // Noop
    }
}
