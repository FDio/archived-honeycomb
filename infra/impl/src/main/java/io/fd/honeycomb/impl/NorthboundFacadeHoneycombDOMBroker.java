/*
 * Copyright (c) 2015, 2017 Cisco and/or its affiliates.
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

package io.fd.honeycomb.impl;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.osgi.framework.BundleContext;

/**
 * Implementation of dom broker to facade VPP pipeline for northbound APIs.
 */
public class NorthboundFacadeHoneycombDOMBroker implements AutoCloseable, Broker {

    private Map<Class<? extends BrokerService>, BrokerService> services;

    public NorthboundFacadeHoneycombDOMBroker(@Nonnull final DOMDataBroker domDataBrokerDependency,
                                              @Nonnull final SchemaService schemaBiService,
                                              @Nonnull final DOMNotificationService domNotificatioNService,
                                              @Nonnull final DOMRpcService domRpcService,
                                              @Nonnull final DOMMountPointService domMountPointService) {
        services = Maps.newHashMap();
        services.put(DOMDataBroker.class, domDataBrokerDependency);
        services.put(SchemaService.class, schemaBiService);
        services.put(DOMNotificationService.class, domNotificatioNService);
        services.put(DOMNotificationPublishService.class, domNotificatioNService);
        services.put(DOMRpcService.class, domRpcService);
        // Required to be present by Restconf northbound even if not used:
        services.put(DOMMountPointService.class, domMountPointService);
    }

    @Override
    public void close() throws Exception {
        // NOOP
    }

    @Override
    public ConsumerSession registerConsumer(final Consumer consumer) {
        final SimpleConsumerSession session = new SimpleConsumerSession(services);
        consumer.onSessionInitiated(session);
        return session;
    }

    @Override
    public ConsumerSession registerConsumer(final Consumer consumer, final BundleContext bundleContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProviderSession registerProvider(final Provider provider) {
        final SimpleProviderSession session = new SimpleProviderSession(services);
        provider.onSessionInitiated(session);
        return session;
    }

    @Override
    public ProviderSession registerProvider(final Provider provider, final BundleContext bundleContext) {
        throw new UnsupportedOperationException();
    }

    @NotThreadSafe
    private static class SimpleConsumerSession implements ConsumerSession {
        private boolean closed;
        private final Map<Class<? extends BrokerService>, BrokerService> services;

        private SimpleConsumerSession(final Map<Class<? extends BrokerService>, BrokerService> services) {
            this.services = services;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public <T extends BrokerService> T getService(final Class<T> serviceClass) {
            return (T)services.get(serviceClass);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @NotThreadSafe
    private static class SimpleProviderSession implements ProviderSession {
        private boolean closed;
        private final Map<Class<? extends BrokerService>, BrokerService> services;

        private SimpleProviderSession(final Map<Class<? extends BrokerService>, BrokerService> services) {
            this.services = services;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public <T extends BrokerService> T getService(final Class<T> serviceClass) {
            return (T)services.get(serviceClass);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
