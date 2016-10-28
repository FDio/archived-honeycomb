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

package io.fd.honeycomb.translate.util.read;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.Reader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class KeepaliveReaderWrapperTest {

    @Mock
    private ReadContext ctx;
    private InstanceIdentifier<DataObject> iid = InstanceIdentifier.create(DataObject.class);
    @Mock
    private Reader<DataObject, Builder<DataObject>> delegate;
    @Mock
    private Builder<DataObject> builder;

    private InstanceIdentifier<DataObject> id = InstanceIdentifier.create(DataObject.class);
    private ScheduledExecutorService exec;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        exec = Executors.newScheduledThreadPool(1);
        when(delegate.getManagedDataObjectType()).thenReturn(iid);
        when(delegate.read(eq(iid), any(ReadContext.class))).thenThrow(TestingException.class);
    }

    @After
    public void tearDown() throws Exception {
        exec.shutdownNow();
    }

    @Test(timeout = 10000)
    public void testKeepalive() throws Exception {
        final CapturingFailListener listener = new CapturingFailListener();

        final KeepaliveReaderWrapper<DataObject, Builder<DataObject>> keepaliveWrapper =
                new KeepaliveReaderWrapper<>(delegate, exec, TestingException.class, 1, listener);

        keepaliveWrapper.readCurrentAttributes(id, builder, ctx);
        verify(delegate).readCurrentAttributes(id, builder, ctx);

        keepaliveWrapper.getBuilder(id);
        verify(delegate).getBuilder(id);


        assertTrue(listener.getTriggerFuture().get());
        verify(delegate).read(any(InstanceIdentifier.class), any(ReadContext.class));

        keepaliveWrapper.close();
    }

    @Test(timeout = 10000)
    public void testKeepaliveCancel() throws Exception {
        final CapturingFailListener listener = new CapturingFailListener();

        final KeepaliveReaderWrapper<DataObject, Builder<DataObject>> keepaliveWrapper =
                new KeepaliveReaderWrapper<>(delegate, exec, TestingException.class, 100000, listener);
        keepaliveWrapper.close();
        assertFalse(listener.getTriggerFuture().isDone());
    }

    private static final class TestingException extends RuntimeException {}

    private static class CapturingFailListener implements KeepaliveReaderWrapper.KeepaliveFailureListener {
        private CompletableFuture<Boolean> booleanCompletableFuture = new CompletableFuture<>();

        @Override
        public synchronized void onKeepaliveFailure() {
            booleanCompletableFuture.complete(true);
        }

        synchronized Future<Boolean> getTriggerFuture() {
            return booleanCompletableFuture;
        }
    }
}