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

package io.fd.honeycomb.infra.bgp.neighbors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstance;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.NetworkInstanceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Dummy customizer for handling network-instance node required by BGP peer configuration (HC implements the same model
 * as ODL BGP).
 *
 * Ensures that at most one network instance is created.
 *
 * Update and delete is not supported, because HC does not support runtime BGP server reconfiguration.
 */
final class NetworkInstanceCustomizer implements ListWriterCustomizer<NetworkInstance, NetworkInstanceKey> {
    private final String networkInstanceName;

    NetworkInstanceCustomizer(@Nonnull final String networkInstanceName) {
        this.networkInstanceName = checkNotNull(networkInstanceName, "network instance name should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<NetworkInstance> id,
                                       @Nonnull final NetworkInstance dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String instanceName = dataAfter.getName();
        checkArgument(networkInstanceName.equals(instanceName),
            "Only single network instance named %s is supported, but %s was given", networkInstanceName, instanceName);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<NetworkInstance> id,
                                        @Nonnull final NetworkInstance dataBefore,
                                        @Nonnull final NetworkInstance dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
            new UnsupportedOperationException("Network instance update is not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<NetworkInstance> id,
                                        @Nonnull final NetworkInstance dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new WriteFailedException.DeleteFailedException(id,
            new UnsupportedOperationException("Network instance delete is not supported"));
    }
}
