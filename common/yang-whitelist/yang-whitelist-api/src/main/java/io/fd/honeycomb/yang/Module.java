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

import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public class Module {

    private String pckg;
    private String description;

    @XmlElement(name = "package", required = true)
    public String getPckg() {
        return pckg;
    }

    public void setPckg(final String pckg) {
        // trims input name to eliminate formatted input
        this.pckg = pckg.trim();
    }

    @XmlElement
    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @XmlTransient
    public String getBindingProviderName() {
        return pckg + ".$YangModelBindingProvider";
    }

    @Override
    public String toString() {
        return "Module{" +
                "pckg='" + pckg + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Module that = (Module) o;

        return Objects.equals(this.pckg, that.pckg) && Objects.equals(this.description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pckg, description);
    }
}
