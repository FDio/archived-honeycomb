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

package io.fd.honeycomb.impl;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class ShutdownHandlerImplTest {

    private ShutdownHandlerImpl shutdownHandler;

    @Before
    public void setUp() {
        shutdownHandler = new ShutdownHandlerImpl();
    }

    @Test
    public void testShutdownOrder() throws Exception {
        AutoCloseable component0 = mock(AutoCloseable.class);
        AutoCloseable component1 = mock(AutoCloseable.class);
        AutoCloseable component2 = mock(AutoCloseable.class);
        InOrder inOrder = Mockito.inOrder(component0, component1, component2);
        shutdownHandler.register("component0", component0);
        shutdownHandler.register("component1", component1);
        shutdownHandler.register("component2", component2);
        shutdownHandler.performShutdown();

        // check the order was reversed
        inOrder.verify(component2).close();
        inOrder.verify(component1).close();
        inOrder.verify(component0).close();
    }

    @Test
    public void testShutdownFail() throws Exception {
        AutoCloseable component0 = mock(AutoCloseable.class);
        AutoCloseable component1 = mock(AutoCloseable.class);
        doThrow(new IllegalStateException()).when(component1).close();
        AutoCloseable component2 = mock(AutoCloseable.class);
        InOrder inOrder = Mockito.inOrder(component0, component1, component2);
        shutdownHandler.register("component0", component0);
        shutdownHandler.register("component1", component1);
        shutdownHandler.register("component2", component2);
        shutdownHandler.performShutdown();

        // shoutdown failed for component1, but other components should be closed
        inOrder.verify(component2).close();
        inOrder.verify(component0).close();
        inOrder.verifyNoMoreInteractions();
    }

}
