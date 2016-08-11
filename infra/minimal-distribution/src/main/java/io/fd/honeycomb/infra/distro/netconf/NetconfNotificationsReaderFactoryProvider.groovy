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
import io.fd.honeycomb.translate.read.ReaderFactory
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder
import io.fd.honeycomb.translate.util.read.BindingBrokerReader
import org.opendaylight.controller.md.sal.binding.api.DataBroker
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.Netconf
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.NetconfBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

import javax.annotation.Nonnull

// TODO backport to karaf distro
@Slf4j
@ToString
class NetconfNotificationsReaderFactoryProvider extends ProviderTrait<ReaderFactory> {

    @Inject
    @Named("netconf")
    DataBroker netconfDataBroker

    def create() {
        new ReaderFactory() {
            @Override
            void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
                registry.add(new BindingBrokerReader<>(InstanceIdentifier.create(Netconf.class),
                        netconfDataBroker, LogicalDatastoreType.OPERATIONAL, NetconfBuilder.class));
            }
        }
    }
}
