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

import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.data.init.ShutdownHandler;
import java.util.ArrayList;
import java.util.Set;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionConsumerContext;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderActivator;
import org.opendaylight.protocol.bgp.rib.spi.RIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContext;
import org.opendaylight.protocol.bgp.rib.spi.SimpleRIBExtensionProviderContextActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RIBExtensionConsumerContextProvider extends ProviderTrait<RIBExtensionConsumerContext> {
    private static final Logger LOG = LoggerFactory.getLogger(RIBExtensionConsumerContextProvider.class);
    @Inject
    private Set<RIBExtensionProviderActivator> activators;
    @Inject
    private ShutdownHandler shutdownHandler;

    @Override
    protected RIBExtensionConsumerContext create() {
        final RIBExtensionProviderContext ctx = new SimpleRIBExtensionProviderContext();
        final SimpleRIBExtensionProviderContextActivator activator =
            new SimpleRIBExtensionProviderContextActivator(ctx, new ArrayList<>(activators));
        LOG.debug("Starting RIBExtensionConsumerContext with activators: {}", activators);
        activator.start();
        shutdownHandler.register("rib-extension-consumer-context-activator", activator);
        return ctx;
    }
}
