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
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.Timer
import org.opendaylight.netconf.api.NetconfServerDispatcher
import org.opendaylight.netconf.api.monitoring.NetconfMonitoringService
import org.opendaylight.netconf.impl.NetconfServerDispatcherImpl
import org.opendaylight.netconf.impl.NetconfServerSessionNegotiatorFactory
import org.opendaylight.netconf.impl.SessionIdProvider
import org.opendaylight.netconf.impl.osgi.AggregatedNetconfOperationServiceFactory
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactory

import java.util.concurrent.TimeUnit
/**
 * Mirror of org.opendaylight.controller.config.yang.config.netconf.northbound.impl.NetconfServerDispatcherModule
 */
@Slf4j
@ToString
class NetconfServerDispatcherProvider extends ProviderTrait<NetconfServerDispatcher> {

    // TODO make configurable
    private static final long CONNECTION_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(20)

    @Inject
    @Named(NetconfModule.NETCONF_MAPPER_AGGREGATOR)
    NetconfOperationServiceFactory aggregator
    @Inject
    NetconfMonitoringService monitoringService
    @Inject
    Timer timer
    @Inject
    NioEventLoopGroup nettyThreadgroup


    def create() {
        def netconfOperationProvider = new AggregatedNetconfOperationServiceFactory()
        netconfOperationProvider.onAddNetconfOperationServiceFactory(aggregator)

        def serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                timer, netconfOperationProvider, new SessionIdProvider(), CONNECTION_TIMEOUT_MILLIS, monitoringService);
        def serverChannelInitializer = new NetconfServerDispatcherImpl.ServerChannelInitializer(
                serverNegotiatorFactory);

        new NetconfServerDispatcherImpl(serverChannelInitializer, nettyThreadgroup, nettyThreadgroup)
    }

}
