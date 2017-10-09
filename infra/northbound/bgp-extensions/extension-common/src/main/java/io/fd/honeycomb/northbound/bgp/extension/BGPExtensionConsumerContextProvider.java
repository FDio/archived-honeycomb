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

import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.ShutdownHandler;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionConsumerContext;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContext;
import org.opendaylight.protocol.bgp.parser.spi.pojo.SimpleBGPExtensionProviderContextActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;

final class BGPExtensionConsumerContextProvider extends ProviderTrait<BGPExtensionConsumerContext> {
    private static final Logger LOG = LoggerFactory.getLogger(BGPExtensionConsumerContextProvider.class);

    @Inject
    private Set<BGPExtensionProviderActivator> activators;

    @Inject
    private ShutdownHandler shutdownHandler;

    @Override
    protected BGPExtensionConsumerContext create() {
        final BGPExtensionProviderContext ctx = new SimpleBGPExtensionProviderContext();
        final SimpleBGPExtensionProviderContextActivator activator =
            new SimpleBGPExtensionProviderContextActivator(ctx, new ArrayList<>(activators));
        LOG.debug("Starting BGPExtensionConsumerContext with activators: {}", activators);
        activator.start();
        shutdownHandler.register("bgp-extension-context-activator", activator);
        return ctx;
    }
}
