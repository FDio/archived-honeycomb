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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.ConfigurationException;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;
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
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.netconf.sal.rest.api.RestConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Initialize the Honeycomb infrastructure + all wired plugins.
     */
    public static Injector init(final List<? extends Module> modules) {
        try {
            LOG.info("Starting honeycomb");
            Injector injector = Guice.createInjector(modules);
            LOG.info("Honeycomb configuration: " + injector.getInstance(HoneycombConfiguration.class));

            // Log all bindings
            injector.getAllBindings().entrySet().stream()
                    .forEach(e -> LOG.trace("Component available under: {} is {}", e.getKey(), e.getValue()));

            final HoneycombConfiguration cfgAttributes = injector.getInstance(HoneycombConfiguration.class);
            Preconditions.checkArgument(cfgAttributes.isRestconfEnabled() || cfgAttributes.isNetconfEnabled(),
                    "At least one interface(Restconf|Netconf) has to be enabled for Honeycomb");
            // Now get instances for all dependency roots

            if (cfgAttributes.isRestconfEnabled()) {
                LOG.info("Starting RESTCONF");
                final Server server = injector.getInstance(Server.class);
                final RestConnector instance = injector.getInstance(RestConnector.class);

                if (cfgAttributes.isRestconfHttpEnabled()) {
                    LOG.info("Starting Restconf on http");
                    injector.getInstance(Key.get(ServerConnector.class, Names.named(RestconfModule.RESTCONF_HTTP)));
                }
                if (cfgAttributes.isRestconfHttpsEnabled()) {
                    LOG.info("Starting Restconf on https");
                    injector.getInstance(Key.get(ServerConnector.class, Names.named(RestconfModule.RESTCONF_HTTPS)));
                }

                try {
                    server.start();
                } catch (Exception e) {
                    LOG.error("Unable to start Restconf", e);
                    throw new RuntimeException("Unable to start Restconf", e);
                }
            }

            if (cfgAttributes.isNetconfEnabled()) {
                LOG.info("Starting HONEYCOMB_NETCONF");
                injector.getInstance(Key.get(NetconfOperationServiceFactory.class,
                        Names.named(NetconfModule.HONEYCOMB_NETCONF_MAPPER_CORE)));
                injector.getInstance(Key.get(NetconfOperationServiceFactory.class,
                        Names.named(NetconfModule.HONEYCOMB_NETCONF_MAPPER_NOTIF)));
                injector.getInstance(Key.get(NetconfOperationServiceFactory.class,
                        Names.named(NetconfModule.HONEYCOMB_NETCONF_MAPPER_OPER)));

                if (cfgAttributes.isNetconfTcpEnabled()) {
                    LOG.info("Starting HONEYCOMB_NETCONF TCP");
                    injector.getInstance(NetconfTcpServerProvider.NetconfTcpServer.class);
                }

                if (cfgAttributes.isNetconfSshEnabled()) {
                    LOG.info("Starting HONEYCOMB_NETCONF SSH");
                    injector.getInstance(NetconfSshServerProvider.NetconfSshServer.class);
                }
                injector.getInstance(HoneycombNotification2NetconfProvider.HoneycombNotification2Netconf.class);
            }

            LOG.info("Honeycomb started successfully!");

            try {
                LOG.info("Initializing configuration");
                injector.getInstance(Key.get(InitializerRegistry.class,
                        Names.named(InitializerPipelineModule.HONEYCOMB_INITIALIZER))).initialize();
                LOG.info("Configuration initialized successfully");
            } catch (DataTreeInitializer.InitializeException e) {
                LOG.error("Unable to initialize configuration", e);
            }

            LOG.info("Honeycomb started successfully!");

            return injector;
        } catch (CreationException | ProvisionException | ConfigurationException e) {
            LOG.error("Failed to initialize Honeycomb components", e);
            throw e;
        } catch (RuntimeException e) {
            LOG.error("Unexpected initialization failure", e);
            throw e;
        } finally {
            // Trigger gc to force collect initial garbage + dedicated classloader
            System.gc();
        }
    }
}
