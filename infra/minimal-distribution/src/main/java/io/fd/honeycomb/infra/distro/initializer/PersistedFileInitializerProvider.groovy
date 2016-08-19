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
import groovy.transform.ToString
import groovy.util.logging.Slf4j

import io.fd.honeycomb.data.init.RestoringInitializer
import io.fd.honeycomb.infra.distro.ProviderTrait
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker
import org.opendaylight.controller.sal.core.api.model.SchemaService

import java.nio.file.Paths

@Slf4j
@ToString
abstract class PersistedFileInitializerProvider extends ProviderTrait<RestoringInitializer> {

    @Inject
    SchemaService schemaService
    @Inject
    HoneycombConfiguration cfgAttributes

    @Inject
    DOMDataBroker domDataBroker

    @Override
    def create() {
        new RestoringInitializer(schemaService, Paths.get(getPersistPath()),
                domDataBroker, RestoringInitializer.RestorationType.valueOf(restorationType), getDataStoreType())
    }

    abstract String getPersistPath()
    abstract LogicalDatastoreType getDataStoreType()
    abstract String getRestorationType()

    static class PersistedContextInitializerProvider extends PersistedFileInitializerProvider {
        String getPersistPath() { cfgAttributes.peristContextPath }
        LogicalDatastoreType getDataStoreType() { LogicalDatastoreType.OPERATIONAL }
        String getRestorationType() { cfgAttributes.persistedContextRestorationType }
    }

    static class PersistedConfigInitializerProvider extends PersistedFileInitializerProvider {
        String getPersistPath() { cfgAttributes.peristConfigPath }
        LogicalDatastoreType getDataStoreType() { LogicalDatastoreType.CONFIGURATION }
        String getRestorationType() { cfgAttributes.persistedConfigRestorationType }
    }
}
