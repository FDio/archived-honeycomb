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

package io.fd.honeycomb.rpc;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class RpcRegistryBuilderTest {

    private RpcRegistry registry;
    @Mock
    private RpcService service1;
    @Mock
    private RpcService service2;
    private static final SchemaPath ID1 = SchemaPath.ROOT.createChild(QName.create("a"));
    private static final SchemaPath ID2 = SchemaPath.ROOT.createChild(QName.create("b"));

    @Before
    public void setUp() {
        service1 = Mockito.mock(RpcService.class);
        Mockito.when(service1.getManagedNode()).thenReturn(ID1);

        service2 = Mockito.mock(RpcService.class);
        Mockito.when(service2.getManagedNode()).thenReturn(ID2);

        final RpcRegistryBuilder builder = new RpcRegistryBuilder();
        builder.addService(service1);
        builder.addService(service2);
        registry = builder.build();
    }

    @Test
    public void testInvokeService() {
        final DataObject request = Mockito.mock(DataObject.class);

        registry.invoke(ID2, request);

        Mockito.verify(service2).invoke(request);
        Mockito.verify(service1, Mockito.never()).invoke(ArgumentMatchers.any());
    }

    @Test
    public void testServiceNotFound() throws ExecutionException, InterruptedException {
        final SchemaPath id = SchemaPath.ROOT.createChild(QName.create("c"));
        final DataObject request = Mockito.mock(DataObject.class);

        try {
            registry.invoke(id, request).toCompletableFuture().get();
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof DOMRpcImplementationNotAvailableException);
            return;
        }
        fail("Exception expected");
    }
}