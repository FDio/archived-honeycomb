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

package io.fd.honeycomb.infra.distro.initializer

import com.google.inject.Inject
import com.google.inject.name.Named
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.data.init.DataTreeInitializer
import io.fd.honeycomb.data.init.InitializerRegistry
import io.fd.honeycomb.data.init.InitializerRegistryImpl
import io.fd.honeycomb.infra.distro.ProviderTrait
import io.fd.honeycomb.infra.distro.data.ConfigAndOperationalPipelineModule
import io.fd.honeycomb.infra.distro.data.context.ContextPipelineModule

@Slf4j
@ToString
class InitializerRegistryProvider extends ProviderTrait<InitializerRegistry> {

    @Inject
    @Named(ContextPipelineModule.HONEYCOMB_CONTEXT)
    DataTreeInitializer contextInitializer
    @Inject
    @Named(ConfigAndOperationalPipelineModule.HONEYCOMB_CONFIG)
    DataTreeInitializer configInitializer
    @Inject(optional = true)
    Set<DataTreeInitializer> pluginInitializers = []

    @Override
    def create() {
        return new InitializerRegistry() {

            @Override
            void initialize() throws DataTreeInitializer.InitializeException {
                log.info("Config initialization started");

                final InitializerRegistry initializer = new InitializerRegistryImpl(new ArrayList<>(pluginInitializers))

                try {
                    // Initialize contexts first so that other initializers can find any relevant mapping before initializing
                    // configuration to what is already in VPP
                    contextInitializer.initialize();
                    log.info("Persisted context restored successfully");
                    // Initialize all registered initializers
                    initializer.initialize();
                    log.info("Configuration initialized successfully");
                    // Initialize stored configuration on top
                    configInitializer.initialize();
                    log.info("Persisted configuration restored successfully");
                } catch (Exception e) {
                    log.warn("Failed to initialize config", e);
                }

                log.info("Honeycomb initialized");
            }

            @Override
            void close() throws Exception {}
        }
    }
}
