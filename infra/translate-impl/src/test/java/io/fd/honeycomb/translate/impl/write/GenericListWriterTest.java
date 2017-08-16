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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GenericListWriterTest {

    private static final InstanceIdentifier<IdentifiableDataObject>
            DATA_OBJECT_ID = InstanceIdentifier.create(IdentifiableDataObject.class);
    @Mock
    private ListWriterCustomizer<IdentifiableDataObject, DataObjectIdentifier> customizer;
    @Mock
    private WriteContext ctx;
    private GenericListWriter<IdentifiableDataObject, DataObjectIdentifier> writer;
    @Mock
    private IdentifiableDataObject before;
    @Mock
    private DataObjectIdentifier beforeKey;
    @Mock
    private IdentifiableDataObject after;
    @Mock
    private DataObjectIdentifier keyAfter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        writer = new GenericListWriter<>(DATA_OBJECT_ID, customizer);
        when(before.getKey()).thenReturn(beforeKey);
        when(after.getKey()).thenReturn(keyAfter);
    }

    @Test
    public void testUpdate() throws Exception {
        assertEquals(DATA_OBJECT_ID, writer.getManagedDataObjectType());

        final InstanceIdentifier<IdentifiableDataObject> keyedIdBefore =
                (InstanceIdentifier<IdentifiableDataObject>) InstanceIdentifier.create(Collections
                        .singleton(new InstanceIdentifier.IdentifiableItem<>(IdentifiableDataObject.class, beforeKey)));
        final InstanceIdentifier<IdentifiableDataObject> keyedIdAfter =
                (InstanceIdentifier<IdentifiableDataObject>) InstanceIdentifier.create(Collections
                        .singleton(new InstanceIdentifier.IdentifiableItem<>(IdentifiableDataObject.class, keyAfter)));

        writer.processModification(DATA_OBJECT_ID, before, after, ctx);
        verify(customizer).updateCurrentAttributes(keyedIdBefore, before, after, ctx);

        writer.processModification(DATA_OBJECT_ID, before, null, ctx);
        verify(customizer).deleteCurrentAttributes(keyedIdBefore, before, ctx);

        writer.processModification(DATA_OBJECT_ID, null, after, ctx);
        verify(customizer).writeCurrentAttributes(keyedIdAfter, after, ctx);
    }

    private abstract static class IdentifiableDataObject implements DataObject, Identifiable<DataObjectIdentifier> {}
    private abstract static class DataObjectIdentifier implements Identifier<IdentifiableDataObject> {}

    @Test(expected = WriteFailedException.CreateFailedException.class)
    public void testWriteFail() throws Exception {
        doThrow(new IllegalStateException("test")).when(customizer).writeCurrentAttributes(DATA_OBJECT_ID, after, ctx);
        writer = new GenericListWriter<>(DATA_OBJECT_ID, customizer);
        writer.writeCurrentAttributes(DATA_OBJECT_ID, after, ctx);
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdateFail() throws Exception {
        doThrow(new IllegalStateException("test")).when(customizer)
                .updateCurrentAttributes(DATA_OBJECT_ID, before, after, ctx);
        writer = new GenericListWriter<>(DATA_OBJECT_ID, customizer);
        writer.updateCurrentAttributes(DATA_OBJECT_ID, before, after, ctx);
    }

    @Test(expected = WriteFailedException.DeleteFailedException.class)
    public void testDeleteFail() throws Exception {
        doThrow(new IllegalStateException("test")).when(customizer)
                .deleteCurrentAttributes(DATA_OBJECT_ID, before, ctx);
        writer = new GenericListWriter<>(DATA_OBJECT_ID, customizer);
        writer.deleteCurrentAttributes(DATA_OBJECT_ID, before, ctx);
    }
}