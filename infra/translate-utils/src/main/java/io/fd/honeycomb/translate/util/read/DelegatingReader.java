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

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.read.ListReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.read.Reader;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * A trait of a delegating reader. Delegates all the calls to its delegate.
 */
public interface DelegatingReader<D extends DataObject, B extends Builder<D>> extends Reader<D, B> {

    Reader<D, B> getDelegate();

    @Override
    @Nonnull
    default Optional<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                                @Nonnull final ReadContext ctx) throws ReadFailedException {
        return getDelegate().read(id, ctx);
    }

    @Override
    default void readCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                       @Nonnull final B builder,
                                       @Nonnull final ReadContext ctx) throws ReadFailedException {
        getDelegate().readCurrentAttributes(id, builder, ctx);
    }

    @Override
    @Nonnull
    default B getBuilder(final InstanceIdentifier<D> id) {
        return getDelegate().getBuilder(id);
    }

    @Override
    default void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                       @Nonnull final D readValue) {
        getDelegate().merge(parentBuilder, readValue);
    }

    @Override
    default boolean isPresent(InstanceIdentifier<D> id, D built, final ReadContext ctx) {
        return getDelegate().isPresent(id, built, ctx);
    }

    @Override
    @Nonnull
    default InstanceIdentifier<D> getManagedDataObjectType() {
        return getDelegate().getManagedDataObjectType();
    }

    /**
     * ListReader specific delegating trait.
     */
    interface DelegatingListReader<D extends DataObject & Identifiable<K>, K extends Identifier<D>, B extends Builder<D>>
            extends DelegatingReader<D, B>, ListReader<D, K, B> {

        @Override
        ListReader<D, K, B> getDelegate();

        @Override
        default List<K> getAllIds(@Nonnull InstanceIdentifier<D> id, @Nonnull ReadContext ctx)
                throws ReadFailedException {
            return getDelegate().getAllIds(id, ctx);
        }

        @Nonnull
        @Override
        default List<D> readList(@Nonnull final InstanceIdentifier<D> id, @Nonnull final ReadContext ctx)
                throws ReadFailedException {
            return getDelegate().readList(id, ctx);
        }

        @Override
        default void merge(@Nonnull final Builder<? extends DataObject> builder,
                           @Nonnull final List<D> readData) {
            getDelegate().merge(builder, readData);
        }

        @Override
        default void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final D readValue) {
            getDelegate().merge(parentBuilder, readValue);
        }
    }
}
