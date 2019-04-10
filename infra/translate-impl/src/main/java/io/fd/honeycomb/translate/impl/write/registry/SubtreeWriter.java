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

package io.fd.honeycomb.translate.impl.write.registry;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Iterables;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.translate.write.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Simple writer delegate for subtree writers (writers handling also children nodes) providing a list of all the
 * children nodes being handled.
 */
final class SubtreeWriter<D extends DataObject> implements Writer<D> {

    private final Writer<D> delegate;
    private final Set<InstanceIdentifier<?>> handledChildTypes = new HashSet<>();
    private boolean isWildcarded = false;

    private SubtreeWriter(final Writer<D> delegate, final Set<InstanceIdentifier<?>> handledTypes) {
        this.delegate = delegate;
        for (InstanceIdentifier<?> handledType : handledTypes) {
            // Iid has to start with writer's handled root type
            checkArgument(delegate.getManagedDataObjectType().getTargetType().equals(
                    handledType.getPathArguments().iterator().next().getType()),
                    "Handled node from subtree has to be identified by an instance identifier starting from: %s."
                            + "Instance identifier was: %s", getManagedDataObjectType().getTargetType(), handledType);
            checkArgument(Iterables.size(handledType.getPathArguments()) > 1,
                    "Handled node from subtree identifier too short: %s", handledType);
            handledChildTypes.add(InstanceIdentifier.create(Iterables.concat(
                    getManagedDataObjectType().getPathArguments(), Iterables.skip(handledType.getPathArguments(), 1))));
        }
    }

    private SubtreeWriter(final Writer<D> delegate) {
        this.delegate = delegate;
        this.isWildcarded = true;
    }

    /**
     * Return set of types also handled by this writer. All of the types are children of the type managed by this
     * writer excluding the type of this writer.
     */
    Set<InstanceIdentifier<?>> getHandledChildTypes() {
        return handledChildTypes;
    }

    @Override
    public void validate(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                         @Nullable final DataObject dataBefore, @Nullable final DataObject dataAfter,
                         @Nonnull final WriteContext ctx) throws DataValidationFailedException {
        delegate.validate(id, dataBefore, dataAfter, ctx);
    }

    @Override
    public void processModification(
            @Nonnull final InstanceIdentifier<? extends DataObject> id,
            @Nullable final DataObject dataBefore,
            @Nullable final DataObject dataAfter, @Nonnull final WriteContext ctx) throws WriteFailedException {
        delegate.processModification(id, dataBefore, dataAfter, ctx);
    }

    @Override
    public boolean supportsDirectUpdate() {
        return delegate.supportsDirectUpdate();
    }

    @Override
    public boolean canProcess(@Nonnull final InstanceIdentifier<? extends DataObject> instanceIdentifier) {
        if (isWildcarded) {
            final Class<D> parent = delegate.getManagedDataObjectType().getTargetType();
            for (InstanceIdentifier.PathArgument pathArgument : instanceIdentifier.getPathArguments()) {
                if (pathArgument.getType().equals(parent)) {
                    return true;
                }
            }
            return false;
        }
        return handledChildTypes.parallelStream()
                .filter(childIiD -> instanceIdentifier.getTargetType().equals(childIiD.getTargetType()))
                .anyMatch(instanceIdentifier1 -> isPathEqual(instanceIdentifier, instanceIdentifier1));
    }

    private boolean isPathEqual(@Nonnull final InstanceIdentifier<?> iid, final InstanceIdentifier<?> childIiD) {
        // Verifying path because the same type can be used in several places.
        Iterator<InstanceIdentifier.PathArgument> pathArguments = iid.getPathArguments().iterator();
        for (final InstanceIdentifier.PathArgument pathArgument : childIiD.getPathArguments()) {
            if (!pathArguments.hasNext() || !pathArgument.getType().equals(pathArguments.next().getType())) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Nonnull
    public InstanceIdentifier<D> getManagedDataObjectType() {
        return delegate.getManagedDataObjectType();
    }

    /**
     * Wrap a writer as a subtree writer.
     */
    static Writer<?> createForWriter(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                     @Nonnull final Writer<? extends DataObject> writer) {
        return new SubtreeWriter<>(writer, handledChildren);
    }

    /**
     * Wrap a writer as a subtree writer.
     */
    static Writer<?> createWildcardedForWriter(@Nonnull final Writer<? extends DataObject> writer) {
        return new SubtreeWriter<>(writer);
    }
}
