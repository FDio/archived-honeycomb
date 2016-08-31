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

package io.fd.honeycomb.infra.distro.initializer;

import com.google.common.collect.Lists;
import io.fd.honeycomb.data.init.DataTreeInitializer;
import io.fd.honeycomb.data.init.InitializerRegistry;
import io.fd.honeycomb.data.init.InitializerRegistryImpl;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InitializerRegistryAdapter implements InitializerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(InitializerRegistryAdapter.class);

    private final DataTreeInitializer configInitializer;
    private final DataTreeInitializer contextInitializer;
    private final List<DataTreeInitializer> pluginInitializers;

    InitializerRegistryAdapter(final DataTreeInitializer configInitializer, final DataTreeInitializer contextInitializer,
                               final Set<DataTreeInitializer> pluginInitializers) {
        this.configInitializer = configInitializer;
        this.contextInitializer = contextInitializer;
        this.pluginInitializers = Lists.newArrayList(pluginInitializers);
    }

    @Override
    public void initialize() throws DataTreeInitializer.InitializeException {
        LOG.info("Config initialization started");

        final InitializerRegistry initializer = new InitializerRegistryImpl(pluginInitializers);
        try {
            // Initialize contexts first so that other initializers can find any relevant mapping before initializing
            // configuration to what is already in VPP
            contextInitializer.initialize();
            LOG.info("Persisted context restored successfully");
            // Initialize all registered initializers
            initializer.initialize();
            LOG.info("Configuration initialized successfully");
            // Initialize stored configuration on top
            configInitializer.initialize();
            LOG.info("Persisted configuration restored successfully");
        } catch (Exception e) {
            LOG.warn("Failed to initialize config", e);
        }

        LOG.info("Honeycomb initialized");
    }

}
