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

package io.fd.honeycomb.data.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Composite DOM transaction that delegates reads to a {@link DOMDataTreeReadTransaction} delegate and writes to a {@link
 * DOMDataTreeWriteTransaction} delegate.
 */
final class ReadWriteTransaction implements DOMDataTreeReadWriteTransaction, ValidableTransaction {

    private final DOMDataTreeReadTransaction delegateReadTx;
    private final ValidableTransaction delegateWriteTx;

    ReadWriteTransaction(@Nonnull final DOMDataTreeReadTransaction delegateReadTx,
                         @Nonnull final ValidableTransaction delegateWriteTx) {
        this.delegateReadTx = Preconditions.checkNotNull(delegateReadTx, "delegateReadTx should not be null");
        this.delegateWriteTx = Preconditions.checkNotNull(delegateWriteTx, "delegateWriteTx should not be null");
    }

    @Override
    public boolean cancel() {
        delegateReadTx.close();
        return delegateWriteTx.cancel();
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                    final NormalizedNode<?, ?> data) {
        delegateWriteTx.put(store, path, data);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                      final NormalizedNode<?, ?> data) {
        delegateWriteTx.merge(store, path, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        delegateWriteTx.delete(store, path);
    }

    @Override
    public FluentFuture<? extends CommitInfo> commit() {
        return delegateWriteTx.commit();
    }

    @Override
    public FluentFuture<Optional<NormalizedNode<?, ?>>> read(final LogicalDatastoreType store,
                                                             final YangInstanceIdentifier path) {
        return delegateReadTx.read(store, path);
    }

    @Override
    public FluentFuture<Boolean> exists(final LogicalDatastoreType store,
                                        final YangInstanceIdentifier path) {
        return delegateReadTx.exists(store, path);
    }

    @Override
    public Object getIdentifier() {
        return this;
    }

    @Override
    public FluentFuture<Void> validate() {
        return delegateWriteTx.validate();
    }
}

