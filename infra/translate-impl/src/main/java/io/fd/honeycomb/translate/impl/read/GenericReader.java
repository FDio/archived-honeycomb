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

package io.fd.honeycomb.translate.impl.read;

import com.google.common.annotations.Beta;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.read.Reader;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.read.AbstractGenericReader;
import io.fd.honeycomb.translate.util.read.ReflexiveReaderCustomizer;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Composite implementation of {@link Reader}.
 */
@Beta
@ThreadSafe
public class GenericReader<C extends DataObject, B extends Builder<C>>
        extends AbstractGenericReader<C, B>
        implements Reader<C, B> {

    protected final ReaderCustomizer<C, B> customizer;

    /**
     * Create a new {@link GenericReader}.
     *
     * @param id         Instance identifier for managed data type
     * @param customizer Customizer instance to customize this generic reader
     */
    public GenericReader(@Nonnull final InstanceIdentifier<C> id,
                         @Nonnull final ReaderCustomizer<C, B> customizer) {
        super(id);
        this.customizer = customizer;
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<C> id,
                                      @Nonnull final B builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        try {
            customizer.readCurrentAttributes(id, builder, ctx);
        } catch (RuntimeException e) {
            throw new ReadFailedException(id, e);
        }
    }

    @Nonnull
    @Override
    public B getBuilder(@Nonnull final InstanceIdentifier<C> id) {
        return customizer.getBuilder(id);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final C readValue) {
        customizer.merge(parentBuilder, readValue);
    }

    @Override
    public boolean isPresent(final InstanceIdentifier<C> id, final C built, final ReadContext ctx) {
        return customizer.isPresent(id, built, ctx);
    }

    public static <C extends DataObject, B extends Builder<C>> Reader<C, B> createReflexive(
            final InstanceIdentifier<C> id, Class<B> builderClass) {
        return new GenericReader<>(id, new ReflexiveReaderCustomizer<>(id.getTargetType(), builderClass));
    }
}
