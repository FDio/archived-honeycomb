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

package io.fd.honeycomb.test.tools;

import static io.fd.honeycomb.test.tools.InjectionTestData.AUGMENT_LIST_DATA_PATH;
import static io.fd.honeycomb.test.tools.InjectionTestData.AUGMENT_LIST_RESOURCE;
import static io.fd.honeycomb.test.tools.InjectionTestData.CONTAINER_UNDER_LIST_DATA_PATH;
import static io.fd.honeycomb.test.tools.InjectionTestData.NESTED_LIST_DATA_PATH;
import static io.fd.honeycomb.test.tools.InjectionTestData.NESTED_LIST_RESOURCE;
import static io.fd.honeycomb.test.tools.InjectionTestData.ROOT_LIST_DATA_PATH;
import static io.fd.honeycomb.test.tools.InjectionTestData.ROOT_LIST_RESOURCE;
import static io.fd.honeycomb.test.tools.InjectionTestData.SIMPLES_LIST_RESOURCE;
import static io.fd.honeycomb.test.tools.InjectionTestData.SIMPLE_LIST_DATA_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.RootList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.simple.container.SimpleList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.simple.container.augmented.container.ListInAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.simple.container.simple.list.NestedList;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ListNodeDataProcessorTest extends AbstractYangDataProcessorTest {

    private ListNodeDataProcessor processor;

    @Override
    void setUp() {
        processor = new ListNodeDataProcessor(moduleInfoBackedContext.getSchemaContext(), serializer);
    }

    @Test
    public void testCanProcessPositive() {
        assertTrue(processor.canProcess(codec.deserialize(SIMPLE_LIST_DATA_PATH)));
        assertTrue(processor.canProcess(codec.deserialize(NESTED_LIST_DATA_PATH)));
    }

    @Test
    public void testCanProcessNegative() {
        assertFalse(processor.canProcess(codec.deserialize(CONTAINER_UNDER_LIST_DATA_PATH)));
        assertFalse(processor.canProcess(YangInstanceIdentifier.EMPTY));
    }

    @Test
    public void testGetNodeDataSimpleList() {
        final DataObject nodeData = processor.getNodeData(codec.deserialize(SIMPLE_LIST_DATA_PATH), SIMPLES_LIST_RESOURCE);
        assertNotNull(nodeData);
        assertEquals("nameUnderSimpleList", ((SimpleList) nodeData).getName());
    }

    @Test
    public void testGetNodeDataNestedList() {
        final DataObject nodeData = processor.getNodeData(codec.deserialize(NESTED_LIST_DATA_PATH), NESTED_LIST_RESOURCE);
        assertNotNull(nodeData);
        assertEquals("nameUnderNestedList", ((NestedList) nodeData).getNestedName());
    }

    @Test
    public void testGetNodeDataRootList() {
        final DataObject nodeData = processor.getNodeData(codec.deserialize(ROOT_LIST_DATA_PATH), ROOT_LIST_RESOURCE);
        assertNotNull(nodeData);
        assertEquals("rootName", ((RootList) nodeData).getRootName());
    }

    @Test
    public void testGetNodeDataAugmentList() {
        final DataObject nodeData = processor.getNodeData(codec.deserialize(AUGMENT_LIST_DATA_PATH), AUGMENT_LIST_RESOURCE);
        assertNotNull(nodeData);
        assertEquals("keyInAugment", ((ListInAugment) nodeData).getKeyInAugment());
    }
}
