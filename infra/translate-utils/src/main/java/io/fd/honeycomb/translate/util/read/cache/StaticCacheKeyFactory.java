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

package io.fd.honeycomb.translate.util.read.cache;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * {@link CacheKeyFactory} that produces the same key irrespective of the given {@link InstanceIdentifier}.
 */
public class StaticCacheKeyFactory implements CacheKeyFactory<Void> {

    private final String key;
    private final Class<?> cachedDataType;

    /**
     * Construct static cache key factory.
     *
     * @param key            string to be used as cache key
     * @param cachedDataType data for which this factory is creating keys
     */
    public StaticCacheKeyFactory(@Nonnull final String key, @Nonnull final Class<?> cachedDataType) {
        this.key = checkNotNull(key, "key should not be null");
        this.cachedDataType = checkNotNull(cachedDataType, "cachedDataType should not be null");
    }

    @Nonnull
    @Override
    public String createKey(@Nonnull final InstanceIdentifier<?> actualContextIdentifier,
                            @Nullable final Void dumpParams) {
        return key;
    }

    @Nonnull
    @Override
    public Class<?> getCachedDataType() {
        return cachedDataType;
    }
}
