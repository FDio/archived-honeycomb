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

package io.fd.honeycomb.translate.impl.read;

import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.MoreExecutors;
import io.fd.honeycomb.translate.read.InitFailedException;
import io.fd.honeycomb.translate.read.InitReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GenericInitReader<O extends DataObject, B extends Builder<O>>
        extends GenericReader<O, B>
        implements InitReader<O, B> {

    private static final Logger LOG = LoggerFactory.getLogger(GenericInitReader.class);

    public GenericInitReader(@Nonnull final InstanceIdentifier<O> id,
                             @Nonnull final InitializingReaderCustomizer<O, B> customizer) {
        super(id, customizer);
    }

    @Override
    public void init(final DataBroker broker, final InstanceIdentifier<O> id, final ReadContext ctx) throws
            InitFailedException {
        LOG.debug("{}: Initializing current: {}", this, id);

        try {
            final Optional<O> operational = readCurrent(id, ctx);
            if (operational.isPresent()) {
                final Initialized<? extends DataObject> init =
                        ((InitializingReaderCustomizer<O, B>) customizer).init(id, operational.get(), ctx);
                LOG.debug("{}: Writing init config : {} at: {}", GenericInitReader.this, init.getData(), init.getId());
                writeInit(broker, init);
            }
        } catch (ReadFailedException e) {
            LOG.warn("{}: Failed to initialize current, unable to read: {}", this, id, e);
            throw new InitFailedException(e.getFailedId(), e);
        }
    }

    @SuppressWarnings("unchecked")
    static void writeInit(final DataBroker broker, final Initialized<? extends DataObject> init) {
        final WriteTransaction writeTx = broker.newWriteOnlyTransaction();

        InstanceIdentifier id = init.getId();
        if (id.getPathArguments() instanceof List && ((List) id.getPathArguments()).size() == 1) {
            //root element only. not necessary to create parents
            writeTx.merge(CONFIGURATION, id, init.getData());
        }
        else {
            writeTx.mergeParentStructureMerge(CONFIGURATION, id, init.getData());
        }
        FluentFuture<? extends CommitInfo> future = writeTx.commit();
        future.addCallback(new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(@NullableDecl final CommitInfo commitInfo) {
                LOG.debug("Transaction: {} successfully committed.", writeTx);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Transaction: {} failed.", writeTx);
            }
        }, MoreExecutors.directExecutor());
    }
}
