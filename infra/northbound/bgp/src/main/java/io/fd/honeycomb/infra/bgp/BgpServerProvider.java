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

package io.fd.honeycomb.infra.bgp;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPDispatcher;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPSessionPreferences;
import org.opendaylight.protocol.bgp.rib.impl.spi.PeerRegistryListener;
import org.opendaylight.protocol.concepts.KeyMapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BgpServerProvider  extends ProviderTrait<BgpServerProvider.BgpServer> {
    private static final Logger LOG = LoggerFactory.getLogger(BgpServerProvider.class);
    @Inject
    private BgpConfiguration cfg;
    @Inject
    private BGPPeerRegistry peerRegistry;
    @Inject
    private BGPDispatcher dispatcher;

    @Override
    protected BgpServer create() {
        // code based on org.opendaylight.controller.config.yang.bgp.rib.impl.BGPPeerAcceptorModule from Boron-SR3
        final InetAddress bindingAddress;
        try {
            bindingAddress = InetAddress.getByName(cfg.bgpBindingAddress.get());
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Illegal BGP binding address", e);
        }
        final InetSocketAddress address = new InetSocketAddress(bindingAddress, cfg.bgpPort.get());
        LOG.debug("Creating BgpServer for {}", address);
        final ChannelFuture localServer = dispatcher.createServer(peerRegistry, address);
        localServer.addListener(future -> {
            Preconditions.checkArgument(future.isSuccess(), "Unable to start bgp server on %s", address, future.cause());
            final Channel channel = localServer.channel();
            if (Epoll.isAvailable()) {
                peerRegistry.registerPeerRegisterListener(new PeerRegistryListenerImpl(channel.config()));
            }
        });
        final BgpServer server = new BgpServer(localServer);
        LOG.debug("BgpServer successfully started.");
        return server;
    }

    public static final class BgpServer {
        private ChannelFuture localServer;

        BgpServer(final ChannelFuture localServer) {
            this.localServer = localServer;
        }

        public ChannelFuture getLocalServer() {
            return localServer;
        }
    }

    private static final class PeerRegistryListenerImpl implements PeerRegistryListener {
        private final ChannelConfig channelConfig;
        private final KeyMapping keys;

        PeerRegistryListenerImpl(final ChannelConfig channelConfig) {
            this.channelConfig = channelConfig;
            this.keys = KeyMapping.getKeyMapping();
        }
        @Override
        public void onPeerAdded(final IpAddress ip, final BGPSessionPreferences prefs) {
            if (prefs.getMd5Password().isPresent()) {
                this.keys.put(IetfInetUtil.INSTANCE.inetAddressFor(ip), prefs.getMd5Password().get());
                this.channelConfig.setOption(EpollChannelOption.TCP_MD5SIG, this.keys);
            }
        }
        @Override
        public void onPeerRemoved(final IpAddress ip) {
            if (this.keys.remove(IetfInetUtil.INSTANCE.inetAddressFor(ip)) != null) {
                this.channelConfig.setOption(EpollChannelOption.TCP_MD5SIG, this.keys);
            }
        }
    }
}
