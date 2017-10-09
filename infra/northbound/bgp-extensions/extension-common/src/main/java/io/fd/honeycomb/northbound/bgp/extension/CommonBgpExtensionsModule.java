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
import org.opendaylight.protocol.bgp.parser.impl.BGPActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers BGP extensions provided by ODL implementation.
 * TODO(HONEYCOMB-378): add support for flowspec (requires some special initialization)
 */
public final class CommonBgpExtensionsModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(CommonBgpExtensionsModule.class);

    protected void configure() {
        LOG.debug("Initializing CommonBgpExtensionsModule");
        // This should be part of BgpModule, but that one is Private and Multibinders + private BASE_MODULES
        // do not work together, that's why there's a dedicated module here
        // https://github.com/google/guice/issues/906
        bind(RIBExtensionConsumerContext.class).toProvider(RIBExtensionConsumerContextProvider.class).in(Singleton.class);
        bind(BGPExtensionConsumerContext.class).toProvider(BGPExtensionConsumerContextProvider.class).in(Singleton.class);
        Multibinder.newSetBinder(binder(), BGPExtensionProviderActivator.class).addBinding().to(BGPActivator.class);
    }
}
