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

package io.fd.honeycomb.infra.distro.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import org.junit.Test;

public class YangModuleMappingIndexTest {

    @Test
    public void testIndex() {
        final YangModuleMappingIndex index = new YangModuleMappingIndex("static-yang-index");
        assertEquals(9, index.applicationModulesCount());
        assertCfgModule(index);
    }

    private void assertCfgModule(YangModuleMappingIndex index) {
        final Collection<String> yangModules =
                index.getByModuleName("io.fd.honeycomb.infra.distro.cfgattrs.CfgAttrsModule");
        assertNotNull(yangModules);
        assertEquals(50, yangModules.size());
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.sal.restconf.event.subscription.rev140708.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.instance.identifier.patch.module.rev151121.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.sal.restconf.service.rev150708.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.rest.connector.rev140724.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.subscribe.to.notification.rev161028.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.netconf.northbound.impl.rev150112.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.netconf.northbound.ssh.rev150114.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.netconf.mdsal.notification.rev150803.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.netconf.mdsal.monitoring.rev150218.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.netconf.mdsal.mapper.rev150114.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.$YangModelBindingProvider"));
        assertTrue(yangModules.contains(
                "org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.extension.rev131210.$YangModelBindingProvider"));
        // etc ...
    }
}