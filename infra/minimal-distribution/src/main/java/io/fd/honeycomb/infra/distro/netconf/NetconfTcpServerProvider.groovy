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
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration
import io.fd.honeycomb.infra.distro.ProviderTrait
import io.netty.channel.ChannelFuture
import io.netty.util.concurrent.GenericFutureListener
import org.opendaylight.netconf.api.NetconfServerDispatcher
/**
 * Mirror of org.opendaylight.controller.config.yang.netconf.northbound.tcp.NetconfNorthboundTcpModule
 */
@Slf4j
@ToString
class NetconfTcpServerProvider extends ProviderTrait<NetconfTcpServer> {

    @Inject
    NetconfServerDispatcher dispatcher
    @Inject
    HoneycombConfiguration cfgAttributes

    @Override
    def create() {

        def name = InetAddress.getByName(cfgAttributes.netconfTcpBindingAddress.get())
        def unresolved = new InetSocketAddress(name, cfgAttributes.netconfTcpBindingPort.get())

        def tcpServer = dispatcher.createServer(unresolved)

        tcpServer.addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isDone() && future.isSuccess()) {
                    log.info("Netconf TCP endpoint started successfully at {}", unresolved)
                } else {
                    log.warn("Unable to start TCP netconf server at {}", unresolved, future.cause())
                    throw new RuntimeException("Unable to start TCP netconf server", future.cause())
                }
            }
        })

        new NetconfTcpServer(tcpServer: tcpServer)
    }

    static class NetconfTcpServer {
        def tcpServer
    }
}
