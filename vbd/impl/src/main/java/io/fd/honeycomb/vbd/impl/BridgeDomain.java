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
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
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
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.external.reference.rev160129.ExternalReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VxlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.l2.interconnection.BridgeBasedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.LinkVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.LinkVbridgeAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.NodeVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TerminationPointVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TerminationPointVbridgeAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.TunnelParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.BridgeMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.BridgeMemberBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.termination.point.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.termination.point._interface.type.TunnelInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.termination.point._interface.type.UserInterface;
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
    private static final String TUNNEL_ID_PREFIX = "vxlan_tunnel";
    private static final String TUNNEL_DESCRIPTION = "virtual interface which interconnects VPPs";
    private static final Long DEFAULT_ENCAP_VRF_ID = 0L;
    private static final String TUNNEL_ID_DEMO = TUNNEL_ID_PREFIX + "0";
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
    private Multimap<NodeId, KeyedInstanceIdentifier<Node, NodeKey>> nodesToVpps = ArrayListMultimap.create();
    private final List<Integer> tunnelIds;

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
        this.tunnelIds = new ArrayList<>();
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
                            modifyNode((DataObjectModification<Node>) child);
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

    private void addTunnel(final NodeId newNode) {
        for (Map.Entry<NodeId, KeyedInstanceIdentifier<Node, NodeKey>> entryToVpp : nodesToVpps.entries()) {
            if (!entryToVpp.getKey().equals(newNode)) {
                //TODO: check whether returned value from nodesToVpps is not null
                final KeyedInstanceIdentifier<Node, NodeKey> iiToOldVpp = entryToVpp.getValue();
                final KeyedInstanceIdentifier<Node, NodeKey> iiToNewVpp = nodesToVpps.get(newNode).iterator().next();
                final NodeId oldNode = entryToVpp.getKey();

                final ListenableFuture<List<Optional<Ipv4AddressNoZone>>> ipAddressesFuture = readIpAddressesFromVpps(iiToOldVpp, iiToNewVpp);
                Futures.addCallback(ipAddressesFuture, new FutureCallback<List<Optional<Ipv4AddressNoZone>>>() {
                    @Override
                    public void onSuccess(List<Optional<Ipv4AddressNoZone>> ipAddresses) {
                        if (ipAddresses.size() == 2) {
                            LOG.debug("All required IP addresses for creating tunnel were obtained.");
                            final Optional<Ipv4AddressNoZone> ipAddressNewVpp = ipAddresses.get(SOURCE_VPP_INDEX);
                            final Optional<Ipv4AddressNoZone> ipAddressOldVpp = ipAddresses.get(DESTINATION_VPP_INDEX);
                            if (ipAddressNewVpp != null && ipAddressOldVpp != null) {
                                if (ipAddressNewVpp.isPresent() && ipAddressOldVpp.isPresent()) {
                                    //writing v3po:vxlan container to new node
                                    Vxlan vxlanData = prepareVxlan(ipAddressOldVpp.get(), ipAddressNewVpp.get());
                                    Interface intfData = prepareVirtualInterfaceData(vxlanData);
                                    createVirtualInterfaceOnVpp(intfData, iiToNewVpp);

                                    //writing v3po:vxlan container to existing node
                                    vxlanData = prepareVxlan(ipAddressNewVpp.get(), ipAddressOldVpp.get());
                                    intfData = prepareVirtualInterfaceData(vxlanData);
                                    createVirtualInterfaceOnVpp(intfData, iiToOldVpp);

                                    addTerminationPoint(topology.child(Node.class, new NodeKey(oldNode)));
                                    addTerminationPoint(topology.child(Node.class, new NodeKey(newNode)));

                                    addLinkBetweenTerminationPoints(newNode, oldNode);
                                    addLinkBetweenTerminationPoints(oldNode, newNode);
                                }
                            }
                        }

                    }

                    private void createVirtualInterfaceOnVpp(final Interface intfData, final KeyedInstanceIdentifier<Node, NodeKey> iiToVpp) {
                        final DataBroker vppDataBroker = resolveDataBrokerForMountPoint(iiToVpp);
                        if (vppDataBroker != null) {
                            final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
                            final KeyedInstanceIdentifier<Interface, InterfaceKey> iiToInterface
                                    = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(TUNNEL_ID_DEMO));
                            wTx.put(LogicalDatastoreType.CONFIGURATION, iiToInterface, intfData);
                            final CheckedFuture<Void, TransactionCommitFailedException> submitFuture = wTx.submit();
                            Futures.addCallback(submitFuture, new FutureCallback<Void>() {
                                @Override
                                public void onSuccess(@Nullable Void result) {
                                    LOG.debug("Writing super virtual interface to {} finished successfully.",iiToVpp.getKey().getNodeId());
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    LOG.debug("Writing super virtual interface to {} failed.",iiToVpp.getKey().getNodeId());
                                }
                            });
                        } else {
                            LOG.debug("Writing virtual interface {} to VPP {} wasn't successfull because missing data broker.",TUNNEL_DESCRIPTION, iiToVpp);
                        }
                    }

                    private Interface prepareVirtualInterfaceData(final Vxlan vxlan) {
                        final InterfaceBuilder interfaceBuilder = new InterfaceBuilder();
                        //TODO implement tunnel counter
                        interfaceBuilder.setName(TUNNEL_ID_DEMO);
                        interfaceBuilder.setType(VxlanTunnel.class);
                        VppInterfaceAugmentationBuilder vppInterfaceAugmentationBuilder = new VppInterfaceAugmentationBuilder();
                        vppInterfaceAugmentationBuilder.setVxlan(vxlan);
                        interfaceBuilder.addAugmentation(VppInterfaceAugmentation.class, vppInterfaceAugmentationBuilder.build());
                        return interfaceBuilder.build();
                    }

                    private Vxlan prepareVxlan(final Ipv4AddressNoZone ipSrc, final Ipv4AddressNoZone ipDst) {
                        final VxlanBuilder vxlanBuilder = new VxlanBuilder();
                        vxlanBuilder.setSrc(ipSrc);
                        vxlanBuilder.setDst(ipDst);
                        final TunnelParameters tunnelParameters = config.getTunnelParameters();
                        if (tunnelParameters instanceof org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.tunnel.parameters.Vxlan) {
                            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.tunnel.parameters.Vxlan vxlan =
                                    (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.tunnel.parameters.Vxlan) tunnelParameters;
                            //TODO: handle NPE
                            vxlanBuilder.setVni(vxlan.getVxlan().getVni());
                        }
                        vxlanBuilder.setEncapVrfId(DEFAULT_ENCAP_VRF_ID);
                        return vxlanBuilder.build();
                    }

                    @Override
                    public void onFailure(Throwable t) {

                    }
                });
            }
        }
    }

    private void addLinkBetweenTerminationPoints(final NodeId newVpp, final NodeId odlVpp) {
        //TODO clarify how should identifier of link looks like
        final String linkIdStr = newVpp.getValue() + "-" + odlVpp.getValue();
        final LinkId linkId = new LinkId(linkIdStr);
        final KeyedInstanceIdentifier<Link, LinkKey> iiToLink = topology.child(Link.class, new LinkKey(linkId));
        final WriteTransaction wTx = chain.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, iiToLink, prepareData(newVpp, odlVpp, linkId),true);
        wTx.submit();

    }

    private Link prepareData(final NodeId newVpp, final NodeId oldVpp, final LinkId linkId) {
        final LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setLinkId(linkId);

        final SourceBuilder sourceBuilder = new SourceBuilder();
        sourceBuilder.setSourceNode(newVpp);
        sourceBuilder.setSourceTp(new TpId(TUNNEL_ID_DEMO));
        linkBuilder.setSource(sourceBuilder.build());


        final DestinationBuilder destinationBuilder = new DestinationBuilder();
        destinationBuilder.setDestNode(oldVpp);
        destinationBuilder.setDestTp(new TpId(TUNNEL_ID_DEMO));
        linkBuilder.setDestination(destinationBuilder.build());

        final LinkVbridgeAugmentBuilder linkVbridgeAugmentBuilder = new LinkVbridgeAugmentBuilder();
        linkVbridgeAugmentBuilder.setTunnel(new ExternalReference(TUNNEL_ID_DEMO));
        linkBuilder.addAugmentation(LinkVbridgeAugment.class, linkVbridgeAugmentBuilder.build());
        return linkBuilder.build();
    }

    private ListenableFuture<List<Optional<Ipv4AddressNoZone>>> readIpAddressesFromVpps(final KeyedInstanceIdentifier<Node, NodeKey>... iiToVpps) {
        final List<ListenableFuture<Optional<Ipv4AddressNoZone>>> ipv4Futures = new ArrayList<>();
        for (final KeyedInstanceIdentifier<Node, NodeKey> iiToVpp : iiToVpps) {
            ipv4Futures.add(readIpAddressFromVpp(iiToVpp));
        }
        return Futures.successfulAsList(ipv4Futures);
    }

    private ListenableFuture<Optional<Ipv4AddressNoZone>> readIpAddressFromVpp(final KeyedInstanceIdentifier<Node, NodeKey> iiToVpp) {
        final SettableFuture<Optional<Ipv4AddressNoZone>> resultFuture = SettableFuture.create();

        final DataBroker vppDataBroker = resolveDataBrokerForMountPoint(iiToVpp);
        final ReadOnlyTransaction rTx = vppDataBroker.newReadOnlyTransaction();
        final CheckedFuture<Optional<Interfaces>, ReadFailedException> interfaceStateFuture
                = rTx.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Interfaces.class));

        Futures.addCallback(interfaceStateFuture, new FutureCallback<Optional<Interfaces>>() {
            @Override
            public void onSuccess(Optional<Interfaces> optInterfaces) {
                if (optInterfaces.isPresent()) {
                    for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface intf : optInterfaces.get().getInterface()) {
                        final Interface1 interface1 = intf.getAugmentation(Interface1.class);
                        if (interface1 != null) {
                            final Ipv4 ipv4 = interface1.getIpv4();
                            if (ipv4 != null) {
                                final List<Address> addresses = ipv4.getAddress();
                                if (!addresses.isEmpty()) {
                                    final Ipv4AddressNoZone ip = addresses.iterator().next().getIp();
                                    if (ip != null) {
                                        resultFuture.set(Optional.of(ip));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                resultFuture.set(Optional.<Ipv4AddressNoZone>absent());
            }

            @Override
            public void onFailure(Throwable t) {
                resultFuture.setException(t);

            }
        });
        return resultFuture;
    }


    private void modifyTerminationPoint(final DataObjectModification<TerminationPoint> nodeChild, final NodeId nodeId) {
        final TerminationPoint terminationPoint = nodeChild.getDataAfter();
        final TerminationPointVbridgeAugment termPointVbridgeAug = terminationPoint.getAugmentation(TerminationPointVbridgeAugment.class);
        if (termPointVbridgeAug != null) {
            final Collection<KeyedInstanceIdentifier<Node, NodeKey>> instanceIdentifiersVPP = nodesToVpps.get(nodeId);
            //TODO: probably iterate via all instance identifiers.
            if (!instanceIdentifiersVPP.isEmpty()) {
                final DataBroker dataBroker = resolveDataBrokerForMountPoint(instanceIdentifiersVPP.iterator().next());
                addInterfaceToBridgeDomainOnVpp(dataBroker, termPointVbridgeAug);
            }
        }
    }

    private void addInterfaceToBridgeDomainOnVpp(final DataBroker vppDataBroker, final TerminationPointVbridgeAugment termPointVbridgeAug) {
        final InterfaceType interfaceType = termPointVbridgeAug.getInterfaceType();
        if (interfaceType instanceof UserInterface) {
            //REMARK: according contract in YANG model this should be URI to data on mount point (accroding to RESTCONF)
            //It was much more easier to just await concrete interface name, thus isn't necessary parse it (splitting on '/')
            final ExternalReference userInterface = ((UserInterface) interfaceType).getUserInterface();
            final KeyedInstanceIdentifier<Interface, InterfaceKey> iiToVpp =
                    InstanceIdentifier.create(Interfaces.class)
                            .child(Interface.class, new InterfaceKey(userInterface.getValue()));
            InstanceIdentifier<L2> iiToV3poL2 = iiToVpp.augmentation(VppInterfaceAugmentation.class).child(L2.class);
            LOG.debug("Writing L2 data to configuration DS to concrete interface.");
            final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
            wTx.put(LogicalDatastoreType.CONFIGURATION, iiToV3poL2, prepareL2Data());
            wTx.submit();
        }
    }

    private L2 prepareL2Data() {
        final L2Builder l2Builder = new L2Builder();
        final BridgeBasedBuilder bridgeBasedBuilder = new BridgeBasedBuilder();
        bridgeBasedBuilder.setSplitHorizonGroup((short) 0);
        bridgeBasedBuilder.setBridgedVirtualInterface(false);
        bridgeBasedBuilder.setBridgeDomain(bridgeDomainName);
        l2Builder.setInterconnection(bridgeBasedBuilder.build());
        return l2Builder.build();
    }


    private DataBroker resolveDataBrokerForMountPoint(final InstanceIdentifier<Node> iiToMountPoint) {
        final Optional<MountPoint> vppMountPointOpt = mountService.getMountPoint(iiToMountPoint);
        if (vppMountPointOpt.isPresent()) {
            final MountPoint vppMountPoint = vppMountPointOpt.get();
            final Optional<DataBroker> dataBrokerOpt = vppMountPoint.getService(DataBroker.class);
            if (dataBrokerOpt.isPresent()) {
                return dataBrokerOpt.get();
            }
        }
        return null;
    }

    private void createNode(final Node node) {
        for (SupportingNode supportingNode : node.getSupportingNode()) {
            final NodeId nodeMount = supportingNode.getNodeRef();
            final TopologyId topologyMount = supportingNode.getTopologyRef();

            final KeyedInstanceIdentifier<Node, NodeKey> iiToMount = InstanceIdentifier
                    .create(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(topologyMount))
                    .child(Node.class, new NodeKey(nodeMount));
            nodesToVpps.put(node.getNodeId(), iiToMount);
            final DataBroker dataBrokerOfMount = resolveDataBrokerForMountPoint(iiToMount);
            addVppToBridgeDomain(dataBrokerOfMount, node);
        }
    }

    private void addVppToBridgeDomain(final DataBroker vppDataBroker, final Node node) {
        if (vppDataBroker != null) {
            final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
            wTx.put(LogicalDatastoreType.CONFIGURATION, iiBridgeDomainOnVPP, prepareNewBridgeDomainData());
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

    private void addTerminationPoint(final KeyedInstanceIdentifier<Node, NodeKey> nodeIID) {
        // build data
        final ExternalReference ref = new ExternalReference(TUNNEL_ID_DEMO);
        final TunnelInterfaceBuilder iFaceBuilder = new TunnelInterfaceBuilder();
        iFaceBuilder.setTunnelInterface(ref);

        final TerminationPointVbridgeAugmentBuilder tpAugmentBuilder = new TerminationPointVbridgeAugmentBuilder();
        tpAugmentBuilder.setInterfaceType(iFaceBuilder.build());

        final TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.addAugmentation(TerminationPointVbridgeAugment.class, tpAugmentBuilder.build());
        tpBuilder.setTpId(new TpId(TUNNEL_ID_DEMO));
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

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain
        prepareNewBridgeDomainData() {
            final BridgeDomainBuilder bridgeDomainBuilder = new BridgeDomainBuilder(config);
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
