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

import com.google.inject.Inject
import com.google.inject.name.Named
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.infra.distro.ProviderTrait
import org.opendaylight.controller.config.yang.netconf.mdsal.notification.BaseCapabilityChangeNotificationPublisher
import org.opendaylight.controller.config.yang.netconf.mdsal.notification.NotificationToMdsalWriter
import org.opendaylight.controller.md.sal.binding.api.DataBroker
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactory
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactoryListener
import org.opendaylight.netconf.mdsal.notification.NetconfNotificationOperationServiceFactory
import org.opendaylight.netconf.notifications.NetconfNotificationCollector
import org.opendaylight.netconf.notifications.NetconfNotificationRegistry
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Capabilities
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
/**
 * Mirror of org.opendaylight.controller.config.yang.netconf.mdsal.notification.NetconfMdsalNotificationMapperModule
 */
@Slf4j
@ToString
class NetconfNotificationMapperProvider extends ProviderTrait<NetconfOperationServiceFactory> {

    public static final capabilitiesIdentifier =
            InstanceIdentifier.create(NetconfState.class).child(Capabilities.class).builder().build()

    @Inject
    NetconfNotificationCollector notificationCollector
    @Inject
    NetconfNotificationRegistry notificationRegistry
    @Inject
    @Named("netconf")
    BindingAwareBroker bindingAwareBroker
    @Inject
    @Named("netconf")
    DataBroker dataBroker
    @Inject
    NetconfOperationServiceFactoryListener aggregator

    def create() {
        def notificationToMdsalWriter = new NotificationToMdsalWriter(notificationCollector)
        bindingAwareBroker.registerProvider(notificationToMdsalWriter)

        def capabilityChangeListenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, capabilitiesIdentifier,
                new BaseCapabilityChangeNotificationPublisher(notificationCollector.registerBaseNotificationPublisher()), AsyncDataBroker.DataChangeScope.SUBTREE)

        def netconfNotificationOperationServiceFactory = new NetconfNotificationOperationServiceFactory(notificationRegistry)
        aggregator.onAddNetconfOperationServiceFactory(netconfNotificationOperationServiceFactory)
        netconfNotificationOperationServiceFactory
    }
}
