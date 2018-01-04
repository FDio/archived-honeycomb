/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.honeycomb.northbound.bgp.extension;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.fd.honeycomb.translate.write.WriterFactory;
import javax.annotation.Nonnull;
import org.opendaylight.protocol.bgp.openconfig.spi.BGPTableTypeRegistryConsumer;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.types.rev151009.AfiSafiType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.ApplicationRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;

/**
 * General blueprint for any module that wants to bind new BGP extension
 */
public abstract class AbstractBgpExtensionModule extends AbstractModule implements BgpExtensionModule {

    public static final InstanceIdentifier<Tables> TABLES_IID = InstanceIdentifier.create(ApplicationRib.class).child(Tables.class);

    @Override
    protected void configure() {
        bindRibActivators();
        bindExtensionActivators();
        bindTableTypes();
        bindAfiSafis();
        bindApplicationRibWriters();
        bind(BGPTableTypeRegistryConsumer.class).toProvider(BGPTableTypeRegistryConsumerProvider.class)
                .in(Singleton.class);

        configureAlso();

        // TODO(HONEYCOMB-395): should all afi-safis use the same send-max value?
    }

    protected abstract Logger getLogger();

    private void bindAfiSafis() {
        final Multibinder<AfiSafi> afiSafiBinder = Multibinder.newSetBinder(binder(), AfiSafi.class);
        getAfiSafiTypeProviders().stream()
                .peek(aClass -> getLogger().debug("Binding afi {}", aClass))
                .forEach(providerClass -> afiSafiBinder.addBinding().toProvider(providerClass));
    }

    private void bindRibActivators() {
        final Multibinder<RIBExtensionProviderActivator> ribBinder = Multibinder.newSetBinder(binder(),
                RIBExtensionProviderActivator.class);
        getRibActivators().stream()
                .peek(aClass -> getLogger().debug("Binding RIB activator {}", aClass))
                .forEach(activator -> ribBinder.addBinding().to(activator));
    }

    private void bindExtensionActivators() {
        final Multibinder<BGPExtensionProviderActivator> extensionsBinder = Multibinder.newSetBinder(binder(),
                BGPExtensionProviderActivator.class);
        getExtensionActivators().stream()
                .peek(aClass -> getLogger().debug("Binding Extension activator {}", aClass))
                .forEach(activator -> extensionsBinder.addBinding().to(activator));
    }

    private void bindTableTypes() {
        final Multibinder<BGPTableTypeRegistryConsumerProvider.BGPTableType> tableTypeBinder =
                Multibinder.newSetBinder(binder(), BGPTableTypeRegistryConsumerProvider.BGPTableType.class);
        getTableTypes().stream()
                .peek(tableTypeRegistration -> getLogger().debug("Binding table type {}", tableTypeRegistration))
                .forEach(registration -> tableTypeBinder.addBinding()
                        .toInstance(provider -> provider.registerBGPTableType(registration.addressFamily, registration.subsequentAddressFamily, registration.afiSafiType)));
    }

    private void bindApplicationRibWriters() {
        final Multibinder<WriterFactory> applicationRibWritersBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);

        getApplicationRibWriters().stream()
                .peek(aClass -> getLogger().debug("Binding writer factory {}", aClass))
                .forEach(aClass -> applicationRibWritersBinder.addBinding().to(aClass));
    }

    public static final class TableTypeRegistration {
        private final Class<? extends AddressFamily> addressFamily;
        private final Class<? extends SubsequentAddressFamily> subsequentAddressFamily;
        private final Class<? extends AfiSafiType> afiSafiType;

        private TableTypeRegistration(final Class<? extends AddressFamily> addressFamily,
                                      final Class<? extends SubsequentAddressFamily> subsequentAddressFamily,
                                      final Class<? extends AfiSafiType> afiSafiType) {
            this.addressFamily = addressFamily;
            this.subsequentAddressFamily = subsequentAddressFamily;
            this.afiSafiType = afiSafiType;
        }

        public static TableTypeRegistration tableType(@Nonnull final Class<? extends AddressFamily> addressFamily,
                                                      @Nonnull final Class<? extends SubsequentAddressFamily> subsequentAddressFamily,
                                                      @Nonnull final Class<? extends AfiSafiType> afiSafiType) {
            return new TableTypeRegistration(addressFamily, subsequentAddressFamily, afiSafiType);
        }

        @Override
        public String toString() {
            return "TableTypeRegistration{" +
                    "addressFamily=" + addressFamily +
                    ", subsequentAddressFamily=" + subsequentAddressFamily +
                    ", afiSafiType=" + afiSafiType +
                    '}';
        }
    }

}
