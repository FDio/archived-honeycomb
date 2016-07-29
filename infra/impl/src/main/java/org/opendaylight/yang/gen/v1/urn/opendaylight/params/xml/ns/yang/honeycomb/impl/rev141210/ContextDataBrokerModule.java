package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.honeycomb.impl.rev141210;

import io.fd.honeycomb.data.impl.DataBroker;
import io.fd.honeycomb.data.impl.ModifiableDataTreeManager;

public class ContextDataBrokerModule extends AbstractContextDataBrokerModule {

    public ContextDataBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ContextDataBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, ContextDataBrokerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return DataBroker.create(new ModifiableDataTreeManager(getContextDataTreeDependency()));
    }

}
