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

package io.fd.honeycomb.translate.util.read;

import com.google.common.util.concurrent.FluentFuture;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.read.Reader;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Simple DataBroker backed reader allowing to delegate reads to different brokers.
 */
public final class BindingBrokerReader<D extends DataObject, B extends Builder<D>>
        implements Reader<D, B> {

    private final InstanceIdentifier<D> instanceIdentifier;
    private final DataBroker dataBroker;
    private final LogicalDatastoreType datastoreType;
    private final ReflexiveReaderCustomizer<D, B> reflexiveReaderCustomizer;

    public BindingBrokerReader(final InstanceIdentifier<D> instanceIdentifier,
                               final DataBroker dataBroker,
                               final LogicalDatastoreType datastoreType,
                               final Class<B> builderClass) {
        this.reflexiveReaderCustomizer = new ReflexiveReaderCustomizer<>(instanceIdentifier.getTargetType(), builderClass);
        this.instanceIdentifier = instanceIdentifier;
        this.dataBroker = dataBroker;
        this.datastoreType = datastoreType;
    }

    @Override
    public boolean isPresent(final InstanceIdentifier<D> id, final D built, final ReadContext ctx) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Nonnull
    @Override
    public Optional<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                               @Nonnull final ReadContext ctx) throws ReadFailedException {
        try (final ReadTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction()) {
            final FluentFuture<? extends Optional<? extends DataObject>>
                read = readOnlyTransaction.read(datastoreType, id);
            try {
                return read.get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new ReadFailedException(id, ex);
            }
        }
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final D readValue) {
        reflexiveReaderCustomizer.merge(parentBuilder, readValue);
    }

    @Nonnull
    @Override
    public B getBuilder(final InstanceIdentifier<D> id) {
        return reflexiveReaderCustomizer.getBuilder(id);
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                      @Nonnull final B builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Nonnull
    @Override
    public InstanceIdentifier<D> getManagedDataObjectType() {
        return instanceIdentifier;
    }
}
