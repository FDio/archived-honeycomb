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
package ${package};

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${packageName}rev160918.${classNamePrefix}State;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${packageName}rev160918.${packageName}params.Element;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${packageName}rev160918.${packageName}params.ElementBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${packageName}rev160918.${packageName}params.ElementKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple example of class handling Crud operations for plugin.
 *
 * No real handling, serves just as an illustration.
 *
 * TODO update javadoc
 */
final class ElementCrudService implements CrudService<Element> {

    private static final Logger LOG = LoggerFactory.getLogger(ElementCrudService.class);

    @Override
    public void writeData(@Nonnull final InstanceIdentifier<Element> identifier, @Nonnull final Element data)
            throws WriteFailedException {
        if (data != null) {

            // identifier.firstKeyOf(SomeClassUpperInHierarchy.class) can be used to identify
            // relationships such as to which parent these data are related to

            // Performs any logic needed for persisting such data
            LOG.info("Writing path[{}] / data [{}]", identifier, data);
        } else {
            throw new WriteFailedException.CreateFailedException(identifier, data,
                    new NullPointerException("Provided data are null"));
        }
    }

    @Override
    public void deleteData(@Nonnull final InstanceIdentifier<Element> identifier, @Nonnull final Element data)
            throws WriteFailedException {
        if (data != null) {

            // identifier.firstKeyOf(SomeClassUpperInHierarchy.class) can be used to identify
            // relationships such as to which parent these data are related to

            // Performs any logic needed for persisting such data
            LOG.info("Removing path[{}] / data [{}]", identifier, data);
        } else {
            throw new WriteFailedException.DeleteFailedException(identifier,
                    new NullPointerException("Provided data are null"));
        }
    }

    @Override
    public void updateData(@Nonnull final InstanceIdentifier<Element> identifier, @Nonnull final Element dataOld,
                           @Nonnull final Element dataNew) throws WriteFailedException {
        if (dataOld != null && dataNew != null) {

            // identifier.firstKeyOf(SomeClassUpperInHierarchy.class) can be used to identify
            // relationships such as to which parent these data are related to

            // Performs any logic needed for persisting such data
            LOG.info("Update path[{}] from [{}] to [{}]", identifier, dataOld, dataNew);
        } else {
            throw new WriteFailedException.DeleteFailedException(identifier,
                    new NullPointerException("Provided data are null"));
        }
    }

    @Override
    public Element readSpecific(@Nonnull final InstanceIdentifier<Element> identifier) throws ReadFailedException {

        // read key specified in path identifier
        final ElementKey key = identifier.firstKeyOf(Element.class);

        // load data by this key
        // *Key class will always contain key of entity, in this case long value

        return new ElementBuilder()
                .setId(key.getId())
                .setKey(key)
                .setDescription("This is a example of loaded data")
                .build();
    }

    @Override
    public List<Element> readAll() throws ReadFailedException {
        // read all data under parent node,in this case {@link ModuleState}
        return Collections.singletonList(
                readSpecific(InstanceIdentifier.create(${classNamePrefix}State.class).child(Element.class, new ElementKey(10L))));
    }
}
