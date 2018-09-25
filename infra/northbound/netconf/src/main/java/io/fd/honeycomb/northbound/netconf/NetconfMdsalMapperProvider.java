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

package io.fd.honeycomb.northbound.netconf;

import static io.fd.honeycomb.infra.distro.data.ConfigAndOperationalPipelineModule.HONEYCOMB_CONFIG;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.binding.init.ProviderTrait;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactoryListener;
import org.opendaylight.netconf.mdsal.connector.MdsalNetconfOperationServiceFactory;

public final class NetconfMdsalMapperProvider extends ProviderTrait<NetconfOperationServiceFactory> {

    @Inject
    private DOMSchemaService schemaService;
    @Inject
    private NetconfOperationServiceFactoryListener aggregator;
    @Inject
    @Named(HONEYCOMB_CONFIG)
    private DOMDataBroker domBroker;
    @Inject
    private NetconfOperationServiceFactoryListener netconfOperationServiceFactoryListener;
    @Inject
    private DOMRpcService rpcService;

    @Override
    protected MdsalNetconfOperationServiceFactory create() {
        MdsalNetconfOperationServiceFactory mdsalNetconfOperationServiceFactory =
                new MdsalNetconfOperationServiceFactory(schemaService, netconfOperationServiceFactoryListener,
                        domBroker, rpcService);
        return mdsalNetconfOperationServiceFactory;
    }
}
