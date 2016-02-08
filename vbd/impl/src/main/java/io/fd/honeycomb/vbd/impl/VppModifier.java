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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VxlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.TopologyVbridgeAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vbridge.topology.rev160129.network.topology.topology.TunnelParameters;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Class which is used for manipulation with VPP
 */
public class VppModifier {
    private static final Long DEFAULT_ENCAP_VRF_ID = 0L;

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomain.class);
    private final MountPointService mountService;
    private TopologyVbridgeAugment config;


    public VppModifier(final MountPointService mountService) {
        this.mountService = mountService;
    }
    /**
     * Tryies to read ipv4 addresses from all specified {@code iiToVpps } vpps.
     *
     * @param iiToVpps collection of instance identifiers which points to concrete mount points.
     * @return future which contains list of ip addreases in the same order as was specified in {@code iiToVpps}
     */
    ListenableFuture<List<Optional<Ipv4AddressNoZone>>> readIpAddressesFromVpps(final KeyedInstanceIdentifier<Node, NodeKey>... iiToVpps) {
        final List<ListenableFuture<Optional<Ipv4AddressNoZone>>> ipv4Futures = new ArrayList<>();
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

        final DataBroker vppDataBroker = resolveDataBrokerForMountPoint(iiToVpp);
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

    void createVirtualInterfaceOnVpp(final Ipv4AddressNoZone ipSrc, final Ipv4AddressNoZone ipDst, final KeyedInstanceIdentifier<Node, NodeKey> iiToVpp) {
        final Vxlan vxlanData = prepareVxlan(ipSrc, ipDst);
        final Interface intfData = prepareVirtualInterfaceData(vxlanData);

        final DataBroker vppDataBroker = resolveDataBrokerForMountPoint(iiToVpp);
        if (vppDataBroker != null) {
            final WriteTransaction wTx = vppDataBroker.newWriteOnlyTransaction();
            final KeyedInstanceIdentifier<Interface, InterfaceKey> iiToInterface
                    = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(BridgeDomain.TUNNEL_ID_DEMO));
            wTx.put(LogicalDatastoreType.CONFIGURATION, iiToInterface, intfData);
            final CheckedFuture<Void, TransactionCommitFailedException> submitFuture = wTx.submit();
            Futures.addCallback(submitFuture, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void result) {
                    LOG.debug("Writing super virtual interface to {} finished successfully.",iiToVpp.getKey().getNodeId());
                }

                @Override
                public void onFailure(Throwable t) {
                    LOG.debug("Writing super virtual interface to {} failed.", iiToVpp.getKey().getNodeId());
                }
            });
        } else {
            LOG.debug("Writing virtual interface {} to VPP {} wasn't successfull because missing data broker.", BridgeDomain.TUNNEL_ID_DEMO, iiToVpp);
        }
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


    private Interface prepareVirtualInterfaceData(final Vxlan vxlan) {
        final InterfaceBuilder interfaceBuilder = new InterfaceBuilder();
        //TODO implement tunnel counter
        interfaceBuilder.setName(BridgeDomain.TUNNEL_ID_DEMO);
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


    public void setConfig(final TopologyVbridgeAugment config) {
        this.config = config;
    }
}
