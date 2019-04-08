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

package io.fd.honeycomb.translate.util.write;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.util.concurrent.FluentFuture;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.write.WriteContext;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Transaction backed WriteContext.
 */
public final class TransactionWriteContext implements WriteContext {

    private final DOMDataTreeReadTransaction beforeTx;
    private final DOMDataTreeReadTransaction afterTx;
    private final ModificationCache ctx;
    private final BindingNormalizedNodeSerializer serializer;
    private final MappingContext mappingContext;

    public TransactionWriteContext(final BindingNormalizedNodeSerializer serializer,
                                   final DOMDataTreeReadTransaction beforeTx,
                                   final DOMDataTreeReadTransaction afterTx,
                                   final MappingContext mappingContext) {
        this.serializer = serializer;
        this.beforeTx = beforeTx;
        this.afterTx = afterTx;
        this.mappingContext = mappingContext;
        this.ctx = new ModificationCache();
    }

    // TODO HONEYCOMB-169 make this asynchronous

    @Override
    public <T extends DataObject> Optional<T> readBefore(@Nonnull final InstanceIdentifier<T> currentId) {
        return read(currentId, beforeTx);
    }

    @Override
    public <T extends DataObject> Optional<T> readAfter(@Nonnull final InstanceIdentifier<T> currentId) {
        return read(currentId, afterTx);
    }


    private <T extends DataObject> Optional<T> read(final InstanceIdentifier<T> currentId,
                                                    final DOMDataTreeReadTransaction tx) {
        final YangInstanceIdentifier path = serializer.toYangInstanceIdentifier(currentId);

        final FluentFuture<Optional<NormalizedNode<?, ?>>> read = tx.read(LogicalDatastoreType.CONFIGURATION, path);

        try {
            // TODO HONEYCOMB-169 once the APIs are asynchronous use just Futures.transform
            final Optional<NormalizedNode<?, ?>> optional = read.get();

            if (!optional.isPresent()) {
                return Optional.empty();
            }

            final NormalizedNode<?, ?> data = optional.get();
            final Map.Entry<InstanceIdentifier<?>, DataObject> entry = serializer.fromNormalizedNode(path, data);

            final Class<T> targetType = currentId.getTargetType();
            checkState(targetType.isAssignableFrom(entry.getValue().getClass()),
                "Unexpected data object type, should be: %s, but was: %s", targetType, entry.getValue().getClass());
            return Optional.of(targetType.cast(entry.getValue()));
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Unable to perform read", e);
        }
    }

    @Nonnull
    @Override
    public ModificationCache getModificationCache() {
        return ctx;
    }

    @Nonnull
    @Override
    public MappingContext getMappingContext() {
        return mappingContext;
    }

    @Override
    public void close() {
        ctx.close();
        mappingContext.close();
        beforeTx.close();
        afterTx.close();
    }
}
