/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

package io.fd.honeycomb.infra.bgp;

import com.google.common.base.MoreObjects;
import java.util.Collections;
import java.util.Optional;
import net.jmob.guice.conf.core.BindConfig;
import net.jmob.guice.conf.core.InjectConfig;
import net.jmob.guice.conf.core.Syntax;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.DefaultPolicyType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.Config;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.routing.policy.rev151009.apply.policy.group.apply.policy.ConfigBuilder;

/**
 * This is the Java equivalent for bgp-policy.json file. We use guice-config library to load all the config attributes
 * into this class instance.
 *
 * The BindConfig annotation tells that bgp-policy.json file should be looked up on classpath root.
 *
 * For now we support only default actions for import and export policies (ACCEPT-ROUTE or REJECT-ROUTE)
 */

// TODO (HONEYCOMB-446) Add full support of initial configuration for BGP policy.
@BindConfig(value = "bgp-policy", syntax = Syntax.JSON)
public class BgpPolicyConfiguration {

    @InjectConfig("default-import-policy")
    public Optional<String> defaultImportPolicy;
    @InjectConfig("default-export-policy")
    public Optional<String> defaultExportPolicy;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("default-import-policy", defaultImportPolicy)
                .add("default-export-policy", defaultExportPolicy)
                .toString();
    }

    public Optional<Config> getPolicyConfig() {
        if (defaultImportPolicy.isPresent() && defaultExportPolicy.isPresent()) {
            Config config = new ConfigBuilder()
                    .setDefaultImportPolicy(
                            DefaultPolicyType.valueOf(defaultImportPolicy.get().replace("-", "")))
                    .setDefaultExportPolicy(
                            DefaultPolicyType.valueOf(defaultExportPolicy.get().replace("-", "")))
                    .setImportPolicy(Collections.emptyList())
                    .setExportPolicy(Collections.emptyList())
                    .build();
            return Optional.of(config);
        } else {
            return Optional.empty();
        }
    }
}
