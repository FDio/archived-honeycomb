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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractListReaderTest {

    protected static final InstanceIdentifier<TestingData>
            DATA_OBJECT_ID = InstanceIdentifier.create(TestingData.class);

    private final Class<? extends ListReaderCustomizer> customizerClass;

    @Mock
    protected Builder<TestingData> builder;
    @Mock
    protected TestingData data;
    @Mock
    protected ReadContext ctx;
    private List<TestingData.TestingKey> keys = Lists.newArrayList(new TestingData.TestingKey(),
            new TestingData.TestingKey());

    private ListReaderCustomizer<TestingData, TestingData.TestingKey, Builder<TestingData>> customizer;
    private GenericListReader<TestingData, TestingData.TestingKey, Builder<TestingData>> reader;

    protected AbstractListReaderTest(final Class<? extends ListReaderCustomizer> customizerClass) {
        this.customizerClass = customizerClass;
    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        customizer = mock(customizerClass);
        when(customizer.getBuilder(any(InstanceIdentifier.class))).thenReturn(builder);
        when(customizer.getAllIds(DATA_OBJECT_ID, ctx)).thenReturn(keys);
        reader = initReader();
        when(builder.build()).thenReturn(data);
    }

    protected abstract GenericListReader<TestingData,TestingData.TestingKey,Builder<TestingData>> initReader();

    public GenericListReader<TestingData, TestingData.TestingKey, Builder<TestingData>> getReader() {
        return reader;
    }

    protected ListReaderCustomizer<TestingData, TestingData.TestingKey, Builder<TestingData>> getCustomizer() {
        return customizer;
    }

    @Test
    public void testGetBuilder() throws Exception {
        assertEquals(builder, reader.getBuilder(DATA_OBJECT_ID));
        verify(customizer).getBuilder(DATA_OBJECT_ID);
    }

    @Test
    public void testManagedType() throws Exception {
        assertEquals(DATA_OBJECT_ID, reader.getManagedDataObjectType());
    }

    @Test
    public void testMerge() throws Exception {
        reader.merge(builder, data);
        verify(customizer).merge(builder, data);
    }

    @Test
    public void testAllIds() throws Exception {
        assertEquals(keys, reader.getAllIds(DATA_OBJECT_ID, ctx));
        verify(customizer).getAllIds(DATA_OBJECT_ID, ctx);
    }

    @Test
    public void testRead() throws Exception {
        reader.read(DATA_OBJECT_ID, ctx);

        verify(customizer).getBuilder(DATA_OBJECT_ID);
        verify(customizer).readCurrentAttributes(DATA_OBJECT_ID, builder, ctx);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReadList() throws Exception {
        reader.readList(DATA_OBJECT_ID, ctx);

        verify(customizer, times(2)).getBuilder(any(InstanceIdentifier.class));
        verify(customizer, times(2))
                .readCurrentAttributes(any(InstanceIdentifier.class), any(Builder.class), any(ReadContext.class));
    }

    static class TestingData implements DataObject, Identifiable<TestingData.TestingKey> {

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return DataObject.class;
        }

        @Override
        public TestingKey key() {
            return new TestingKey();
        }

        static class TestingKey implements Identifier<TestingData> {}
    }
}