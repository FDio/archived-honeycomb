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

package io.fd.honeycomb.translate.impl.read.registry;

import static io.fd.honeycomb.translate.util.DataObjects.DataObject4;
import static io.fd.honeycomb.translate.util.DataObjects.DataObject4.DataObject41;
import static io.fd.honeycomb.translate.util.DataObjects.DataObjectK;
import static io.fd.honeycomb.translate.util.DataObjects.DataObjectKey;
import static io.fd.honeycomb.translate.impl.read.registry.CompositeReaderRegistryTest.mockReader;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.read.ListReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.Reader;
import io.fd.honeycomb.translate.util.DataObjects;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class CompositeReaderTest {

    @Mock
    private ReadContext ctx;
    private Reader<DataObject41, Builder<DataObject41>> reader41;
    private Reader<DataObject4, Builder<DataObject4>> reader4;
    private Reader<DataObject4, Builder<DataObject4>> compositeReader;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        reader41 = mockReader(DataObject41.class);
        reader4 = mockReader(DataObject4.class);
        compositeReader = CompositeReader
                .createForReader(reader4, ImmutableMap.of(DataObject41.class, reader41));
    }

    @Test
    public void testReadCurrent() throws Exception {
        compositeReader.read(DataObject4.IID, ctx);
        verify(reader4).readCurrentAttributes(eq(DataObject4.IID), any(Builder.class), eq(ctx));
        verify(reader41).read(DataObject41.IID, ctx);
    }

    @Test
    public void testReadJustChild() throws Exception {
        // Delegating read to child
        compositeReader.read(DataObject41.IID, ctx);
        verify(reader4, times(0))
                .readCurrentAttributes(any(InstanceIdentifier.class), any(Builder.class), any(ReadContext.class));
        verify(reader41).read(DataObject41.IID, ctx);
    }

    @Test
    public void testReadFallback() throws Exception {
        // Delegating read to delegate as a fallback since IID does not fit, could be handled by the delegate if its
        // a subtree handler
        compositeReader.read(DataObjects.DataObject4.DataObject42.IID, ctx);
        verify(reader4).read(DataObjects.DataObject4.DataObject42.IID, ctx);
        verify(reader41, times(0)).read(any(InstanceIdentifier.class), any(ReadContext.class));
    }

    @Test
    public void testList() throws Exception {
        final Reader<DataObjectK.DataObjectK1, Builder<DataObjectK.DataObjectK1>> readerK1 =
                mockReader(DataObjectK.DataObjectK1.class);
        final ListReader<DataObjectK, DataObjectKey, Builder<DataObjectK>> readerK =
                mockListReader(DataObjectK.class, Lists.newArrayList(new DataObjectKey(), new DataObjectKey()));
        final ListReader<DataObjectK, DataObjectKey, Builder<DataObjectK>>
                compositeReaderK = (ListReader<DataObjectK, DataObjectKey, Builder<DataObjectK>>)
                CompositeReader.createForReader(readerK, ImmutableMap.of(DataObject41.class, readerK1));

        compositeReaderK.readList(DataObjectK.IID, ctx);

        verify(readerK).getAllIds(DataObjectK.IID, ctx);
        verify(readerK, times(2))
                .readCurrentAttributes(any(InstanceIdentifier.class), any(Builder.class), any(ReadContext.class));
    }

    @SuppressWarnings("unchecked")
    static <D extends DataObject & Identifiable<K>, K extends Identifier<D>, B extends Builder<D>> ListReader<D, K, B> mockListReader(
            final Class<D> dataType, List<K> keys)
            throws Exception {
        final ListReader r = mock(ListReader.class);
        final Object iid = dataType.getDeclaredField("IID").get(null);
        when(r.getManagedDataObjectType()).thenReturn((InstanceIdentifier) iid);
        final Builder builder = mock(Builder.class);
        when(builder.build()).thenReturn(mock(dataType));
        when(r.getBuilder(any(InstanceIdentifier.class))).thenReturn(builder);
        when(r.read(any(InstanceIdentifier.class), any(ReadContext.class))).thenReturn(Optional.of(mock(dataType)));
        when(r.getAllIds(any(InstanceIdentifier.class), any(ReadContext.class))).thenReturn(keys);
        return (ListReader<D, K, B>) r;
    }

}