package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.honeycomb.impl.rev141210;

import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.read.BindingBrokerReader;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfStateBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NetconfMonitoringReaderModule extends AbstractNetconfMonitoringReaderModule {
    public NetconfMonitoringReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetconfMonitoringReaderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, NetconfMonitoringReaderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new NetconfMonitoringReaderFactory(getNetconfMonitoringBindingBrokerDependency());
    }

    public static final class NetconfMonitoringReaderFactory implements AutoCloseable, io.fd.honeycomb.translate.read.ReaderFactory {

        private final DataBroker netconfMonitoringBindingBrokerDependency;

        public NetconfMonitoringReaderFactory(final DataBroker netconfMonitoringBindingBrokerDependency) {
            this.netconfMonitoringBindingBrokerDependency = netconfMonitoringBindingBrokerDependency;
        }

        @Override
        public void init(final ModifiableReaderRegistryBuilder registry) {
            registry.add(new BindingBrokerReader<>(InstanceIdentifier.create(NetconfState.class),
                    netconfMonitoringBindingBrokerDependency,
                    LogicalDatastoreType.OPERATIONAL, NetconfStateBuilder.class));
        }
    }
}
