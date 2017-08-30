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

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class YangModuleWhitelistWriterTest {

    private YangModuleWhitelistWriter writer;
    private Path path;

    @Before
    public void init() throws IOException {
        writer = new YangModuleWhitelistWriter();
        path = Files.createTempFile("tmp", "whitelist");
    }

    @Test
    public void write() throws Exception {
        final Module moduleA = new Module();
        final Module moduleB = new Module();
        moduleA.setPckg("module.a.package");
        moduleA.setDescription("desc");

        moduleB.setPckg("module.b.package");

        final YangModuleWhitelist whitelist = new YangModuleWhitelist();
        whitelist.setModules(ImmutableList.of(moduleA, moduleB));

        writer.write(whitelist, path, false);
        final String output = Files.readAllLines(path).stream().collect(Collectors.joining());
        final String expectedOutput = Resources
                .toString(this.getClass().getClassLoader().getResource("expected-whitelist.xml"),
                        StandardCharsets.UTF_8);
        assertEquals(expectedOutput, output);
    }


    @After
    public void clean() {
        path.toFile().delete();
    }
}