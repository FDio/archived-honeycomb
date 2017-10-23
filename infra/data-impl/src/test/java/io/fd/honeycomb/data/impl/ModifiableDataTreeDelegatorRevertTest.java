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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.data.DataModification;
import io.fd.honeycomb.translate.TranslationException;
import io.fd.honeycomb.translate.write.DataObjectUpdate;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.registry.UpdateFailedException;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;

public class ModifiableDataTreeDelegatorRevertTest extends ModifiableDataTreeDelegatorBaseTest {

    /**
     * Test scenario when commit fails, but there's nothing to revert because very first crud operation failed
     */
    @Test
    public void testCommitFailedNoRevert() throws Exception {
        final MapNode nestedList = getNestedList("listEntry", "listValue");

        // Fail on update:
        final TranslationException failedOnUpdateException = new TranslationException("update failed");
        doThrow(new UpdateFailedException(failedOnUpdateException, Collections.emptyList(), update))// fail on update
                .when(writer)
                .processModifications(any(WriterRegistry.DataObjectUpdates.class), any(WriteContext.class));

        try {
            // Run the test
            final DataModification dataModification = configDataTree.newModification();
            dataModification.write(NESTED_LIST_ID, nestedList);
            dataModification.validate();
            dataModification.commit();
            fail("UpdateFailedException was expected");
        } catch (UpdateFailedException e) {
            // writer was called only one for update, and it was only one operation so no revert needed
            // exception was just rethrown
            verify(writer).processModifications(any(WriterRegistry.DataObjectUpdates.class), any(WriteContext.class));
            assertEquals(e.getFailed().getId(), DEFAULT_ID);
            assertTrue(e.getProcessed().isEmpty());
        }
    }

    /**
     * Test whether
     *  - Correct operations were invoked(when creating and reverting)
     *  - Create and revert both failed
     *  - Correct exception has been thrown
     * Steps:
     * - Prepares state with nested list written
     * - Attempts to rewrite that list with new list with value with different key
     * - Simulates fail(both on modification and revert)
     * - Checks modifications
     * Asserts
     * - index 0 - Represents create of original data
     * - index 1 - Represents override with new list(fails)(include delete of data created by index 0 and create of new)
     * - index 2 - Represents revert of removal of original data
     */
    @Test
    public void testCommitWithRevertFailed() throws Exception {
        // configure initial state
        final MapNode originalList = getNestedList("listEntryOriginal", "listValueOriginal");

        final DataModification preModification = configDataTree.newModification();
        preModification.write(NESTED_LIST_ID, originalList);
        preModification.validate();
        preModification.commit();

        // then test
        final MapNode nestedList = getNestedList("listEntry", "listValueNew");

        // Fail on update:
        final TranslationException failedOnUpdateException = new TranslationException("update failed");
        final DataObjectUpdate.DataObjectDelete mockRevertData = mock(DataObjectUpdate.DataObjectDelete.class);
        final DataObjectUpdate.DataObjectDelete mockRevertDataReverted = mock(DataObjectUpdate.DataObjectDelete.class);
        when(mockRevertData.getId()).thenReturn((InstanceIdentifier) InstanceIdentifier.create(DataObject.class));
        when(mockRevertDataReverted.getId())
                .thenReturn((InstanceIdentifier) InstanceIdentifier.create(DataObject.class));
        when(mockRevertData.getDataBefore()).thenReturn(DEFAULT_DATA_OBJECT);// to simulate that delete of original data
        //should be reverted
        when(mockRevertDataReverted.getDataAfter())
                .thenReturn(DEFAULT_DATA_OBJECT);// to simulate that delete of original data
        //should be reverted
        when(mockRevertData.reverse()).thenReturn(mockRevertDataReverted);

        final UpdateFailedException cause =
                new UpdateFailedException(failedOnUpdateException,
                        Collections.singletonList(mockRevertData),//fail on new one
                        update);
        doThrow(cause)
                .when(writer)
                .processModifications(any(WriterRegistry.DataObjectUpdates.class), any(WriteContext.class));

        try {
            // Run the test
            final DataModification dataModification = configDataTree.newModification();
            dataModification.write(NESTED_LIST_ID, nestedList);
            dataModification.validate();
            dataModification.commit();
            fail("WriterRegistry.Reverter.RevertFailedException was expected");
        } catch (Reverter.RevertFailedException e) {
            assertRewriteModificationWithRevert(writer, updatesCaptor, DEFAULT_DATA_OBJECT);
            assertEquals(cause, e.getCause());
        }
    }

