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

package io.fd.honeycomb.infra.distro;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import groovy.util.logging.Slf4j;
import io.fd.honeycomb.data.init.DataTreeInitializer;
import io.fd.honeycomb.data.init.InitializerRegistry;
import io.fd.honeycomb.infra.distro.cfgattrs.CfgAttrsModule;
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration;
import io.fd.honeycomb.infra.distro.data.ConfigAndOperationalPipelineModule;
import io.fd.honeycomb.infra.distro.data.context.ContextPipelineModule;
import io.fd.honeycomb.infra.distro.initializer.InitializerPipelineModule;
import io.fd.honeycomb.infra.distro.netconf.HoneycombNotification2NetconfProvider;
import io.fd.honeycomb.infra.distro.netconf.NetconfModule;
import io.fd.honeycomb.infra.distro.netconf.NetconfReadersModule;
import io.fd.honeycomb.infra.distro.netconf.NetconfSshServerProvider;
import io.fd.honeycomb.infra.distro.netconf.NetconfTcpServerProvider;
import io.fd.honeycomb.infra.distro.restconf.RestconfModule;
import io.fd.honeycomb.infra.distro.schema.SchemaModule;
import io.fd.honeycomb.infra.distro.schema.YangBindingProviderModule;
import java.util.List;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.netconf.sal.rest.api.RestConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static final List<Module> BASE_MODULES = ImmutableList.of(
            // Infra
            new YangBindingProviderModule(),
            new SchemaModule(),
            new ConfigAndOperationalPipelineModule(),
            new ContextPipelineModule(),
            new InitializerPipelineModule(),
            new NetconfModule(),
            new NetconfReadersModule(),
            new RestconfModule(),
            // Json config attributes
            new CfgAttrsModule());

    public static void main(String[] args) {
        // TODO add "clean" argument
        init(BASE_MODULES);
    }

    public static Injector init(final List<? extends Module> modules) {
        LOG.info("Starting honeycomb");

        Injector injector = Guice.createInjector(modules);
        LOG.info("Honeycomb configuration: " + injector.getInstance(HoneycombConfiguration.class));

        // Log all bindings
        injector.getAllBindings().entrySet().stream()
                .forEach(e -> LOG.trace("Component available under: {} is {}", e.getKey(), e.getValue()));

        final HoneycombConfiguration cfgAttributes = injector.getInstance(HoneycombConfiguration.class);

        // Now get instances for all dependency roots

        LOG.info("Starting RESTCONF");
        injector.getInstance(RestConnector.class);

        LOG.info("Starting NETCONF");
        injector.getInstance(
                Key.get(NetconfOperationServiceFactory.class, Names.named("netconf-mapper-honeycomb")));
        injector.getInstance(
                Key.get(NetconfOperationServiceFactory.class, Names.named("netconf-mapper-notification")));
        injector.getInstance(
                Key.get(NetconfOperationServiceFactory.class, Names.named("netconf-mapper-monitoring")));

        if (cfgAttributes.isNetconfTcpServerEnabled()) {
            injector.getInstance(NetconfTcpServerProvider.NetconfTcpServer.class);
        }

        injector.getInstance(NetconfSshServerProvider.NetconfSshServer.class);
        injector.getInstance(HoneycombNotification2NetconfProvider.HoneycombNotification2Netconf.class);

        LOG.info("Honeycomb started successfully!");

        try {
            LOG.info("Initializing configuration");
            injector.getInstance(Key.get(InitializerRegistry.class, Names.named("honeycomb-initializer"))).initialize();
            LOG.info("Configuration initialized successfully");
        } catch (DataTreeInitializer.InitializeException e) {
            LOG.error("Unable to initialize configuration", e);
        }

        LOG.info("Honeycomb started successfully!");

        return injector;
    }
}
