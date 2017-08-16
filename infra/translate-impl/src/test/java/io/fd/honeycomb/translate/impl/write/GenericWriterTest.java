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

package io.fd.honeycomb.translate.impl.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GenericWriterTest {

    private static final InstanceIdentifier<DataObject>
            DATA_OBJECT_ID = InstanceIdentifier.create(DataObject.class);
    @Mock
    private WriterCustomizer<DataObject> customizer;
    @Mock
    private WriteContext ctx;
    private GenericWriter<DataObject> writer;
    @Mock
    private DataObject before;
    @Mock
    private DataObject after;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        writer = new GenericWriter<>(DATA_OBJECT_ID, customizer);
    }

    @Test
    public void testUpdate() throws Exception {
        assertEquals(DATA_OBJECT_ID, writer.getManagedDataObjectType());
        writer.processModification(DATA_OBJECT_ID, before, after, ctx);
        verify(customizer).updateCurrentAttributes(DATA_OBJECT_ID, before, after, ctx);

        writer.processModification(DATA_OBJECT_ID, before, null, ctx);
        verify(customizer).deleteCurrentAttributes(DATA_OBJECT_ID, before, ctx);

        writer.processModification(DATA_OBJECT_ID, null, after, ctx);
        verify(customizer).writeCurrentAttributes(DATA_OBJECT_ID, after, ctx);
    }

    @Test(expected = WriteFailedException.CreateFailedException.class)
    public void testWriteFail() throws Exception {
        doThrow(new IllegalStateException("test")).when(customizer).writeCurrentAttributes(DATA_OBJECT_ID, after, ctx);
        writer = new GenericWriter<>(DATA_OBJECT_ID, customizer);
        writer.writeCurrentAttributes(DATA_OBJECT_ID, after, ctx);
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdateFail() throws Exception {
        doThrow(new IllegalStateException("test")).when(customizer)
                .updateCurrentAttributes(DATA_OBJECT_ID, before, after, ctx);
        writer = new GenericWriter<>(DATA_OBJECT_ID, customizer);
        writer.updateCurrentAttributes(DATA_OBJECT_ID, before, after, ctx);
    }

    @Test(expected = WriteFailedException.DeleteFailedException.class)
    public void testDeleteFail() throws Exception {
        doThrow(new IllegalStateException("test")).when(customizer)
                .deleteCurrentAttributes(DATA_OBJECT_ID, before, ctx);
        writer = new GenericWriter<>(DATA_OBJECT_ID, customizer);
        writer.deleteCurrentAttributes(DATA_OBJECT_ID, before, ctx);
    }

    @Test
    public void testUpdateSupported() {
        assertFalse(GenericWriter.isUpdateSupported(new NoopWriters.NonDirectUpdateWriterCustomizer()));
        assertTrue(GenericWriter.isUpdateSupported(new NoopWriters.DirectUpdateWriterCustomizer()));
        assertTrue(GenericWriter.isUpdateSupported(new NoopWriters.ParentImplDirectUpdateWriterCustomizer()));
    }
}