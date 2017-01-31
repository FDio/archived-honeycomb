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

package io.fd.honeycomb.benchmark.memory;

import javax.annotation.Nonnull;
import javax.management.openmbean.CompositeDataSupport;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Wrapped from data extracted from JMX beans of type Memory
 * */
public final class MemoryInfo {

    public static final String MEMORY_MBEAN_TYPE = "java.lang:type=Memory";
    public static final String HEAP_MEMORY = "HeapMemoryUsage";
    public static final String NON_HEAP_MEMORY = "NonHeapMemoryUsage";

    public static final String COMMITTED = "committed";
    public static final String INIT = "init";
    public static final String MAX = "max";
    public static final String USED = "used";

    private final String memoryInfoTypeName;
    private final long committed, init, max, used;

    public MemoryInfo(@Nonnull final CompositeDataSupport compositeData, @Nonnull final String memoryInfoTypeName) {
        checkArgument(compositeData.getCompositeType().keySet().containsAll(Arrays.asList(COMMITTED, INIT, MAX, USED)),
                "Submitted composite data %s does not contains necessary attributes", compositeData);
        this.memoryInfoTypeName = memoryInfoTypeName;
        this.committed = compositeDataAttributeValue(compositeData.get(COMMITTED));
        this.init = compositeDataAttributeValue(compositeData.get(INIT));
        this.max = compositeDataAttributeValue(compositeData.get(MAX));
        this.used = compositeDataAttributeValue(compositeData.get(USED));

    }

    public String getMemoryInfoTypeName() {
        return memoryInfoTypeName;
    }

    public long getCommitted() {
        return committed;
    }

    public long getInit() {
        return init;
    }

    public long getMax() {
        return max;
    }

    public long getUsed() {
        return used;
    }

    @Override
    public String toString() {
        return memoryInfoTypeName + "[" +
                "committed=" + committed +
                ",init=" + init +
                ",max=" + max +
                ",used=" + used +
                ']';
    }

    private static Long compositeDataAttributeValue(final Object commited) {
        checkArgument(commited instanceof Long, "Unsupported memory attribute value %s", commited.getClass());
        return Long.class.cast(commited);
    }
}
