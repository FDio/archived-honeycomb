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


import org.junit.Test;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import static org.junit.Assert.*;

public class ContainerNodeDataProcessorTest extends AbstractYangDataProcessorTest {

    private ContainerNodeDataProcessor processor;

    @Override
    void setUp() {
        processor = new ContainerNodeDataProcessor(moduleInfoBackedContext.getSchemaContext(), serializer);
    }

    @Test
    public void testGetNodeDataNestedContainer() {
        final DataObject nodeData = processor.getNodeData(codec.deserialize(InjectionTestData.CONTAINER_UNDER_LIST_DATA_PATH),
                InjectionTestData.CONTAINER_UNDER_LIST_RESOURCE);
        assertNotNull(nodeData);
    }

    @Test
    public void testGetNodeDataRootContainer() {
        final DataObject nodeData = processor.getNodeData(YangInstanceIdentifier.EMPTY, InjectionTestData.CONTAINER_IN_ROOT_RESOURCE);
        assertNotNull(nodeData);
    }


    @Test
    public void testCanProcessNegative() {
        assertFalse(processor.canProcess(codec.deserialize(InjectionTestData.SIMPLE_LIST_DATA_PATH)));
        assertFalse(processor.canProcess(codec.deserialize(InjectionTestData.NESTED_LIST_DATA_PATH)));
    }

    @Test
    public void testCanProcessPositive() {
        assertTrue(processor.canProcess(YangInstanceIdentifier.EMPTY));
        assertTrue(processor.canProcess(codec.deserialize(InjectionTestData.CONTAINER_UNDER_LIST_DATA_PATH)));
    }
}
