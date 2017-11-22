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

package io.fd.honeycomb.infra.bgp;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import io.fd.honeycomb.infra.bgp.neighbors.BgpPeerWriterFactory;
import io.fd.honeycomb.northbound.bgp.extension.CommonBgpExtensionsModule;
import io.fd.honeycomb.translate.write.WriterFactory;
import org.opendaylight.protocol.bgp.rib.impl.StrictBGPPeerRegistry;
import org.opendaylight.protocol.bgp.rib.impl.spi.BGPPeerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides integration of various BGP components with WriterRegistry
 * in order to enable configuration updates/read via RESTCONF/NETCONF.
 */
public final class BgpWritersModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(BgpWritersModule.class);

    @Override
    protected void configure() {
        LOG.debug("Initializing BgpWritersModule");
        // This should be part of BgpModule, but that one is Private and Multibinders + private BASE_MODULES
        // do not work together, that's why there's a dedicated module here
        // https://github.com/google/guice/issues/906

        // Configure peer registry
        bind(BGPPeerRegistry.class).toInstance(StrictBGPPeerRegistry.instance());

        // install common binding for extensions
        install(new CommonBgpExtensionsModule());

        final Multibinder<WriterFactory> binder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        binder.addBinding().to(ApplicationRibWriterFactory.class).in(Singleton.class);
        binder.addBinding().to(BgpPeerWriterFactory.class).in(Singleton.class);
    }
}
