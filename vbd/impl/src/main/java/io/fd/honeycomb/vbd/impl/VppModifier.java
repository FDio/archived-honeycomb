/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.fd.honeycomb.vbd.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.attributes.interconnection.BridgeBasedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TerminationPointVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.TunnelParameters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.termination.point.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.node.termination.point._interface.type.UserInterface;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Class which is used for manipulation with VPP
 */
final class VppModifier {
    private static final Long DEFAULT_ENCAP_VRF_ID = 0L;

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomain.class);
    private final MountPointService mountService;
    private final String bridgeDomainName;
    private TopologyVbridgeAugment config;
    private final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain> iiBridgeDomainOnVPP;


    VppModifier(final MountPointService mountService, final String bridgeDomainName) {
        this.mountService = mountService;
        this.bridgeDomainName = bridgeDomainName;
        this.iiBridgeDomainOnVPP = InstanceIdentifier.create(Vpp.class)
                .child(BridgeDomains.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain.class, new BridgeDomainKey(bridgeDomainName));
    }

    /**
     * Tryies to read ipv4 addresses from all specified {@code iiToVpps } vpps.
     *
     * @param iiToVpps collection of instance identifiers which points to concrete mount points.
     * @return future which contains list of ip addreases in the same order as was specified in {@code iiToVpps}
     */
    @SafeVarargs
    final ListenableFuture<List<Optional<Ipv4AddressNoZone>>> readIpAddressesFromVpps(final KeyedInstanceIdentifier<Node, NodeKey>... iiToVpps) {
        final List<ListenableFuture<Optional<Ipv4AddressNoZone>>> ipv4Futures = new ArrayList<>(iiToVpps.length);
        for (final KeyedInstanceIdentifier<Node, NodeKey> iiToVpp : iiToVpps) {
            ipv4Futures.add(readIpAddressFromVpp(iiToVpp));
        }
        return Futures.successfulAsList(ipv4Futures);
    }

    /**
     * Passes through interfaces at mount point specified via {@code iiToVpp}.
     *
     * When first ipv4 address is found then it is returned.
     *
     * @param iiToVpp instance idenfifier which point to mounted vpp
     * @return if set ipv4 address is found at mounted vpp then it is returned as future. Otherwise absent value is returned
     * in future or exception which has been thrown
     */
    private ListenableFuture<Optional<Ipv4AddressNoZone>> readIpAddressFromVpp(final KeyedInstanceIdentifier<Node, NodeKey> iiToVpp) {
        final SettableFuture<Optional<Ipv4AddressNoZone>> resultFuture = SettableFuture.create();

        final DataBroker vppDataBroker = VbdUtil.resolveDataBrokerForMountPoint(iiToVpp, mountService);
        if (vppDataBroker != null) {
            final ReadOnlyTransaction rTx = vppDataBroker.newReadOnlyTransaction();
            final CheckedFuture<Optional<Interfaces>, ReadFailedException> interfaceStateFuture
                    = rTx.read(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Interfaces.class));

            Futures.addCallback(interfaceStateFuture, new FutureCallback<Optional<Interfaces>>() {
                @Override
                public void onSuccess(final Optional<Interfaces> optInterfaces) {
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
                    } else {
                        LOG.debug("There is no inferface with ipv4 address set at VPP {}.", iiToVpp);
                        resultFuture.set(Optional.<Ipv4AddressNoZone>absent());
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    resultFuture.setException(t);
                }
            });
        } else {
            LOG.debug("Data broker for vpp {} is missing.", iiToVpp);
            resultFuture.set(Optional.<Ipv4AddressNoZone>absent());
        }
        return resultFuture;
    }

    void createVirtualInterfaceOnVpp(final Ipv4AddressNoZone ipSrc, final Ipv4AddressNoZone ipDst, final KeyedInstanceIdentifier<Node, NodeKey> iiToVpp,
                                     final Integer vxlanTunnelId) {
        final Vxlan vxlanData = prepareVxlan(ipSrc, ipDst);
        final Interface intfData = prepareVirtualInterfaceData(vxlanData, vxlanTunnelId);

        final DataBroker vppDataBroker = VbdUtil.resolveDataBrokerForMountPoint(iiToVpp, mountService);
        if (vppDataBroker != null) {
            final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
            final KeyedInstanceIdentifier<Interface, InterfaceKey> iiToInterface
                    = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(VbdUtil.provideVxlanId(vxlanTunnelId)));
            wTx.put(LogicalDatastoreType.CONFIGURATION, iiToInterface, intfData);
            final CheckedFuture<Void, TransactionCommitFailedException> submitFuture = wTx.submit();
            Futures.addCallback(submitFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void result) {
                    LOG.debug("Writing super virtual interface to {} finished successfully.",iiToVpp.getKey().getNodeId());
                }

                @Override
                public void onFailure(Throwable t) {
                    LOG.debug("Writing super virtual interface to {} failed.", iiToVpp.getKey().getNodeId(), t);
                }
            });
        } else {
            LOG.debug("Writing virtual interface {} to VPP {} wasn't successfull because missing data broker.", VbdUtil.provideVxlanId(vxlanTunnelId), iiToVpp);
        }
    }

    private static Interface prepareVirtualInterfaceData(final Vxlan vxlan, Integer vxlanTunnelId) {
        final InterfaceBuilder interfaceBuilder = new InterfaceBuilder();
        //TODO implement tunnel counter
        interfaceBuilder.setName(VbdUtil.provideVxlanId(vxlanTunnelId));
        interfaceBuilder.setType(VxlanTunnel.class);
        VppInterfaceAugmentationBuilder vppInterfaceAugmentationBuilder = new VppInterfaceAugmentationBuilder();
        vppInterfaceAugmentationBuilder.setVxlan(vxlan);
        interfaceBuilder.addAugmentation(VppInterfaceAugmentation.class, vppInterfaceAugmentationBuilder.build());
        return interfaceBuilder.build();
    }

    private Vxlan prepareVxlan(final Ipv4AddressNoZone ipSrc, final Ipv4AddressNoZone ipDst) {
        final VxlanBuilder vxlanBuilder = new VxlanBuilder();
        vxlanBuilder.setSrc(new IpAddress(ipSrc));
        vxlanBuilder.setDst(new IpAddress(ipDst));
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

    void addInterfaceToBridgeDomainOnVpp(final DataBroker vppDataBroker, final TerminationPointVbridgeAugment termPointVbridgeAug) {
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
            wTx.put(LogicalDatastoreType.CONFIGURATION, iiToV3poL2, prepareL2Data(), true);
            wTx.submit();
        }
    }

    ListenableFuture<Void> addVppToBridgeDomain(final KeyedInstanceIdentifier<Node, NodeKey> iiToVpp, final Node node) {
        final DataBroker vppDataBroker = VbdUtil.resolveDataBrokerForMountPoint(iiToVpp, mountService);
        if (vppDataBroker != null) {
            final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
            wTx.put(LogicalDatastoreType.CONFIGURATION, iiBridgeDomainOnVPP, prepareNewBridgeDomainData(), true);
            return wTx.submit();
        }
        return Futures.immediateFailedFuture(new IllegalStateException("Data broker for vpp is missing"));
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain
    prepareNewBridgeDomainData() {
        final BridgeDomainBuilder bridgeDomainBuilder = new BridgeDomainBuilder(config);
        bridgeDomainBuilder.setName(bridgeDomainName);
        return bridgeDomainBuilder.build();
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

    public void setConfig(final TopologyVbridgeAugment config) {
        this.config = config;
    }
}
