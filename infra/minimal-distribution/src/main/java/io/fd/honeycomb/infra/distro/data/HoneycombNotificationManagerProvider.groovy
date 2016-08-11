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

package io.fd.honeycomb.infra.distro.data

import com.google.inject.Inject
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.infra.distro.ProviderTrait
import io.fd.honeycomb.notification.ManagedNotificationProducer
import io.fd.honeycomb.notification.NotificationCollector
import io.fd.honeycomb.notification.impl.HoneycombNotificationCollector
import io.fd.honeycomb.notification.impl.NotificationProducerRegistry
import io.fd.honeycomb.notification.impl.NotificationProducerTracker
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMNotificationPublishServiceAdapter
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter
/**
 * Mirror of org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.notification.impl.rev160601.HoneycombNotificationManagerModule
 */
@Slf4j
@ToString
class HoneycombNotificationManagerProvider extends ProviderTrait<NotificationCollector> {

    @Inject
    DOMNotificationRouter notificationRouter
    @Inject(optional = true)
    Set<ManagedNotificationProducer> notificationProducers = []
    @Inject
    BindingToNormalizedNodeCodec codec

    @Override
    def create() {
        // Create the registry to keep track of what'OPERATIONAL registered
        def notificationProducerRegistry = new NotificationProducerRegistry(notificationProducers as List);

        // Create BA version of notification service (implementation is free from ODL)
        def bindingDOMNotificationPublishServiceAdapter =
                new BindingDOMNotificationPublishServiceAdapter(codec, notificationRouter);

        // Create Collector on top of BA notification service and registry
        def honeycombNotificationCollector =
                new HoneycombNotificationCollector(bindingDOMNotificationPublishServiceAdapter, notificationProducerRegistry);

        // Create tracker, responsible for starting and stopping registered notification producers whenever necessary
        def notificationProducerTracker =
                new NotificationProducerTracker(notificationProducerRegistry, honeycombNotificationCollector,
                        notificationRouter);

        // TODO wire with restconf
        // DOMNotificationService is already provided by DOMBroker injected into RESTCONF, however RESTCONF
        // only supports data-change notification, nothing else. So currently its impossible.

        honeycombNotificationCollector
    }
}
