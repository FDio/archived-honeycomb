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

package io.fd.honeycomb.yang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.nio.file.Paths;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class YangModuleWhitelistReaderTest {

    private YangModuleWhitelistReader reader;

    @Before
    public void init() throws Exception {
        reader = new YangModuleWhitelistReader();
    }

    @Test
    public void read() throws Exception {
        final YangModuleWhitelist whitelist = reader.read(
                Paths.get(this.getClass().getClassLoader().getResource("expected-whitelist.xml").getPath()));

        assertNotNull(whitelist);
        final List<Module> modules = whitelist.getModules();
        assertEquals(2, modules.size());

        final Module moduleA = new Module();
        final Module moduleB = new Module();
        moduleA.setPckg("module.a.package");
        moduleA.setDescription("desc");

        moduleB.setPckg("module.b.package");

        assertThat(modules, CoreMatchers.hasItems(moduleA, moduleB));
    }
}