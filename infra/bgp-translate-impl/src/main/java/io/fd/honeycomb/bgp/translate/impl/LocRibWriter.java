/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.honeycomb.bgp.translate.impl;

import com.google.common.base.Preconditions;
import io.fd.honeycomb.translate.bgp.RibWriter;
import io.fd.honeycomb.translate.bgp.RouteWriter;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.bgp.rib.rib.LocRib;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Provides route translation for routes in local RIB.
 */
public final class LocRibWriter implements RibWriter {
    private final DataBroker bgpDataBroker;

    public LocRibWriter(final DataBroker bgpDataBroker) {
        this.bgpDataBroker = bgpDataBroker;
    }

    @Override
    public void register(@Nonnull final RouteWriter writer) {
        @SuppressWarnings("unchecked")
        final InstanceIdentifier<Route> managedId = (InstanceIdentifier<Route>)writer.getManagedDataObjectType();
        final Class routeType = managedId.getTargetType();
        Preconditions.checkArgument(Route.class.isAssignableFrom(routeType),
            "{} managed by {} is not subclass of Route", routeType, writer);
        Preconditions.checkArgument(managedId.firstIdentifierOf(LocRib.class) != null,
            "{} managed by {} does not contain LocRib.class", managedId, writer);
        Preconditions.checkArgument(managedId.isWildcarded(),
            "{} managed by {} should not contain route key", managedId, writer);

        // TODO(HONEYCOMB-367): updates for whole list instead of list item
        // are needed to support deleteALL (might be required for performance reasons).
        bgpDataBroker
            .registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, managedId),
            new LocRibChangeListener(writer));
    }
}
