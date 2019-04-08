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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.util.concurrent.FluentFuture;
import io.fd.honeycomb.data.DataModification;
import io.fd.honeycomb.data.ReadableDataManager;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ReadOnlyTransactionTest {

    @Mock
    private ReadableDataManager operationalData;
    @Mock
    private DataModification configSnapshot;

    private ReadOnlyTransaction readOnlyTx;

    @Before
    public void setUp() {
        initMocks(this);
        readOnlyTx = ReadOnlyTransaction.create(configSnapshot, operationalData);
    }

    @Test
    public void testExists() {
        final YangInstanceIdentifier path = mock(YangInstanceIdentifier.class);
        final FluentFuture<Optional<NormalizedNode<?, ?>>> future = mock(FluentFuture.class);
        when(operationalData.read(path)).thenReturn(future);

        readOnlyTx.exists(LogicalDatastoreType.OPERATIONAL, path);

        verify(operationalData).read(path);
    }

    @Test
    public void testGetIdentifier() {
        assertNotNull(readOnlyTx.getIdentifier());
    }
}