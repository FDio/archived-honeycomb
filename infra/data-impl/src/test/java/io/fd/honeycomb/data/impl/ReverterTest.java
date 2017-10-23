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

package io.fd.honeycomb.data.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.TranslationException;
import io.fd.honeycomb.translate.write.DataObjectUpdate;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.registry.UpdateFailedException;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import java.util.Collections;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ReverterTest {

    private static final InstanceIdentifier<DataObject> IID_0 = InstanceIdentifier.create(DataObject.class);
    private static final InstanceIdentifier<DataObject1> IID_1 = InstanceIdentifier.create(DataObject1.class);
    private static final InstanceIdentifier<DataObject2> IID_2 = InstanceIdentifier.create(DataObject2.class);

    @Mock
    private WriterRegistry registry;

    @Mock
    private WriteContext writeContext;

    @Captor
    private ArgumentCaptor<WriterRegistry.DataObjectUpdates> updateCaptor;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void revertSingle() throws Exception {
        final DataObjectUpdate create = DataObjectUpdate.create(IID_0, null, mock(DataObject.class));

        new Reverter(ImmutableList.of(create), registry).revert(writeContext);
        assertSingleRevert(create);
    }

    @Test
    public void revertSingleFailed() throws TranslationException {
        final DataObjectUpdate create = DataObjectUpdate.create(IID_0, null, mock(DataObject.class));
        final UpdateFailedException ex =
                new UpdateFailedException(new IllegalStateException(), Collections.emptyList(), create);
        doThrow(ex).when(registry)
                .processModifications(any(WriterRegistry.DataObjectUpdates.class), any(WriteContext.class));

        try {
            new Reverter(ImmutableList.of(create), registry).revert(writeContext);
        } catch (Reverter.RevertFailedException e) {
            assertEquals(ex, e.getCause());
            assertSingleRevert(create);
            return;
        }
        fail("Reverter.RevertFailedException was expected");
    }


    @Test
    public void revertSingleFailedWithUnexpectedEx() throws TranslationException {
        final DataObjectUpdate create = DataObjectUpdate.create(IID_0, null, mock(DataObject.class));
        final IllegalStateException ex = new IllegalStateException();
        doThrow(ex).when(registry)
                .processModifications(any(WriterRegistry.DataObjectUpdates.class), any(WriteContext.class));

        try {
            new Reverter(ImmutableList.of(create), registry).revert(writeContext);
        } catch (Reverter.RevertFailedException e) {
            assertEquals(ex, e.getCause());
            assertSingleRevert(create);
            return;
        }
        fail("IllegalStateException was expected");
    }


    @Test
    public void revertMultiple() throws Exception {
        final DataObjectUpdate create = DataObjectUpdate.create(IID_0, null, mock(DataObject.class));
        final DataObjectUpdate update =
                DataObjectUpdate.create(IID_1, mock(DataObject1.class), mock(DataObject1.class));
        final DataObjectUpdate delete = DataObjectUpdate.create(IID_2, mock(DataObject2.class), null);

        new Reverter(ImmutableList.of(create, update, delete), registry).revert(writeContext);
        assertMultiRevert(create, update, delete);
    }


    @Test
    public void revertMultipleFailed() throws Exception {
        final DataObjectUpdate create = DataObjectUpdate.create(IID_0, null, mock(DataObject.class));
        final DataObjectUpdate update =
                DataObjectUpdate.create(IID_1, mock(DataObject1.class), mock(DataObject1.class));
        final DataObjectUpdate delete = DataObjectUpdate.create(IID_2, mock(DataObject2.class), null);

        final UpdateFailedException ex =
                new UpdateFailedException(new IllegalStateException(), ImmutableList.of(create, update), create);
        doThrow(ex).when(registry)
                .processModifications(any(WriterRegistry.DataObjectUpdates.class), any(WriteContext.class));

        try {
            new Reverter(ImmutableList.of(create, update, delete), registry).revert(writeContext);
        } catch (Reverter.RevertFailedException e) {
            assertEquals(ex, e.getCause());
            assertMultiRevert(create, update, delete);
            return;
        }
        fail("Reverter.RevertFailedException was expected");
    }

    @Test
    public void revertMultipleFailedWithUnnexpectedException() throws Exception {
        final DataObjectUpdate create = DataObjectUpdate.create(IID_0, null, mock(DataObject.class));
        final DataObjectUpdate update =
                DataObjectUpdate.create(IID_1, mock(DataObject1.class), mock(DataObject1.class));
        final DataObjectUpdate delete = DataObjectUpdate.create(IID_2, mock(DataObject2.class), null);

        final IllegalStateException ex = new IllegalStateException();
        doThrow(ex).when(registry)
                .processModifications(any(WriterRegistry.DataObjectUpdates.class), any(WriteContext.class));

        try {
            new Reverter(ImmutableList.of(create, update, delete), registry).revert(writeContext);
        } catch (Reverter.RevertFailedException e) {
            assertEquals(ex, e.getCause());
            assertMultiRevert(create, update, delete);
            return;
        }
        fail("IllegalStateException was expected");
    }


    private void assertSingleRevert(final DataObjectUpdate create) throws TranslationException {
        verify(registry, times(1)).processModifications(updateCaptor.capture(), eq(writeContext));
        final WriterRegistry.DataObjectUpdates updates = updateCaptor.getValue();
        assertTrue(updates.getDeletes().containsValue(create.reverse()));
        assertTrue(updates.getUpdates().isEmpty());
    }

    private void assertMultiRevert(final DataObjectUpdate create, final DataObjectUpdate update,
                                   final DataObjectUpdate delete) throws TranslationException {
        verify(registry, times(1)).processModifications(updateCaptor.capture(), eq(writeContext));
        final WriterRegistry.DataObjectUpdates updates = updateCaptor.getValue();
        final Iterator<DataObjectUpdate.DataObjectDelete> deletesIterator = updates.getDeletes().values().iterator();
        final Iterator<DataObjectUpdate> updatesIterator = updates.getUpdates().values().iterator();

        assertEquals(updatesIterator.next(), delete.reverse());
        assertEquals(updatesIterator.next(), update.reverse());
        assertEquals(deletesIterator.next(), create.reverse());
    }


    private interface DataObject1 extends DataObject {
    }

    private interface DataObject2 extends DataObject {
    }
}