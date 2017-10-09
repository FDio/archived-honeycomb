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

import com.google.inject.Provider;
import io.fd.honeycomb.translate.write.WriterFactory;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.bgp.multiprotocol.rev151009.bgp.common.afi.safi.list.AfiSafi;

import java.util.Set;

/**
 * Interface to define all bindings that BGP extension module should expose
 */
public interface BgpExtensionModule {

    /**
     * RIB activator/s for module
     */
    Set<Class<? extends RIBExtensionProviderActivator>> getRibActivators();

    /**
     * Extension activator/s for module
     */
    Set<Class<? extends BGPExtensionProviderActivator>> getExtensionActivators();

    /**
     * Supported table types
     */
    Set<AbstractBgpExtensionModule.TableTypeRegistration> getTableTypes();

    /**
     * Supported afi's
     */
    Set<Class<? extends Provider<AfiSafi>>> getAfiSafiTypeProviders();

    /**
     * Returns {@code WriterFactory} that binds all nodes that extension is handling
     */
    Set<Class<? extends WriterFactory>> getApplicationRibWriters();

    /**
     * Any additional bindings that are needed
     */
    default void configureAlso() {
        //NOOP - override to add additional configuration
    }
}
