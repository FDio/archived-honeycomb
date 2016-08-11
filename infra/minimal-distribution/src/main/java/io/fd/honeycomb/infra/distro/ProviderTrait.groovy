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

package io.fd.honeycomb.infra.distro

import com.google.inject.Provider
import groovy.util.logging.Slf4j

// TODO this should be a trait, but groovy compilation/stub generation sometimes fails (only) in Intellij if using trait

@Slf4j
abstract class ProviderTrait<T> implements Provider<T> {

    @Override
    T get() {
        log.info "Providing: {}", this
        T create = create() as T
        log.debug "Provided: {}", create
        create
    }

    abstract def create()
}
