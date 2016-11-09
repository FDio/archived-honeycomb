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

import com.google.common.collect.Sets;
import io.fd.honeycomb.translate.write.DataObjectUpdate;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RevertFailedExceptionTest {

    private InstanceIdentifier<?> id = InstanceIdentifier.create(DataObject.class);
    @Mock
    private WriterRegistry.Reverter reverter;
    @Mock
    private DataObject before;
    @Mock
    private DataObject after;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testNonRevert() throws Exception {
        final Set<InstanceIdentifier<?>> notReverted = Sets.newHashSet(id);
        final WriterRegistry.Reverter.RevertFailedException revertFailedException =
                new WriterRegistry.Reverter.RevertFailedException(
                        new WriterRegistry.BulkUpdateException(id, DataObjectUpdate.create(id, before, after),
                        notReverted, reverter, new RuntimeException()));
        assertEquals(notReverted, revertFailedException.getNotRevertedChanges());
    }
}