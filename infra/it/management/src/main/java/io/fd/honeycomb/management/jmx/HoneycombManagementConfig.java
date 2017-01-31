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

package io.fd.honeycomb.management.jmx;


import net.jmob.guice.conf.core.BindConfig;
import net.jmob.guice.conf.core.InjectConfig;
import net.jmob.guice.conf.core.Syntax;

import java.util.Optional;

@BindConfig(value = "management", syntax = Syntax.JSON)
public class HoneycombManagementConfig {

    public static final String JMX_PROTOCOL = "rmi";
    public static final String JXM_CONNECTOR_SERVER_NAME = "rmi";

    public static final String PROP_JMX_HOST = "jetty.jmxrmihost";
    public static final String PROP_JMX_PORT = "jetty.jmxrmiport";


    @InjectConfig(PROP_JMX_HOST)
    private String jmxHost;

    @InjectConfig(PROP_JMX_PORT)
    private String jmxPort;

    public String getJmxHost() {
        return Optional.ofNullable(jmxHost).orElse("localhost");
    }

    public int getJmxPort() {
        return Integer.parseInt(Optional.ofNullable(jmxPort).orElse("1099"));
    }
}
