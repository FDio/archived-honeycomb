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

package io.fd.honeycomb.translate.write.registry;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Sets;
import io.fd.honeycomb.translate.write.WriteContext;
import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BulkUpdateExceptionTest {

    private InstanceIdentifier<?> id = InstanceIdentifier.create(DataObject.class);

    @Mock
    private WriteContext writeContext;

    @Mock
    private WriterRegistry.Reverter reverter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRevert() throws Exception {
        final HashSet<InstanceIdentifier<?>> failedIds = Sets.newHashSet(id);
        final WriterRegistry.BulkUpdateException bulkUpdateException =
                new WriterRegistry.BulkUpdateException(failedIds, reverter, new RuntimeException());

        assertEquals(failedIds, bulkUpdateException.getFailedIds());

        bulkUpdateException.revertChanges(writeContext);
        verify(reverter).revert(writeContext);
    }
}