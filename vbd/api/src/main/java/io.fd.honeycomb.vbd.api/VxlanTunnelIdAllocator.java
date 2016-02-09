/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.fd.honeycomb.vbd.api;


import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;


public interface VxlanTunnelIdAllocator {

    /**
     * Allocate next available vxlan tunnel ID
     *
     * @param vpp specify contret vpp for which is next available vxlan id looked for
     * @return next available (in order) vxlan id.
     */
    Integer nextIdFor(final KeyedInstanceIdentifier<Node, NodeKey> vpp);
}
