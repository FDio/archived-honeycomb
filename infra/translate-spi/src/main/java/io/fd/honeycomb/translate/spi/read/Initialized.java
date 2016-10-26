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

package io.fd.honeycomb.translate.spi.read;

import com.google.common.base.Preconditions;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Just a DTO holding configuration data ID and the data itself.
 */
public final class Initialized<T extends DataObject> {

    private final InstanceIdentifier<T> id;
    private final T data;

    private Initialized(final InstanceIdentifier<T> id, final T data) {
        this.id = id;
        this.data = data;
    }

    public InstanceIdentifier<T> getId() {
        return id;
    }

    public T getData() {
        return data;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Initialized<?> that = (Initialized<?>) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, data);
    }

    @Override
    public String toString() {
        return "Initialized{" +
                "id=" + id +
                ", data=" + data +
                '}';
    }

    public static <C extends DataObject> Initialized<C> create(@Nonnull final InstanceIdentifier<C> id,
                                                               @Nonnull final C data) {
        return new Initialized<>(Preconditions.checkNotNull(id, "Id cannot be null"),
                Preconditions.checkNotNull(data, "Data cannot be null"));
    }
}
