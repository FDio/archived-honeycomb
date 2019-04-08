/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import org.junit.Before;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.$YangModuleInfoImpl;
import org.opendaylight.yangtools.yang.data.util.AbstractModuleStringInstanceIdentifierCodec;

abstract class AbstractYangDataProcessorTest implements InjectablesProcessor, YangContextProducer {

    ModuleInfoBackedContext moduleInfoBackedContext;
    AbstractModuleStringInstanceIdentifierCodec codec;
    BindingToNormalizedNodeCodec serializer;

    @Before
    public void init() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        moduleInfoBackedContext = provideSchemaContextFor(Collections.singleton($YangModuleInfoImpl.getInstance()));
        codec = getIIDCodec(moduleInfoBackedContext);
        serializer = createSerializer(moduleInfoBackedContext);

        // to init children
        setUp();
    }

    // for children init
    abstract void setUp();
}
