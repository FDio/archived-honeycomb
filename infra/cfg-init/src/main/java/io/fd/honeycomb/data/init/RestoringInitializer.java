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

package io.fd.honeycomb.data.init;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.VisibleForTesting;
import io.fd.honeycomb.translate.util.JsonUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestoringInitializer implements DataTreeInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(RestoringInitializer.class);

    private final SchemaService schemaService;
    private final Path path;
    private final DOMDataBroker dataTree;
    private final RestorationType restorationType;
    private final LogicalDatastoreType datastoreType;
    private final JsonReader jsonReader;

    public RestoringInitializer(@Nonnull final SchemaService schemaService,
                                @Nonnull final Path path,
                                @Nonnull final DOMDataBroker dataTree,
                                @Nonnull final RestorationType restorationType,
                                @Nonnull final LogicalDatastoreType datastoreType,
                                @Nonnull final JsonReader jsonReader) {
        this.schemaService = schemaService;
        this.datastoreType = datastoreType;
        this.path = checkStorage(path);
        this.dataTree = dataTree;
        this.restorationType = restorationType;
        this.jsonReader = jsonReader;
    }

    public RestoringInitializer(@Nonnull final SchemaService schemaService,
                                @Nonnull final Path path,
                                @Nonnull final DOMDataBroker dataTree,
                                @Nonnull final RestorationType restorationType,
                                @Nonnull final LogicalDatastoreType datastoreType) {
        this(schemaService, path, dataTree, restorationType, datastoreType, new JsonReader());
    }

    private Path checkStorage(final Path path) {
        if (Files.exists(path)) {
            checkArgument(!Files.isDirectory(path), "File %s is a directory", path);
            checkArgument(Files.isReadable(path), "File %s is not readable", path);
        }

        return path;
    }

    @Override
    public void initialize() throws InitializeException {
        LOG.debug("Starting restoration of {} from {} using {}", dataTree, path, restorationType);
        if (!Files.exists(path)) {
            LOG.debug("Persist file {} does not exist. Skipping restoration", path);
            return;
        }

        try {
            final ContainerNode containerNode = jsonReader.readData(schemaService.getGlobalContext(), path);

            final DOMDataWriteTransaction domDataWriteTransaction = dataTree.newWriteOnlyTransaction();
            for (DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> dataContainerChild : containerNode
                .getValue()) {
                final YangInstanceIdentifier iid = YangInstanceIdentifier.create(dataContainerChild.getIdentifier());
                LOG.trace("Restoring {} from {}", iid, path);

                switch (restorationType) {
                    case Merge:
                        domDataWriteTransaction.merge(datastoreType, iid, dataContainerChild);
                        break;
                    case Put:
                        domDataWriteTransaction.put(datastoreType, iid, dataContainerChild);
                        break;
                    default:
                        throw new InitializeException(
                            "Unable to initialize data using " + restorationType + " restoration strategy. Unsupported");
                }
            }

            // Block here to prevent subsequent initializers processing before context is fully restored
            domDataWriteTransaction.submit().checkedGet();
            LOG.debug("Data from {} restored successfully", path);

        } catch (IOException | TransactionCommitFailedException e) {
            throw new InitializeException("Unable to restore data from " + path, e);
        }
    }

    /**
     * Type of operation to use when writing restored data.
     */
    public enum RestorationType {
        Put, Merge
    }

    @VisibleForTesting
    static class JsonReader {

        public ContainerNode readData(final SchemaContext globalContext, final Path path) throws IOException {
            return JsonUtils.readJsonRoot(globalContext, Files.newInputStream(path, StandardOpenOption.READ));
        }
    }
}
