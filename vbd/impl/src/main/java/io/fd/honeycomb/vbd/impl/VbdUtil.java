/**
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.fd.honeycomb.vbd.impl;


import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VbdUtil {

    private VbdUtil() {
        throw new UnsupportedOperationException("Can't instantiate util class");
    }

    static DataBroker resolveDataBrokerForMountPoint(final InstanceIdentifier<Node> iiToMountPoint, final MountPointService mountService) {
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


}
