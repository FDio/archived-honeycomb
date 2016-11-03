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

package io.fd.honeycomb.translate.util.read.cache;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.noop.NoopDumpPostProcessingFunction;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager responsible for returning Data object dumps<br> either from cache or by invoking specified {@link
 * EntityDumpExecutor}
 *
 * @param <T> Type of data returned by {@code EntityDumpExecutor},and stored cache
 * @param <U> Type of dumping params
 */
public final class DumpCacheManager<T, U> {

    private static final Logger LOG = LoggerFactory.getLogger(DumpCacheManager.class);

    private final EntityDumpExecutor<T, U> dumpExecutor;
    private final EntityDumpPostProcessingFunction<T> postProcessor;
    private final CacheKeyFactory cacheKeyFactory;

    private DumpCacheManager(DumpCacheManagerBuilder<T, U> builder) {
        this.dumpExecutor = builder.dumpExecutor;
        this.postProcessor = builder.postProcessingFunction;
        this.cacheKeyFactory = builder.cacheKeyFactory;
    }

    /**
     * Returns {@link Optional<T>} of dump
     *
     * @param identifier identifier for origin of dumping context
     * @param cache      modification cache of current transaction
     * @param dumpParams parameters to configure dump request
     * @throws ReadFailedException if execution of dumping request failed
     * @returns If present in cache ,returns cached instance, if not, tries to dump data using provided executor,
     * otherwise Optional.absent()
     */
    public Optional<T> getDump(@Nonnull final InstanceIdentifier<?> identifier,
                               @Nonnull final ModificationCache cache, final U dumpParams)
            throws ReadFailedException {

        final String entityKey = this.cacheKeyFactory.createKey(identifier);
        // this key binding to every log has its logic ,because every customizer have its own cache manager and if
        // there is need for debugging/fixing some complex call with a lot of data,you can get lost in those logs
        LOG.debug("Loading dump for KEY[{}]", entityKey);

        T dump = (T) cache.get(entityKey);

        if (dump == null) {
            LOG.debug("Dump for KEY[{}] not present in cache,invoking dump executor", entityKey);
            // binds and execute dump to be thread-save
            dump = postProcessor.apply(dumpExecutor.executeDump(identifier, dumpParams));
            // no need to check dump, if no data were dumped , DTO with empty list is returned
            // no need to check if post processor is active,if it wasn't set,default no-op will be used
            LOG.debug("Caching dump for KEY[{}]", entityKey);
            cache.put(entityKey, dump);
            return Optional.of(dump);
        } else {
            LOG.debug("Cached instance of dump was found for KEY[{}]", entityKey);
            return Optional.of(dump);
        }
    }

    public static final class DumpCacheManagerBuilder<T, U> {

        private static final CacheKeyFactory DEFAULT_CACHE_KEY_FACTORY_INSTANCE = new IdentifierCacheKeyFactory();

        private EntityDumpExecutor<T, U> dumpExecutor;
        private EntityDumpPostProcessingFunction<T> postProcessingFunction;
        private CacheKeyFactory cacheKeyFactory;

        public DumpCacheManagerBuilder() {
            // for cases when user does not set specific post-processor
            postProcessingFunction = new NoopDumpPostProcessingFunction<T>();

            //use no additional scopes version by default
            cacheKeyFactory = DEFAULT_CACHE_KEY_FACTORY_INSTANCE;
        }

        public DumpCacheManagerBuilder<T, U> withExecutor(@Nonnull final EntityDumpExecutor<T, U> executor) {
            this.dumpExecutor = executor;
            return this;
        }

        public DumpCacheManagerBuilder<T, U> withPostProcessingFunction(
                @Nonnull final EntityDumpPostProcessingFunction<T> postProcessingFunction) {
            this.postProcessingFunction = postProcessingFunction;
            return this;
        }

        public DumpCacheManagerBuilder<T, U> withCacheKeyFactory(@Nonnull final CacheKeyFactory cacheKeyFactory) {
            this.cacheKeyFactory = cacheKeyFactory;
            return this;
        }

        public DumpCacheManager<T, U> build() {
            checkNotNull(dumpExecutor, "Dump executor cannot be null");
            checkNotNull(postProcessingFunction,
                    "Dump post-processor cannot be null cannot be null, default implementation is used when not set explicitly");
            checkNotNull(cacheKeyFactory,
                    "Cache key factory cannot be null, default non-extended implementation is used when not set explicitly");

            return new DumpCacheManager<>(this);
        }
    }
}


