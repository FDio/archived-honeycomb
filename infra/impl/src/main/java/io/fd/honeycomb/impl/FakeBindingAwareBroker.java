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

package io.fd.honeycomb.impl;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.osgi.framework.BundleContext;

/**
 * Binding aware broker wrapping just a DataBroker.
 */
public final class FakeBindingAwareBroker implements BindingAwareBroker, AutoCloseable {

    private DataBroker netconfBindingBrokerDependency;

    public FakeBindingAwareBroker(final DataBroker netconfBindingBrokerDependency) {

        this.netconfBindingBrokerDependency = netconfBindingBrokerDependency;
    }

    @Override
    public ConsumerContext registerConsumer(final BindingAwareConsumer bindingAwareConsumer,
                                            final BundleContext bundleContext) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public ConsumerContext registerConsumer(final BindingAwareConsumer bindingAwareConsumer) {
        final ConsumerContext consumerContext = new ConsumerContext() {
            @Override
            public <T extends BindingAwareService> T getSALService(final Class<T> serviceClass) {
                return serviceClass.equals(DataBroker.class)
                        ? (T) netconfBindingBrokerDependency
                        : null;
            }

            @Override
            public <T extends RpcService> T getRpcService(final Class<T> serviceClass) {
                return null;
            }
        };
        bindingAwareConsumer.onSessionInitialized(consumerContext);
        return consumerContext;
    }

    @Override
    public ProviderContext registerProvider(final BindingAwareProvider bindingAwareProvider,
                                            final BundleContext bundleContext) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public ProviderContext registerProvider(final BindingAwareProvider bindingAwareProvider) {
        final ProviderContext context = new FakeProviderContext(netconfBindingBrokerDependency);
        bindingAwareProvider.onSessionInitiated(context);
        return context;
    }

    @Override
    public void close() throws Exception {
        netconfBindingBrokerDependency = null;
    }

    private static final class FakeProviderContext implements ProviderContext {

        private Object netconfBindingBrokerDependency;

        FakeProviderContext(final DataBroker netconfBindingBrokerDependency) {
            this.netconfBindingBrokerDependency = netconfBindingBrokerDependency;
        }

        @Override
        public <L extends RouteChangeListener<RpcContextIdentifier, InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(
                final L listener) {
            throw new UnsupportedOperationException("Unsupported");
        }

        @Override
        public <T extends RpcService> T getRpcService(final Class<T> serviceClass) {
            throw new UnsupportedOperationException("Unsupported");
        }

        @Override
        public <T extends RpcService> RpcRegistration<T> addRpcImplementation(final Class<T> rpcClass, final T t)
                throws IllegalStateException {
            throw new UnsupportedOperationException("Unsupported");
        }

        @Override
        public <T extends RpcService> RoutedRpcRegistration<T> addRoutedRpcImplementation(
                final Class<T> rpcClass, final T type) throws IllegalStateException {
            throw new UnsupportedOperationException("Unsupported");
        }

        @Override
        public <T extends BindingAwareService> T getSALService(final Class<T> serviceClass) {
            return serviceClass.equals(DataBroker.class)
                    ? (T) netconfBindingBrokerDependency
                    : null;
        }
    }
}
