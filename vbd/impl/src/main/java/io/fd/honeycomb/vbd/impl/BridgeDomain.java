/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.fd.honeycomb.vbd.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import java.util.Collection;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.external.reference.rev160129.ExternalReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.NodeVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.BridgeMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.BridgeMemberBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a single Virtual Bridge Domain. It is bound to a particular network topology instance, manages
 * bridge members and projects state into the operational data store.
 */
final class BridgeDomain implements DataTreeChangeListener<Topology> {
    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomain.class);
    private final KeyedInstanceIdentifier<Topology, TopologyKey> topology;
    @GuardedBy("this")

    private final BindingTransactionChain chain;
    private final ListenerRegistration<?> reg;
    private final MountPointService mountService;
    private TopologyVbridgeAugment config;
    private final String bridgeDomainName;
    private final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain> iiBridgeDomainOnVPP;
    private final String iiBridgeDomainOnVPPRest;
    private final DataBroker dataBroker;

    private BridgeDomain(final DataBroker dataBroker, final MountPointService mountService, final KeyedInstanceIdentifier<Topology, TopologyKey> topology,
            final BindingTransactionChain chain) {
        this.topology = Preconditions.checkNotNull(topology);
        this.chain = Preconditions.checkNotNull(chain);
        this.mountService = mountService;

        this.bridgeDomainName = topology.getKey().getTopologyId().getValue();
        this.iiBridgeDomainOnVPPRest = provideIIBrdigeDomainOnVPPRest();
        this.iiBridgeDomainOnVPP = InstanceIdentifier.create(Vpp.class)
                .child(BridgeDomains.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain.class, new BridgeDomainKey(bridgeDomainName));

        this.dataBroker = dataBroker;
        reg = dataBroker.registerDataTreeChangeListener(
                                     new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, topology), this);
    }

    private String provideIIBrdigeDomainOnVPPRest() {
        final StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("v3po:vpp/bridge-domains/bridge-domain/");
        strBuilder.append(bridgeDomainName);
        return strBuilder.toString();
    }

    static BridgeDomain create(final DataBroker dataBroker,
                               MountPointService mountService, final KeyedInstanceIdentifier<Topology, TopologyKey> topology, final BindingTransactionChain chain) {

        LOG.debug("Wiping operational state of {}", topology);

        final WriteTransaction tx = chain.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, topology);
        tx.submit();

        return new BridgeDomain(dataBroker, mountService, topology, chain);
    }

    synchronized void forceStop() {
        LOG.info("Bridge domain {} for {} going down", this, topology);
        reg.close();
        chain.close();
        LOG.info("Bridge domain {} for {} is down", this, topology);
    }

    synchronized void stop() {
        LOG.debug("Bridge domain {} for {} shutting down", this, topology);

        final WriteTransaction tx = chain.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, topology);
        tx.submit();
        chain.close();
    }

    @Override
    public synchronized void onDataTreeChanged(final Collection<DataTreeModification<Topology>> changes) {
        for (DataTreeModification<Topology> c : changes) {
            LOG.debug("Domain {} for {} processing change {}", this, topology, c);

            final DataObjectModification<Topology> mod = c.getRootNode();
            switch (mod.getModificationType()) {
                case DELETE:
                    LOG.debug("Topology {} deleted, expecting shutdown", topology);
                    break;
                case SUBTREE_MODIFIED:
                    // First check if the configuration has changed
                    final DataObjectModification<TopologyVbridgeAugment> newConfig = mod.getModifiedAugmentation(TopologyVbridgeAugment.class);
                    if (newConfig != null) {
                        if (newConfig.getModificationType() != ModificationType.DELETE) {
                            LOG.debug("Topology {} modified configuration {}", topology, newConfig);
                            updateConfiguration(newConfig);
                        } else {
                            // FIXME: okay, what can we do about this one?
                            LOG.error("Topology {} configuration deleted, good luck!", topology);
                        }
                    }

                    for (DataObjectModification<? extends DataObject> child : mod.getModifiedChildren()) {
                        LOG.debug("Topology {} modified child {}", topology, child);

                        if (Node.class.isAssignableFrom(child.getDataType())) {
                            modifyNode((DataObjectModification<Node>) child, newConfig.getDataAfter());
                        }
                    }

                    break;
                case WRITE:
                    final Topology data = mod.getDataAfter();

                    // Read configuration
                    final TopologyVbridgeAugment config = data.getAugmentation(TopologyVbridgeAugment.class);
                    if (config != null) {
                        setConfiguration(config);
                    } else {
                        LOG.error("Topology {} has no configuration, good luck!", topology);
                    }

                    // FIXME: deal with nodes

                    break;
                default:
                    LOG.warn("Unhandled topology modification {}", mod);
                    break;
            }
        }
    }

    private void modifyNode(final DataObjectModification<Node> child, final TopologyVbridgeAugment topologyVbridgeAugment) {
        switch (child.getModificationType()) {
            case DELETE:
                LOG.debug("Topology {} node {} deleted", topology, child.getIdentifier());
                // FIXME: do something
                break;
            case SUBTREE_MODIFIED:
                LOG.debug("Topology {} node {} modified", topology, child.getIdentifier());
                // FIXME: do something
                break;
            case WRITE:
                LOG.debug("Topology {} node {} created", topology, child.getIdentifier());
                createNode(child.getDataAfter(), topologyVbridgeAugment);
                break;
            default:
                LOG.warn("Unhandled node modification {} in topology {}", child, topology);
                break;
        }
    }

    private void createNode(final Node node, final TopologyVbridgeAugment topologyVbridgeAugment) {
        for (SupportingNode supportingNode : node.getSupportingNode()) {
            final NodeId nodeMount = supportingNode.getNodeRef();
            final TopologyId topologyMount = supportingNode.getTopologyRef();

            final KeyedInstanceIdentifier<Node, NodeKey> iiToMount = InstanceIdentifier
                    .create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(topologyMount))
                    .child(Node.class, new NodeKey(nodeMount));
            final Optional<MountPoint> vppMountOption = mountService.getMountPoint(iiToMount);
            if (vppMountOption.isPresent()) {
                final MountPoint vppMount = vppMountOption.get();
                addVppToBridgeDomain(topologyVbridgeAugment, vppMount, node);
            }
        }
    }

    private void addVppToBridgeDomain(final TopologyVbridgeAugment topologyVbridgeAugment, final MountPoint vppMount, final Node node) {
        final Optional<DataBroker> dataBrokerOpt = vppMount.getService(DataBroker.class);
        if (dataBrokerOpt.isPresent()) {
            final DataBroker vppDataBroker = dataBrokerOpt.get();
            final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
            wTx.put(LogicalDatastoreType.OPERATIONAL, iiBridgeDomainOnVPP, prepareNewBridgeDomainData(topologyVbridgeAugment));
            final CheckedFuture<Void, TransactionCommitFailedException> addVppToBridgeDomainFuture = wTx.submit();
            addSupportingBridgeDomain(addVppToBridgeDomainFuture, node);
        }
    }

    private void addSupportingBridgeDomain(final CheckedFuture<Void, TransactionCommitFailedException> addVppToBridgeDomainFuture, final Node node) {
        Futures.addCallback(addVppToBridgeDomainFuture, new FutureCallback() {
            @Override
            public void onSuccess(Object result) {
                LOG.debug("Storing bridge member to operational DS....");
                final BridgeMemberBuilder bridgeMemberBuilder = new BridgeMemberBuilder();
                bridgeMemberBuilder.setSupportingBridgeDomain(new ExternalReference(iiBridgeDomainOnVPPRest));
                final InstanceIdentifier<BridgeMember> iiToBridgeMember = topology.child(Node.class, node.getKey()).augmentation(NodeVbridgeAugment.class).child(BridgeMember.class);
                final WriteTransaction wTx = chain.newWriteOnlyTransaction();
                wTx.put(LogicalDatastoreType.OPERATIONAL, iiToBridgeMember, bridgeMemberBuilder.build(), true);
                wTx.submit();
            }

            @Override
            public void onFailure(Throwable t) {
                //TODO handle this state
            }
        });

    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain
        prepareNewBridgeDomainData(TopologyVbridgeAugment topologyVbridgeAugment) {
            final BridgeDomainBuilder bridgeDomainBuilder = new BridgeDomainBuilder(topologyVbridgeAugment);
            bridgeDomainBuilder.setName(topology.getKey().getTopologyId().getValue());
            return bridgeDomainBuilder.build();
    }

    private void setConfiguration(final TopologyVbridgeAugment config) {
        LOG.debug("Topology {} configuration set to {}", topology, config);

        this.config = config;
    }

    @GuardedBy("this")
    private void updateConfiguration(final DataObjectModification<TopologyVbridgeAugment> mod) {
        LOG.debug("Topology {} configuration changed", topology);

        // FIXME: do something smarter
        setConfiguration(mod.getDataAfter());
    }
}
