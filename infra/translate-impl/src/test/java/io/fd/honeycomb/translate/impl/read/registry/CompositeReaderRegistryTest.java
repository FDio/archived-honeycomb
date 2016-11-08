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

import static io.fd.honeycomb.translate.util.DataObjects.DataObject3;
import static io.fd.honeycomb.translate.util.DataObjects.DataObject3.DataObject31;
import static io.fd.honeycomb.translate.util.DataObjects.DataObject4;
import static io.fd.honeycomb.translate.util.DataObjects.DataObject4.DataObject41;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.Reader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class CompositeReaderRegistryTest {

    @Mock
    private ReadContext ctx;
    private CompositeReaderRegistry reg;
    private Reader<DataObject31, Builder<DataObject31>> reader31;
    private Reader<DataObject3, Builder<DataObject3>> rootReader3;
    private Reader<DataObject41, Builder<DataObject41>> reader41;
    private Reader<DataObject4, Builder<DataObject4>> rootReader4;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        reader31 = mockReader(DataObject31.class);
        final Reader<DataObject3, Builder<DataObject3>> mockedReader3 = mockReader(DataObject3.class);
        rootReader3 =
                Mockito.spy(CompositeReader.createForReader(
                        mockedReader3,
                        ImmutableMap.of(DataObject31.class, reader31)));
        // This is a workaround. This functionality is already present in CompositeReader, however when wrapping as spy
        // null is always returned. That's why we need to explicitly stub the method with its actual implementation.
        // The problem is that the method is inherited as default method from an interface and mockito's spy seems to
        // have a problem there
        doReturn(mockedReader3.getBuilder(InstanceIdentifier.create(DataObject3.class)))
                .when(rootReader3).getBuilder(any(InstanceIdentifier.class));

        reader41 = mockReader(DataObject41.class);
        final Reader<DataObject4, Builder<DataObject4>> mockedReader4 = mockReader(DataObject4.class);
        rootReader4 =
                Mockito.spy(CompositeReader.createForReader(
                        mockedReader4, ImmutableMap.of(
                                DataObject41.class, reader41)));
        // Workaround
        doReturn(mockedReader4.getBuilder(InstanceIdentifier.create(DataObject4.class)))
                .when(rootReader4).getBuilder(any(InstanceIdentifier.class));

        reg = new CompositeReaderRegistry(Lists.newArrayList(rootReader3, rootReader4));
    }

    @Test
    public void testReadAll() throws Exception {
        reg.readAll(ctx);

        // Invoked according to composite ordering
        final InOrder inOrder = inOrder(rootReader3, rootReader4, reader31, reader41);
        inOrder.verify(rootReader3).read(any(InstanceIdentifier.class), any(ReadContext.class));
        inOrder.verify(reader31).read(any(InstanceIdentifier.class), any(ReadContext.class));
        inOrder.verify(rootReader4).read(any(InstanceIdentifier.class), any(ReadContext.class));
        inOrder.verify(reader41).read(any(InstanceIdentifier.class), any(ReadContext.class));
    }

    @Test
    public void testReadSingleRoot() throws Exception {
        reg.read(DataObject3.IID, ctx);

        // Invoked according to composite ordering
        final InOrder inOrder = inOrder(rootReader3, rootReader4, reader31, reader41);
        inOrder.verify(rootReader3).read(any(InstanceIdentifier.class), any(ReadContext.class));
        inOrder.verify(reader31).read(any(InstanceIdentifier.class), any(ReadContext.class));

        // Only subtree under DataObject3 should be read
        verify(rootReader4, times(0)).read(any(InstanceIdentifier.class), any(ReadContext.class));
        verify(reader41, times(0)).read(any(InstanceIdentifier.class), any(ReadContext.class));
    }

    @SuppressWarnings("unchecked")
    static <D extends DataObject, B extends Builder<D>> Reader<D, B> mockReader(final Class<D> dataType)
            throws Exception {
        final Reader r = mock(Reader.class);
        final Object iid = dataType.getDeclaredField("IID").get(null);
        when(r.getManagedDataObjectType()).thenReturn((InstanceIdentifier) iid);
        final Builder builder = mock(Builder.class);
        when(builder.build()).thenReturn(mock(dataType));
        when(r.getBuilder(any(InstanceIdentifier.class))).thenReturn(builder);
        when(r.read(any(InstanceIdentifier.class), any(ReadContext.class))).thenReturn(Optional.of(mock(dataType)));
        return (Reader<D, B>) r;
    }
}