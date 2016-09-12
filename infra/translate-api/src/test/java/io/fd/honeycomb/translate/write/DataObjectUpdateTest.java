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

package io.fd.honeycomb.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataObjectUpdateTest {

    private InstanceIdentifier<?> id = InstanceIdentifier.create(DataObject.class);
    @Mock
    private DataObject first;
    @Mock
    private DataObject second;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDataObjectUpdate() throws Exception {
        final DataObjectUpdate dataObjectUpdate = DataObjectUpdate.create(id, first, second);
        assertEquals(id, dataObjectUpdate.getId());
        assertEquals(first, dataObjectUpdate.getDataBefore());
        assertEquals(second, dataObjectUpdate.getDataAfter());

        final DataObjectUpdate reverse = dataObjectUpdate.reverse();
        // DataObjectUpdate is identifiable only by the ID
        assertEquals(dataObjectUpdate, reverse);
        assertEquals(dataObjectUpdate.hashCode(), reverse.hashCode());

        assertEquals(dataObjectUpdate, reverse.reverse());
    }

    @Test
    public void testDataObjectDelete() throws Exception {
        final DataObjectUpdate dataObjectUpdate = DataObjectUpdate.create(id, first, null);

        assertTrue(DataObjectUpdate.DataObjectDelete.class.isAssignableFrom(dataObjectUpdate.getClass()));
    }
}