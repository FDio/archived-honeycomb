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

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.Reader;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Statically preconfigured reader.
 */
@NotThreadSafe
public final class StaticReader<T extends DataObject, B extends Builder<T>> implements Reader<T, B> {

    private final InstanceIdentifier<T> id;
    private final DataProvider data;
    private long counter = 0;

    public StaticReader(final InstanceIdentifier<T> id, final DataProvider data) {
        this.id = id;
        this.data = data;
    }

    @Nonnull
    @Override
    public InstanceIdentifier<T> getManagedDataObjectType() {
        return id;
    }

    @Override
    public String toString() {
        return "NoopReader{" +
                id.getTargetType().getSimpleName() +
                ", counter=" + counter +
                '}';
    }

    @Override
    public boolean isPresent(@Nonnull final InstanceIdentifier<T> id, @Nonnull final T built,
                             @Nonnull final ReadContext ctx) {
        return true;
    }

    @Nonnull
    @Override
    public Optional<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                               @Nonnull final ReadContext ctx) {
        counter++;
        return Optional.of(data.getData(counter));
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<T> id, @Nonnull final B builder,
                                      @Nonnull final ReadContext ctx) {
        throw new UnsupportedOperationException("No read current attrs!");
    }

    @Nonnull
    @Override
    public B getBuilder(final InstanceIdentifier<T> id) {
        throw new UnsupportedOperationException("No builder!");
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final T readValue) {
        throw new UnsupportedOperationException("No merge!");
    }
}
