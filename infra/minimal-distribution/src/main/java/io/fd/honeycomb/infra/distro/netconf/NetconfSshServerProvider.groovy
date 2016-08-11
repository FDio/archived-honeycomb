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
import io.netty.channel.local.LocalAddress
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.concurrent.GenericFutureListener
import io.netty.util.concurrent.GlobalEventExecutor
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider
import org.opendaylight.netconf.api.NetconfServerDispatcher
import org.opendaylight.netconf.ssh.SshProxyServer
import org.opendaylight.netconf.ssh.SshProxyServerConfigurationBuilder

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
/**
 * Mirror of org.opendaylight.controller.config.yang.netconf.northbound.ssh.NetconfNorthboundSshModule
 */
@Slf4j
@ToString
class NetconfSshServerProvider extends ProviderTrait<NetconfSshServer> {

    @Inject
    NetconfServerDispatcher dispatcher
    @Inject
    HoneycombConfiguration cfgAttributes
    @Inject
    NioEventLoopGroup nettyThreadgroup

    // TODO merge with other executors .. one of the brokers creates also 2 internal executors
    private ScheduledExecutorService pool = Executors.newScheduledThreadPool(1)

    @Override
    def create() {
        def name = InetAddress.getByName(cfgAttributes.netconfSshBindingAddress)
        def bindingAddress = new InetSocketAddress(name, cfgAttributes.netconfSshBindingPort)

        def localAddress = new LocalAddress(cfgAttributes.netconfSshBindingPort.toString())
        def localServer = dispatcher.createLocalServer(localAddress)

        def sshProxyServer = new SshProxyServer(pool, nettyThreadgroup, GlobalEventExecutor.INSTANCE)

        def sshConfigBuilder = new SshProxyServerConfigurationBuilder()
        sshConfigBuilder.bindingAddress = bindingAddress
        sshConfigBuilder.localAddress = localAddress
        // TODO only simple authProvider checking ConfigAttributes
        sshConfigBuilder.authenticator = { String uname, String passwd ->
            cfgAttributes.username == uname && cfgAttributes.password == passwd
        }
        sshConfigBuilder.idleTimeout = Integer.MAX_VALUE
        sshConfigBuilder.keyPairProvider = new PEMGeneratorHostKeyProvider()

        localServer.addListener(new GenericFutureListener<ChannelFuture>() {

            @Override
            public void operationComplete(final ChannelFuture future) {
                if(future.isDone() && !future.isCancelled()) {
                    try {
                        sshProxyServer.bind(sshConfigBuilder.createSshProxyServerConfiguration())
                        log.info "Netconf SSH endpoint started successfully at {}", bindingAddress
                    } catch (final IOException e) {
                        throw new RuntimeException("Unable to start SSH netconf server", e)
                    }
                } else {
                    log.warn "Unable to start SSH netconf server at {}", bindingAddress, future.cause()
                    throw new RuntimeException("Unable to start SSH netconf server", future.cause())
                }
            }
        })

        return new NetconfSshServer(localServer: localServer, sshProxyServer: sshProxyServer)
    }

    static class NetconfSshServer {
        def localServer
        def sshProxyServer
    }
}
