package io.fd.honeycomb.infra.distro.restconf

import com.google.inject.Inject
import io.fd.honeycomb.infra.distro.ProviderTrait
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.util.ssl.SslContextFactory

class HttpsConnectorProvider extends ProviderTrait<ServerConnector> {

    @Inject
    HoneycombConfiguration cfg
    @Inject
    Server server

    @Override
    def create() {

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
        def keystoreURL = getClass().getResource(cfg.restconfKeystore.get())
        sslContextFactory.setKeyStorePath(keystoreURL.path)
        sslContextFactory.setKeyStorePassword(cfg.keystorePassword.get())
        sslContextFactory.setKeyManagerPassword((cfg.keystoreManagerPassword.get()))
        def truststoreURL = getClass().getResource(cfg.restconfTruststore.get())
        sslContextFactory.setTrustStorePath(truststoreURL.path)
        sslContextFactory.setTrustStorePassword((cfg.truststorePassword.get()))
        // TODO make this more configurable
        sslContextFactory.setExcludeCipherSuites(
                "SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_DSS_WITH_DES_CBC_SHA",
                "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA")

        // SSL Connector
        def sslConnector = new ServerConnector(server, cfg.httpsAcceptorsSize.get(), cfg.httpsSelectorsSize.get(),
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()))
        sslConnector.setHost(cfg.restconfHttpsBindingAddress.get())
        sslConnector.setPort(cfg.restconfHttpsPort.get())
        server.addConnector(sslConnector)
        return sslConnector
    }
}
