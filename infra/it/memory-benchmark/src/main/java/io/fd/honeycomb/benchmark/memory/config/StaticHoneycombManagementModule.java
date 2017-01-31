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
package io.fd.honeycomb.benchmark.memory.config;

import io.fd.honeycomb.management.jmx.ConnectorServerProvider;
import io.fd.honeycomb.management.jmx.HoneycombManagementConfig;
import io.fd.honeycomb.management.jmx.HoneycombManagementModule;
import io.fd.honeycomb.management.jmx.JMXServiceUrlProvider;
import org.eclipse.jetty.jmx.ConnectorServer;
import org.eclipse.jetty.jmx.MBeanContainer;

import javax.management.remote.JMXServiceURL;
import java.lang.management.ManagementFactory;

/**
 * In this case we need to override Honeycomb config, but if standard management module is active,
 * configuration module injection will cause attempt to inject attributes in HoneycombConfiguration,
 * even if static instance is provider. Therefore here we need to override configure to also provide
 * static instance for HoneycombManagementConfig
 */
public class StaticHoneycombManagementModule extends HoneycombManagementModule {

    @Override
    protected void configure() {
        // all values are wrapped in Optionals with default values
        bind(HoneycombManagementConfig.class).toInstance(new HoneycombManagementConfig());
        bind(MBeanContainer.class).toInstance(new MBeanContainer(ManagementFactory.getPlatformMBeanServer()));
        bind(JMXServiceURL.class).toProvider(JMXServiceUrlProvider.class);
        bind(ConnectorServer.class).toProvider(ConnectorServerProvider.class).asEagerSingleton();

        showAvailableBeans();
    }
}
