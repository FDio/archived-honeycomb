package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbd.impl.rev160202;

import io.fd.honeycomb.vbd.impl.VirtualBridgeDomainManager;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;

public class VbdModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbd.impl.rev160202.AbstractVbdModule {
    public VbdModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VbdModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbd.impl.rev160202.VbdModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BindingAwareBroker brokerDependency = getBrokerDependency();
        BindingAwareBroker.ProviderContext session = brokerDependency.registerProvider(new VbdProvider());

        DataBroker dataBroker = session.getSALService(DataBroker.class);
        MountPointService mountService = session.getSALService(MountPointService.class);
        return VirtualBridgeDomainManager.create(dataBroker, mountService);
    }

}
