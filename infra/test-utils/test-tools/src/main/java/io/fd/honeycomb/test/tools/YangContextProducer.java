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

package io.fd.honeycomb.test.tools;

import static com.google.common.base.Preconditions.checkState;

import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javassist.ClassPool;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.util.AbstractModuleStringInstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Common logic to initialize serializers/deserializers/etc while working with yang based data
 */
interface YangContextProducer {

    default ModuleInfoBackedContext getCheckedModuleInfoContext(final Object test)
            throws IllegalAccessException, InvocationTargetException {
        final Method[] suitableMethods =
                MethodUtils.getMethodsWithAnnotation(test.getClass(), SchemaContextProvider.class);
        checkState(suitableMethods.length == 1, "Only single method should be @SchemaContextProvider, actual : %s",
                suitableMethods);
        final Object possibleContext = suitableMethods[0].invoke(test);
        checkState(possibleContext instanceof ModuleInfoBackedContext, "%s is not ModuleInfoBackedContext",
                possibleContext);
        return ModuleInfoBackedContext.class.cast(possibleContext);
    }

    /**
     * Get a codec for instance identifiers.
     */
    default AbstractModuleStringInstanceIdentifierCodec getIIDCodec(final ModuleInfoBackedContext ctx)
            throws NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException,
            java.lang.reflect.InvocationTargetException {
        // Reusing codec for JSON ... not public so here goes reflection

        final JSONCodecFactory jsonCodecFactory = JSONCodecFactory.create(ctx.getSchemaContext());
        final Constructor<?> cstr =
                Class.forName("org.opendaylight.yangtools.yang.data.codec.gson.JSONStringInstanceIdentifierCodec")
                        .getDeclaredConstructor(SchemaContext.class, JSONCodecFactory.class);
        cstr.setAccessible(true);
        return (AbstractModuleStringInstanceIdentifierCodec) cstr.newInstance(ctx.getSchemaContext(), jsonCodecFactory);
    }

    default BindingToNormalizedNodeCodec createSerializer(final ModuleInfoBackedContext moduleInfoBackedContext,
                                                          final SchemaContext schemaContexts) {

        final BindingNormalizedNodeCodecRegistry codecRegistry =
                new BindingNormalizedNodeCodecRegistry(
                        StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault())));
        codecRegistry
                .onBindingRuntimeContextUpdated(BindingRuntimeContext.create(moduleInfoBackedContext, schemaContexts));
        return new BindingToNormalizedNodeCodec(moduleInfoBackedContext, codecRegistry);
    }
}
