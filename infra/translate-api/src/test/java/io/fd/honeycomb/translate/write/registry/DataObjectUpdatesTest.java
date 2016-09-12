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

package io.fd.honeycomb.translate.write.registry;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import io.fd.honeycomb.translate.write.DataObjectUpdate;
import java.util.Collections;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataObjectUpdatesTest {

    @Mock
    private DataObjectUpdate update1;
    @Mock
    private DataObjectUpdate.DataObjectDelete update2;
    private InstanceIdentifier<DataObject> id = InstanceIdentifier.create(DataObject.class);

    @Test
    public void testUpdates() throws Exception {
        final SetMultimap<InstanceIdentifier<?>, DataObjectUpdate> ups =
                Multimaps.forMap(Collections.singletonMap(id, update1));
        final SetMultimap<InstanceIdentifier<?>, DataObjectUpdate.DataObjectDelete> dels =
                Multimaps.forMap(Collections.singletonMap(id, update2));

        final WriterRegistry.DataObjectUpdates updates = new WriterRegistry.DataObjectUpdates(ups, dels);

        assertSame(ups, updates.getUpdates());
        assertSame(dels, updates.getDeletes());
        assertTrue(updates.containsOnlySingleType());
        assertThat(updates.getTypeIntersection().size(), is(1));
        assertThat(updates.getTypeIntersection(), hasItem(id));
        assertFalse(updates.isEmpty());

        assertTrue(updates.equals(updates));
        assertFalse(updates.equals(new WriterRegistry.DataObjectUpdates(HashMultimap.create(), HashMultimap.create())));
    }
}