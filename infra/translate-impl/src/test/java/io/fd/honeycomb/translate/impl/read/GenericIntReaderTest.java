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

package io.fd.honeycomb.translate.impl.read;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import io.fd.honeycomb.translate.read.InitFailedException;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;

public class GenericIntReaderTest extends AbstractReaderTest {

    @Mock
    private DataBroker broker;
    @Mock
    private WriteTransaction writeTx;

    public GenericIntReaderTest() {
        super(InitializingReaderCustomizer.class);
    }

    @Override
    protected GenericReader<DataObject, Builder<DataObject>> initReader() {
        return new GenericInitReader<>(DATA_OBJECT_ID, getCustomizer());
    }

    @Override
    public GenericInitReader<DataObject, Builder<DataObject>> getReader() {
        return (GenericInitReader<DataObject, Builder<DataObject>>)super.getReader();
    }

    @Override
    public InitializingReaderCustomizer<DataObject, Builder<DataObject>> getCustomizer() {
        return (InitializingReaderCustomizer<DataObject, Builder<DataObject>>)super.getCustomizer();
    }

    @Test
    public void testInit() throws Exception {
        final Initialized<DataObject> initialized = Initialized.create(DATA_OBJECT_ID, data);

        when(getCustomizer().isPresent(DATA_OBJECT_ID, data, ctx)).thenReturn(true);
        doReturn(initialized).when(getCustomizer()).init(DATA_OBJECT_ID, data, ctx);
        when(writeTx.commit()).thenReturn(FluentFuture.from(Futures.immediateFuture(null)));
        when(broker.newWriteOnlyTransaction()).thenReturn(writeTx);

        getReader().init(broker, DATA_OBJECT_ID, ctx);

        verify(writeTx).merge(LogicalDatastoreType.CONFIGURATION, DATA_OBJECT_ID, data, true);
        verify(writeTx).commit();
    }

    @Test(expected = InitFailedException.class)
    public void testInitFailed() throws Exception {
        doThrow(new ReadFailedException(DATA_OBJECT_ID)).when(getCustomizer())
            .readCurrentAttributes(DATA_OBJECT_ID, builder, ctx);

        getReader().init(broker, DATA_OBJECT_ID, ctx);

        verifyZeroInteractions(writeTx);
    }
}