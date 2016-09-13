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

package io.fd.honeycomb.translate.util.write;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AbstractGenericWriterTest {

    private InstanceIdentifier<DataObject> id = InstanceIdentifier.create(DataObject.class);
    @Mock
    private DataObject before;
    @Mock
    private DataObject after;
    @Mock
    private WriteContext ctx;
    private TestingWriter t;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        t = spy(new TestingWriter());
    }

    @Test
    public void testDelete() throws Exception {
        before = mock(DataObject.class);
        t.update(id, before, null, ctx);

        verify(t).deleteCurrentAttributes(id, before, ctx);
    }

    @Test
    public void testUpdate() throws Exception {
        before = mock(DataObject.class);
        t.update(id, before, after, ctx);

        verify(t).updateCurrentAttributes(id, before, after, ctx);
    }

    @Test
    public void testNoUpdate() throws Exception {
        before = mock(DataObject.class);
        t.update(id, before, before, ctx);

        verify(t, times(0)).updateCurrentAttributes(id, before, after, ctx);
    }

    @Test
    public void testCreate() throws Exception {
        before = mock(DataObject.class);
        t.update(id, null, after, ctx);

        verify(t).writeCurrentAttributes(id, after, ctx);
    }

    private static class TestingWriter extends AbstractGenericWriter<DataObject> {

        TestingWriter() {
            super(InstanceIdentifier.create(DataObject.class));
        }

        @Override
        protected void writeCurrentAttributes(@Nonnull final InstanceIdentifier<DataObject> id,
                                              @Nonnull final DataObject data, @Nonnull final WriteContext ctx)
                throws WriteFailedException {

        }

        @Override
        protected void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<DataObject> id,
                                               @Nonnull final DataObject dataBefore, @Nonnull final WriteContext ctx)
                throws WriteFailedException {

        }

        @Override
        protected void updateCurrentAttributes(@Nonnull final InstanceIdentifier<DataObject> id,
                                               @Nonnull final DataObject dataBefore,
                                               @Nonnull final DataObject dataAfter,
                                               @Nonnull final WriteContext ctx) throws WriteFailedException {

        }
    }

}