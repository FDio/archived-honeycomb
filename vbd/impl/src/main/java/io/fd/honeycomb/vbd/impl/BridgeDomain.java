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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.fd.honeycomb.vbd.api.VxlanTunnelIdAllocator;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.external.reference.rev160129.ExternalReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.LinkVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.LinkVbridgeAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.NodeVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TerminationPointVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TerminationPointVbridgeAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.BridgeMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.BridgeMemberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.termination.point._interface.type.TunnelInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
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

    private static final int SOURCE_VPP_INDEX = 0;
    private static final int DESTINATION_VPP_INDEX = 1;
    private final KeyedInstanceIdentifier<Topology, TopologyKey> topology;
    @GuardedBy("this")

    private final BindingTransactionChain chain;
    private final ListenerRegistration<?> reg;
    private final MountPointService mountService;
    private final VppModifier vppModifier;
    private final VxlanTunnelIdAllocator tunnelIdAllocator;
    private TopologyVbridgeAugment config;
    private final String bridgeDomainName;
    private final String iiBridgeDomainOnVPPRest;
    private Multimap<NodeId, KeyedInstanceIdentifier<Node, NodeKey>> nodesToVpps = ArrayListMultimap.create();

    private BridgeDomain(final DataBroker dataBroker, final MountPointService mountService, final KeyedInstanceIdentifier<Topology, TopologyKey> topology,
                         final BindingTransactionChain chain, VxlanTunnelIdAllocator tunnelIdAllocator) {
        this.bridgeDomainName = topology.getKey().getTopologyId().getValue();
        this.vppModifier = new VppModifier(mountService, bridgeDomainName);

        this.topology = Preconditions.checkNotNull(topology);
        this.chain = Preconditions.checkNotNull(chain);
        this.mountService = mountService;
        this.tunnelIdAllocator = tunnelIdAllocator;

        this.iiBridgeDomainOnVPPRest = provideIIBrdigeDomainOnVPPRest();

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
                               MountPointService mountService, final KeyedInstanceIdentifier<Topology, TopologyKey> topology, final BindingTransactionChain chain,
                               final VxlanTunnelIdAllocator tunnelIdAllocator) {

        LOG.debug("Wiping operational state of {}", topology);

        final WriteTransaction tx = chain.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, topology);
        tx.submit();

        return new BridgeDomain(dataBroker, mountService, topology, chain, tunnelIdAllocator);
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
                            modifyNode((DataObjectModification<Node>) child);
                        }
                    }

                    break;
                case WRITE:
                    final Topology data = mod.getDataAfter();

                    // Read configuration
                    final TopologyVbridgeAugment config = data.getAugmentation(TopologyVbridgeAugment.class);
                    vppModifier.setConfig(config);
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

    private void modifyNode(final DataObjectModification<Node> nodeMod) {
        switch (nodeMod.getModificationType()) {
            case DELETE:
                LOG.debug("Topology {} node {} deleted", topology, nodeMod.getIdentifier());
                // FIXME: do something
                break;
            case SUBTREE_MODIFIED:
                LOG.debug("Topology {} node {} modified", topology, nodeMod.getIdentifier());
                for (DataObjectModification<? extends DataObject>  nodeChild : nodeMod.getModifiedChildren()) {
                    if (TerminationPoint.class.isAssignableFrom(nodeChild.getDataType())) {
                        modifyTerminationPoint((DataObjectModification<TerminationPoint>) nodeChild,nodeMod.getDataAfter().getNodeId());
                    }
                }
                break;
            case WRITE:
                LOG.debug("Topology {} node {} created", topology, nodeMod.getIdentifier());
                final int numberVppsBeforeAddition = nodesToVpps.keySet().size();
                final Node newNode = nodeMod.getDataAfter();
                createNode(newNode);
                final int numberVppsAfterAddition = nodesToVpps.keySet().size();
                if ((numberVppsBeforeAddition < numberVppsAfterAddition) && (numberVppsBeforeAddition >= 1)) {
                    addTunnel(newNode.getNodeId());
                }
                break;
            default:
                LOG.warn("Unhandled node modification {} in topology {}", nodeMod, topology);
                break;
        }
    }

    private void modifyTerminationPoint(final DataObjectModification<TerminationPoint> nodeChild, final NodeId nodeId) {
        final TerminationPoint terminationPoint = nodeChild.getDataAfter();
        final TerminationPointVbridgeAugment termPointVbridgeAug = terminationPoint.getAugmentation(TerminationPointVbridgeAugment.class);
        if (termPointVbridgeAug != null) {
            final Collection<KeyedInstanceIdentifier<Node, NodeKey>> instanceIdentifiersVPP = nodesToVpps.get(nodeId);
            //TODO: probably iterate via all instance identifiers.
            if (!instanceIdentifiersVPP.isEmpty()) {
                final DataBroker dataBroker = VbdUtil.resolveDataBrokerForMountPoint(instanceIdentifiersVPP.iterator().next(), mountService);
                vppModifier.addInterfaceToBridgeDomainOnVpp(dataBroker, termPointVbridgeAug);
            }
        }
    }

    private void addTunnel(final NodeId sourceNode) {
        final KeyedInstanceIdentifier<Node, NodeKey> iiToSrcVpp = nodesToVpps.get(sourceNode).iterator().next();
        final Integer srcVxlanTunnelId = tunnelIdAllocator.nextIdFor(iiToSrcVpp);
        for (Map.Entry<NodeId, KeyedInstanceIdentifier<Node, NodeKey>> nodeToVpp : nodesToVpps.entries()) {
            if (!nodeToVpp.getKey().equals(sourceNode)) {
                //TODO: check whether returned value from nodesToVpps is not null
                final KeyedInstanceIdentifier<Node, NodeKey> iiToDstVpp = nodeToVpp.getValue();
                final Integer dstVxlanTunnelId = tunnelIdAllocator.nextIdFor(iiToDstVpp);
                final NodeId dstNode = nodeToVpp.getKey();

                final ListenableFuture<List<Optional<Ipv4AddressNoZone>>> ipAddressesFuture = vppModifier.readIpAddressesFromVpps(iiToSrcVpp, iiToDstVpp);
                Futures.addCallback(ipAddressesFuture, new FutureCallback<List<Optional<Ipv4AddressNoZone>>>() {
                    @Override
                    public void onSuccess(List<Optional<Ipv4AddressNoZone>> ipAddresses) {
                        if (ipAddresses.size() == 2) {
                            LOG.debug("All required IP addresses for creating tunnel were obtained.");
                            final Optional<Ipv4AddressNoZone> ipAddressSrcVpp = ipAddresses.get(SOURCE_VPP_INDEX);
                            final Optional<Ipv4AddressNoZone> ipAddressDstVpp = ipAddresses.get(DESTINATION_VPP_INDEX);
                            if (ipAddressSrcVpp != null && ipAddressDstVpp != null) {
                                if (ipAddressSrcVpp.isPresent() && ipAddressDstVpp.isPresent()) {
                                    //writing v3po:vxlan container to source node
                                    vppModifier.createVirtualInterfaceOnVpp(ipAddressSrcVpp.get(), ipAddressDstVpp.get(), iiToSrcVpp, srcVxlanTunnelId);

                                    //writing v3po:vxlan container to existing node
                                    vppModifier.createVirtualInterfaceOnVpp(ipAddressDstVpp.get(), ipAddressSrcVpp.get(), iiToDstVpp, dstVxlanTunnelId);

                                    addTerminationPoint(topology.child(Node.class, new NodeKey(dstNode)), dstVxlanTunnelId);
                                    addTerminationPoint(topology.child(Node.class, new NodeKey(sourceNode)), srcVxlanTunnelId);

                                    addLinkBetweenTerminationPoints(sourceNode, dstNode, srcVxlanTunnelId, dstVxlanTunnelId);
                                    addLinkBetweenTerminationPoints(dstNode, sourceNode, srcVxlanTunnelId, dstVxlanTunnelId);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        LOG.debug("Reading of IP addresses has failed {}.", t);
                    }
                });
            }
        }
    }

    private void addLinkBetweenTerminationPoints(final NodeId newVpp, final NodeId odlVpp,
                                                 final int srcVxlanTunnelId, final int dstVxlanTunnelId) {
        //TODO clarify how should identifier of link looks like
        final String linkIdStr = newVpp.getValue() + "-" + odlVpp.getValue();
        final LinkId linkId = new LinkId(linkIdStr);
        final KeyedInstanceIdentifier<Link, LinkKey> iiToLink = topology.child(Link.class, new LinkKey(linkId));
        final WriteTransaction wTx = chain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, iiToLink, prepareLinkData(newVpp, odlVpp, linkId, srcVxlanTunnelId, dstVxlanTunnelId), true);
        wTx.submit();
    }

    private static Link prepareLinkData(final NodeId newVpp, final NodeId oldVpp, final LinkId linkId,
                                 final int srcVxlanTunnelId, final int dstVxlanTunnelId) {
        final LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setLinkId(linkId);

        final SourceBuilder sourceBuilder = new SourceBuilder();
        sourceBuilder.setSourceNode(newVpp);
        sourceBuilder.setSourceTp(new TpId(VbdUtil.provideVxlanId(srcVxlanTunnelId)));
        linkBuilder.setSource(sourceBuilder.build());

        final DestinationBuilder destinationBuilder = new DestinationBuilder();
        destinationBuilder.setDestNode(oldVpp);
        destinationBuilder.setDestTp(new TpId(VbdUtil.provideVxlanId(dstVxlanTunnelId)));
        linkBuilder.setDestination(destinationBuilder.build());

        final LinkVbridgeAugmentBuilder linkVbridgeAugmentBuilder = new LinkVbridgeAugmentBuilder();
        linkVbridgeAugmentBuilder.setTunnel(new ExternalReference(VbdUtil.provideVxlanId(srcVxlanTunnelId)));
        linkBuilder.addAugmentation(LinkVbridgeAugment.class, linkVbridgeAugmentBuilder.build());
        return linkBuilder.build();
    }

    private void createNode(final Node node) {
        for (SupportingNode supportingNode : node.getSupportingNode()) {
            final NodeId nodeMount = supportingNode.getNodeRef();
            final TopologyId topologyMount = supportingNode.getTopologyRef();

            final KeyedInstanceIdentifier<Node, NodeKey> iiToVpp = InstanceIdentifier
                    .create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(topologyMount))
                    .child(Node.class, new NodeKey(nodeMount));
            nodesToVpps.put(node.getNodeId(), iiToVpp);
            ListenableFuture<Void> addVppToBridgeDomainFuture = vppModifier.addVppToBridgeDomain(iiToVpp, node);
            addSupportingBridgeDomain(addVppToBridgeDomainFuture, node);
        }
    }

    private void addSupportingBridgeDomain(final ListenableFuture<Void> addVppToBridgeDomainFuture, final Node node) {
        Futures.addCallback(addVppToBridgeDomainFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
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

    private void addTerminationPoint(final KeyedInstanceIdentifier<Node, NodeKey> nodeIID, final int vxlanTunnelId) {
        // build data
        final ExternalReference ref = new ExternalReference(VbdUtil.provideVxlanId(vxlanTunnelId));
        final TunnelInterfaceBuilder iFaceBuilder = new TunnelInterfaceBuilder();
        iFaceBuilder.setTunnelInterface(ref);

        final TerminationPointVbridgeAugmentBuilder tpAugmentBuilder = new TerminationPointVbridgeAugmentBuilder();
        tpAugmentBuilder.setInterfaceType(iFaceBuilder.build());

        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.addAugmentation(TerminationPointVbridgeAugment.class, tpAugmentBuilder.build());
        tpBuilder.setTpId(new TpId(VbdUtil.provideVxlanId(vxlanTunnelId)));
        final TerminationPoint tp = tpBuilder.build();

        // process data
        final WriteTransaction wTx = chain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, nodeIID.child(TerminationPoint.class, tp.getKey()), tp, true);
        final CheckedFuture<Void, TransactionCommitFailedException> future = wTx.submit();

        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("Termination point successfully added to {}.", nodeIID);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.warn("Failed to add termination point to {}.", nodeIID);
            }
        });
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
