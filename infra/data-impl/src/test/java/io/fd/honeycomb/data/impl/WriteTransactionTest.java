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

package io.fd.honeycomb.data.impl;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.util.concurrent.FluentFuture;
import io.fd.honeycomb.data.DataModification;
import io.fd.honeycomb.translate.TranslationException;
import io.fd.honeycomb.translate.ValidationFailedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class WriteTransactionTest {

    @Mock
    private DataModification configSnapshot;
    @Mock
    private YangInstanceIdentifier path;
    @Mock
    private NormalizedNode<?,?> data;

    private WriteTransaction writeTx;

    @Before
    public void setUp() {
        initMocks(this);
        writeTx = WriteTransaction.createConfigOnly(configSnapshot);
    }

    @Test
    public void testPut() {
        writeTx.put(LogicalDatastoreType.CONFIGURATION, path, data);
        verify(configSnapshot).write(path, data);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutOperational() {
        writeTx.put(LogicalDatastoreType.OPERATIONAL, path, data);
        verify(configSnapshot).write(path, data);
    }

    @Test(expected = IllegalStateException.class)
    public void testOnFinishedTx() {
        writeTx.commit();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, path, data);
        verify(configSnapshot).write(path, data);
    }

    @Test
    public void testMerge() {
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, path, data);
        verify(configSnapshot).merge(path, data);
    }

    @Test
    public void testCancel() {
        assertTrue(writeTx.cancel());
    }

    @Test
    public void testCancelFinished() {
        writeTx.commit();
        assertFalse(writeTx.cancel());
    }

    @Test
    public void testDelete() {
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, path);
        verify(configSnapshot).delete(path);
    }

    @Test
    public void testSubmit() throws Exception {
        writeTx.commit();
        verify(configSnapshot).commit();
    }

    @Test
    public void testSubmitFailed() throws Exception {
        doThrow(mock(ValidationFailedException.class)).when(configSnapshot).commit();
        final FluentFuture<? extends CommitInfo> future = writeTx.commit();
        try {
            future.get();
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof TransactionCommitFailedException);
            return;
        }
        fail("Expected exception to be thrown");

    }

    @Test
    public void testCommit() throws TranslationException {
        writeTx.commit();
        verify(configSnapshot).commit();
    }

    @Test
    public void testGetIdentifier() {
        assertNotNull(writeTx.getIdentifier());
    }
}