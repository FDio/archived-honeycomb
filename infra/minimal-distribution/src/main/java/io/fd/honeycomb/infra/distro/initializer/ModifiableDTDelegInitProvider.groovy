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
import io.fd.honeycomb.data.ModifiableDataManager
import io.fd.honeycomb.data.impl.ModifiableDataTreeDelegator
import io.fd.honeycomb.infra.distro.ProviderTrait
import io.fd.honeycomb.infra.distro.data.ConfigAndOperationalPipelineModule
import io.fd.honeycomb.infra.distro.data.context.ContextPipelineModule
import io.fd.honeycomb.translate.util.write.NoopWriterRegistry
import org.opendaylight.controller.md.sal.binding.api.DataBroker
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree

@Slf4j
@ToString
class ModifiableDTDelegInitProvider extends ProviderTrait<ModifiableDataManager> {

    @Inject
    BindingToNormalizedNodeCodec serializer
    @Inject
    @Named(ConfigAndOperationalPipelineModule.HONEYCOMB_CONFIG_NONPERSIST)
    DataTree dataTree
    @Inject
    @Named(ContextPipelineModule.HONEYCOMB_CONTEXT)
    DataBroker contextBroker

    @Override
    def create() { new ModifiableDataTreeDelegator(serializer, dataTree, new NoopWriterRegistry(), contextBroker) }
}
