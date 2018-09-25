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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import io.fd.honeycomb.translate.read.InitFailedException;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.concepts.Builder;

public class GenericInitListReaderTest extends AbstractListReaderTest {

    @Mock
    private DataBroker broker;
    @Mock
    private WriteTransaction writeTx;

    public GenericInitListReaderTest() {
        super(InitializingListReaderCustomizer.class);
    }

    @Override
    protected GenericListReader<AbstractListReaderTest.TestingData, AbstractListReaderTest.TestingData.TestingKey, Builder<AbstractListReaderTest.TestingData>> initReader() {
        return new GenericInitListReader<>(DATA_OBJECT_ID, getCustomizer());
    }

    @Override
    protected InitializingListReaderCustomizer<AbstractListReaderTest.TestingData, AbstractListReaderTest.TestingData.TestingKey, Builder<AbstractListReaderTest.TestingData>> getCustomizer() {
        return (InitializingListReaderCustomizer<AbstractListReaderTest.TestingData, AbstractListReaderTest.TestingData.TestingKey, Builder<AbstractListReaderTest.TestingData>>) super
            .getCustomizer();
    }

    @Override
    public GenericInitListReader<AbstractListReaderTest.TestingData, AbstractListReaderTest.TestingData.TestingKey, Builder<AbstractListReaderTest.TestingData>> getReader() {
        return (GenericInitListReader<AbstractListReaderTest.TestingData, AbstractListReaderTest.TestingData.TestingKey, Builder<AbstractListReaderTest.TestingData>>) super
            .getReader();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInit() throws Exception {
        final Initialized<TestingData> initialized = Initialized.create(DATA_OBJECT_ID, data);
        when(getCustomizer().isPresent(any(), any(), any())).thenReturn(true);
        doReturn(initialized).when(getCustomizer()).init(any(), any(), any());
        when(writeTx.commit()).thenReturn(FluentFuture.from(Futures.immediateFuture(null)));
        when(broker.newWriteOnlyTransaction()).thenReturn(writeTx);

        getReader().init(broker, DATA_OBJECT_ID, ctx);

        verify(writeTx, times(2)).merge(LogicalDatastoreType.CONFIGURATION, DATA_OBJECT_ID, data, true);
        verify(writeTx, times(2)).commit();
    }

    @Test(expected = InitFailedException.class)
    public void testInitFailed() throws Exception {
        doThrow(new ReadFailedException(DATA_OBJECT_ID)).when(getCustomizer())
            .readCurrentAttributes(any(), any(), any());

        getReader().init(broker, DATA_OBJECT_ID, ctx);

        verifyZeroInteractions(writeTx);
    }
}