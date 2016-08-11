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

import com.google.inject.Inject
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.infra.distro.ProviderTrait
import javassist.ClassPool
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils

@Slf4j
@ToString
class SerializerProvider extends ProviderTrait<BindingToNormalizedNodeCodec> {

    @Inject
    ModuleInfoBackedContext mibCtx;

    def create() {
        def serializerGenerator = new StreamWriterGenerator(JavassistUtils.forClassPool(ClassPool.getDefault()))
        def codecRegistry = new BindingNormalizedNodeCodecRegistry(serializerGenerator)
        def ctx = BindingRuntimeContext.create(mibCtx, mibCtx.getSchemaContext())
        codecRegistry.onBindingRuntimeContextUpdated(ctx)
        def codec = new BindingToNormalizedNodeCodec(mibCtx, codecRegistry)
        codec.onGlobalContextUpdated(mibCtx.getSchemaContext())
        codec
    }
}