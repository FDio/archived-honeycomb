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

package io.fd.honeycomb.translate.util.read;

import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.read.ReadContext;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.*;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ReflexiveReaderCustomizerTest {

    @Mock
    private ReadContext ctx;
    private InstanceIdentifier<TestingDataObject> id = InstanceIdentifier.create(TestingDataObject.class);

    @Test
    public void testReflexCustomizer() throws Exception {
        final ReflexiveReaderCustomizer<TestingDataObject, TestingBuilder> reflexReaderCustomizer =
                new ReflexiveReaderCustomizer<>(TestingDataObject.class, TestingBuilder.class);

        assertEquals(TestingDataObject.class, reflexReaderCustomizer.getTypeClass());
        assertEquals(TestingBuilder.class, reflexReaderCustomizer.getBuilderClass());

        assertNotNull(reflexReaderCustomizer.getBuilder(id));

        // NOOP
        final TestingBuilder builder = spy(new TestingBuilder());
        reflexReaderCustomizer.readCurrentAttributes(id, builder, ctx);
        verifyZeroInteractions(builder);

        final TestingBuilderParent parentBuilder = new TestingBuilderParent();
        final TestingDataObject readValue = new TestingDataObject();
        reflexReaderCustomizer.merge(parentBuilder, readValue);
        assertEquals(readValue, parentBuilder.getTestingDataObject());
    }

    @Test
    public void testReflexCustomizerAugment() throws Exception {
        final ReflexiveReaderCustomizer<TestingAugmentation, TestingAugmentBuilder> reflexReaderCustomizer =
                new ReflexiveReaderCustomizer<>(TestingAugmentation.class, TestingAugmentBuilder.class);

        final TestingBuilderParent parentBuilder = new TestingBuilderParent();
        final TestingAugmentation readValue = new TestingAugmentation();
        reflexReaderCustomizer.merge(parentBuilder, readValue);
        assertEquals(readValue, parentBuilder.getAugmentation());
    }

    @Test
    public void testReflexCustomizerList() throws Exception {
        final ReflexiveListReaderCustomizer<TestingListObject, TestingListObject.TestingListKey, TestingListBuilder>
                reflexReaderCustomizer =
                new ReflexiveListReaderCustomizer<TestingListObject, TestingListObject.TestingListKey, TestingListBuilder>
                        (TestingListObject.class, TestingListBuilder.class, Collections.singletonList(new TestingListObject.TestingListKey()));

        final TestingBuilderParent parentBuilder = new TestingBuilderParent();
        final List<TestingListObject> readValue = Lists.newArrayList(new TestingListObject(), new TestingListObject());
        reflexReaderCustomizer.merge(parentBuilder, readValue);
        assertEquals(readValue, parentBuilder.getTestingListObject());

        final TestingListObject single = new TestingListObject();
        reflexReaderCustomizer.merge(parentBuilder, single);
        assertEquals(Collections.singletonList(single), parentBuilder.getTestingListObject());
    }

    static class TestingDataObject implements DataObject {

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return DataObject.class;
        }
    }

    static class TestingAugmentation implements DataObject, Augmentation<DataObject> {

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return DataObject.class;
        }
    }

    static class TestingListObject implements DataObject, Identifiable<TestingListObject.TestingListKey> {

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return DataObject.class;
        }

        @Override
        public TestingListKey getKey() {
            return new TestingListKey();
        }

        static class TestingListKey implements Identifier<TestingListObject> {
        }
    }


    static class TestingBuilder implements Builder<TestingDataObject> {

        @Override
        public TestingDataObject build() {
            return new TestingDataObject();
        }
    }

    static class TestingAugmentBuilder implements Builder<TestingAugmentation> {

        @Override
        public TestingAugmentation build() {
            return new TestingAugmentation();
        }
    }

    static class TestingListBuilder implements Builder<TestingListObject> {

        @Override
        public TestingListObject build() {
            return new TestingListObject();
        }
    }

    static class TestingBuilderParent implements Builder<DataObject> {

        private TestingDataObject object;
        private Augmentation<DataObject> augmentation;
        private List<TestingListObject> listObjects;

        public TestingBuilderParent setTestingDataObject(@Nonnull final TestingDataObject object) {
            this.object = object;
            return this;
        }

        public TestingDataObject getTestingDataObject() {
            return object;
        }

        public TestingBuilderParent setTestingListObject(@Nonnull final List<TestingListObject> listObjects) {
            this.listObjects = listObjects;
            return this;
        }

        public List<TestingListObject> getTestingListObject() {
            return listObjects;
        }

        public TestingBuilderParent addAugmentation(Class<? extends Augmentation<DataObject>> augmentationType,
                                                    Augmentation<DataObject> augmentation) {
            this.augmentation = augmentation;
            return this;
        }

        public Augmentation<DataObject> getAugmentation() {
            return augmentation;
        }

        @Override
        public TestingDataObject build() {
            return new TestingDataObject();
        }
    }
}