    /**
     * Test whether
     *  - Correct operations were invoked(when creating and reverting)
     *  - Create failed and revert passed
     *  - Correct exception has been thrown
     * Steps:
     * - Prepares state with nested list written
     * - Attempts to rewrite that list with new list with value with different key
     * - Simulates fail on create
     * - Passes on revert
     * - Checks modifications
     * Asserts
     * - index 0 - Represents create of original data
     * - index 1 - Represents override with new list(fails)(include delete of data created by index 0 and create of new)
     * - index 2 - Represents revert of removal of original data
     */
    @Test
    public void testCommitWithRevertSuccessfull() throws Exception {
        // configure initial state
        final MapNode originalList = getNestedList("listEntryOriginal", "listValueOriginal");

        final DataModification preModification = configDataTree.newModification();
        preModification.write(NESTED_LIST_ID, originalList);
        preModification.validate();
        preModification.commit();

        // then test
        final MapNode nestedList = getNestedList("listEntry", "listValueNew");

        // Fail on update:
        final TranslationException failedOnUpdateException = new TranslationException("update failed");
        final DataObjectUpdate.DataObjectDelete mockRevertData = mock(DataObjectUpdate.DataObjectDelete.class);
        final DataObjectUpdate.DataObjectDelete mockRevertDataReverted = mock(DataObjectUpdate.DataObjectDelete.class);
        when(mockRevertData.getId()).thenReturn((InstanceIdentifier) InstanceIdentifier.create(DataObject.class));
        when(mockRevertDataReverted.getId())
                .thenReturn((InstanceIdentifier) InstanceIdentifier.create(DataObject.class));
        when(mockRevertData.getDataBefore()).thenReturn(DEFAULT_DATA_OBJECT);// to simulate that delete of original data
        //should be reverted
        when(mockRevertDataReverted.getDataAfter())
                .thenReturn(DEFAULT_DATA_OBJECT);// to simulate that delete of original data
        //should be reverted
        when(mockRevertData.reverse()).thenReturn(mockRevertDataReverted);

        final UpdateFailedException cause =
                new UpdateFailedException(failedOnUpdateException,
                        Collections.singletonList(mockRevertData),//fail on new one
                        update);
        doThrow(cause) // fails on create
                .doNothing()//to pass on revert
                .when(writer)
                .processModifications(any(WriterRegistry.DataObjectUpdates.class), any(WriteContext.class));

        try {
            // Run the test
            final DataModification dataModification = configDataTree.newModification();
            dataModification.write(NESTED_LIST_ID, nestedList);
            dataModification.validate();
            dataModification.commit();
            fail("WriterRegistry.Reverter.RevertFailedException was expected");
        } catch (Reverter.RevertSuccessException e) {
            assertRewriteModificationWithRevert(writer, updatesCaptor, DEFAULT_DATA_OBJECT);
            assertNull(e.getCause());
        }
    }

    private static void assertRewriteModificationWithRevert(final WriterRegistry writer,
                                                            final ArgumentCaptor<WriterRegistry.DataObjectUpdates> updatesCaptor,
                                                            final DataObject DEFAULT_DATA_OBJECT)
            throws TranslationException {
        verify(writer, times(3)).processModifications(updatesCaptor.capture(), any(WriteContext.class));
        final List<WriterRegistry.DataObjectUpdates> allUpdates = updatesCaptor.getAllValues();
        assertEquals(3, allUpdates.size());

        // represent create of original data
        final WriterRegistry.DataObjectUpdates originalCreate = allUpdates.get(0);
        assertContainsOnlySingleUpdate(originalCreate);

        final DataObjectUpdate createOriginalData = originalCreate.getUpdates().values().iterator().next();
        assertCreateWithData(createOriginalData, DEFAULT_DATA_OBJECT);

        // delete of original data was successful
        // create of new data - failed
        final WriterRegistry.DataObjectUpdates originalDelete = allUpdates.get(1);
        assertConstainsSingleUpdateAndDelete(originalDelete);

        final DataObjectUpdate.DataObjectDelete deleteOriginalData =
                originalDelete.getDeletes().values().iterator().next();
        assertDeleteWithData(deleteOriginalData, DEFAULT_DATA_OBJECT);

        final DataObjectUpdate newCreate = originalDelete.getUpdates().values().iterator().next();
        assertCreateWithData(newCreate, DEFAULT_DATA_OBJECT);

        final WriterRegistry.DataObjectUpdates revert = allUpdates.get(2);
        assertContainsOnlySingleUpdate(revert);
    }

    private static void assertDeleteWithData(final DataObjectUpdate.DataObjectDelete deleteOriginalData,
                                             final DataObject DEFAULT_DATA_OBJECT) {
        assertNull(deleteOriginalData.getDataAfter());
        assertEquals(DEFAULT_DATA_OBJECT, deleteOriginalData.getDataBefore());
    }

    private static void assertCreateWithData(final DataObjectUpdate newCreate, final DataObject DEFAULT_DATA_OBJECT) {
        assertNull(newCreate.getDataBefore());
        assertEquals(DEFAULT_DATA_OBJECT, newCreate.getDataAfter());
    }

    private static void assertContainsOnlySingleUpdate(final WriterRegistry.DataObjectUpdates originalCreate) {
        assertThat(originalCreate.getDeletes().size(), is(0));
        assertThat(originalCreate.getUpdates().size(), is(1));
    }

    private static void assertConstainsSingleUpdateAndDelete(final WriterRegistry.DataObjectUpdates originalDelete) {
        assertThat(originalDelete.getDeletes().size(), is(1));
        assertThat(originalDelete.getUpdates().size(), is(1));
    }

}
