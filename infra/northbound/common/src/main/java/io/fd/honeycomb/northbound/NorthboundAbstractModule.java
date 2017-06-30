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

package io.fd.honeycomb.northbound;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Base class for Honeycomb's AbstractModules, that are initialized conditionally based on specific configuration.
 *
 * @param <T> module configuration type
 */
public abstract class NorthboundAbstractModule<T> extends AbstractModule implements ConfigurationSupplier<T> {

    private final Class<T> configClass;
    private final Module configurationModule;
    private final Injector injector;

    protected NorthboundAbstractModule(final Module configurationModule, final Class<T> configClass) {
        this.configurationModule = configurationModule;
        this.configClass = configClass;
        this.injector = Guice.createInjector(configurationModule);
    }

    @Override
    public final Module getConfigurationModule() {
        return configurationModule;
    }

    @Override
    public final T getConfiguration() {
        return injector.getInstance(configClass);
    }
}
