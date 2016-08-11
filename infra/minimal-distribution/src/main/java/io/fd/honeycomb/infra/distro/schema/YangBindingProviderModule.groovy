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

package io.fd.honeycomb.infra.distro.schema

import com.google.common.base.Charsets
import com.google.inject.AbstractModule
import com.google.inject.Singleton
import com.google.inject.multibindings.Multibinder
import groovy.util.logging.Slf4j
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider
/**
 * Load all YangModelBindingProvider classes from classpath.
 * <p/>
 * Relying on /META-INF/services/ metadata.
 */
@Slf4j
class YangBindingProviderModule extends AbstractModule {

    static final String YANG_BA_PROVIDER_PATH = "META-INF/services/" + YangModelBindingProvider.class.getName();

    void configure() {
        Multibinder.newSetBinder(binder(), YangModelBindingProvider.class).with {
            def resources = Collections.list(getClass().getClassLoader().getResources(YANG_BA_PROVIDER_PATH))
            log.debug "ModuleProviders found at {}", resources
            resources.forEach {
                it.getText(Charsets.UTF_8.displayName()).split("\n")
                        .findAll { it != null && !it.isEmpty() && !it.isAllWhitespace()}
                        .collect { this.getClass().forName(it) }
                        .forEach {
                    log.debug "ModuleProvider found for {}", it
                    addBinding().to(it).in(Singleton)
                }
            }
        }
    }
}
