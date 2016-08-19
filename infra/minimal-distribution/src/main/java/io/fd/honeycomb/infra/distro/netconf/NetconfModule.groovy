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


    public static final String HONEYCOMB_NETCONF = "honeycomb-netconf"
    public static final String HONEYCOMB_NETCONF_MAPPER_AGGR = "netconf-mapper-aggregator"
    public static final String HONEYCOMB_NETCONF_MAPPER_NOTIF = "netconf-mapper-notification"
    public static final String HONEYCOMB_NETCONF_MAPPER_CORE = "netconf-mapper-honeycomb"
    public static final String HONEYCOMB_NETCONF_MAPPER_OPER = "netconf-mapper-monitoring"

    @Override
    protected void configure() {
        // Create inmemory data store for HONEYCOMB_NETCONF config metadata
        bind(InMemoryDOMDataStore)
                .annotatedWith(Names.named(CONFIG))
                .toProvider(new DataStoreProvider(type: LogicalDatastoreType.CONFIGURATION, name: CONFIG))
                .in(Singleton)
        // Create inmemory data store for HONEYCOMB_NETCONF operational metadata
        bind(InMemoryDOMDataStore)
                .annotatedWith(Names.named(OPERATIONAL))
                .toProvider(new DataStoreProvider(type: LogicalDatastoreType.OPERATIONAL, name: OPERATIONAL))
                .in(Singleton)
        // Wrap datastores as DOMDataBroker
        bind(DOMDataBroker).toProvider(InmemoryDOMDataBrokerProvider).in(Singleton)

        // Wrap DOMDataBroker as BA data broker
        bind(DataBroker)
                .annotatedWith(Names.named(HONEYCOMB_NETCONF))
                .toProvider(BindingDataBrokerProvider)
                .in(Singleton)
        expose(DataBroker).annotatedWith(Names.named(HONEYCOMB_NETCONF))

        // Wrap BA data broker as BindingAwareBroker (requied by HONEYCOMB_NETCONF)
        bind(BindingAwareBroker)
                .annotatedWith(Names.named(HONEYCOMB_NETCONF))
                .toProvider(NetconfBindingBrokerProvider)
                .in(Singleton)

        // Create netconf operation service factory aggregator to aggregate different services
        def factory = new AggregatedNetconfOperationServiceFactory()
        bind(NetconfOperationServiceFactory)
                .annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_AGGR))
                .toInstance(factory)
        bind(NetconfOperationServiceFactoryListener).toInstance(factory)

        // Create netconf notification manager
        def manager = new NetconfNotificationManager()
        bind(NetconfNotificationCollector).toInstance(manager)
        bind(NetconfNotificationRegistry).toInstance(manager)
        bind(NetconfNotificationListener).toInstance(manager)

        // Netconf notification service factory
        bind(NetconfOperationServiceFactory)
                .annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_NOTIF))
                .toProvider(NetconfNotificationMapperProvider)
                .in(Singleton)
        expose(NetconfOperationServiceFactory).annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_NOTIF))

        // Netconf core part - mapping between Honeycomb and Netconf
        bind(NetconfOperationServiceFactory)
                .annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_CORE))
                .toProvider(NetconfMdsalMapperProvider)
                .in(Singleton)
        expose(NetconfOperationServiceFactory).annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_CORE))

        // Netconf monitoring service factory
        bind(NetconfMonitoringService).toProvider(NetconfMonitoringServiceProvider).in(Singleton)
        bind(NetconfOperationServiceFactory)
                .annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_OPER))
                .toProvider(NetconfMonitoringMapperProvider)
                .in(Singleton)
        expose(NetconfOperationServiceFactory).annotatedWith(Names.named(HONEYCOMB_NETCONF_MAPPER_OPER))

        // Create HC notification manager + HC2Netconf translator
        bind(NotificationCollector).toProvider(HoneycombNotificationManagerProvider).in(Singleton)
        bind(HoneycombNotification2NetconfProvider.HoneycombNotification2Netconf)
                .toProvider(HoneycombNotification2NetconfProvider)
                .in(Singleton)
        expose(HoneycombNotification2NetconfProvider.HoneycombNotification2Netconf)

        configureServer()
    }

    /**
     * Provide HONEYCOMB_NETCONF TCP and SSH servers
     */
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
