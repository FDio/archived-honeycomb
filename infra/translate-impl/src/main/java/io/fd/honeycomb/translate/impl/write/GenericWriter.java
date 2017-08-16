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

package io.fd.honeycomb.translate.impl.write;

import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.util.write.AbstractGenericWriter;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic writer with customizable behavior thanks to injected customizer.
 */
public final class GenericWriter<D extends DataObject> extends AbstractGenericWriter<D> {

    private static final Logger LOG = LoggerFactory.getLogger(GenericWriter.class);
    private static final String UPDATE_M = "updateCurrentAttributes";
    private final WriterCustomizer<D> customizer;

    public GenericWriter(@Nonnull final InstanceIdentifier<D> type,
                         @Nonnull final WriterCustomizer<D> customizer) {
        super(type, isUpdateSupported(customizer));
        this.customizer = customizer;

    }

    static boolean isUpdateSupported(final @Nonnull WriterCustomizer<?> customizer) {
        try {
            // if customizer overrides updateCurrentAttributes method, it will be used, otherwise updates will be broken into individual
            // delete + create pairs
            final Class<? extends WriterCustomizer> customizerClass = customizer.getClass();
            final Class<?> updateDeclaringClass = customizerClass
                    .getMethod(UPDATE_M, InstanceIdentifier.class, DataObject.class, DataObject.class, WriteContext.class)
                    .getDeclaringClass();
            final boolean supportsUpdate = !WriterCustomizer.class.equals(updateDeclaringClass);
            LOG.debug("Customizer {} update support : {}|Update declaring class {}", customizerClass, supportsUpdate,
                    updateDeclaringClass);
            return supportsUpdate;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to detect if customizer supports update", e);
        }
    }

    @Override
    protected void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D data,
                                          @Nonnull final WriteContext ctx) throws WriteFailedException {
        try {
            customizer.writeCurrentAttributes(id, data, ctx);
        } catch (RuntimeException e) {
            throw new WriteFailedException.CreateFailedException(id, data, e);
        }
    }

    @Override
    protected void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull final WriteContext ctx) throws WriteFailedException {
        try {
            customizer.deleteCurrentAttributes(id, dataBefore, ctx);
        } catch (RuntimeException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    @Override
    protected void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                           @Nonnull final D dataBefore,
                                           @Nonnull final D dataAfter,
                                           @Nonnull final WriteContext ctx) throws WriteFailedException {
        try {
            customizer.updateCurrentAttributes(id, dataBefore, dataAfter, ctx);
        } catch (RuntimeException e) {
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }
}
