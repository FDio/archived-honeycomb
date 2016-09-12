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

package io.fd.honeycomb.translate.write;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class WriteFailedExceptionTest {

    private InstanceIdentifier<?> id = InstanceIdentifier.create(DataObject.class);
    @Mock
    private DataObject dataAfter;
    @Mock
    private DataObject dataBefore;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateFailed() throws Exception {
        final WriteFailedException.CreateFailedException cause =
                new WriteFailedException.CreateFailedException(id, dataAfter);
        final WriteFailedException.CreateFailedException createFailedException =
                new WriteFailedException.CreateFailedException(id, dataAfter, cause);

        assertEquals(createFailedException.getFailedId(), id);
        assertEquals(createFailedException.getData(), dataAfter);
        assertEquals(createFailedException.getCause(), cause);
        assertThat(createFailedException.getMessage(), CoreMatchers.containsString("Failed to create"));
    }

    @Test
    public void testUpdateFailed() throws Exception {
        final WriteFailedException.UpdateFailedException cause =
                new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter);
        final WriteFailedException.UpdateFailedException createFailedException =
                new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, cause);

        assertEquals(createFailedException.getFailedId(), id);
        assertEquals(createFailedException.getDataBefore(), dataBefore);
        assertEquals(createFailedException.getDataAfter(), dataAfter);
        assertEquals(createFailedException.getCause(), cause);
        assertThat(createFailedException.getMessage(), CoreMatchers.containsString("Failed to update"));
    }

    @Test
    public void testDeleteFailed() throws Exception {
        final WriteFailedException.DeleteFailedException cause = new WriteFailedException.DeleteFailedException(id);
        final WriteFailedException.DeleteFailedException createFailedException =
                new WriteFailedException.DeleteFailedException(id, cause);

        assertEquals(createFailedException.getFailedId(), id);
        assertEquals(createFailedException.getCause(), cause);
        assertThat(createFailedException.getMessage(), CoreMatchers.containsString("Failed to delete"));
    }
}