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

package io.fd.honeycomb.northbound;

import java.util.Optional;
import net.jmob.guice.conf.core.BindConfig;
import net.jmob.guice.conf.core.InjectConfig;
import net.jmob.guice.conf.core.Syntax;

@BindConfig(value = "netconf", syntax = Syntax.JSON)
public class NetconfConfiguration {

    @InjectConfig("netconf-netty-threads")
    public Integer netconfNettyThreads;
    @InjectConfig("netconf-tcp-enabled")
    public String netconfTcp;
    @InjectConfig("netconf-tcp-binding-address")
    public Optional<String> netconfTcpBindingAddress;
    @InjectConfig("netconf-tcp-binding-port")
    public Optional<Integer> netconfTcpBindingPort;
    @InjectConfig("netconf-ssh-enabled")
    public String netconfSsh;
    @InjectConfig("netconf-ssh-binding-address")
    public Optional<String> netconfSshBindingAddress;
    @InjectConfig("netconf-ssh-binding-port")
    public Optional<Integer> netconfSshBindingPort;
    @InjectConfig("netconf-notification-stream-name")
    public Optional<String> netconfNotificationStreamName = Optional.of("honeycomb");

    public boolean isNetconfTcpEnabled() {
        return Boolean.valueOf(netconfTcp);
    }

    public boolean isNetconfSshEnabled() {
        return Boolean.valueOf(netconfSsh);
    }

    public boolean isNetconfEnabled() {
        return isNetconfTcpEnabled() || isNetconfSshEnabled();
    }

    @Override
    public String toString() {
        return "NetconfConfiguration{"
            + "netconfNettyThreads=" + netconfNettyThreads
            + ", netconfTcp='" + netconfTcp + '\''
            + ", netconfTcpBindingAddress=" + netconfTcpBindingAddress
            + ", netconfTcpBindingPort=" + netconfTcpBindingPort
            + ", netconfSsh='" + netconfSsh + '\''
            + ", netconfSshBindingAddress=" + netconfSshBindingAddress
            + ", netconfSshBindingPort=" + netconfSshBindingPort
            + ", netconfNotificationStreamName=" + netconfNotificationStreamName
            + '}';
    }
}
