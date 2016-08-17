package io.fd.honeycomb.infra.distro.restconf

import com.google.inject.Inject
import io.fd.honeycomb.infra.distro.ProviderTrait
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector

class HttpConnectorProvider extends ProviderTrait<ServerConnector> {

    @Inject
    HoneycombConfiguration cfg
    @Inject
    Server server

    @Override
    def create() {
        def httpConnector = new ServerConnector(server, cfg.acceptorsSize.get(), cfg.selectorsSize.get())
        httpConnector.setHost(cfg.restconfBindingAddress.get())
        httpConnector.setPort(cfg.restconfPort.get())
        server.addConnector(httpConnector)
        httpConnector
    }
}
