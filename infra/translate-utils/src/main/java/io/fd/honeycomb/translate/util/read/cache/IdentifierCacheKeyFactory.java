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

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Factory providing cache keys to easier switching between scopes of caching
 */
public final class IdentifierCacheKeyFactory implements CacheKeyFactory {

    private static final String KEY_PARTS_SEPARATOR = "|";

    // should be Set<Class<? extends DataObject & Identificable<?>>>, but that's not possible for wildcards
    private final Set<Class<? extends DataObject>> additionalKeyTypes;

    /**
     * Construct simple cache key factory
     */
    public IdentifierCacheKeyFactory() {
        this(Collections.emptySet());
    }

    /**
     * @param additionalKeyTypes Additional types from path of cached type, that are specifying scope
     */
    public IdentifierCacheKeyFactory(@Nonnull final Set<Class<? extends DataObject>> additionalKeyTypes) {
        // verify that all are non-null and identifiable
        this.additionalKeyTypes = checkNotNull(additionalKeyTypes, "Additional key types can't be null").stream()
                .map(IdentifierCacheKeyFactory::verifyNotNull)
                .map(IdentifierCacheKeyFactory::verifyIsIdentifiable)
                .collect(Collectors.toSet());
    }

    @Override
    public String createKey(@Nonnull final InstanceIdentifier<?> actualContextIdentifier) {

        checkNotNull(actualContextIdentifier, "Cannot construct key for null InstanceIdentifier");

        // easiest case when only simple key is needed
        if (additionalKeyTypes.isEmpty()) {
            return actualContextIdentifier.getTargetType().toString();
        }

        checkArgument(isUniqueKeyConstructable(actualContextIdentifier),
                "Unable to construct unique key, required key types : %s, provided paths : %s", additionalKeyTypes,
                actualContextIdentifier.getPathArguments());

        return String
                .join(KEY_PARTS_SEPARATOR, additionalKeys(actualContextIdentifier),
                        actualContextIdentifier.getTargetType().toString());
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
                .map(IdentifierCacheKeyFactory::bindKeyString)
                .collect(Collectors.joining(KEY_PARTS_SEPARATOR));
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
}
