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

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * &lt;yang-modules-whitelist&gt; &lt;modules&gt; &lt;module&gt; &lt;package&gt;io.fd.aaa.bbb.ccc&lt;/package&gt; &lt;description&gt;XYZ&lt;/description&gt;
 * &lt;/module&gt; &lt;/modules&gt; &lt;/yang-modules-whitelist&gt;
 */
@XmlRootElement
public class YangModuleWhitelist {

    private List<Module> modules;

    public List<Module> getModules() {
        return modules;
    }

    @XmlElementWrapper(name = "modules")
    @XmlElement(name = "module")
    public void setModules(final List<Module> modules) {
        this.modules = modules;
    }

    @Override
    public String toString() {
        return "YangModuleWhitelist{" +
                "modules=" + modules +
                '}';
    }
}
