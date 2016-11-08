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

package io.fd.honeycomb.translate.impl.read.registry;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import io.fd.honeycomb.translate.util.DataObjects;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class TypeHierarchyTest {

    @Test
    public void testHierarchy() throws Exception {
        final TypeHierarchy typeHierarchy = TypeHierarchy.create(Sets.newHashSet(
                DataObjects.DataObject4.DataObject41.DataObject411.IID,
                DataObjects.DataObject4.DataObject41.IID,/* Included in previous already */
                DataObjects.DataObject1.IID,
                DataObjects.DataObject3.DataObject31.IID));

        // Roots
        assertThat(typeHierarchy.getRoots().size(), is(3));
        assertThat(typeHierarchy.getRoots(), CoreMatchers
                .hasItems(DataObjects.DataObject1.IID, DataObjects.DataObject3.IID, DataObjects.DataObject4.IID));

        // Leaves
        assertThat(typeHierarchy.getDirectChildren(DataObjects.DataObject1.IID).size(), is(0));
        assertThat(typeHierarchy.getDirectChildren(DataObjects.DataObject3.DataObject31.IID).size(), is(0));
        assertThat(typeHierarchy.getDirectChildren(DataObjects.DataObject4.DataObject41.DataObject411.IID).size(), is(0));

        // Intermediate leaves
        assertThat(typeHierarchy.getDirectChildren(DataObjects.DataObject3.IID).size(), is(1));
        assertThat(typeHierarchy.getDirectChildren(DataObjects.DataObject3.IID), CoreMatchers
                .hasItem(DataObjects.DataObject3.DataObject31.IID));
        assertEquals(typeHierarchy.getDirectChildren(DataObjects.DataObject3.IID), typeHierarchy.getAllChildren(
                DataObjects.DataObject3.IID));

        assertThat(typeHierarchy.getDirectChildren(DataObjects.DataObject4.DataObject41.IID).size(), is(1));
        assertThat(typeHierarchy.getDirectChildren(DataObjects.DataObject4.DataObject41.IID), CoreMatchers.hasItem(
                DataObjects.DataObject4.DataObject41.DataObject411.IID));
        assertEquals(typeHierarchy.getDirectChildren(DataObjects.DataObject4.DataObject41.IID), typeHierarchy.getAllChildren(
                DataObjects.DataObject4.DataObject41.IID));

        assertThat(typeHierarchy.getDirectChildren(DataObjects.DataObject4.IID).size(), is(1));
        assertThat(typeHierarchy.getDirectChildren(DataObjects.DataObject4.IID), CoreMatchers
                .hasItem(DataObjects.DataObject4.DataObject41.IID));
        assertThat(typeHierarchy.getAllChildren(DataObjects.DataObject4.IID).size(), is(2));
        assertTrue(typeHierarchy.getAllChildren(DataObjects.DataObject4.IID).contains(DataObjects.DataObject4.DataObject41.IID));
        assertTrue(typeHierarchy.getAllChildren(DataObjects.DataObject4.IID).contains(DataObjects.DataObject4.DataObject41.DataObject411.IID));
    }
}

