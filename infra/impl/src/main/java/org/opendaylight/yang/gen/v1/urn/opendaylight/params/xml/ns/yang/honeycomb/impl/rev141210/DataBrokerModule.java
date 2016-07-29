package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.honeycomb.impl.rev141210;

import io.fd.honeycomb.data.impl.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBrokerModule extends
        AbstractDataBrokerModule {

    private static final Logger LOG = LoggerFactory.getLogger(DataBrokerModule.class);

    public DataBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DataBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
                            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
                            DataBrokerModule oldModule,
                            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("DataBrokerModule.createInstance()");
        return DataBroker.create(getConfigDataTreeDependency(), getOperationalDataTreeDependency());
    }
}
