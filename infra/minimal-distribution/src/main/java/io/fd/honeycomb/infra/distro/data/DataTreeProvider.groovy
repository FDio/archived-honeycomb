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
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.data.impl.PersistingDataTreeAdapter
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration
import io.fd.honeycomb.infra.distro.ProviderTrait
import org.opendaylight.controller.sal.core.api.model.SchemaService
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory

import java.nio.file.Paths

@Slf4j
@ToString
abstract class DataTreeProvider extends ProviderTrait<DataTree> {

    @Inject
    SchemaService schemaService
    @Inject
    HoneycombConfiguration config

    def create() {
        def delegate = InMemoryDataTreeFactory.getInstance().create(getType())
        delegate.setSchemaContext(schemaService.getGlobalContext())
        new PersistingDataTreeAdapter(delegate, schemaService, Paths.get(getPath()))
    }

    abstract String getPath()
    abstract TreeType getType()

    static class ConfigDataTreeProvider extends DataTreeProvider {
        String getPath() { config.peristConfigPath }
        TreeType getType() { TreeType.CONFIGURATION }
    }

    static class ContextDataTreeProvider extends DataTreeProvider {
        String getPath() { config.peristContextPath }
        TreeType getType() { TreeType.OPERATIONAL }
    }
}
