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

package io.fd.honeycomb.data.init;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
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
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AbstractDataTreeConverterTest {

    private static final InstanceIdentifier<Operational>
            OPER_ROOT_ID = InstanceIdentifier.create(Operational.class);
    private static final InstanceIdentifier<Configuration>
            CFG_ROOT_ID = InstanceIdentifier.create(Configuration.class);
    @Mock
    private DataBroker bindingDataBroker;
    @Mock
    private ReadOnlyTransaction readTx;
    @Mock
    private WriteTransaction writeTx;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(bindingDataBroker.newReadOnlyTransaction()).thenReturn(readTx);
        when(readTx.read(LogicalDatastoreType.OPERATIONAL, OPER_ROOT_ID)).thenReturn(
                        Futures.immediateCheckedFuture(Optional.of(Operational.instance)));
        when(bindingDataBroker.newWriteOnlyTransaction()).thenReturn(writeTx);
        when(writeTx.submit()).thenReturn(Futures.immediateCheckedFuture(null));
    }

    @Test
    public void testConvert() throws Exception {
        final TestingDataTreeConverter converter = new TestingDataTreeConverter(bindingDataBroker);
        converter.initialize();

        verify(bindingDataBroker).newReadOnlyTransaction();
        verify(readTx).read(LogicalDatastoreType.OPERATIONAL, OPER_ROOT_ID);
        verify(bindingDataBroker).newWriteOnlyTransaction();
        verify(writeTx).merge(LogicalDatastoreType.CONFIGURATION, CFG_ROOT_ID, Configuration.instance);
    }

    @Test
    public void testReadFailNoop() throws Exception {
        when(readTx.read(LogicalDatastoreType.OPERATIONAL, OPER_ROOT_ID)).thenReturn(
                Futures.immediateFailedCheckedFuture(new ReadFailedException("failing")));

        final TestingDataTreeConverter converter = new TestingDataTreeConverter(bindingDataBroker);
        converter.initialize();

        verify(bindingDataBroker).newReadOnlyTransaction();
        verify(readTx).read(LogicalDatastoreType.OPERATIONAL, OPER_ROOT_ID);
        verify(bindingDataBroker, times(0)).newWriteOnlyTransaction();
        verifyZeroInteractions(writeTx);
    }

    private static class Configuration implements DataObject {

        static final Configuration instance = mock(Configuration.class);

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return DataObject.class;
        }
    }

    private static class Operational implements DataObject {

        static final Operational instance = mock(Operational.class);

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return DataObject.class;
        }
    }

    private static class TestingDataTreeConverter extends AbstractDataTreeConverter<Operational, Configuration> {

        public TestingDataTreeConverter(final DataBroker bindingDataBroker) {
            super(bindingDataBroker, OPER_ROOT_ID, CFG_ROOT_ID);
        }

        @Override
        protected Configuration convert(
                final Operational operationalData) {
            return Configuration.instance;
        }
    }
}