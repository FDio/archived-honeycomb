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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Factory providing cache keys to easier switching between scopes of caching
 */
public final class TypeAwareIdentifierCacheKeyFactory<U> implements CacheKeyFactory<U> {

    private static final String KEY_PARTS_SEPARATOR = "|";
    @VisibleForTesting
    protected static final String NO_PARAMS_KEY = "NO_PARAMS";

    // should be Set<Class<? extends DataObject & Identifiable<?>>>, but that's not possible for wildcards
    private final Set<Class<? extends DataObject>> additionalKeyTypes;
    // factory must be aware of type of data, to prevent creating same key for same identifier but different data
    private final Class<?> type;

    /**
     * Construct simple cache key factory
     */
    public TypeAwareIdentifierCacheKeyFactory(@Nonnull final Class<?> type) {
        this(type, Collections.emptySet());
    }

    /**
     * @param additionalKeyTypes Additional types from path of cached type, that are specifying scope
     */
    public TypeAwareIdentifierCacheKeyFactory(@Nonnull final Class<?> type,
                                              @Nonnull final Set<Class<? extends DataObject>> additionalKeyTypes) {
        // verify that all are non-null and identifiable
        this.type = checkNotNull(type, "Type cannot be null");
        this.additionalKeyTypes = checkNotNull(additionalKeyTypes, "Additional key types can't be null").stream()
                .map(TypeAwareIdentifierCacheKeyFactory::verifyNotNull)
                .map(TypeAwareIdentifierCacheKeyFactory::verifyIsIdentifiable)
                .collect(Collectors.toSet());
    }

    private static String bindKeyString(IdentifiableItem identifiableItem) {
        return String.format("%s[%s]", identifiableItem.getType().getTypeName(), identifiableItem.getKey());
    }

    private static Class<? extends DataObject> verifyNotNull(final Class<? extends DataObject> type) {
        return checkNotNull(type, "Cannot use null as key");
    }

    /**
     * Initial check if provided scope variables are identifiable aka. can be used to create unique cache key
     */
    private static Class<? extends DataObject> verifyIsIdentifiable(final Class<? extends DataObject> type) {
        checkArgument(Identifiable.class.isAssignableFrom(type), "Type %s is not Identifiable", type);
        return type;
    }

    @Override
    public String createKey(@Nonnull final InstanceIdentifier<?> actualContextIdentifier, @Nullable final U dumpParams) {

        checkNotNull(actualContextIdentifier, "Cannot construct key for null InstanceIdentifier");

        // easiest case when only simple key is needed
        if (additionalKeyTypes.isEmpty()) {
            return String
                    .join(KEY_PARTS_SEPARATOR, type.getTypeName(), params(dumpParams),
                        actualContextIdentifier.getTargetType().toString());
        }

        checkArgument(isUniqueKeyConstructable(actualContextIdentifier),
                "Unable to construct unique key, required key types : %s, provided paths : %s", additionalKeyTypes,
                actualContextIdentifier.getPathArguments());

        // joins unique key in form : type | additional keys | actual context
        return String
                .join(KEY_PARTS_SEPARATOR, type.getTypeName(), additionalKeys(actualContextIdentifier),
                    params(dumpParams), actualContextIdentifier.getTargetType().toString());
    }

    private String params(final U dumpParams) {
        if (dumpParams == null) {
            return NO_PARAMS_KEY;
        } else {
            return String.valueOf(dumpParams.hashCode());
        }
    }

    @Override
    public Class<?> getCachedDataType() {
        return type;
    }

    /**
     * Verifies that all requested key parts have keys
     */
    private boolean isUniqueKeyConstructable(final InstanceIdentifier<?> actualContextIdentifier) {
        return StreamSupport.stream(actualContextIdentifier.getPathArguments().spliterator(), false)
                .filter(this::isAdditionalScope)
                .filter(this::isIdentifiable)
                .count() == additionalKeyTypes.size();
    }

    private boolean isAdditionalScope(final InstanceIdentifier.PathArgument pathArgument) {
        return additionalKeyTypes.contains(pathArgument.getType());
    }

    private boolean isIdentifiable(final InstanceIdentifier.PathArgument pathArgument) {
        return pathArgument instanceof IdentifiableItem;
    }

    private String additionalKeys(final InstanceIdentifier<?> actualContextIdentifier) {
        return StreamSupport.stream(actualContextIdentifier.getPathArguments().spliterator(), false)
                .filter(this::isAdditionalScope)
                .filter(this::isIdentifiable)
                .map(IdentifiableItem.class::cast)
                .map(TypeAwareIdentifierCacheKeyFactory::bindKeyString)
                .collect(Collectors.joining(KEY_PARTS_SEPARATOR));
    }
}
