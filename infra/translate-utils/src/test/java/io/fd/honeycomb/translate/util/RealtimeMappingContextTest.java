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

package io.fd.honeycomb.translate.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RealtimeMappingContextTest {

    @Mock
    private DataBroker broker;
    private RealtimeMappingContext ctx;
    private InstanceIdentifier<DataObject> id = InstanceIdentifier.create(DataObject.class);
    @Mock
    private ReadOnlyTransaction readTx;
    @Mock
    private WriteTransaction writeTx;
    @Mock
    private DataObject data;
    private TransactionCommitFailedException ex = new TransactionCommitFailedException("test fail");

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ctx = new RealtimeMappingContext(broker);

        when(broker.newReadOnlyTransaction()).thenReturn(readTx);
        when(broker.newWriteOnlyTransaction()).thenReturn(writeTx);
        when(writeTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
    }

    @Test
    public void testRead() throws Exception {
        final CheckedFuture<Optional<DataObject>, ReadFailedException> futureData =
                Futures.immediateCheckedFuture(Optional.of((data)));
        when(readTx.read(LogicalDatastoreType.OPERATIONAL, id)).thenReturn(futureData);

        assertSame(ctx.read(id).get(), data);
        verify(broker).newReadOnlyTransaction();
        verify(readTx).read(LogicalDatastoreType.OPERATIONAL, id);

        when(readTx.read(LogicalDatastoreType.OPERATIONAL, id)).thenReturn(Futures.immediateCheckedFuture(Optional.absent()));
        assertFalse(ctx.read(id).isPresent());
    }

    @Test
    public void testMerge() throws Exception {
        ctx.merge(id, data);
        verify(broker).newWriteOnlyTransaction();
        verify(writeTx).merge(LogicalDatastoreType.OPERATIONAL, id, data);
    }

    @Test(expected = IllegalStateException.class)
    public void testMergeFailure() throws Exception {
        when(writeTx.submit()).thenReturn(Futures.immediateFailedCheckedFuture(ex));
        ctx.merge(id, data);
    }

    @Test
    public void testPut() throws Exception {
        ctx.put(id, data);
        verify(broker).newWriteOnlyTransaction();
        verify(writeTx).put(LogicalDatastoreType.OPERATIONAL, id, data);
    }

    @Test(expected = IllegalStateException.class)
    public void testPutFailure() throws Exception {
        when(writeTx.submit()).thenReturn(Futures.immediateFailedCheckedFuture(ex));
        ctx.put(id, data);
    }

    @Test
    public void testDelete() throws Exception {
        ctx.delete(id);
        verify(broker).newWriteOnlyTransaction();
        verify(writeTx).delete(LogicalDatastoreType.OPERATIONAL, id);
    }

    @Test(expected = IllegalStateException.class)
    public void testDeleteFailure() throws Exception {
        when(writeTx.submit()).thenReturn(Futures.immediateFailedCheckedFuture(ex));
        ctx.delete(id);
    }
}