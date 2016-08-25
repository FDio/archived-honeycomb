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

package io.fd.honeycomb.infra.distro.netconf;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.infra.distro.ProviderTrait;
import java.lang.reflect.Constructor;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactoryListener;
import org.opendaylight.netconf.mdsal.notification.NetconfNotificationOperationServiceFactory;
import org.opendaylight.netconf.notifications.BaseNotificationPublisherRegistration;
import org.opendaylight.netconf.notifications.NetconfNotificationCollector;
import org.opendaylight.netconf.notifications.NetconfNotificationRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Capabilities;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NetconfNotificationMapperProvider extends ProviderTrait<NetconfOperationServiceFactory> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfNotificationMapperProvider.class);

    public static final InstanceIdentifier<Capabilities> capabilitiesIdentifier =
            InstanceIdentifier.create(NetconfState.class).child(Capabilities.class).builder().build();
    @Inject
    private NetconfNotificationCollector notificationCollector;
    @Inject
    private NetconfNotificationRegistry notificationRegistry;
    @Inject
    @Named(NetconfModule.HONEYCOMB_NETCONF)
    private BindingAwareBroker bindingAwareBroker;
    @Inject
    @Named(NetconfModule.HONEYCOMB_NETCONF)
    private DataBroker dataBroker;
    @Inject
    private NetconfOperationServiceFactoryListener aggregator;

    @Override
    protected NetconfNotificationOperationServiceFactory create() {
        try {
            final Class<?> notificationWriter = Class.forName(
                    "org.opendaylight.controller.config.yang.netconf.mdsal.notification.NotificationToMdsalWriter");
            Constructor<?> declaredConstructor =
                    notificationWriter.getDeclaredConstructor(NetconfNotificationCollector.class);
            declaredConstructor.setAccessible(true);
            final BindingAwareProvider writer =
                    (BindingAwareProvider) declaredConstructor.newInstance(notificationCollector);
            bindingAwareBroker.registerProvider(writer);

            final Class<?> notifPublisherCls = Class.forName(
                    "org.opendaylight.controller.config.yang.netconf.mdsal.notification.BaseCapabilityChangeNotificationPublisher");
            declaredConstructor =
                    notifPublisherCls.getDeclaredConstructor(BaseNotificationPublisherRegistration.class);
            declaredConstructor.setAccessible(true);
            final DataChangeListener publisher = (DataChangeListener) declaredConstructor.newInstance(
                    notificationCollector.registerBaseNotificationPublisher());

            ListenerRegistration<DataChangeListener> capabilityChangeListenerRegistration = dataBroker
                    .registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, capabilitiesIdentifier,
                            publisher, AsyncDataBroker.DataChangeScope.SUBTREE);
            NetconfNotificationOperationServiceFactory netconfNotificationOperationServiceFactory =
                    new NetconfNotificationOperationServiceFactory(notificationRegistry);
            aggregator.onAddNetconfOperationServiceFactory(netconfNotificationOperationServiceFactory);

            return netconfNotificationOperationServiceFactory;
        } catch (final ReflectiveOperationException e) {
            final String msg = "Unable to instantiate notification mapper using reflection";
            LOG.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }
}
