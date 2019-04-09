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

package io.fd.honeycomb.benchmark.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.ContainerInListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.container.in.list.NestedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.container.in.list.NestedListBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface DataProvider {

    String SIMPLE_CONTAINER = "simple-container";
    String LIST_IN_CONTAINER = "list-in-container";
    String COMPLEX_LIST_IN_CONTAINER = "complex-list-in-container";

    InstanceIdentifier<?> getId(final long counter);

    DataObject getData(final long counter);

    static DataProvider from(String data) {
        return new MapBackedDataProvider(data);
    }

    final class MapBackedDataProvider implements DataProvider {

        private static final Map<String, DataProvider> map;

        static {
            map = new HashMap<>();

            final InstanceIdentifier<SimpleContainer> simpleContainerId = InstanceIdentifier.create(SimpleContainer.class);
            map.put(SIMPLE_CONTAINER, new MultiValueDataProvider(Lists.newArrayList(
                    // Multiple values of container to ensure each time the value in DS after commit changes to trigger
                    // writers in test
                    new SingleValueDataProvider<>(
                            new SimpleContainerBuilder().setSimpleContainerName("first").build(), simpleContainerId),
                    new SingleValueDataProvider<>(
                            new SimpleContainerBuilder().setSimpleContainerName("second").build(), simpleContainerId),
                    new SingleValueDataProvider<>(
                            new SimpleContainerBuilder().setSimpleContainerName("third").build(), simpleContainerId))
            ));
            map.put(LIST_IN_CONTAINER, new MultiValueDataProvider(getListInContainerValues(100_000)));
            map.put(COMPLEX_LIST_IN_CONTAINER,
                    new MultiValueDataProvider(getComplexListInContainerValues(100_000)));
        }

        private final DataProvider delegate;

        MapBackedDataProvider(final String data) {
            checkArgument(map.containsKey(data));
            this.delegate = map.get(data);
        }

        @Override
        public InstanceIdentifier<?> getId(final long counter) {
            return delegate.getId(counter);
        }

        @Override
        public DataObject getData(final long counter) {
            return delegate.getData(counter);
        }

        @Override
        public String toString() {
            return "MapBackedDataProvider{" +
                    "delegate=" + delegate +
                    '}';
        }
    }


    static List<DataProvider> getListInContainerValues(final int i) {
        return IntStream.range(0, i)
                .mapToObj(idx -> new SingleValueDataProvider<>(
                        new ListInContainerBuilder()
                                .setId((long) idx)
                                .build(),
                        InstanceIdentifier.create(ContainerWithList.class)
                                .child(ListInContainer.class, new ListInContainerKey((long) idx))
                ))
                .collect(Collectors.toList());
    }

    static List<DataProvider> getComplexListInContainerValues(final int i) {
        return IntStream.range(0, i)
                .mapToObj(idx -> new SingleValueDataProvider<>(
                        new ListInContainerBuilder()
                                .setId((long) idx)
                                .setContainerInList(new ContainerInListBuilder()
                                        .setName("nested container")
                                        .setNestedList(Lists.newArrayList(
                                                getNestedList("1"),
                                                getNestedList("2"),
                                                getNestedList("3")))
                                        .build())
                                .build(),
                        InstanceIdentifier.create(ContainerWithList.class)
                                .child(ListInContainer.class, new ListInContainerKey((long) idx))
                ))
                .collect(Collectors.toList());
    }

    static NestedList getNestedList(final String value) {
        return new NestedListBuilder()
                .setNestedId(value)
                .setNestedName(value + "N")
                .build();
    }

    final class SingleValueDataProvider<T extends DataObject> implements DataProvider {

        private final DataObject data;
        private final InstanceIdentifier<?> id;

        SingleValueDataProvider(final T data, final InstanceIdentifier<T> id) {
            this.data = data;
            this.id = id;
        }

        @Override
        public InstanceIdentifier<?> getId(final long counter) {
            return id;
        }

        @Override
        public DataObject getData(final long counter) {
            return data;
        }

        @Override
        public String toString() {
            return "SingleValueDataProvider{" +
                    "data=" + data +
                    ", id=" + id +
                    '}';
        }
    }

    final class MultiValueDataProvider<T extends DataObject> implements DataProvider {

        private final List<DataProvider> values;
        private int valueSize;

        MultiValueDataProvider(final List<DataProvider> values) {
            // Wrap as array list so that index lookup is fast
            this.values = Lists.newArrayList(values);
            this.valueSize = values.size();
        }

        @Override
        public InstanceIdentifier<?> getId(final long counter) {
            return values.get((int) (counter % valueSize)).getId(counter);
        }

        @Override
        public DataObject getData(final long counter) {
            return values.get((int) (counter % valueSize)).getData(counter);
        }

        @Override
        public String toString() {
            return "MultiValueDataProvider{" +
                    "valueSize=" + valueSize +
                    '}';
        }
    }
}
