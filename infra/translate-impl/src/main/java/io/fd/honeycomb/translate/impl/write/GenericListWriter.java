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

import static io.fd.honeycomb.translate.impl.write.GenericWriter.isUpdateSupported;

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.write.AbstractGenericWriter;
import io.fd.honeycomb.translate.write.ListWriter;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Generic list node writer with customizable behavior thanks to injected customizer.
 */
public final class GenericListWriter<D extends DataObject & Identifiable<K>, K extends Identifier<D>> extends
        AbstractGenericWriter<D> implements ListWriter<D, K> {

    private final WriterCustomizer<D> customizer;

    public GenericListWriter(@Nonnull final InstanceIdentifier<D> type,
                             @Nonnull final ListWriterCustomizer<D, K> customizer) {
        super(type, isUpdateSupported(customizer));
        this.customizer = customizer;
    }

    public GenericListWriter(@Nonnull final InstanceIdentifier<D> type,
                             @Nonnull final ListWriterCustomizer<D, K> customizer,
                             @Nonnull final Validator<D> validator) {
        super(type, isUpdateSupported(customizer), validator);
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
    protected void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull final D dataAfter, @Nonnull final WriteContext ctx)
        throws WriteFailedException {
        try {
            customizer.updateCurrentAttributes(id, dataBefore, dataAfter, ctx);
        } catch (RuntimeException e) {
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected InstanceIdentifier<D> getSpecificId(@Nonnull final InstanceIdentifier<? extends DataObject> currentId,
                                                  @Nonnull final D current) {
        final InstanceIdentifier<D> id = (InstanceIdentifier<D>) currentId;
        // Make sure the key is present
        if (isWildcarded(id)) {
            return RWUtils.replaceLastInId(id,
                new InstanceIdentifier.IdentifiableItem<>(id.getTargetType(), current.getKey()));
        } else {
            return id;
        }
    }

    private boolean isWildcarded(final InstanceIdentifier<D> id) {
        return id.firstIdentifierOf(getManagedDataObjectType().getTargetType()).isWildcarded();
    }
}
