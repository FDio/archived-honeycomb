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

/**
 * Generic writer with customizable behavior thanks to injected customizer.
 */
public final class GenericWriter<D extends DataObject> extends AbstractGenericWriter<D> {

    private final WriterCustomizer<D> customizer;

    public GenericWriter(@Nonnull final InstanceIdentifier<D> type,
                         @Nonnull final WriterCustomizer<D> customizer) {
        super(type);
        this.customizer = customizer;
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
