/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.fd.honeycomb.vbd.impl;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyTypes1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.topology.types.VbridgeTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.TopologyTypes;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tip for the Virtual Bridge Domain implementation. This class is instantiated when the application is started
 * and {@link #close()}d when it is shut down.
 */
public final class VirtualBridgeDomainManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualBridgeDomainManager.class);
    private static final DataTreeIdentifier<VbridgeTopology> LISTEN_TREE =
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class).child(TopologyTypes.class)
                    .augmentation(TopologyTypes1.class).child(VbridgeTopology.class).build());

    private final ListenerRegistration<TopologyMonitor> reg;
    private boolean closed;

    private VirtualBridgeDomainManager(final ListenerRegistration<TopologyMonitor> reg) {
        this.reg = Preconditions.checkNotNull(reg);
    }

    public static VirtualBridgeDomainManager create(@Nonnull final DataBroker dataBroker) {
        final ListenerRegistration<TopologyMonitor> reg =
                dataBroker.registerDataTreeChangeListener(LISTEN_TREE, new TopologyMonitor(dataBroker));

        return new VirtualBridgeDomainManager(reg);
    }

    @Override
    public void close() {
        if (!closed) {
            LOG.debug("Virtual Bridge Domain manager shut down started");

            final TopologyMonitor monitor = reg.getInstance();
            reg.close();
            LOG.debug("Topology monitor {} unregistered", monitor);
            monitor.close();

            closed = true;
            LOG.debug("Virtual Bridge Domain manager shut down completed");
        }
    }
}
