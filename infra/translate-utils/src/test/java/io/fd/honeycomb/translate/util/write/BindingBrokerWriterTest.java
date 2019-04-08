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

package io.fd.honeycomb.translate.util.write;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BindingBrokerWriterTest {

    @Mock
    private DataBroker broker;
    private InstanceIdentifier<DataObject> id = InstanceIdentifier.create(DataObject.class);
    @Mock
    private WriteContext ctx;
    @Mock
    private WriteTransaction tx;
    @Mock
    private DataObject data;
    private BindingBrokerWriter<DataObject> bbWriter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(broker.newWriteOnlyTransaction()).thenReturn(tx);
        when(tx.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());
        bbWriter = new BindingBrokerWriter<>(id, broker);
    }

    @Test
    public void testWrite() throws Exception {
        assertEquals(id, bbWriter.getManagedDataObjectType());

        bbWriter.processModification(id, data, data, ctx);
        verify(broker).newWriteOnlyTransaction();
        verify(tx).put(LogicalDatastoreType.CONFIGURATION, id, data);
        verify(tx).commit();
    }

    @Test(expected = io.fd.honeycomb.translate.write.WriteFailedException.class)
    public void testFailedWrite() throws Exception {
        when(tx.commit()).thenReturn(FluentFutures.immediateFailedFluentFuture(new InterruptedException("failing")));
        bbWriter.processModification(id, data, data, ctx);
    }
}