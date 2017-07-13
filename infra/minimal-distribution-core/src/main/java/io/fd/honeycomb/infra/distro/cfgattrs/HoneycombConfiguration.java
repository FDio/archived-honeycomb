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

package io.fd.honeycomb.infra.distro.cfgattrs;

import com.google.common.base.MoreObjects;
import java.util.Optional;
import net.jmob.guice.conf.core.BindConfig;
import net.jmob.guice.conf.core.InjectConfig;
import net.jmob.guice.conf.core.Syntax;

/**
 * This is the Java equivalent for honeyconb.json file. We use guice-config library to load all the config attributes
 * into this class instance.
 *
 * The BindConfig annotation tells that honeycomb.json file should be looked up on classpath root.
 */
@BindConfig(value = "honeycomb", syntax = Syntax.JSON)
public class HoneycombConfiguration {

    public boolean isNetconfTcpEnabled() {
        return Boolean.valueOf(netconfTcp);
    }

    public boolean isNetconfSshEnabled() {
        return Boolean.valueOf(netconfSsh);
    }

    public boolean isNetconfEnabled() {
        return isNetconfTcpEnabled() || isNetconfSshEnabled();
    }

    public boolean isConfigPersistenceEnabled() {
        return persistConfig.isPresent() && Boolean.valueOf(persistConfig.get());
    }
    public boolean isContextPersistenceEnabled() {
        return persistContext.isPresent() && Boolean.valueOf(persistContext.get());
    }

    @InjectConfig("persist-context")
    public Optional<String> persistContext = Optional.of("true");
    @InjectConfig("persisted-context-path")
    public String peristContextPath;
    @InjectConfig("persisted-context-restoration-type")
    public String persistedContextRestorationType;
    @InjectConfig("persist-config")
    public Optional<String> persistConfig = Optional.of("true");
    @InjectConfig("persisted-config-path")
    public String peristConfigPath;
    @InjectConfig("persisted-config-restoration-type")
    public String persistedConfigRestorationType;
    @InjectConfig("notification-service-queue-depth")
    public int notificationServiceQueueDepth;

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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("peristContextPath", peristContextPath)
                .add("persistedContextRestorationType", persistedContextRestorationType)
                .add("peristConfigPath", peristConfigPath)
                .add("persistedConfigRestorationType", persistedConfigRestorationType)
                .add("notificationServiceQueueDepth", notificationServiceQueueDepth)
                .add("netconfNettyThreads", netconfNettyThreads)
                .add("netconfTcp", netconfTcp)
                .add("netconfTcpBindingAddress", netconfTcpBindingAddress)
                .add("netconfTcpBindingPort", netconfTcpBindingPort)
                .add("netconfSsh", netconfSsh)
                .add("netconfSshBindingAddress", netconfSshBindingAddress)
                .add("netconfSshBindingPort", netconfSshBindingPort)
                .add("netconfNotificationStreamName", netconfNotificationStreamName)
                .toString();
    }
}
