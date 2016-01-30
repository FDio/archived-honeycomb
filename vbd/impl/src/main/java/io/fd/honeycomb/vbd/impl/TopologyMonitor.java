/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.fd.honeycomb.vbd.impl;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.topology.types.VbridgeTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible for monitoring /network-topology/topology and activating a {@link BridgeDomain} when a particular
 * topology is marked as a bridge domain.
 */
final class TopologyMonitor implements DataTreeChangeListener<VbridgeTopology>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyMonitor.class);

    @GuardedBy("this")
    private final Map<TopologyKey, BridgeDomain> domains = new HashMap<>();
    private final DataBroker dataBroker;

    TopologyMonitor(final DataBroker dataBroker) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<VbridgeTopology>> changes) {
        for (DataTreeModification<VbridgeTopology> c : changes) {
            @SuppressWarnings("unchecked")
            final KeyedInstanceIdentifier<Topology, TopologyKey> topology =
                    (KeyedInstanceIdentifier<Topology, TopologyKey>) c.getRootPath().getRootIdentifier()
                    .firstIdentifierOf(Topology.class);

            Preconditions.checkArgument(!topology.isWildcarded(), "Wildcard topology %s is not supported", topology);

            final DataObjectModification<VbridgeTopology> mod = c.getRootNode();
            switch (mod.getModificationType()) {
                case DELETE:
                    LOG.debug("Topology {} removed", topology);
                    stopDomain(topology);
                    break;
                case WRITE:
                    LOG.debug("Topology {} added", topology);
                    startDomain(topology);
                    break;
                default:
                    LOG.warn("Ignoring unhandled modification type {}", mod.getModificationType());
                    break;
            }
        }
    }

    private synchronized void completeDomain(final KeyedInstanceIdentifier<Topology, TopologyKey> topology) {
        LOG.debug("Bridge domain for {} completed operation", topology);
        domains.remove(topology);

        synchronized (domains) {
            domains.notify();
        }
    }

    private synchronized void restartDomain(final KeyedInstanceIdentifier<Topology, TopologyKey> topology) {
        final BridgeDomain prev = domains.remove(topology);
        if (prev == null) {
            LOG.warn("No domain for {}, not restarting", topology);
            return;
        }

        prev.forceStop();
        startDomain(topology);
    }

    @GuardedBy("this")
    private void startDomain(final KeyedInstanceIdentifier<Topology, TopologyKey> topology) {
        final BridgeDomain prev = domains.get(topology.getKey());
        if (prev != null) {
            LOG.warn("Bridge domain {} for {} already started", prev, topology);
            return;
        }

        LOG.debug("Starting bridge domain for {}", topology);

        final BindingTransactionChain chain = dataBroker.createTransactionChain(new TransactionChainListener() {
            @Override
            public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
                completeDomain(topology);
            }

            @Override
            public void onTransactionChainFailed(final TransactionChain<?, ?> chain,
                    final AsyncTransaction<?, ?> transaction, final Throwable cause) {
                LOG.warn("Bridge domain for {} failed, restarting it", cause);
                restartDomain(topology);
            }
        });

        final BridgeDomain domain = BridgeDomain.create(dataBroker, topology, chain);
        domains.put(topology.getKey(), domain);

        LOG.debug("Bridge domain {} for {} started", domain, topology);
    }

    @GuardedBy("this")
    private void stopDomain(final KeyedInstanceIdentifier<Topology, TopologyKey> topology) {
        final BridgeDomain domain = domains.remove(topology.getKey());
        if (domain == null) {
            LOG.warn("Bridge domain for {} not present", topology);
            return;
        }

        domain.stop();
    }

    @Override
    public synchronized void close() {
        LOG.debug("Topology monitor {} shut down started", this);

        for (Entry<TopologyKey, BridgeDomain> e : domains.entrySet()) {
            LOG.debug("Shutting down bridge domain {} (key {})", e.getValue(), e.getKey());
            e.getValue().stop();
        }

        while (!domains.isEmpty()) {
            LOG.debug("Waiting for domains for {} to complete", domains.keySet());
            synchronized (domains) {
                try {
                    domains.wait();
                } catch (InterruptedException e) {
                    LOG.warn("Interrupted while waiting for domain shutdown, {} have not completed yet",
                        domains.keySet(), e);
                    break;
                }
            }
        }

        LOG.debug("Topology monitor {} shut down completed", this);
    }
}
