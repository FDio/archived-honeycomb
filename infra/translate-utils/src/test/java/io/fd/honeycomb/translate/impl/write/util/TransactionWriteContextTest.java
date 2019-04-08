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

package io.fd.honeycomb.translate.impl.write.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.util.DataObjects;
import io.fd.honeycomb.translate.util.write.TransactionWriteContext;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class TransactionWriteContextTest {

    @Mock
    private BindingNormalizedNodeSerializer serializer;
    @Mock
    private DOMDataTreeReadTransaction beforeTx;
    @Mock
    private DOMDataTreeReadTransaction afterTx;
    @Mock
    private Map.Entry entry;
    @Mock
    private MappingContext contextBroker;

    private TransactionWriteContext transactionWriteContext;
    private YangInstanceIdentifier yangId;

    @Before
    public void setUp() {
        initMocks(this);
        transactionWriteContext = new TransactionWriteContext(serializer, beforeTx, afterTx, contextBroker);
        yangId = YangInstanceIdentifier.builder().node(QName.create("n", "d")).build();
        when(serializer.toYangInstanceIdentifier(any(InstanceIdentifier.class))).thenReturn(yangId);
    }

    @Test
    public void testReadBeforeNoData() throws Exception {
        when(beforeTx.read(eq(LogicalDatastoreType.CONFIGURATION), any(YangInstanceIdentifier.class))).thenReturn(
                FluentFutures.immediateFluentFuture(Optional.empty()));

        final InstanceIdentifier<DataObjects.DataObject1> instanceId =
                InstanceIdentifier.create(DataObjects.DataObject1.class);

        final Optional<DataObjects.DataObject1> dataObjects = transactionWriteContext.readBefore(instanceId);
        assertNotNull(dataObjects);
        assertFalse(dataObjects.isPresent());

        verify(serializer).toYangInstanceIdentifier(instanceId);
        verify(serializer, never()).fromNormalizedNode(any(YangInstanceIdentifier.class), any(NormalizedNode.class));
    }

    @Test
    public void testReadBefore() throws Exception {
        when(beforeTx.read(eq(LogicalDatastoreType.CONFIGURATION), any(YangInstanceIdentifier.class))).thenReturn(
                FluentFutures.immediateFluentFuture(Optional.of(mock(NormalizedNode.class))));

        final InstanceIdentifier<DataObjects.DataObject1> instanceId =
                InstanceIdentifier.create(DataObjects.DataObject1.class);
        when(serializer.fromNormalizedNode(eq(yangId), any(NormalizedNode.class))).thenReturn(entry);
        when(entry.getValue()).thenReturn(mock(DataObjects.DataObject1.class));

        final Optional<DataObjects.DataObject1> dataObjects = transactionWriteContext.readBefore(instanceId);
        assertNotNull(dataObjects);
        assertTrue(dataObjects.isPresent());

        verify(serializer).toYangInstanceIdentifier(instanceId);
        verify(serializer).fromNormalizedNode(eq(yangId), any(NormalizedNode.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testReadBeforeFailed() throws Exception {
        when(beforeTx.read(LogicalDatastoreType.CONFIGURATION, yangId)).thenReturn(
                FluentFutures.immediateFailedFluentFuture(mock(ReadFailedException.class)));
        transactionWriteContext.readBefore(mock(InstanceIdentifier.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testReadAfterFailed() throws Exception {
        when(afterTx.read(LogicalDatastoreType.CONFIGURATION, yangId)).thenReturn(
                FluentFutures.immediateFailedFluentFuture(mock(ReadFailedException.class)));
        transactionWriteContext.readAfter(mock(InstanceIdentifier.class));
    }

    @Test
    public void testGetContext() throws Exception {
        assertNotNull(transactionWriteContext.getModificationCache());
    }

    @Test
    public void testClose() throws Exception {
        final ModificationCache context = transactionWriteContext.getModificationCache();
        final Object o = new Object();
        context.put(o, o);
        assertTrue(context.containsKey(o));
        transactionWriteContext.close();
        assertFalse(context.containsKey(o));
    }
}