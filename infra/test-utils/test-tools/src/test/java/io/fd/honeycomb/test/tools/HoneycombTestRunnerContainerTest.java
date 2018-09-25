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

import static io.fd.honeycomb.test.tools.InjectionTestData.AUGMENT_LIST_DATA_PATH;
import static io.fd.honeycomb.test.tools.InjectionTestData.AUGMENT_LIST_RESOURCE;
import static io.fd.honeycomb.test.tools.InjectionTestData.NESTED_LIST_DATA_PATH;
import static io.fd.honeycomb.test.tools.InjectionTestData.NESTED_LIST_RESOURCE;
import static io.fd.honeycomb.test.tools.InjectionTestData.ROOT_LIST_DATA_PATH;
import static io.fd.honeycomb.test.tools.InjectionTestData.ROOT_LIST_RESOURCE;
import static io.fd.honeycomb.test.tools.InjectionTestData.SIMPLES_LIST_RESOURCE;
import static io.fd.honeycomb.test.tools.InjectionTestData.SIMPLE_LIST_DATA_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.test.tools.annotations.InjectablesProcessor;
import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.AugContainerAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.RootList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.SimpleContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.SimpleContainerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.simple.container.NestedContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.simple.container.SimpleList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.simple.container.augmented.container.ListInAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.simple.container.simple.list.ContUnderList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.simple.container.simple.list.ContUnderListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.data.rev150105.simple.container.simple.list.NestedList;

@RunWith(HoneycombTestRunner.class)
public class HoneycombTestRunnerContainerTest implements InjectablesProcessor {

    @InjectTestData(resourcePath = "/simpleContainerEmpty.json")
    private SimpleContainer simpleContainer;

    @InjectTestData(resourcePath = "/nestedContainer.json", id = "/hc-data:simple-container/hc-data:nested-container")
    private NestedContainer nestedContainer;

    @InjectTestData(resourcePath = "/leafInAugment.json")
    private SimpleContainer containerWithLeafInAugment;

    @InjectTestData(resourcePath = "/containerInList.json", id = "/hc-data:simple-container" +
            "/hc-data:simple-list[hc-data:name='nameUnderSimpleList']" +
            "/hc-data:cont-under-list")
    private ContUnderList containerUnderList;

    @InjectTestData(resourcePath = AUGMENT_LIST_RESOURCE, id = AUGMENT_LIST_DATA_PATH)
    private ListInAugment listInAugment;

    @InjectTestData(resourcePath = NESTED_LIST_RESOURCE, id = NESTED_LIST_DATA_PATH)
    private NestedList nestedList;

    @InjectTestData(resourcePath = ROOT_LIST_RESOURCE, id = ROOT_LIST_DATA_PATH)
    private RootList rootList;

    @InjectTestData(resourcePath = SIMPLES_LIST_RESOURCE, id = SIMPLE_LIST_DATA_PATH)
    private SimpleList simpleList;


    @SchemaContextProvider
    public ModuleInfoBackedContext getSchemaContext() {
        return provideSchemaContextFor(Collections.singleton($YangModuleInfoImpl.getInstance()));
    }

    @Test
    public void testSimpleContainer() {
        assertNotNull(simpleContainer);
    }

    @Test
    public void testNestedContainer() {
        assertNotNull(nestedContainer);
        assertEquals("abcd", nestedContainer.getName());
    }

    @Test
    public void testLeafInAugmentedContainer() {
        assertNotNull(containerWithLeafInAugment);
        assertNotNull(containerWithLeafInAugment.getAugmentedContainer());
        assertEquals("nameInAugment", containerWithLeafInAugment.getAugmentedContainer().augmentation(
                AugContainerAugmentation.class).getNameInAugment());
    }

    @Test
    public void testContainerUnderList() {
        assertNotNull(containerUnderList);
        assertEquals("nestedName", containerUnderList.getNestedName());
    }

    @Test
    public void testParameterInjectionRootNode(
            @InjectTestData(resourcePath = "/simpleContainerEmpty.json") SimpleContainer container) {
        assertNotNull(container);
    }

    @Test
    public void testParameterInjectionChildNode(
            @InjectTestData(resourcePath = "/nestedContainer.json",
                    id = "/hc-data:simple-container/hc-data:nested-container") NestedContainer container) {
        assertNotNull(container);
    }

    @Test
    public void testParameterInjectionMultiple(
            @InjectTestData(resourcePath = "/simpleContainerEmpty.json") SimpleContainer containerFirst,
            @InjectTestData(resourcePath = "/nestedContainer.json",
                    id = "/hc-data:simple-container/hc-data:nested-container") NestedContainer containerSecond) {
        assertNotNull(containerFirst);
        assertNotNull(containerSecond);
    }

    @Test
    public void testParameterInjectionOneNonInjectable(
            @InjectTestData(resourcePath = "/simpleContainerEmpty.json") SimpleContainer containerFirst,
            String thisOneShouldBeNull) {
        assertNotNull(containerFirst);
        assertNull(thisOneShouldBeNull);
    }

    @Test
    public void testConsistenceSimpleNode(
            @InjectTestData(resourcePath = "/simpleContainerEmpty.json") SimpleContainer container) {

        assertEquals(new SimpleContainerBuilder().build(), simpleContainer);
    }

    @Test
    public void testConsistenceWithLeafNode(
            @InjectTestData(resourcePath = "/containerInList.json", id = "/hc-data:simple-container" +
                    "/hc-data:simple-list[hc-data:name='nameUnderSimpleList']" +
                    "/hc-data:cont-under-list") ContUnderList containerUnderList) {
        assertEquals(new ContUnderListBuilder().setNestedName("nestedName").build(), containerUnderList);
    }
}