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
import io.fd.honeycomb.data.ModifiableDataManager
import io.fd.honeycomb.data.impl.ModifiableDataTreeDelegator
import io.fd.honeycomb.infra.distro.ProviderTrait
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder
import org.opendaylight.controller.md.sal.binding.api.DataBroker
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree

@Slf4j
@ToString
class ModifiableDTDelegProvider extends ProviderTrait<ModifiableDataManager> {

    @Inject
    BindingToNormalizedNodeCodec serializer
    @Inject
    @Named("honeycomb-config")
    DataTree dataTree
    @Inject
    ModifiableWriterRegistryBuilder registry
    @Inject
    @Named("honeycomb-context")
    DataBroker contextBroker

    @Override
    def create() { new ModifiableDataTreeDelegator(serializer, dataTree, registry.build(), contextBroker) }
}
