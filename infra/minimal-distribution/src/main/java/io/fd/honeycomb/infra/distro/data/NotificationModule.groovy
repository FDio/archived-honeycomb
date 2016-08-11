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

import com.google.inject.AbstractModule
import com.google.inject.Singleton
import com.google.inject.name.Names
import groovy.util.logging.Slf4j
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter
import org.opendaylight.controller.sal.core.api.Broker

@Slf4j
class NotificationModule extends AbstractModule {

    protected void configure() {
        def provider = new DOMNotificationServiceProvider()
        bind(DOMNotificationService)
                .annotatedWith(Names.named("honeycomb"))
                .toProvider(provider)
                .in(Singleton)
        bind(DOMNotificationRouter)
                .annotatedWith(Names.named("honeycomb"))
                .toProvider(provider)
                .in(Singleton)
        bind(Broker)
                .annotatedWith(Names.named("honeycomb"))
                .toProvider(HoneycombDOMBrokerProvider)
                .in(Singleton)
    }
}
