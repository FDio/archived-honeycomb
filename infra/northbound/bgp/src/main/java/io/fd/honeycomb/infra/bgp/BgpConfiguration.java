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

import com.google.common.base.MoreObjects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.jmob.guice.conf.core.BindConfig;
import net.jmob.guice.conf.core.InjectConfig;
import net.jmob.guice.conf.core.Syntax;
import org.opendaylight.protocol.bgp.rib.impl.config.PeerGroupConfigLoader;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.neighbor.group.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroup;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.peer.group.PeerGroupBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.rev151009.bgp.top.Bgp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * This is the Java equivalent for bgp.json file. We use guice-config library to load all the config attributes into
 * this class instance.
 *
 * The BindConfig annotation tells that bgp.json file should be looked up on classpath root.
 */
@BindConfig(value = "bgp", syntax = Syntax.JSON)
public class BgpConfiguration implements PeerGroupConfigLoader {

    @InjectConfig("bgp-binding-address")
    public Optional<String> bgpBindingAddress;
    @InjectConfig("bgp-port")
    public Optional<Integer> bgpPort;
    @InjectConfig("bgp-as-number")
    public Optional<Integer> bgpAsNumber;
    @InjectConfig("bgp-receive-multiple-paths")
    public Optional<String> bgpMultiplePaths;
    @InjectConfig("bgp-send-max-paths")
    public Optional<Integer> bgpSendMaxMaths;
    @InjectConfig("bgp-network-instance-name")
    public String bgpNetworkInstanceName;
    @InjectConfig("bgp-protocol-instance-name")
    public Optional<String> bgpProtocolInstanceName;
    @InjectConfig("bgp-netty-threads")
    public Integer bgpNettyThreads;

    public boolean isBgpMultiplePathsEnabled() {
        return Boolean.valueOf(bgpMultiplePaths.get());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("bgpBindingAddress", bgpBindingAddress)
            .add("bgpPort", bgpPort)
            .add("bgpAsNumber", bgpAsNumber)
            .add("bgpMultiplePaths", bgpMultiplePaths)
            .add("bgpSendMaxMaths", bgpSendMaxMaths)
            .add("bgpNetworkInstanceName", bgpNetworkInstanceName)
            .add("bgpProtocolInstanceName", bgpProtocolInstanceName)
            .add("bgpNettyThreads", bgpNettyThreads)
            .toString();
    }

    @Nullable
    @Override
    public PeerGroup getPeerGroup(@Nonnull final InstanceIdentifier<Bgp> instanceIdentifier,
                                  @Nonnull final String neighbor) {
        return new PeerGroupBuilder().setConfig(new ConfigBuilder().build()).build();
    }
}
