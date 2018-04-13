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

package io.fd.honeycomb.data.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;
import static io.fd.honeycomb.data.impl.ModifiableDataTreeManager.DataTreeContextFactory.DataTreeContext;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import io.fd.honeycomb.data.DataModification;
import io.fd.honeycomb.data.ModifiableDataManager;
import io.fd.honeycomb.translate.ValidationFailedException;
import io.fd.honeycomb.translate.TranslationException;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataTree backed implementation for modifiable data manager.
 */
public class ModifiableDataTreeManager implements ModifiableDataManager {

    private static final Logger LOG = LoggerFactory.getLogger(ModifiableDataTreeManager.class);

    private final DataTree dataTree;
    private final DataTreeContextFactory contextFactory;

    public ModifiableDataTreeManager(@Nonnull final DataTree dataTree) {
        this(dataTree, candidate -> (() -> candidate));
    }

    public ModifiableDataTreeManager(@Nonnull final DataTree dataTree,
                                     @Nonnull final DataTreeContextFactory contextFactory) {
        this.dataTree = checkNotNull(dataTree, "dataTree should not be null");
        this.contextFactory = contextFactory;
    }

    @Override
    public DataModification newModification() {
        return new ConfigSnapshot();
    }

    @Override
    public final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(@Nonnull final YangInstanceIdentifier path) {
        return newModification().read(path);
    }

    protected class ConfigSnapshot implements DataModification {
        private final DataTreeSnapshot snapshot;
        private final DataTreeModification modification;

        ConfigSnapshot() {
            this.snapshot = dataTree.takeSnapshot();
            this.modification = snapshot.newModification();
        }

        @Override
        public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
                @Nonnull final YangInstanceIdentifier path) {
            // FIXME switch to java.util.Optional when rest of ODL infra does
            final Optional<NormalizedNode<?, ?>> node = Optional.fromNullable(modification.readNode(path).orElse(null));
            if (LOG.isTraceEnabled() && node.isPresent()) {
                LOG.trace("ConfigSnapshot.read: {}", node.get());
            }
            return immediateCheckedFuture(node);
        }

        @Override
        public final void delete(final YangInstanceIdentifier path) {
            modification.delete(path);
        }

        @Override
        public final void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
            modification.merge(path, data);
        }

        @Override
        public final void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
            modification.write(path, data);
        }

        @Override
        public final void commit() throws TranslationException {
            final DataTreeContext candidateContext = prepareCandidateContext(modification);
            validateCandidate(candidateContext);
            processCandidate(candidateContext);
            dataTree.commit(candidateContext.getCandidate());
        }

        private DataTreeContext prepareCandidateContext(final DataTreeModification dataTreeModification)
            throws ValidationFailedException {
            // Seal the modification (required to perform validate)
            dataTreeModification.ready();

            // Check if modification can be applied to data tree
            try {
                dataTree.validate(dataTreeModification);
            } catch (DataValidationFailedException e) {
                throw new ValidationFailedException(e);
            }

            return contextFactory.create(dataTree.prepare(dataTreeModification));
        }

        protected void validateCandidate(final DataTreeContext dataTreeContext) throws ValidationFailedException {
            // NOOP
        }

        protected void processCandidate(final DataTreeContext dataTreeContext) throws TranslationException {
            // NOOP
        }

        @Override
        public final void validate() throws ValidationFailedException {
            // Modification requires to be sealed before validation.
            // Sealed modification cannot be altered, so create copy.
            final CursorAwareDataTreeModification modificationCopy =
                (CursorAwareDataTreeModification) snapshot.newModification();
            final DataTreeModificationCursor cursor = modificationCopy.createCursor(dataTree.getRootPath());
            checkState(cursor != null, "DataTreeModificationCursor for root path should not be null");
            modification.applyToCursor(cursor);
            // Then validate it.
            validateCandidate(prepareCandidateContext(modificationCopy));
        }

        @Override
        public String toString() {
            return "ConfigSnapshot{modification="
                + ReflectionToStringBuilder.toString(
                modification,
                RecursiveToStringStyle.MULTI_LINE_STYLE,
                false,
                false
            ) + '}';
        }
    }

    interface DataTreeContextFactory {
        DataTreeContext create(final DataTreeCandidate candidate);

        /**
         * Stores DataTreeCandidate. Implementation may also store additional data to optimize validation
         * and translation process.
         */
        interface DataTreeContext {
            DataTreeCandidate getCandidate();
        }
    }
}
