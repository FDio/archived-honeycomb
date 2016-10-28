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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.util.DataObjects;
import io.fd.honeycomb.translate.util.write.TransactionWriteContext;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class TransactionWriteContextTest {

    @Mock
    private BindingNormalizedNodeSerializer serializer;
    @Mock
    private DOMDataReadOnlyTransaction beforeTx;
    @Mock
    private DOMDataReadOnlyTransaction afterTx;
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
                Futures.immediateCheckedFuture(Optional.absent()));

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
                Futures.immediateCheckedFuture(Optional.of(mock(NormalizedNode.class))));

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
                Futures.immediateFailedCheckedFuture(mock(ReadFailedException.class)));
        transactionWriteContext.readBefore(mock(InstanceIdentifier.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testReadAfterFailed() throws Exception {
        when(afterTx.read(LogicalDatastoreType.CONFIGURATION, yangId)).thenReturn(
                Futures.immediateFailedCheckedFuture(mock(ReadFailedException.class)));
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