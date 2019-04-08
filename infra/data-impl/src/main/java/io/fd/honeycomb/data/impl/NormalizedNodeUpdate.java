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

package io.fd.honeycomb.data.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Update to a normalized node identifiable by its {@link YangInstanceIdentifier}.
 */
final class NormalizedNodeUpdate {

    @Nonnull
    private final YangInstanceIdentifier id;
    @Nullable
    private final NormalizedNode<?, ?> dataBefore;
    @Nullable
    private final NormalizedNode<?, ?> dataAfter;

    NormalizedNodeUpdate(@Nonnull final YangInstanceIdentifier id,
                         @Nullable final NormalizedNode<?, ?> dataBefore,
                         @Nullable final NormalizedNode<?, ?> dataAfter) {
        this.id = checkNotNull(id);
        this.dataAfter = dataAfter;
        this.dataBefore = dataBefore;
    }

    @Nullable
    public NormalizedNode<?, ?> getDataBefore() {
        return dataBefore;
    }

    @Nullable
    public NormalizedNode<?, ?> getDataAfter() {
        return dataAfter;
    }

    @Nonnull
    public YangInstanceIdentifier getId() {
        return id;
    }

    static NormalizedNodeUpdate create(@Nonnull final Modification modification) {
        final Optional<NormalizedNode<?, ?>> beforeData = modification.getDataBefore();
        final Optional<NormalizedNode<?, ?>> afterData = modification.getDataAfter();
        checkArgument(beforeData.isPresent() || afterData.isPresent(),
                "Both before and after data are null for %s", modification.getId());
        return NormalizedNodeUpdate.create(modification.getId(), beforeData.orElse(null), afterData.orElse(null));
    }

    static NormalizedNodeUpdate create(@Nonnull final YangInstanceIdentifier id,
                                       @Nullable final NormalizedNode<?, ?> dataBefore,
                                       @Nullable final NormalizedNode<?, ?> dataAfter) {
        return new NormalizedNodeUpdate(id, dataBefore, dataAfter);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        final NormalizedNodeUpdate that = (NormalizedNodeUpdate) other;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "NormalizedNodeUpdate{" + "id=" + id
                + ", dataBefore=" + dataBefore
                + ", dataAfter=" + dataAfter
                + '}';
    }
}
