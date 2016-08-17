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
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.security.ConstraintMapping
import org.eclipse.jetty.security.ConstraintSecurityHandler
import org.eclipse.jetty.security.HashLoginService
import org.eclipse.jetty.security.authentication.BasicAuthenticator
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.util.security.Constraint
import org.eclipse.jetty.util.security.Password
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.webapp.WebAppContext
import org.opendaylight.controller.sal.core.api.Broker
import org.opendaylight.netconf.sal.rest.api.RestConnector
import org.opendaylight.netconf.sal.restconf.impl.RestconfProviderImpl
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber

@Slf4j
@ToString
class RestconfProvider extends ProviderTrait<RestConnector> {

    public static final String KEYSTORE_PASSWORD = "OBF:1v9s1unr1unn1vv51zlk1t331vg91x1b1vgl1t331zly1vu51uob1uo71v8u"
    public static final String KEYSTORE_NAME = "/honeycomb-keystore"
    public static final String REALM = "HCRealm"

    @Inject
    HoneycombConfiguration cfg

    @Inject
    Broker domBroker

    def create() {
        def instance = new RestconfProviderImpl()
        instance.setWebsocketPort(new PortNumber(cfg.restconfWebsocketPort))
        domBroker.registerProvider(instance)

        def server = new Server(InetSocketAddress.createUnresolved(cfg.restconfBindingAddress, cfg.restconfPort))

        // Load Realm for basic auth
        def service = new HashLoginService(REALM)
        // Reusing the name as role
        // TODO make this more configurable
        service.putUser(cfg.username, new Password(cfg.password), cfg.username)
        server.addBean(service)

        final URL resource = getClass().getResource("/")
        WebAppContext webapp = new WebAppContext(resource.getPath(), cfg.restconfRootPath)

        ConstraintSecurityHandler security = getBaseAuth(service, webapp)
        server.setHandler(security)

        // SSL Context Factory
        // Based on:
        // https://github.com/eclipse/jetty.project/blob/jetty-9.3.x/examples/embedded/src/main/java/org/eclipse/jetty/embedded/LikeJettyXml.java
        // https://wiki.eclipse.org/Jetty/Howto/Configure_SSL#Loading_Keys_and_Certificates_via_PKCS12
        // Keystore created with:
        // openssl genrsa -des3 -out honeycomb.key
        // openssl req -new -x509 -key honeycomb.key -out honeycomb.crt
        // openssl pkcs12 -inkey honeycomb.key -in honeycomb.crt -export -out honeycomb.pkcs12
        // keytool -importkeystore -srckeystore honeycomb.pkcs12 -srcstoretype PKCS12 -destkeystore honeycomb-keystore
        def sslContextFactory = new SslContextFactory()
        def keystoreURL = getClass().getResource(KEYSTORE_NAME)
        sslContextFactory.setKeyStorePath(keystoreURL.path)
        sslContextFactory.setKeyStorePassword(KEYSTORE_PASSWORD)
        sslContextFactory.setKeyManagerPassword(KEYSTORE_PASSWORD)
        sslContextFactory.setTrustStorePath(keystoreURL.path)
        sslContextFactory.setTrustStorePassword(KEYSTORE_PASSWORD)
        sslContextFactory.setExcludeCipherSuites(
                "SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA")

        // SSL Connector
        def sslConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory())
        sslConnector.setHost(cfg.restconfHttpsBindingAddress)
        sslConnector.setPort(cfg.restconfHttpsPort)
        server.addConnector(sslConnector)

        try {
            server.start()
        } catch (Exception e) {
            log.error "Unable to start Restconf", e
            throw new RuntimeException("Unable to start Restconf", e)
        }

        return instance
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
