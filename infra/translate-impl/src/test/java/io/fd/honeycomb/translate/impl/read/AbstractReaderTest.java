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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class AbstractReaderTest {

    protected static final InstanceIdentifier<DataObject>
            DATA_OBJECT_ID = InstanceIdentifier.create(DataObject.class);
    private final Class<? extends ReaderCustomizer> customizerClass;

    @Mock
    protected Builder<DataObject> builder;
    @Mock
    protected DataObject data;
    @Mock
    protected ReadContext ctx;

    private ReaderCustomizer<DataObject, Builder<DataObject>> customizer;
    private GenericReader<DataObject, Builder<DataObject>> reader;

    protected AbstractReaderTest(final Class<? extends ReaderCustomizer> customizerClass) {
        this.customizerClass = customizerClass;
    }

    @SuppressWarnings("unchecked")
    @Before
    public final void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        customizer = mock(customizerClass);
        when(customizer.getBuilder(DATA_OBJECT_ID)).thenReturn(builder);
        reader = initReader();
        when(builder.build()).thenReturn(data);
    }

    protected abstract GenericReader<DataObject,Builder<DataObject>> initReader();

    protected GenericReader<DataObject, Builder<DataObject>> getReader() {
        return reader;
    }

    protected ReaderCustomizer<DataObject, Builder<DataObject>> getCustomizer() {
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
    public void testRead() throws Exception {
        reader.read(DATA_OBJECT_ID, ctx);

        verify(customizer).getBuilder(DATA_OBJECT_ID);
        verify(customizer).readCurrentAttributes(DATA_OBJECT_ID, builder, ctx);
    }
}