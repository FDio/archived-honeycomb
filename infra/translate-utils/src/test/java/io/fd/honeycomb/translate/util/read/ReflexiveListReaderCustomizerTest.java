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
package io.fd.honeycomb.translate.util.read;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ReflexiveListReaderCustomizerTest {

    @Mock
    private ReadContext readContext;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testReadCurrentAttributes() throws ReadFailedException {
        final TestingListObject.TestingListKey keyOne = new TestingListObject.TestingListKey("1");
        final TestingListObject.TestingListKey keyTwo = new TestingListObject.TestingListKey("2");
        final List<TestingListObject.TestingListKey> staticKeys = Arrays.asList(keyOne, keyTwo);

        final ReflexiveListReaderCustomizer<TestingListObject, TestingListObject.TestingListKey, TestingListObjectBuilder> customizer
            = new ReflexiveListReaderCustomizer<>(TestingListObject.class, TestingListObjectBuilder.class, staticKeys);

        final TestingListObjectBuilder builder = new TestingListObjectBuilder();
        final InstanceIdentifier<TestingListObject> id =
            (InstanceIdentifier<TestingListObject>) InstanceIdentifier.create(
                    Collections.singletonList(InstanceIdentifier.IdentifiableItem.of(TestingListObject.class, keyOne)));
        customizer.readCurrentAttributes(id, builder, readContext);

        assertEquals(keyOne, builder.getKey());
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {
        final TestingListObject.TestingListKey keyOne = new TestingListObject.TestingListKey("1");
        final TestingListObject.TestingListKey keyTwo = new TestingListObject.TestingListKey("2");
        final List<TestingListObject.TestingListKey> staticKeys = Arrays.asList(keyOne, keyTwo);

        final ReflexiveListReaderCustomizer<TestingListObject, TestingListObject.TestingListKey, TestingListObjectBuilder> customizer
            = new ReflexiveListReaderCustomizer<>(TestingListObject.class, TestingListObjectBuilder.class, staticKeys);

        final List<TestingListObject.TestingListKey> allIds =
            customizer.getAllIds(InstanceIdentifier.create(TestingListObject.class), readContext);

        assertThat(allIds, hasSize(2));
        assertThat(allIds, contains(keyOne, keyTwo));
    }

    static class TestingListObject implements DataObject, Identifiable<TestingListObject.TestingListKey> {

        private final TestingListKey key;

        TestingListObject(final TestingListKey key) {
            this.key = key;
        }

        @Override
        public Class<? extends DataContainer> getImplementedInterface() {
            return DataObject.class;
        }

        @Override
        public TestingListKey key() {
            return key;
        }

        static class TestingListKey implements Identifier<TestingListObject> {

            private final String value;

            TestingListKey(final String value) {
                this.value = value;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                TestingListKey that = (TestingListKey) o;

                return value.equals(that.value);
            }

            @Override
            public int hashCode() {
                return value.hashCode();
            }
        }
    }

    static class TestingListObjectBuilder implements Builder<TestingListObject> {

        private TestingListObject.TestingListKey key;

        @Override
        public TestingListObject build() {
            return new TestingListObject(key);
        }

        public TestingListObjectBuilder withKey(final TestingListObject.TestingListKey key) {
            this.key = key;
            return this;
        }

        public TestingListObject.TestingListKey getKey() {
            return key;
        }
    }
}
