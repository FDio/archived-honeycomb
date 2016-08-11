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

package io.fd.honeycomb.infra.distro.restconf

import com.google.inject.Inject
import com.google.inject.name.Named
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration
import io.fd.honeycomb.infra.distro.ProviderTrait
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.opendaylight.controller.sal.core.api.Broker
import org.opendaylight.netconf.sal.rest.api.RestConnector
import org.opendaylight.netconf.sal.restconf.impl.RestconfProviderImpl
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber

@Slf4j
@ToString
class RestconfProvider extends ProviderTrait<RestConnector> {

    @Inject
    HoneycombConfiguration cfg

    @Inject
    @Named("honeycomb")
    Broker domBroker

    def create() {
        RestconfProviderImpl instance = new RestconfProviderImpl()
        instance.setWebsocketPort(new PortNumber(cfg.restconfWebsocketPort))
        domBroker.registerProvider(instance)

        Server server = new Server(cfg.restconfPort);
        final URL resource = getClass().getResource("/");
        WebAppContext webapp = new WebAppContext(resource.getPath(), cfg.restconfRootPath);
        server.setHandler(webapp);

        try {
            server.start();
        } catch (Exception e) {
            log.error "Unable to start Restconf", e
            throw new RuntimeException("Unable to start Restconf", e)
        }

        return instance
    }
}
