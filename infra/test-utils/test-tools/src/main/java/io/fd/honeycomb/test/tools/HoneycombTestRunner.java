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

import io.fd.honeycomb.test.tools.annotations.InjectablesProcessor;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.data.util.AbstractModuleStringInstanceIdentifierCodec;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HoneycombTestRunner extends BlockJUnit4ClassRunner implements YangContextProducer, InjectablesProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(HoneycombTestRunner.class);

    private SchemaContext schemaContext;
    private BindingToNormalizedNodeCodec serializer;
    private AbstractModuleStringInstanceIdentifierCodec iidParser;

    private YangDataProcessorRegistry processorRegistry;

    public HoneycombTestRunner(final Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected Object createTest() throws Exception {
        final Object test = super.createTest();
        LOG.debug("Initializing test {}", test);

        // Get schema context from annotated method
        final ModuleInfoBackedContext ctx = getCheckedModuleInfoContext(test);
        schemaContext = ctx.getSchemaContext();
        // Create serializer from it in order to later transform NormalizedNodes into BA
        serializer = createSerializer(ctx);
        // Create InstanceIdentifier Codec in order to later transform string represented IID into YangInstanceIdentifier
        iidParser = getIIDCodec(ctx);

        processorRegistry = YangDataProcessorRegistry.create(schemaContext, serializer);

        injectFields(test);
        return test;
    }

    @Override
    protected void validatePublicVoidNoArgMethods(final Class<? extends Annotation> annotation, final boolean isStatic,
                                                  final List<Throwable> errors) {
        // ignores this check,in case its super needed, can be implemented, but without checking no args(because of custom invoker)
    }

    /**
     * Allows method parameters injection
     */
    @Override
    protected Statement methodInvoker(final FrameworkMethod method, final Object test) {
        return new InjectableTestMethodInvoker(method, test, Arrays.stream(method.getMethod().getParameters())
                .map(this::injectValueOrNull)
                .collect(Collectors.toList())
                .toArray());
    }

    private Object injectValueOrNull(final Parameter parameter) {
        return isInjectable(parameter)
                ? processorRegistry.getNodeData(instanceIdentifier(iidParser, parameter), resourcePath(parameter))
                : null;
    }

    /**
     * Inject fields with dat from @InjectTestData.resourcePath
     */
    private void injectFields(final Object testInstance) {

        // iterate over all injectable fields
        injectableFields(testInstance.getClass()).forEach(field -> {
            LOG.debug("Processing field {}", field);
            injectField(field, testInstance,
                    processorRegistry.getNodeData(instanceIdentifier(iidParser, field), resourcePath(field)));
        });
    }
}
