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
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.infra.distro.ProviderTrait
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration
import io.fd.honeycomb.notification.NotificationCollector
import io.fd.honeycomb.notification.impl.NotificationProducerRegistry
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter
import org.opendaylight.controller.sal.core.api.model.SchemaService
import org.opendaylight.netconf.notifications.NetconfNotificationCollector
import io.fd.honeycomb.notification.impl.TranslationUtil
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.streams.StreamBuilder
import org.opendaylight.yangtools.yang.model.api.SchemaPath
/**
 * Mirror of org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.notification.impl.rev160601.HoneycombNotificationToNetconfTranslatorModule
 */
@Slf4j
@ToString
class HoneycombNotification2NetconfProvider extends ProviderTrait<HoneycombNotification2Netconf> {

    @Inject
    DOMNotificationRouter notificationRouter
    @Inject
    SchemaService schemaService
    @Inject
    HoneycombConfiguration cfgAttributes
    @Inject
    NotificationCollector hcNotificationCollector
    @Inject
    NetconfNotificationCollector netconfNotificationCollector

    // TODO refactor HoneycombNotificationToNetconfTranslatorModule for easier reuse here

    @Override
    def create() {
        def streamType = new StreamNameType(cfgAttributes.netconfNotificationStreamName.get());

        // Register as NETCONF notification publisher under configured name
        def netconfNotifReg = netconfNotificationCollector.registerNotificationPublisher(new StreamBuilder()
                        .setName(streamType)
                        .setReplaySupport(false)
                        .setDescription(cfgAttributes.netconfNotificationStreamName.get()).build());

        // Notification Translator, get notification from HC producers and put into NETCONF notification collector
        def domNotificationListener = { notif ->
                log.debug "Propagating notification: {} into NETCONF", notif.type
                netconfNotifReg.onNotification(streamType, TranslationUtil.notificationToXml(notif, schemaService.globalContext))
        }

        // NotificationManager is used to provide list of available notifications (which are all of the notifications registered)
        // TODO make available notifications configurable here so that any number of notification streams for NETCONF
        // can be configured on top of a single notification manager
        log.debug "Current notifications to be exposed over NETCONF: {}", hcNotificationCollector.notificationTypes
        def currentNotificationSchemaPaths = hcNotificationCollector.notificationTypes
                .collect {SchemaPath.create(true, NotificationProducerRegistry.getQName(it))}

        // Register as listener to HC'OPERATIONAL DOM notification service
        // TODO This should only be triggered when NETCONF notifications are activated
        // Because this way we actually start all notification producers
        // final Collection<QName> notificationQNames =
        def domNotifListenerReg = notificationRouter
                .registerNotificationListener(domNotificationListener, currentNotificationSchemaPaths);

        log.info "Exposing NETCONF notification stream: {}", streamType.value

        new HoneycombNotification2Netconf(domNotifListenerReg: domNotifListenerReg, netconfNotifReg: netconfNotifReg)
    }

    static class HoneycombNotification2Netconf {
        def domNotifListenerReg
        def netconfNotifReg
    }
}
