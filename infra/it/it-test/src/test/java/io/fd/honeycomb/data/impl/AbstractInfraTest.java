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

package io.fd.honeycomb.data.impl;

import static org.mockito.Mockito.when;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Arrays;
import java.util.Map;
import javassist.ClassPool;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Base IT test infrastructure.
 */
abstract class AbstractInfraTest {

    protected BindingNormalizedNodeSerializer serializer;
    protected SchemaContext schemaContext;

    @Mock
    protected org.opendaylight.mdsal.binding.api.DataBroker contextBroker;
    @Mock
    private org.opendaylight.mdsal.binding.api.ReadWriteTransaction ctxTx;

    static BindingToNormalizedNodeCodec getSerializer(final ModuleInfoBackedContext moduleInfoBackedContext,
                                                      final SchemaContext schemaContext) {
        final DataObjectSerializerGenerator serializerGenerator =
            StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault()));
        final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(serializerGenerator);
        final BindingRuntimeContext ctx =
                BindingRuntimeContext.create(moduleInfoBackedContext, schemaContext);
        codecRegistry.onBindingRuntimeContextUpdated(ctx);
        return new BindingToNormalizedNodeCodec(moduleInfoBackedContext, codecRegistry);
    }

    static ModuleInfoBackedContext getSchemaContext() {
        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        moduleInfoBackedContext.addModuleInfos(Arrays.asList(
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.aug.test.rev170109.$YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.subtree.test.rev180116.$YangModuleInfoImpl.getInstance()

        ));
        return moduleInfoBackedContext;
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(contextBroker.newReadWriteTransaction()).thenReturn(ctxTx);
        when(ctxTx.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());

        initSerializer();
        postSetup();
    }

    abstract void postSetup();

    private void initSerializer() {
        final ModuleInfoBackedContext moduleInfoBackedContext = getSchemaContext();
        schemaContext = moduleInfoBackedContext.tryToCreateSchemaContext().get();
        serializer = getSerializer(moduleInfoBackedContext, schemaContext);
    }

    protected Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> toBinding(
            final NormalizedNode<?, ?> read) {
        Multimap<InstanceIdentifier<? extends DataObject>, DataObject> baNodes = HashMultimap.create();

        for (DataContainerChild<?, ?> o : ((DataContainerNode<?>) read).getValue()) {
            final YangInstanceIdentifier yid = YangInstanceIdentifier.of(o.getNodeType());
            final Map.Entry<InstanceIdentifier<?>, DataObject> baEntry = serializer.fromNormalizedNode(yid, o);
            baNodes.put(baEntry.getKey(), baEntry.getValue());
        }
        return baNodes;
    }
}
