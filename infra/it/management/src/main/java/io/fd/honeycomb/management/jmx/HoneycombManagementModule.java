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

package io.fd.honeycomb.management.jmx;


import com.google.inject.AbstractModule;
import net.jmob.guice.conf.core.ConfigurationModule;
import org.eclipse.jetty.jmx.ConnectorServer;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectInstance;
import javax.management.remote.JMXServiceURL;
import java.lang.management.ManagementFactory;

public class HoneycombManagementModule extends AbstractModule {

    private static final Logger LOG = LoggerFactory.getLogger(HoneycombManagementModule.class);

    @Override
    protected void configure() {
        install(ConfigurationModule.create());
        requestInjection(HoneycombManagementConfig.class);
        bind(MBeanContainer.class).toInstance(new MBeanContainer(ManagementFactory.getPlatformMBeanServer()));
        bind(JMXServiceURL.class).toProvider(JMXServiceUrlProvider.class);
        // .asEagerSingleton(); will cause also start defined in provider
        bind(ConnectorServer.class).toProvider(ConnectorServerProvider.class).asEagerSingleton();

        showAvailableBeans();
    }

    /**
     * Prints all available JMX beans
     */
    protected static void showAvailableBeans() {
        for (final ObjectInstance instance : ManagementFactory.getPlatformMBeanServer().queryMBeans(null, null)) {
            LOG.info("MBean Found:");
            LOG.info("Class Name:{}", instance.getClassName());
            LOG.info("Object Name:{}", instance.getObjectName());
            LOG.info("****************************************");
        }
    }
}
