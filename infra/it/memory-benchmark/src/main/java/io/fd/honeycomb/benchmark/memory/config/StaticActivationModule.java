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
package io.fd.honeycomb.benchmark.memory.config;

import io.fd.honeycomb.infra.distro.activation.ActivationConfig;
import io.fd.honeycomb.infra.distro.activation.ActivationModule;
import io.fd.honeycomb.infra.distro.activation.ActiveModuleProvider;
import io.fd.honeycomb.infra.distro.activation.ActiveModules;

// TODO HONEYCOMB-383 - Use config files
// Used to bind static configuration to DI
public class StaticActivationModule extends ActivationModule {

    private BindableCfgAttrsModule cfgAttrsModule;

    public StaticActivationModule(final BindableCfgAttrsModule cfgAttrsModule) {
        this.cfgAttrsModule = cfgAttrsModule;
    }

    @Override
    protected void configure() {
        install(cfgAttrsModule);
        bind(ActivationConfig.class).toInstance(new StaticActivationConfig());
        bind(ActiveModules.class).toProvider(ActiveModuleProvider.class).asEagerSingleton();
    }
}
