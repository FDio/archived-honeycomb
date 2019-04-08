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

package io.fd.honeycomb.rpc;

import static net.javacrumbs.futureconverter.java8guava.FutureConverter.toListenableFuture;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.mdsal.dom.api.DOMRpcResult;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class HoneycombDOMRpcService implements DOMRpcService {

    private final BindingNormalizedNodeSerializer serializer;
    private final RpcRegistry rpcRegistry;

    public HoneycombDOMRpcService(@Nonnull final BindingNormalizedNodeSerializer serializer,
                                  @Nonnull final RpcRegistry rpcRegistry) {
        this.serializer = serializer;
        this.rpcRegistry = rpcRegistry;
    }

    @Nonnull
    @Override
    public FluentFuture<DOMRpcResult> invokeRpc(@Nonnull final SchemaPath schemaPath,
                                                @Nullable final NormalizedNode<?, ?> normalizedNode) {
        DataObject input = null;
        if (normalizedNode != null) {
            // RPC input is optional
            final SchemaPath nodePatch = schemaPath.createChild(normalizedNode.getNodeType());
            input = serializer.fromNormalizedNodeRpcData(nodePatch, (ContainerNode) normalizedNode);
        }
        final CompletableFuture<DataObject> result = rpcRegistry.invoke(schemaPath, input).toCompletableFuture();
        final ListenableFuture<DOMRpcResult> output = getDOMRpcResult(toListenableFuture(result));
        return FluentFuture.from(output);
    }

    private ListenableFuture<DOMRpcResult> getDOMRpcResult(final ListenableFuture<DataObject> invoke) {
        return Futures.transform(
            invoke,
            output -> {
                // If result is available convert it to BI form. Otherwise pass null as DOMRpcResult expects.
                ContainerNode outputNode = null;
                if (output != null) {
                    outputNode = serializer.toNormalizedNodeRpcData(output);
                }
                return new DefaultDOMRpcResult(outputNode);
            }, MoreExecutors.directExecutor());
    }

    @Nonnull
    @Override
    public <T extends DOMRpcAvailabilityListener> ListenerRegistration<T> registerRpcListener(@Nonnull final T t) {
        return new ListenerRegistration<T>() {
            @Override
            public void close() {
                // Noop
            }

            @Override
            public T getInstance() {
                return t;
            }
        };
    }
}
