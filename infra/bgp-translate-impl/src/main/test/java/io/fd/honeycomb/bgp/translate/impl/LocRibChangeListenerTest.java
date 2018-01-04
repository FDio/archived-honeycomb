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

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

import io.fd.honeycomb.translate.bgp.RouteWriter;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev171207.Route;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LocRibChangeListenerTest {

    private static final DataTreeIdentifier<Route> ID =
        new DataTreeIdentifier<>(OPERATIONAL, InstanceIdentifier.create(Route.class));

    @Mock
    private RouteWriter<Route> routeWriter;
    @Mock
    private DataObjectModification<Route> rootNode;

    private LocRibChangeListener locRibListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        locRibListener = new LocRibChangeListener(routeWriter);
    }

    @Test
    public void testDataTreeChanged() throws WriteFailedException {
        final Route route1 = Mockito.mock(Route.class);
        final Route route2 = Mockito.mock(Route.class);
        locRibListener.onDataTreeChanged(Arrays.asList(
            mockDateTreeModification(null, route1),
            mockDateTreeModification(route1, route2),
            mockDateTreeModification(route2, null))
        );
        Mockito.verify(routeWriter).create(ID.getRootIdentifier(), route1);
        Mockito.verify(routeWriter).update(ID.getRootIdentifier(), route1, route2);
        Mockito.verify(routeWriter).delete(ID.getRootIdentifier(), route2);
    }

    @Test
    public void testDataTreeChangedFailed() throws WriteFailedException.CreateFailedException {
        final Route dataAfter = Mockito.mock(Route.class);
        Mockito.doThrow(new WriteFailedException.CreateFailedException(ID.getRootIdentifier(), dataAfter))
            .when(routeWriter)
            .create(ArgumentMatchers.any(), ArgumentMatchers.any());
        locRibListener.onDataTreeChanged(Collections.singletonList(mockDateTreeModification(null, dataAfter)));
        Mockito.verify(routeWriter).create(ID.getRootIdentifier(), dataAfter);
    }

    @SuppressWarnings("unchecked")
    private DataTreeModification<Route> mockDateTreeModification(final Route dataBefore, final Route dataAfter) {
        final DataTreeModification<Route> modification = Mockito.mock(DataTreeModification.class);
        final DataObjectModification<Route> rootNode = Mockito.mock(DataObjectModification.class);
        Mockito.when(rootNode.getDataBefore()).thenReturn(dataBefore);
        Mockito.when(rootNode.getDataAfter()).thenReturn(dataAfter);
        Mockito.when(modification.getRootPath()).thenReturn(ID);
        Mockito.when(modification.getRootNode()).thenReturn(rootNode);
        return modification;
    }
}