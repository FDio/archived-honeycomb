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

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TransactionMappingContextTest {

    private TransactionMappingContext ctx;
    private InstanceIdentifier<DataObject> id = InstanceIdentifier.create(DataObject.class);
    @Mock
    private ReadWriteTransaction writeTx;
    @Mock
    private DataObject data;
    private ReadFailedException ex = new ReadFailedException("test fail");

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ctx = new TransactionMappingContext(writeTx);

        when(writeTx.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());
    }

    @Test
    public void testRead() throws Exception {
        final FluentFuture<Optional<DataObject>> futureData = FluentFutures.immediateFluentFuture(Optional.of((data)));
        when(writeTx.read(LogicalDatastoreType.OPERATIONAL, id)).thenReturn(futureData);

        assertSame(ctx.read(id).get(), data);
        verify(writeTx).read(LogicalDatastoreType.OPERATIONAL, id);

        when(writeTx.read(LogicalDatastoreType.OPERATIONAL, id))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.empty()));
        assertFalse(ctx.read(id).isPresent());
    }


    @Test(expected = IllegalStateException.class)
    public void testReadFailure() throws Exception {
        final FluentFuture<Optional<DataObject>> futureData =
                FluentFutures.immediateFailedFluentFuture(ex);
        when(writeTx.read(LogicalDatastoreType.OPERATIONAL, id)).thenReturn(futureData);
        assertSame(ctx.read(id).get(), data);
    }

    @Test
    public void testMerge() throws Exception {
        ctx.merge(id, data);
        verify(writeTx).merge(LogicalDatastoreType.OPERATIONAL, id, data, true);
    }

    @Test
    public void testPut() throws Exception {
        ctx.put(id, data);
        verify(writeTx).put(LogicalDatastoreType.OPERATIONAL, id, data, true);
    }

    @Test
    public void testDelete() throws Exception {
        ctx.delete(id);
        verify(writeTx).delete(LogicalDatastoreType.OPERATIONAL, id);
    }

    @Test
    public void testClose() throws Exception {
        ctx.close();
        verify(writeTx).cancel();
    }

}