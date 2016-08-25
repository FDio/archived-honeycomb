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

#macro( ccase $str )
#foreach( $word in $rootArtifactId.split('-') )$word.substring(0,1).toUpperCase()$word.substring(1)#end
#end
#set( $classNamePrefix = "#ccase( $rootArtifactId )" )
#macro( dotted $str )
#foreach( $word in $rootArtifactId.split('-') )$word.#end
#end
#set( $packageName = "#dotted( $rootArtifactId )" )
package ${package}.init;

import io.fd.honeycomb.data.init.AbstractDataTreeConverter;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${packageName}rev160918.${classNamePrefix};
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${packageName}rev160918.${classNamePrefix}Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${packageName}rev160918.${classNamePrefix}State;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Initialize configuration data based on operational data.
 * <p/>
 * Very useful when a plugin is initiated but the underlying layer already contains some operation state.
 * Deriving the configuration from existing operational state enables reconciliation in case when Honeycomb's persistence
 * is not available to do the work for us.
 */
public final class ConfigDataInitializer extends AbstractDataTreeConverter<${classNamePrefix}State, ${classNamePrefix}> {

    @Inject
    public ConfigDataInitializer(@Named("honeycomb-initializer") @Nonnull final DataBroker bindingDataBroker) {
        super(bindingDataBroker, InstanceIdentifier.create(${classNamePrefix}State.class), InstanceIdentifier.create(${classNamePrefix}.class));
    }

    @Override
    public ${classNamePrefix} convert(final ${classNamePrefix}State operationalData) {
        // Transfer all the operational data into configuration
        return new ${classNamePrefix}Builder()
                .setElement(operationalData.getElement())
                .build();
    }
}
