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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import io.fd.honeycomb.translate.util.JsonUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.annotation.Nonnull;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for a DataTree that stores current state of data in backing DataTree on each successful commit.
 * Uses JSON format.
 */
public class PersistingDataTreeAdapter implements DataTree {

    private static final Logger LOG = LoggerFactory.getLogger(PersistingDataTreeAdapter.class);

    private final DataTree delegateDependency;
    private final JsonPersister persister;

    /**
     * Create new Persisting DataTree adapter
     *
     * @param delegate backing data tree that actually handles all the operations
     * @param persistPath path to a file (existing or not) to be used as storage for persistence. Full control over
     *                    a file at peristPath is expected
     * @param schemaService schemaContext provier
     */
    public PersistingDataTreeAdapter(@Nonnull final DataTree delegate,
                                     @Nonnull final SchemaService schemaService,
                                     @Nonnull final Path persistPath) {
        this(delegate, new JsonPersister(persistPath, schemaService));
    }

    public PersistingDataTreeAdapter(final DataTree delegate,
                                     final JsonPersister persister) {
        this.delegateDependency = checkNotNull(delegate, "delegate is null");
        this.persister = persister;
    }

    @Override
    public DataTreeSnapshot takeSnapshot() {
        return delegateDependency.takeSnapshot();
    }

    @Override
    public void setSchemaContext(final SchemaContext schemaContext) {
        delegateDependency.setSchemaContext(schemaContext);
    }

    @Override
    public void commit(final DataTreeCandidate dataTreeCandidate) {
        LOG.trace("Commit detected");
        delegateDependency.commit(dataTreeCandidate);
        LOG.debug("Delegate commit successful. Persisting data");

        // FIXME doing full read and full write might not be the fastest way of persisting data here
        final DataTreeSnapshot dataTreeSnapshot = delegateDependency.takeSnapshot();

        // TODO this can be handled in background by a dedicated thread + a limited blocking queue
        // TODO enable configurable granularity for persists. Maybe doing write on every modification is too much
        // and we could do bulk persist
        persister.persistCurrentData(dataTreeSnapshot.readNode(YangInstanceIdentifier.EMPTY));
    }

    @VisibleForTesting
    static class JsonPersister {

        private final Path path;
        private final SchemaService schemaServiceDependency;

        JsonPersister(final Path persistPath, final SchemaService schemaService) {
            this.path = testPersistPath(checkNotNull(persistPath, "persistPath is null"));
            this.schemaServiceDependency = checkNotNull(schemaService, "schemaService is null");
        }

        void persistCurrentData(final Optional<NormalizedNode<?, ?>> currentRoot) {
            if (currentRoot.isPresent()) {
                try {
                    LOG.trace("Persisting current data: {} into: {}", currentRoot.get(), path);
                    JsonUtils.writeJsonRoot(currentRoot.get(), schemaServiceDependency.getGlobalContext(),
                            Files.newOutputStream(path, StandardOpenOption.CREATE,
                                    StandardOpenOption.TRUNCATE_EXISTING));
                    LOG.trace("Data persisted successfully in {}", path);
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to persist current data", e);
                }
            } else {
                LOG.debug("Skipping persistence, since there's no data to persist");
            }
        }

        /**
         * Test whether file at persistPath exists and is readable or create it along with its parent structure.
         */
        private static Path testPersistPath(final Path persistPath) {
            try {
                checkArgument(!Files.isDirectory(persistPath), "Path %s points to a directory", persistPath);
                if (Files.exists(persistPath)) {
                    checkArgument(Files.isReadable(persistPath),
                            "Provided path %s points to existing, but non-readable file", persistPath);
                    return persistPath;
                }
                Files.createDirectories(persistPath.getParent());
                Files.write(persistPath, new byte[]{}, StandardOpenOption.CREATE);
            } catch (IOException | UnsupportedOperationException e) {
                LOG.warn("Provided path for persistence: {} is not usable", persistPath, e);
                throw new IllegalArgumentException("Path " + persistPath + " cannot be used as ");
            }

            return persistPath;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("path", path)
                    .toString();
        }
    }

    @Override
    public YangInstanceIdentifier getRootPath() {
        return delegateDependency.getRootPath();
    }

    @Override
    public void validate(final DataTreeModification dataTreeModification) throws DataValidationFailedException {
        delegateDependency.validate(dataTreeModification);
    }

    @Override
    public DataTreeCandidate prepare(
        final DataTreeModification dataTreeModification) {
        return delegateDependency.prepare(dataTreeModification);
    }

}
