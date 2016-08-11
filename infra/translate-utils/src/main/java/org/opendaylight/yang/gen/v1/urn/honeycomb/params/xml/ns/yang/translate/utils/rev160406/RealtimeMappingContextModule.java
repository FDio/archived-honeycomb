package org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406;

public class RealtimeMappingContextModule extends org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406.AbstractRealtimeMappingContextModule {
    public RealtimeMappingContextModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public RealtimeMappingContextModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.translate.utils.rev160406.RealtimeMappingContextModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // Provides real time CRUD on top of Context data broker
        return new io.fd.honeycomb.translate.util.RealtimeMappingContext(getContextBindingBrokerDependency());
    }

}
