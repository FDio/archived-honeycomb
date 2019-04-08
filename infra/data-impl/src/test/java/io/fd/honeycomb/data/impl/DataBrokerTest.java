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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.ClassToInstanceMap;
import io.fd.honeycomb.data.DataModification;
import io.fd.honeycomb.data.ModifiableDataManager;
import io.fd.honeycomb.data.ReadableDataManager;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.netconf.mdsal.connector.DOMDataTransactionValidator;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class DataBrokerTest {

    @Mock
    private ReadableDataManager operationalData;
    @Mock
    private ModifiableDataManager confiDataTree;
    @Mock
    private DataModification configSnapshot;
    private DataBroker broker;

    @Before
    public void setUp() {
        initMocks(this);
        when(confiDataTree.newModification()).thenReturn(configSnapshot);
        broker = DataBroker.create(confiDataTree, operationalData);
    }

    @Test
    public void testNewReadWriteTransaction() {
        final DOMDataTreeReadWriteTransaction readWriteTx = broker.newReadWriteTransaction();
        final YangInstanceIdentifier path = mock(YangInstanceIdentifier.class);
        readWriteTx.read(LogicalDatastoreType.CONFIGURATION, path);

        // verify that read and write transactions use the same config snapshot
        verify(configSnapshot).read(path);
        verify(confiDataTree).newModification();
    }

    @Test
    public void testNewWriteOnlyTransaction() {
        broker.newWriteOnlyTransaction();

        // verify that write transactions use config snapshot
        verify(confiDataTree).newModification();
    }

    @Test
    public void testNewReadOnlyTransaction() {
        final DOMDataTreeReadTransaction readTx = broker.newReadOnlyTransaction();

        final YangInstanceIdentifier path = mock(YangInstanceIdentifier.class);
        readTx.read(LogicalDatastoreType.CONFIGURATION, path);

        // verify that read transactions use config snapshot
        verify(configSnapshot).read(path);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCreateTransactionChain() {
        final DOMTransactionChainListener listener = mock(DOMTransactionChainListener.class);
        broker.createTransactionChain(listener);
    }

    @Test
    public void testGetSupportedExtensions() {
        final @NonNull ClassToInstanceMap<DOMDataBrokerExtension> supportedExtensions =
                broker.getExtensions();
        assertEquals(1, supportedExtensions.size());
        assertNotNull(supportedExtensions.get(DOMDataTransactionValidator.class));
    }

    public static class DataBrokerForContextTest {

        @Mock
        private ModifiableDataManager contextDataTree;
        @Mock
        private DataModification contextSnapshot;
        private DataBroker broker;

        @Before
        public void setUp() {
            initMocks(this);
            when(contextDataTree.newModification()).thenReturn(contextSnapshot);
            broker = DataBroker.create(contextDataTree);
        }

        @Test
        public void testNewReadWriteTransaction() {
            final DOMDataTreeReadWriteTransaction readWriteTx = broker.newReadWriteTransaction();
            final YangInstanceIdentifier path = mock(YangInstanceIdentifier.class);
            readWriteTx.read(LogicalDatastoreType.OPERATIONAL, path);

            verify(contextSnapshot).read(path);
            verify(contextDataTree).newModification();
        }

        @Test
        public void testNewWriteOnlyTransaction() {
            broker.newWriteOnlyTransaction();
            verify(contextDataTree).newModification();
        }

        @Test
        public void testNewReadOnlyTransaction() {
            final DOMDataTreeReadTransaction readTx = broker.newReadOnlyTransaction();
            final YangInstanceIdentifier path = mock(YangInstanceIdentifier.class);
            readTx.read(LogicalDatastoreType.OPERATIONAL, path);

            // operational data are read directly from data tree
            verify(contextDataTree).read(path);
        }

        @Test(expected = IllegalArgumentException.class)
        public void testReadConfig() {
            final DOMDataTreeReadTransaction readTx = broker.newReadOnlyTransaction();

            final YangInstanceIdentifier path = mock(YangInstanceIdentifier.class);
            readTx.read(LogicalDatastoreType.CONFIGURATION, path);
        }

        @Test(expected = UnsupportedOperationException.class)
        public void testCreateTransactionChain() {
            final DOMTransactionChainListener listener = mock(DOMTransactionChainListener.class);
            broker.createTransactionChain(listener);
        }

        @Test
        public void testGetSupportedExtensions() {
            final @NonNull ClassToInstanceMap<DOMDataBrokerExtension> supportedExtensions = broker.getExtensions();
            assertTrue(supportedExtensions.isEmpty());
        }
    }
}