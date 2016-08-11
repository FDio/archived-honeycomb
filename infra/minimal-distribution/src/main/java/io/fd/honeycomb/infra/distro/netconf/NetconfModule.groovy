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

package io.fd.honeycomb.infra.distro.netconf

import com.google.inject.PrivateModule
import com.google.inject.Singleton
import com.google.inject.name.Names
import io.fd.honeycomb.infra.distro.data.BindingDataBrokerProvider
import io.fd.honeycomb.infra.distro.data.DataStoreProvider
import io.fd.honeycomb.infra.distro.data.HoneycombNotificationManagerProvider
import io.fd.honeycomb.infra.distro.data.InmemoryDOMDataBrokerProvider
import io.fd.honeycomb.notification.NotificationCollector
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.Timer
import org.opendaylight.controller.md.sal.binding.api.DataBroker
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker
import org.opendaylight.netconf.api.NetconfServerDispatcher
import org.opendaylight.netconf.api.monitoring.NetconfMonitoringService
import org.opendaylight.netconf.impl.osgi.AggregatedNetconfOperationServiceFactory
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactory
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactoryListener
import org.opendaylight.netconf.notifications.NetconfNotificationCollector
import org.opendaylight.netconf.notifications.NetconfNotificationListener
import org.opendaylight.netconf.notifications.NetconfNotificationRegistry
import org.opendaylight.netconf.notifications.impl.NetconfNotificationManager

import static InmemoryDOMDataBrokerProvider.CONFIG
import static InmemoryDOMDataBrokerProvider.OPERATIONAL

class NetconfModule extends PrivateModule {

    public static final String NETCONF = "netconf"
    public static final String NETCONF_MAPPER_AGGREGATOR = "netconf-mapper-aggregator"
    public static final String NETCONF_MAPPER_NOTIFICATION = "netconf-mapper-notification"
    public static final String NETCONF_MAPPER_MONITORING = "netconf-mapper-monitoring"
    public static final String NETCONF_MAPPER_HONEYCOMB = "netconf-mapper-honeycomb"

    @Override
    protected void configure() {
        bind(InMemoryDOMDataStore)
                .annotatedWith(Names.named(CONFIG))
                .toProvider(new DataStoreProvider(type: LogicalDatastoreType.CONFIGURATION, name: CONFIG))
                .in(Singleton)
        bind(InMemoryDOMDataStore)
                .annotatedWith(Names.named(OPERATIONAL))
                .toProvider(new DataStoreProvider(type: LogicalDatastoreType.OPERATIONAL, name: OPERATIONAL))
                .in(Singleton)
        bind(DOMDataBroker).toProvider(InmemoryDOMDataBrokerProvider).in(Singleton)

        bind(DataBroker)
                .annotatedWith(Names.named(NETCONF))
                .toProvider(BindingDataBrokerProvider)
                .in(Singleton)
        expose(DataBroker).annotatedWith(Names.named(NETCONF))
        bind(BindingAwareBroker)
                .annotatedWith(Names.named(NETCONF))
                .toProvider(NetconfBindingBrokerProvider)
                .in(Singleton)

        // Mirror of org.opendaylight.controller.config.yang.config.netconf.northbound.impl.NetconfMapperAggregatorModule
        def factory = new AggregatedNetconfOperationServiceFactory()
        bind(NetconfOperationServiceFactory)
                .annotatedWith(Names.named(NETCONF_MAPPER_AGGREGATOR))
                .toInstance(factory)
        bind(NetconfOperationServiceFactoryListener).toInstance(factory)

        // Mirror of org.opendaylight.controller.config.yang.netconf.northbound.notification.impl.NetconfNotificationManagerModule
        def manager = new NetconfNotificationManager()
        bind(NetconfNotificationCollector).toInstance(manager)
        bind(NetconfNotificationRegistry).toInstance(manager)
        bind(NetconfNotificationListener).toInstance(manager)

        // Netconf notification part
        bind(NetconfOperationServiceFactory)
                .annotatedWith(Names.named(NETCONF_MAPPER_NOTIFICATION))
                .toProvider(NetconfNotificationMapperProvider)
                .in(Singleton)
        expose(NetconfOperationServiceFactory).annotatedWith(Names.named(NETCONF_MAPPER_NOTIFICATION))

        // Netconf core part - mapping between Honeycomb and Netconf
        bind(NetconfOperationServiceFactory)
                .annotatedWith(Names.named(NETCONF_MAPPER_HONEYCOMB))
                .toProvider(NetconfMdsalMapperProvider)
                .in(Singleton)
        expose(NetconfOperationServiceFactory).annotatedWith(Names.named(NETCONF_MAPPER_HONEYCOMB))

        // Netconf monitoring part
        bind(NetconfMonitoringService).toProvider(NetconfMonitoringServiceProvider).in(Singleton)
        bind(NetconfOperationServiceFactory)
                .annotatedWith(Names.named(NETCONF_MAPPER_MONITORING))
                .toProvider(NetconfMonitoringMapperProvider)
                .in(Singleton)
        expose(NetconfOperationServiceFactory).annotatedWith(Names.named(NETCONF_MAPPER_MONITORING))

        bind(NotificationCollector).toProvider(HoneycombNotificationManagerProvider).in(Singleton)
        bind(HoneycombNotification2NetconfProvider.HoneycombNotification2Netconf)
                .toProvider(HoneycombNotification2NetconfProvider)
                .in(Singleton)
        expose(HoneycombNotification2NetconfProvider.HoneycombNotification2Netconf)

        configureServer()
    }

    def configureServer() {
        bind(NioEventLoopGroup).toProvider(NettyThreadGroupProvider).in(Singleton)
        bind(Timer).toProvider(NettyTimerProvider).in(Singleton)
        bind(NetconfServerDispatcher).toProvider(NetconfServerDispatcherProvider).in(Singleton)
        bind(NetconfTcpServerProvider.NetconfTcpServer).toProvider(NetconfTcpServerProvider).in(Singleton)
        expose(NetconfTcpServerProvider.NetconfTcpServer)
        bind(NetconfSshServerProvider.NetconfSshServer).toProvider(NetconfSshServerProvider).in(Singleton)
        expose(NetconfSshServerProvider.NetconfSshServer)
    }
}
