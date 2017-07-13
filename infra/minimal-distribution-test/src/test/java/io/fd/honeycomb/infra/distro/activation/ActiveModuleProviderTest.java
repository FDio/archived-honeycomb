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

package io.fd.honeycomb.infra.distro.activation;


import static com.google.common.collect.ImmutableList.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import io.fd.honeycomb.infra.distro.Modules;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class ActiveModuleProviderTest {

    @Test
    public void testLoadActiveModulesSuccessfull() {
        final ImmutableList<String> rawResources = of(
                "// this should be skipped",
                "// io.fd.honeycomb.infra.distro.Modules$ChildModule1",
                "               io.fd.honeycomb.infra.distro.Modules$ChildModule2",
                "io.fd.honeycomb.infra.distro.Modules$ChildModule3     ",
                "io.fd.honeycomb.infra.distro.Modules$ChildModule3",
                "io.fd.honeycomb.infra.distro.Modules$NonModule"
        );
        // have to be without wildcard, otherwise mockito has problem with it
        final Set<Module> activeModules = (Set<Module>) new ActiveModules(ActiveModuleProvider.loadActiveModules(rawResources)).createModuleInstances();

        // first and second line should be ignored due to comment
        // second,third,and fourth are valid,but should reduce module count to 2,because of duplicity
        // last one does is not ancestor of Module, so it should be ignored/skipped
        assertThat(activeModules, hasSize(2));
        //hasItems or containsInAnyOrder does not have/is deprecated in variant with matcher
        assertThat(activeModules, hasItem(isA(Modules.ChildModule2.class)));
        assertThat(activeModules, hasItem(isA(Modules.ChildModule3.class)));
    }

    @Test(expected = IllegalStateException.class)
    public void testLoadActiveModulesFailed() {
        final ImmutableList rawResources = of(
                "// this should be skipped",
                "// io.fd.honeycomb.infra.distro.Modules$ChildModule1",
                "               io.fd.honeycomb.infra.distro.Modules$ChildModule2",
                "### io.fd.honeycomb.infra.distro.Modules$ChildModule3     ",// it should fail because of this
                "io.fd.honeycomb.infra.distro.Modules$ChildModule3",
                "io.fd.honeycomb.infra.distro.Modules$NonModule"
        );

        ActiveModuleProvider.loadActiveModules(rawResources);
    }

    @Test
    public void testAggregateResourcesNonEmpty() {
        final List<String> aggregatedResources =
                new ActiveModuleProvider().aggregateResources("modules");
        assertThat(aggregatedResources, hasSize(5));
        assertThat(aggregatedResources, hasItems("        Non-commented non-trimmed",
                "//Commented",
                "// Commented non-trimmed",
                "Not commented",
                "// Line from second file"));
    }

    @Test(expected = IllegalStateException.class)
    public void testAggregateResourcesEmpty() {
        new ActiveModuleProvider().aggregateResources("/non-existing-folder");
    }

}
