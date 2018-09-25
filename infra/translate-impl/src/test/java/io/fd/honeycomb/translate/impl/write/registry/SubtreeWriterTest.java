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

package io.fd.honeycomb.translate.impl.write.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.impl.write.NoopWriters.DirectUpdateWriterCustomizer;
import io.fd.honeycomb.translate.impl.write.NoopWriters.NonDirectUpdateWriterCustomizer;
import io.fd.honeycomb.translate.impl.write.NoopWriters.ParentImplDirectUpdateWriterCustomizer;
import io.fd.honeycomb.translate.util.DataObjects;
import io.fd.honeycomb.translate.write.Writer;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Set;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubtreeWriterTest {

    @Mock
    Writer<DataObjects.DataObject4> writer;
    @Mock
    Writer<DataObjects.DataObject4.DataObject41> writer11;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(writer.getManagedDataObjectType()).thenReturn(DataObjects.DataObject4.IID);
        when(writer11.getManagedDataObjectType()).thenReturn(DataObjects.DataObject4.DataObject41.IID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubtreeWriterCreationFail() throws Exception {
        // The subtree node identified by IID.c(DataObject.class) is not a child of writer.getManagedDataObjectType
        SubtreeWriter.createForWriter(Collections.singleton(InstanceIdentifier.create(DataObject.class)), writer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubtreeWriterCreationFailInvalidIid() throws Exception {
        // The subtree node identified by IID.c(DataObject.class) is not a child of writer.getManagedDataObjectType
        SubtreeWriter.createForWriter(Collections.singleton(DataObjects.DataObject4.IID), writer);
    }

    @Test
    public void testCanHandleChild() throws Exception {
        final SubtreeWriter<?> forWriter = createSubtreeWriter();

        InstanceIdentifier<DataObjects.DataObject4.DataObject41.DataObject411> testIid = InstanceIdentifier.create(
                DataObjects.DataObject4.class).child(DataObjects.DataObject4.DataObject41.class).child(
                DataObjects.DataObject4.DataObject41.DataObject411.class);
        assertTrue(forWriter.canProcess(testIid));
    }

    @Test
    public void testSubtreeWriterCreation() throws Exception {
        final SubtreeWriter<?> forWriter = createSubtreeWriter();

        assertEquals(writer.getManagedDataObjectType(), forWriter.getManagedDataObjectType());
        assertEquals(3, forWriter.getHandledChildTypes().size());
    }

    private SubtreeWriter<?> createSubtreeWriter() {
        return (SubtreeWriter<?>) SubtreeWriter.createForWriter(Sets.newHashSet(
                DataObjects.DataObject4.DataObject41.IID,
                DataObjects.DataObject4.DataObject41.DataObject411.IID,
                DataObjects.DataObject4.DataObject42.IID),
                writer);
    }

    @Test
    public void testSubtreeWriterHandledTypes() throws Exception {
        final SubtreeWriter<?> forWriter = (SubtreeWriter<?>) SubtreeWriter.createForWriter(Sets.newHashSet(
                DataObjects.DataObject4.DataObject41.DataObject411.IID),
                writer);

        assertEquals(writer.getManagedDataObjectType(), forWriter.getManagedDataObjectType());
        assertEquals(1, forWriter.getHandledChildTypes().size());
        assertThat(forWriter.getHandledChildTypes(),
                CoreMatchers.hasItem(DataObjects.DataObject4.DataObject41.DataObject411.IID));
    }

    @Test
    public void testUpdateSupported() {
        // test supportsDirectUpdate(), because subtree writer overrides this method and delegate call on delegate writer
        final InstanceIdentifier<DataObject> fakeIID = InstanceIdentifier.create(DataObject.class);
        final Set<InstanceIdentifier<?>> handledChildren = Collections.emptySet();

        final NonDirectUpdateWriterCustomizer nonDirectCustomizer = new NonDirectUpdateWriterCustomizer();
        final DirectUpdateWriterCustomizer directCustomizer = new DirectUpdateWriterCustomizer();
        final ParentImplDirectUpdateWriterCustomizer parentImplCustomizer =
                new ParentImplDirectUpdateWriterCustomizer();

        final GenericWriter<DataObject> nonDirectWriter = new GenericWriter<>(fakeIID, nonDirectCustomizer);
        final GenericWriter<DataObject> directWriter = new GenericWriter<>(fakeIID, directCustomizer);
        final GenericWriter<DataObject> parentImplWriter = new GenericWriter<>(fakeIID, parentImplCustomizer);

        assertFalse(SubtreeWriter.createForWriter(handledChildren, nonDirectWriter).supportsDirectUpdate());
        assertTrue(SubtreeWriter.createForWriter(handledChildren, directWriter).supportsDirectUpdate());
        assertTrue(SubtreeWriter.createForWriter(handledChildren, parentImplWriter).supportsDirectUpdate());
    }

}