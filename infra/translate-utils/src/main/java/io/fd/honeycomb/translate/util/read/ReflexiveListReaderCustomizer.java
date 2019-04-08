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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.ReflectionUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Might be slow.
 */
public class ReflexiveListReaderCustomizer<C extends DataObject & Identifiable<K>, K extends Identifier<C>, B extends Builder<C>>
        extends ReflexiveReaderCustomizer<C, B>
        implements ListReaderCustomizer<C, K, B> {

    private final List<K> staticKeys;
    private final Class<? extends Identifier> keyType;

    public ReflexiveListReaderCustomizer(@Nonnull final Class<C> typeClass, @Nonnull final Class<B> builderClass,
                                         @Nonnull final List<K> staticKeys) {
        super(typeClass, builderClass);
        this.staticKeys = checkNotNull(staticKeys, "Static keys cannot be null");
        checkState(!this.staticKeys.isEmpty(), "No static keys provided");
        keyType = staticKeys.get(0).getClass();
    }

    @Override
    public void readCurrentAttributes(final InstanceIdentifier<C> id, final B builder, final ReadContext context)
        throws ReadFailedException {
        final Optional<Method> method =
            ReflectionUtils.findMethodReflex(builder.getClass(), "withKey",
                Collections.singletonList(keyType), builder.getClass());
        checkArgument(method.isPresent(), "Unable to build withKey for %s", builder);

        try {
            method.get().invoke(builder, ((KeyedInstanceIdentifier)id).getKey());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to build withKey for " + builder, e);
        }
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final C readValue) {
        merge(parentBuilder, Collections.singletonList(readValue));
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final List<C> readData) {
        final Optional<Method> method =
                ReflectionUtils.findMethodReflex(parentBuilder.getClass(), "set" + getTypeClass().getSimpleName(),
                        Collections.singletonList(List.class), parentBuilder.getClass());

        checkArgument(method.isPresent(), "Unable to set %s to %s", readData, parentBuilder);

        try {
            method.get().invoke(parentBuilder, readData);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to set " + readData + " to " + parentBuilder, e);
        }
    }

    @Nonnull
    @Override
    public List<K> getAllIds(@Nonnull InstanceIdentifier<C> id, @Nonnull ReadContext context) throws ReadFailedException {
        return staticKeys;
    }
}
