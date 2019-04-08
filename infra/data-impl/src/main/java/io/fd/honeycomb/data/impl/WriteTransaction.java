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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import io.fd.honeycomb.data.DataModification;
import io.fd.honeycomb.translate.TranslationException;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.netconf.mdsal.connector.DOMDataTransactionValidator;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
final class WriteTransaction implements ValidableTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(WriteTransaction.class);

    @Nullable
    private DataModification operationalModification;
    @Nullable
    private DataModification configModification;
    private TransactionStatus status = TransactionStatus.NEW;

    private WriteTransaction(@Nullable final DataModification configModification,
                             @Nullable final DataModification operationalModification) {
        this.operationalModification = operationalModification;
        this.configModification = configModification;
    }

    private void checkIsNew() {
        Preconditions.checkState(status == TransactionStatus.NEW, "Transaction was submitted or canceled");
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                    final NormalizedNode<?, ?> data) {
        LOG.debug("WriteTransaction.put() store={}, path={}, data={}", store, path, data);
        checkIsNew();
        handleOperation(store, (modification) -> modification.write(path, data));
    }

    private void handleOperation(final LogicalDatastoreType store,
                                 final Consumer<DataModification> modificationHandler) {
        switch (store) {
            case CONFIGURATION:
                checkArgument(configModification != null, "Modification of %s is not supported", store);
                modificationHandler.accept(configModification);
                break;
            case OPERATIONAL:
                checkArgument(operationalModification != null, "Modification of %s is not supported", store);
                modificationHandler.accept(operationalModification);
                break;
            default:
                throw new IllegalArgumentException("Unable to handle operation for type " + store);
        }
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                      final NormalizedNode<?, ?> data) {
        LOG.debug("WriteTransaction.merge() store={}, path={}, data={}", store, path, data);
        checkIsNew();
        handleOperation(store, (modification) -> modification.merge(path, data));
    }

    @Override
    public boolean cancel() {
        if (status != TransactionStatus.NEW) {
            // only NEW transactions can be cancelled
            return false;
        } else {
            if (configModification != null) {
                configModification.close();
            }
            status = TransactionStatus.CANCELED;
            return true;
        }
    }

    @Override
    public void delete(LogicalDatastoreType store, final YangInstanceIdentifier path) {
        LOG.debug("WriteTransaction.delete() store={}, path={}", store, path);
        checkIsNew();
        handleOperation(store, (modification) -> modification.delete(path));
    }

    private void doCommit() throws TranslationException {
        status = TransactionStatus.SUBMITED;
        if (configModification != null) {
            configModification.commit();
        }
        if (operationalModification != null) {
            operationalModification.commit();
        }

        status = TransactionStatus.COMMITED;
    }

    @Override
    public @NonNull FluentFuture<? extends CommitInfo> commit() {
        LOG.trace("WriteTransaction.commit()");
        checkIsNew();
        try {
            doCommit();
        } catch (Exception e) {
            status = TransactionStatus.FAILED;
            LOG.error("Submit failed", e);
            return FluentFuture
                    .from(Futures.immediateFailedFuture(
                            new TransactionCommitFailedException("Failed to validate DataTreeModification", e)));
        }
        return FluentFuture.from(Futures.immediateFuture(null));
    }

    @Override
    public Object getIdentifier() {
        return this;
    }

    @Override
    public FluentFuture<Void> validate() {
        try {
            if (configModification != null) {
                configModification.validate();
            }
            if (operationalModification != null) {
                operationalModification.validate();
            }
        } catch (Exception e) {
            return FluentFutures.immediateFailedFluentFuture(
                    new DOMDataTransactionValidator.ValidationFailedException(e.getMessage(), e.getCause()));
        }
        return FluentFutures.immediateNullFluentFuture();
    }


    @Nonnull
    static WriteTransaction createOperationalOnly(@Nonnull final DataModification operationalData) {
        return new WriteTransaction(null, requireNonNull(operationalData));
    }

    @Nonnull
    static WriteTransaction createConfigOnly(@Nonnull final DataModification configData) {
        return new WriteTransaction(requireNonNull(configData), null);
    }

    @Nonnull
    static WriteTransaction create(@Nonnull final DataModification configData,
                            @Nonnull final DataModification operationalData) {
        return new WriteTransaction(requireNonNull(configData), requireNonNull(operationalData));
    }

    // TODO consider refactor based on implemented contract.
    enum TransactionStatus {
        /**
         * The transaction has been freshly allocated. The user is still accessing
         * it and it has not been sealed.
         */
        NEW,
        /**
         * The transaction has been completed by the user and sealed. It is currently
         * awaiting execution.
         */
        SUBMITED,
        /**
         * The transaction has been successfully committed to backing store.
         */
        COMMITED,
        /**
         * The transaction has failed to commit due to some underlying issue.
         */
        FAILED,
        /**
         * Currently unused.
         */
        CANCELED,
    }
}
