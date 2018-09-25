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
import io.fd.honeycomb.translate.bgp.RouteWriter;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev180329.Route;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LocRibChangeListener implements DataTreeChangeListener<Route> {

    private static final Logger LOG = LoggerFactory.getLogger(LocRibChangeListener.class);
    private final RouteWriter writer;

    LocRibChangeListener(final RouteWriter writer) {
        this.writer = writer;
    }

    @Override
    public void onDataTreeChanged(@Nonnull final Collection<DataTreeModification<Route>> changes) {
        for (DataTreeModification<Route> change : changes) {
            final DataObjectModification<Route> rootNode = change.getRootNode();
            final DataTreeIdentifier<Route> rootPath = change.getRootPath();
            final Route dataBefore = rootNode.getDataBefore();
            final Route dataAfter = rootNode.getDataAfter();
            LOG.trace("Received LocRib change({}): before={} after={}", rootNode.getModificationType(), dataBefore, dataAfter);

            try {
                processChange(rootPath.getRootIdentifier(), dataBefore, dataAfter);
            } catch (WriteFailedException e) {
                LOG.warn("Route translation failed", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processChange(final InstanceIdentifier<Route> id, final Route dataBefore, final Route dataAfter) throws WriteFailedException {
        if (isCreate(dataBefore, dataAfter)) {
            writer.create(id, dataAfter);
        } else if (isDelete(dataBefore, dataAfter)) {
            writer.delete(id, dataBefore);
        } else {
            Preconditions.checkArgument(dataBefore != null && dataAfter != null, "No data to process");
            writer.update(id, dataBefore, dataAfter);
        }
    }

    private static boolean isCreate(final DataObject dataBefore, final DataObject dataAfter) {
        return dataBefore == null && dataAfter != null;
    }

    private static boolean isDelete(final DataObject dataBefore, final DataObject dataAfter) {
        return dataAfter == null && dataBefore != null;
    }
}
