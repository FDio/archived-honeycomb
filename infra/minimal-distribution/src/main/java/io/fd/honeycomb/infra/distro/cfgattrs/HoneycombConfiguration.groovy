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

package io.fd.honeycomb.infra.distro.cfgattrs

import groovy.transform.ToString
import net.jmob.guice.conf.core.BindConfig
import net.jmob.guice.conf.core.InjectConfig
import net.jmob.guice.conf.core.Syntax

@ToString(includeNames = true)
@BindConfig(value = "honeycomb", syntax = Syntax.JSON)
class HoneycombConfiguration {

    // TODO break into smaller pieces, config, context, rest, netconf etc.

    @InjectConfig("persisted-context-path")
    String peristContextPath
    @InjectConfig("persisted-context-restoration-type")
    String persistedContextRestorationType

    @InjectConfig("persisted-config-path")
    String peristConfigPath
    @InjectConfig("persisted-config-restoration-type")
    String persistedConfigRestorationType

    @InjectConfig("notification-service-queue-depth")
    int notificationServiceQueueDepth

    // RESTCONF
    @InjectConfig("restconf-binding-address")
    String restconfBindingAddress
    @InjectConfig("restconf-port")
    int restconfPort
    @InjectConfig("restconf-https-binding-address")
    String restconfHttpsBindingAddress
    @InjectConfig("restconf-https-port")
    int restconfHttpsPort
    @InjectConfig("restconf-websocket-port")
    int restconfWebsocketPort
    @InjectConfig("restconf-root-path")
    String restconfRootPath

    // NETCONF
    @InjectConfig("netconf-netty-threads")
    Optional<Integer> netconfNettyThreads
    // NETCONF TCP optional
    @InjectConfig("netconf-tcp-binding-address")
    Optional<String> netconfTcpBindingAddress
    @InjectConfig("netconf-tcp-binding-port")
    Optional<Integer> netconfTcpBindingPort
    // NETCONF SSH
    @InjectConfig("netconf-ssh-binding-address")
    String netconfSshBindingAddress
    @InjectConfig("netconf-ssh-binding-port")
    Integer netconfSshBindingPort

    @InjectConfig("netconf-notification-stream-name")
    String netconfNotificationStreamName

    boolean isNetconfTcpServerEnabled() { netconfTcpBindingAddress.isPresent() && netconfTcpBindingPort.isPresent() }

    @InjectConfig("username")
    String username
    @InjectConfig("password")
    String password
}
