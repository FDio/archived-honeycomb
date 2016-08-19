/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.infra.distro.ProviderTrait
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration
import org.eclipse.jetty.security.ConstraintMapping
import org.eclipse.jetty.security.ConstraintSecurityHandler
import org.eclipse.jetty.security.HashLoginService
import org.eclipse.jetty.security.authentication.BasicAuthenticator
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.security.Constraint
import org.eclipse.jetty.util.security.Password
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.webapp.WebAppContext

@Slf4j
@ToString
class JettyServerProvider extends ProviderTrait<Server> {

    public static final String REALM = "HCRealm"

    @Inject
    HoneycombConfiguration cfg

    def create() {
        def server = new Server(new QueuedThreadPool(cfg.restPoolMaxSize.get(), cfg.restPoolMinSize.get()))

        // Load Realm for basic auth
        def service = new HashLoginService(REALM)
        // Reusing the name as role
        service.putUser(cfg.username, new Password(cfg.password), cfg.username)
        server.addBean(service)

        final URL resource = getClass().getResource("/")
        WebAppContext webapp = new WebAppContext(resource.getPath(), cfg.restconfRootPath.get())

        ConstraintSecurityHandler security = getBaseAuth(service, webapp)
        server.setHandler(security)

        return server
    }

    private ConstraintSecurityHandler getBaseAuth(HashLoginService service, WebAppContext webapp) {
        ConstraintSecurityHandler security = new ConstraintSecurityHandler()

        Constraint constraint = new Constraint()
        constraint.setName("auth")
        constraint.setAuthenticate(true)
        constraint.setRoles(cfg.username)

        ConstraintMapping mapping = new ConstraintMapping()
        mapping.setPathSpec("/*")
        mapping.setConstraint(constraint)

        security.setConstraintMappings(Collections.singletonList(mapping))
        security.setAuthenticator(new BasicAuthenticator())
        security.setLoginService(service)

        security.setHandler(webapp)
        security
    }
}
