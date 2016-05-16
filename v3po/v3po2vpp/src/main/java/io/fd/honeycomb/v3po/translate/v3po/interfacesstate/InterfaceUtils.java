/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.fd.honeycomb.v3po.translate.v3po.interfacesstate.InterfaceCustomizer.getCachedInterfaceDump;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Tap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VhostUser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanTunnel;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.dto.SwInterfaceDetailsReplyDump;
import org.openvpp.jvpp.dto.SwInterfaceDump;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InterfaceUtils {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceUtils.class);

    private static final Gauge64 vppLinkSpeed0 = new Gauge64(BigInteger.ZERO);
    private static final Gauge64 vppLinkSpeed1 = new Gauge64(BigInteger.valueOf(10 * 1000000));
    private static final Gauge64 vppLinkSpeed2 = new Gauge64(BigInteger.valueOf(100 * 1000000));
    private static final Gauge64 vppLinkSpeed4 = new Gauge64(BigInteger.valueOf(1000 * 1000000));
    private static final Gauge64 vppLinkSpeed8 = new Gauge64(BigInteger.valueOf(10000L * 1000000));
    private static final Gauge64 vppLinkSpeed16 = new Gauge64(BigInteger.valueOf(40000L * 1000000));
    private static final Gauge64 vppLinkSpeed32 = new Gauge64(BigInteger.valueOf(100000L * 1000000));

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private static final int PHYSICAL_ADDRESS_LENGTH = 6;

    private InterfaceUtils() {
        throw new UnsupportedOperationException("This utility class cannot be instantiated");
    }

    /**
     * Convert VPP's link speed bitmask to Yang type. 1 = 10M, 2 = 100M, 4 = 1G, 8 = 10G, 16 = 40G, 32 = 100G
     *
     * @param vppLinkSpeed Link speed in bitmask format from VPP.
     * @return Converted value from VPP link speed
     */
    public static Gauge64 vppInterfaceSpeedToYang(byte vppLinkSpeed) {
        switch (vppLinkSpeed) {
            case 1:
                return vppLinkSpeed1;
            case 2:
                return vppLinkSpeed2;
            case 4:
                return vppLinkSpeed4;
            case 8:
                return vppLinkSpeed8;
            case 16:
                return vppLinkSpeed16;
            case 32:
                return vppLinkSpeed32;
            default:
                return vppLinkSpeed0;
        }
    }

    private static final void appendHexByte(final StringBuilder sb, final byte b) {
        final int v = b & 0xFF;
        sb.append(HEX_CHARS[v >>> 4]);
        sb.append(HEX_CHARS[v & 15]);
    }

    // TODO rename and move to V3poUtils

    /**
     * Reads first 6 bytes of supplied byte array and converts to string as Yang dictates <p> Replace later with
     * https://git.opendaylight.org/gerrit/#/c/34869/10/model/ietf/ietf-type- util/src/main/
     * java/org/opendaylight/mdsal/model/ietf/util/AbstractIetfYangUtil.java
     *
     * @param vppPhysAddress byte array of bytes in big endian order, constructing the network IF physical address.
     * @return String like "aa:bb:cc:dd:ee:ff"
     * @throws NullPointerException     if vppPhysAddress is null
     * @throws IllegalArgumentException if vppPhysAddress.length < 6
     */
    public static String vppPhysAddrToYang(@Nonnull final byte[] vppPhysAddress) {
        Objects.requireNonNull(vppPhysAddress, "Empty physical address bytes");
        Preconditions.checkArgument(PHYSICAL_ADDRESS_LENGTH <= vppPhysAddress.length,
                "Invalid physical address size %s, expected >= 6", vppPhysAddress.length);
        StringBuilder physAddr = new StringBuilder();

        appendHexByte(physAddr, vppPhysAddress[0]);
        for (int i = 1; i < PHYSICAL_ADDRESS_LENGTH; i++) {
            physAddr.append(":");
            appendHexByte(physAddr, vppPhysAddress[i]);
        }

        return physAddr.toString();
    }

    /**
     * VPP's interface index is counted from 0, whereas ietf-interface's if-index is from 1. This function converts from
     * VPP's interface index to YANG's interface index.
     *
     * @param vppIfIndex the sw interface index VPP reported.
     * @return VPP's interface index incremented by one
     */
    public static int vppIfIndexToYang(int vppIfIndex) {
        return vppIfIndex + 1;
    }

    /**
     * This function does the opposite of what {@link #vppIfIndexToYang(int)} does.
     *
     * @param yangIfIndex if-index from ietf-interfaces.
     * @return VPP's representation of the if-index
     */
    public static int yangIfIndexToVpp(int yangIfIndex) {
        Preconditions.checkArgument(yangIfIndex >= 1, "YANG if-index has invalid value %s", yangIfIndex);
        return yangIfIndex - 1;
    }


    /**
     * Queries VPP for interface description given interface key.
     *
     * @param futureJvpp    VPP Java Future API
     * @param key           interface key
     * @param index         VPP index of the interface
     * @param ctx           per-tx scope context containing cached dump with all the interfaces. If the cache is not
     *                      available or outdated, another dump will be performed.
     * @return SwInterfaceDetails DTO or null if interface was not found
     * @throws IllegalArgumentException If interface cannot be found
     */
    @Nonnull
    public static SwInterfaceDetails getVppInterfaceDetails(@Nonnull final FutureJVpp futureJvpp,
                                                            @Nonnull InterfaceKey key, final int index,
                                                            @Nonnull final Context ctx) {
        final SwInterfaceDump request = new SwInterfaceDump();
        request.nameFilter = key.getName().getBytes();
        request.nameFilterValid = 1;

        final Map<Integer, SwInterfaceDetails> allInterfaces = getCachedInterfaceDump(ctx);

        // Returned cached if available
        if (allInterfaces.containsKey(index)) {
            return allInterfaces.get(index);
        }

        CompletionStage<SwInterfaceDetailsReplyDump> requestFuture = futureJvpp.swInterfaceDump(request);
        SwInterfaceDetailsReplyDump ifaces = V3poUtils.getReply(requestFuture.toCompletableFuture());
        if (null == ifaces || null == ifaces.swInterfaceDetails || ifaces.swInterfaceDetails.isEmpty()) {
            request.nameFilterValid = 0;

            LOG.warn("VPP returned null instead of interface by key {} and its not cached", key.getName());
            LOG.warn("Iterating through all the interfaces to find interface: {}", key.getName());

            // Or else just perform full dump and do inefficient filtering
            requestFuture = futureJvpp.swInterfaceDump(request);
            ifaces = V3poUtils.getReply(requestFuture.toCompletableFuture());

            // Update the cache
            allInterfaces.clear();
            allInterfaces
                    .putAll(ifaces.swInterfaceDetails.stream().collect(Collectors.toMap(d -> d.swIfIndex, d -> d)));

            if (allInterfaces.containsKey(index)) {
                return allInterfaces.get(index);
            }
            throw new IllegalArgumentException("Unable to find interface " + key.getName());
        }

        final SwInterfaceDetails iface = Iterables.getOnlyElement(ifaces.swInterfaceDetails);
        allInterfaces.put(index, iface); // update the cache
        return iface;
    }

    /**
     * Determine interface type based on its VPP name (relying on VPP's interface naming conventions)
     *
     * @param interfaceName VPP generated interface name
     * @return Interface type
     */
    @Nonnull
    public static Class<? extends InterfaceType> getInterfaceType(@Nonnull final String interfaceName) {
        if (interfaceName.startsWith("tap")) {
            return Tap.class;
        }

        if (interfaceName.startsWith("vxlan")) {
            return VxlanTunnel.class;
        }

        if (interfaceName.startsWith("VirtualEthernet")) {
            return VhostUser.class;
        }

        if (interfaceName.contains(".")) {
            return SubInterface.class;
        }

        return EthernetCsmacd.class;
    }

    static boolean isInterfaceOfType(final Context ctx, final int index,
                                     final Class<? extends InterfaceType> ifcType) {
        final SwInterfaceDetails cachedDetails =
                checkNotNull(getCachedInterfaceDump(ctx).get(index),
                        "Interface {} cannot be found in context", index);
        return ifcType.equals(getInterfaceType(V3poUtils.toString(cachedDetails.interfaceName)));
    }
}