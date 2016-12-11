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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class RpcRegistryBuilder {

    private Map<SchemaPath, RpcService> services = new HashMap<>();

    public RpcRegistry build() {
        return new RpcRegistryImpl(services);
    }

    public void addService(@Nonnull final RpcService service) {
        services.put(service.getManagedNode(), service);
    }


    private static final class RpcRegistryImpl implements RpcRegistry {
        private final Map<SchemaPath, RpcService> services;

        private RpcRegistryImpl(@Nonnull final Map<SchemaPath, RpcService> services) {
            this.services = services;
        }

        @Override
        @Nonnull
        public CompletionStage invoke(@Nonnull final SchemaPath schemaPath, @Nullable final DataObject request) {
            final RpcService rpcService = services.get(schemaPath);
            if (rpcService == null) {
                final CompletableFuture<DataObject> result = new CompletableFuture<>();
                result.completeExceptionally(
                    new DOMRpcImplementationNotAvailableException("Service not found: %s", schemaPath));
                return result;
            }
            return rpcService.invoke(request);

        }
    }
}
