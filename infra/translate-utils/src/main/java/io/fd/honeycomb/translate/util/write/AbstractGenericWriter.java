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

package io.fd.honeycomb.translate.util.write;

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.translate.write.Writer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGenericWriter<D extends DataObject> implements Writer<D> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractGenericWriter.class);

    private final InstanceIdentifier<D> instanceIdentifier;
    private final boolean supportsUpdate;
    private Validator<D> validator;

    protected AbstractGenericWriter(final InstanceIdentifier<D> type, final boolean supportsUpdate) {
        this.instanceIdentifier = RWUtils.makeIidWildcarded(type);
        this.supportsUpdate = supportsUpdate;
    }

    protected AbstractGenericWriter(final InstanceIdentifier<D> type, final boolean supportsUpdate,
                                    final Validator<D> validator) {
        this(type, supportsUpdate);
        this.validator = validator;
    }

    @Override
    public void processModification(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                    @Nullable final DataObject dataBefore,
                                    @Nullable final DataObject dataAfter,
                                    @Nonnull final WriteContext ctx) throws WriteFailedException {
        LOG.debug("{}: Updating : {}", this, id);
        LOG.trace("{}: Updating : {}, from: {} to: {}", this, id, dataBefore, dataAfter);

        checkArgument(idPointsToCurrent(id), "Cannot handle data: %s. Only: %s can be handled by writer: %s",
                id, getManagedDataObjectType(), this);

        if (isWrite(dataBefore, dataAfter)) {
            LOG.debug("{}: Writing {} data: {}", this, id, dataAfter);
            final D after = castToManaged(dataAfter);
            writeCurrentAttributes(getSpecificId(id, after), after, ctx);
        } else if (isDelete(dataBefore, dataAfter)) {
            LOG.debug("{}: Deleting {} data: {}", this, id, dataBefore);
            final D before = castToManaged(dataBefore);
            deleteCurrentAttributes(getSpecificId(id, before), before, ctx);
        } else {
            LOG.debug("{}: Updating {} dataBefore: {}, datAfter: {}", this, id, dataBefore, dataAfter);
            checkArgument(dataBefore != null && dataAfter != null, "No data to process");
            if (dataBefore.equals(dataAfter)) {
                LOG.debug("{}: Skipping modification (no update): {}", this, id);
                // No change, ignore
                return;
            }
            final D before = castToManaged(dataBefore);
            updateCurrentAttributes(getSpecificId(id, before), before, castToManaged(dataAfter), ctx);
        }
    }

    @Override
    public void validate(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                         @Nullable final DataObject dataBefore, @Nullable final DataObject dataAfter,
                         @Nonnull final WriteContext ctx) throws DataValidationFailedException {
        if (validator == null) {
            LOG.trace("{}: validator is not defined. Skipping validation for {}", this, id);
            return;
        }
        LOG.trace("{}: Validating : {}, before: {} after: {}", this, id, dataBefore, dataAfter);

        checkArgument(idPointsToCurrent(id), "Cannot handle data: %s. Only: %s can be handled by writer: %s",
            id, getManagedDataObjectType(), this);

        if (isWrite(dataBefore, dataAfter)) {
            final D after = castToManaged(dataAfter);
            validator.validateWrite(getSpecificId(id, after), after, ctx);
        } else if (isDelete(dataBefore, dataAfter)) {
            final D before = castToManaged(dataBefore);
            validator.validateDelete(getSpecificId(id, before), before, ctx);
        } else {
            checkArgument(dataBefore != null && dataAfter != null, "No data to process");
            if (dataBefore.equals(dataAfter)) {
                LOG.debug("{}: Skipping validation (no update) for: {}", this, id);
                // No change, ignore
                return;
            }
            final D before = castToManaged(dataBefore);
            validator.validateUpdate(getSpecificId(id, before), before, castToManaged(dataAfter), ctx);
        }
    }

    @SuppressWarnings("unchecked")
    protected InstanceIdentifier<D> getSpecificId(@Nonnull final InstanceIdentifier<? extends DataObject> currentId,
                                                  @Nonnull final D current) {
        return (InstanceIdentifier<D>) currentId;
    }

    private void checkDataType(@Nonnull final DataObject dataAfter) {
        checkArgument(getManagedDataObjectType().getTargetType().isAssignableFrom(dataAfter.getClass()));
    }

    private D castToManaged(final DataObject data) {
        checkDataType(data);
        return getManagedDataObjectType().getTargetType().cast(data);
    }

    private static boolean isWrite(final DataObject dataBefore,
                                    final DataObject dataAfter) {
        return dataBefore == null && dataAfter != null;
    }

    private static boolean isDelete(final DataObject dataBefore,
                                    final DataObject dataAfter) {
        return dataAfter == null && dataBefore != null;
    }

    private boolean idPointsToCurrent(final @Nonnull InstanceIdentifier<? extends DataObject> id) {
        return id.getTargetType().equals(getManagedDataObjectType().getTargetType());
    }

    protected abstract void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                                   @Nonnull final D data,
                                                   @Nonnull final WriteContext ctx) throws WriteFailedException;

    protected abstract void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                                    @Nonnull final D dataBefore,
                                                    @Nonnull final WriteContext ctx) throws WriteFailedException;

    protected abstract void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                                    @Nonnull final D dataBefore,
                                                    @Nonnull final D dataAfter,
                                                    @Nonnull final WriteContext ctx) throws WriteFailedException;

    @Nonnull
    @Override
    public InstanceIdentifier<D> getManagedDataObjectType() {
        return instanceIdentifier;
    }


    @Override
    public String toString() {
        return String.format("Writer[%s]", getManagedDataObjectType().getTargetType().getSimpleName());
    }

    @Override
    public boolean supportsDirectUpdate() {
        return supportsUpdate;
    }
}
