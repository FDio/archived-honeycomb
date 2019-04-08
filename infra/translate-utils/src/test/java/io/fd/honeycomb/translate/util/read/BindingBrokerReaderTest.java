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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.read.ReadContext;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BindingBrokerReaderTest {

    @Mock
    private DataBroker broker;
    private InstanceIdentifier<DataObject> id = InstanceIdentifier.create(DataObject.class);
    @Mock
    private ReadContext ctx;
    @Mock
    private ReadTransaction tx;
    @Mock
    private DataObject data;
    private BindingBrokerReader<DataObject, DataObjectBuilder> bbReader;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(broker.newReadOnlyTransaction()).thenReturn(tx);
        when(tx.read(LogicalDatastoreType.CONFIGURATION, id))
                .thenReturn(FluentFutures.immediateFluentFuture(Optional.of(data)));
        bbReader = new BindingBrokerReader<>(id, broker, LogicalDatastoreType.CONFIGURATION, DataObjectBuilder.class);
    }

    @Test
    public void testRead() throws Exception {
        assertEquals(id, bbReader.getManagedDataObjectType());
        assertNotNull(bbReader.getBuilder(id));

        final Optional<? extends DataObject> read = bbReader.read(id, ctx);
        assertSame(data, read.get());
        verify(broker).newReadOnlyTransaction();
    }

    @Test(expected = io.fd.honeycomb.translate.read.ReadFailedException.class)
    public void testFailedRead() throws Exception {
        when(tx.read(LogicalDatastoreType.CONFIGURATION, id))
                .thenReturn(FluentFutures.immediateFailedFluentFuture(new ReadFailedException("failing")));
        bbReader.read(id, ctx);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadCurrentAttrs() throws Exception {
        bbReader.readCurrentAttributes(id, new DataObjectBuilder(), ctx);
    }

    static final class DataObjectBuilder implements Builder<DataObject> {

        @Override
        public DataObject build() {
            return mock(DataObject.class);
        }
    }
}