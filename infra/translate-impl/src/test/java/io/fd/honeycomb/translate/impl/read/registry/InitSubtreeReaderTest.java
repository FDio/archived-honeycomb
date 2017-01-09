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

package io.fd.honeycomb.translate.impl.read.registry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Sets;
import io.fd.honeycomb.translate.read.InitListReader;
import io.fd.honeycomb.translate.read.InitReader;
import io.fd.honeycomb.translate.read.ListReader;
import io.fd.honeycomb.translate.read.ReadContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InitSubtreeReaderTest {

    @Mock
    private DataBroker broker;
    @Mock
    private InitReader<DataObject1, Builder<DataObject1>> delegate;
    @Mock
    private InitListReader<DataObject2, DataObject2.DataObject2Key, Builder<DataObject2>> listDelegate;
    @Mock
    private ReadContext ctx;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(DataObject1.IID).when(delegate).getManagedDataObjectType();
        doReturn(DataObject2.IID).when(listDelegate).getManagedDataObjectType();
    }

    @Test
    public void testInit() throws Exception {
        final InitReader<DataObject1, Builder<DataObject1>> initSubReader =
            InitSubtreeReader.createForReader(Sets.newHashSet(DataObject1.DataObject11.IID), delegate);
        assertFalse(initSubReader instanceof ListReader);

        initSubReader.init(broker, DataObject1.IID, ctx);
        verify(delegate).init(broker, DataObject1.IID, ctx);
    }

    @Test
    public void testInitList() throws Exception {
        final InitReader<DataObject2, Builder<DataObject2>> initSubReader =
            InitSubtreeReader.createForReader(Sets.newHashSet(DataObject2.DataObject22.IID), listDelegate);
        assertTrue(initSubReader instanceof ListReader);

        initSubReader.init(broker, DataObject2.IID, ctx);
        verify(listDelegate).init(broker, DataObject2.IID, ctx);
    }

    private abstract static class DataObject1 implements DataObject {
        private static InstanceIdentifier<DataObject1> IID = InstanceIdentifier.create(DataObject1.class);
        private abstract static class DataObject11 implements DataObject, ChildOf<DataObject1> {
            private static InstanceIdentifier<DataObject11> IID = DataObject1.IID.child(DataObject11.class);
        }
    }

    private abstract static class DataObject2 implements Identifiable<DataObject2.DataObject2Key>, DataObject {
        private static InstanceIdentifier<DataObject2> IID = InstanceIdentifier.create(DataObject2.class);
        abstract static class DataObject2Key implements Identifier<DataObject2> {
        }
        private abstract static class DataObject22 implements DataObject, ChildOf<DataObject2> {
            public static InstanceIdentifier<DataObject22> IID = DataObject2.IID.child(DataObject22.class);
        }
    }
}