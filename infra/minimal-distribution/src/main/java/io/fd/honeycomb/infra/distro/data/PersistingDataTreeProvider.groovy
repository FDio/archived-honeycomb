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

package io.fd.honeycomb.infra.distro.data

import com.google.inject.Inject
import com.google.inject.name.Named
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.data.impl.PersistingDataTreeAdapter
import io.fd.honeycomb.infra.distro.ProviderTrait
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration
import io.fd.honeycomb.infra.distro.data.context.ContextPipelineModule
import org.opendaylight.controller.sal.core.api.model.SchemaService
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType

import java.nio.file.Paths

@Slf4j
@ToString
abstract class PersistingDataTreeProvider extends ProviderTrait<DataTree> {

    @Inject
    SchemaService schemaService
    @Inject
    HoneycombConfiguration config

    def create() {
        new PersistingDataTreeAdapter(delegate, schemaService, Paths.get(path))
    }

    abstract String getPath()
    abstract TreeType getType()
    abstract DataTree getDelegate()

    static class ConfigPersistingDataTreeProvider extends PersistingDataTreeProvider {

        @Inject
        @Named(ConfigAndOperationalPipelineModule.HONEYCOMB_CONFIG_NONPERSIST)
        DataTree delegate

        String getPath() { config.peristConfigPath }
        TreeType getType() { TreeType.CONFIGURATION }
        DataTree getDelegate() { return delegate }
    }

    static class ContextPersistingDataTreeProvider extends PersistingDataTreeProvider {

        @Inject
        @Named(ContextPipelineModule.HONEYCOMB_CONTEXT_NOPERSIST)
        DataTree delegate

        String getPath() { config.peristContextPath }
        TreeType getType() { TreeType.OPERATIONAL }
        DataTree getDelegate() { return delegate }
    }
}
