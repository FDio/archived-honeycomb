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

package io.fd.honeycomb.data.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.write.DataObjectUpdate;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import java.util.AbstractMap;
import java.util.Map;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

abstract class ModifiableDataTreeDelegatorBaseTest extends ModificationBaseTest {

    @Mock
    WriterRegistry writer;
    @Mock
    BindingNormalizedNodeSerializer serializer;
    @Mock
    org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification modification;
    @Mock
    DataBroker contextBroker;
    @Mock
    org.opendaylight.mdsal.binding.api.ReadWriteTransaction tx;

    @Captor
    ArgumentCaptor<WriteContext> writeContextCaptor;

    @Captor
    ArgumentCaptor<WriterRegistry.DataObjectUpdates> updatesCaptor;

    final InstanceIdentifier<?> DEFAULT_ID = InstanceIdentifier.create(DataObject.class);
    final DataObject DEFAULT_DATA_OBJECT = mockDataObject("serialized", DataObject.class);

    DataTree dataTree;
    ModifiableDataTreeManager configDataTree;
    DataObjectUpdate update = DataObjectUpdate.create(DEFAULT_ID, null, DEFAULT_DATA_OBJECT);

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        dataTree = getDataTree();
        when(contextBroker.newReadWriteTransaction()).thenReturn(tx);
        when(tx.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());

        when(serializer.fromYangInstanceIdentifier(any(YangInstanceIdentifier.class)))
                .thenReturn(((InstanceIdentifier) DEFAULT_ID));
        final Map.Entry<InstanceIdentifier<?>, DataObject> parsed =
                new AbstractMap.SimpleEntry<>(DEFAULT_ID, DEFAULT_DATA_OBJECT);
        when(serializer.fromNormalizedNode(any(YangInstanceIdentifier.class), any(NormalizedNode.class)))
                .thenReturn(parsed);

        configDataTree = new ModifiableDataTreeDelegator(serializer, dataTree, getSchemaCtx(), writer, contextBroker);

        additionalSetup();
    }

    void additionalSetup() throws Exception {
    }

    private static DataObject mockDataObject(final String name, final Class<? extends DataObject> classToMock) {
        final DataObject dataBefore = mock(classToMock, name);
        doReturn(classToMock).when(dataBefore).getImplementedInterface();
        return dataBefore;
    }

    abstract static class DataObject1 implements DataObject {
    }

    abstract static class DataObject2 implements DataObject {
    }

    abstract static class DataObject3 implements DataObject {
    }

    <D extends DataObject> D mockDataObject(final YangInstanceIdentifier yid1,
                                                    final InstanceIdentifier iid1,
                                                    final NormalizedNode nn1B,
                                                    final Class<D> type) {
        final D do1B = mock(type);
        when(serializer.fromNormalizedNode(yid1, nn1B)).thenReturn(new AbstractMap.SimpleEntry<>(iid1, do1B));
        return do1B;
    }

    NormalizedNode mockNormalizedNode(final QName nn1) {
        final NormalizedNode nn1B = mock(NormalizedNode.class);
        when(nn1B.getNodeType()).thenReturn(nn1);
        return nn1B;
    }

    InstanceIdentifier mockIid(final YangInstanceIdentifier yid1,
                                       final Class<? extends DataObject> type) {
        final InstanceIdentifier iid1 = InstanceIdentifier.create(type);
        when(serializer.fromYangInstanceIdentifier(yid1)).thenReturn(iid1);
        return iid1;
    }

    YangInstanceIdentifier mockYid(final QName nn1) {
        final YangInstanceIdentifier yid1 = mock(YangInstanceIdentifier.class);
        when(yid1.getLastPathArgument()).thenReturn(new YangInstanceIdentifier.NodeIdentifier(nn1));
        return yid1;
    }
}
