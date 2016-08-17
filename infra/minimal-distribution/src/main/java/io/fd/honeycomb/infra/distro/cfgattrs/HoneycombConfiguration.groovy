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
    // HTTP
    @InjectConfig("restconf-http-enabled")
    String restconfHttp

    @InjectConfig("restconf-binding-address")
    Optional<String> restconfBindingAddress
    @InjectConfig("restconf-port")
    Optional<Integer> restconfPort
    // HTTPS
    @InjectConfig("restconf-https-enabled")
    String restconfHttps
    @InjectConfig("restconf-https-binding-address")
    Optional<String> restconfHttpsBindingAddress
    @InjectConfig("restconf-https-port")
    Optional<Integer> restconfHttpsPort

    @InjectConfig("restconf-websocket-port")
    Optional<Integer> restconfWebsocketPort = Optional.of(7779)

    @InjectConfig("restconf-root-path")
    Optional<String> restconfRootPath = Optional.of("/restconf")
    @InjectConfig("restconf-pool-max-size")
    Optional<Integer> restPoolMaxSize = Optional.of(10)
    @InjectConfig("restconf-pool-min-size")
    Optional<Integer> restPoolMinSize = Optional.of(1)

    @InjectConfig("restconf-acceptors-size")
    Optional<Integer> acceptorsSize = Optional.of(1)
    @InjectConfig("restconf-selectors-size")
    Optional<Integer> selectorsSize = Optional.of(1)
    @InjectConfig("restconf-https-acceptors-size")
    Optional<Integer> httpsAcceptorsSize = Optional.of(1)
    @InjectConfig("restconf-https-selectors-size")
    Optional<Integer> httpsSelectorsSize = Optional.of(1)

    // Booleans not supported
    boolean isRestconfHttpEnabled() { Boolean.valueOf(restconfHttp) }
    boolean isRestconfHttpsEnabled() { Boolean.valueOf(restconfHttps) }
    boolean isRestconfEnabled() { isRestconfHttpEnabled() || isRestconfHttpsEnabled() }

    // NETCONF
    @InjectConfig("netconf-netty-threads")
    Integer netconfNettyThreads

    // NETCONF TCP
    @InjectConfig("netconf-tcp-enabled")
    String netconfTcp
    @InjectConfig("netconf-tcp-binding-address")
    Optional<String> netconfTcpBindingAddress
    @InjectConfig("netconf-tcp-binding-port")
    Optional<Integer> netconfTcpBindingPort

    // NETCONF SSH
    @InjectConfig("netconf-ssh-enabled")
    String netconfSsh
    @InjectConfig("netconf-ssh-binding-address")
    Optional<String> netconfSshBindingAddress
    @InjectConfig("netconf-ssh-binding-port")
    Optional<Integer> netconfSshBindingPort

    @InjectConfig("netconf-notification-stream-name")
    Optional<String> netconfNotificationStreamName = Optional.of("honeycomb")

    boolean isNetconfTcpEnabled() { Boolean.valueOf(netconfTcp) }
    boolean isNetconfSshEnabled() { Boolean.valueOf(netconfSsh) }
    boolean isNetconfEnabled() { isNetconfTcpEnabled() || isNetconfSshEnabled() }

    @InjectConfig("username")
    String username
    @InjectConfig("password")
    String password
}
