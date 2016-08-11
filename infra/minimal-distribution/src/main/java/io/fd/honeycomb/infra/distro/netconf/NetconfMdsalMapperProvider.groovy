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

package io.fd.honeycomb.infra.distro.netconf

import com.google.inject.Inject
import com.google.inject.name.Named
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.infra.distro.ProviderTrait
import org.opendaylight.controller.sal.core.api.Broker
import org.opendaylight.controller.sal.core.api.model.SchemaService
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactory
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactoryListener
import org.opendaylight.netconf.mdsal.connector.MdsalNetconfOperationServiceFactory
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext
/**
 * Mirror of org.opendaylight.controller.config.yang.netconf.mdsal.mapper.NetconfMdsalMapperModule
 */
@Slf4j
@ToString
class NetconfMdsalMapperProvider extends ProviderTrait<NetconfOperationServiceFactory> {

    @Inject
    SchemaService schemaService
    @Inject
    NetconfOperationServiceFactoryListener aggregator
    @Inject
    ModuleInfoBackedContext moduleInfoBackedContext

    @Inject
    @Named("honeycomb")
    Broker domBroker

    def create() {
        def mdsalNetconfOperationServiceFactory =
                new MdsalNetconfOperationServiceFactory(schemaService, moduleInfoBackedContext)
        domBroker.registerConsumer(mdsalNetconfOperationServiceFactory);
        aggregator.onAddNetconfOperationServiceFactory(mdsalNetconfOperationServiceFactory);
        return mdsalNetconfOperationServiceFactory;
    }
}
