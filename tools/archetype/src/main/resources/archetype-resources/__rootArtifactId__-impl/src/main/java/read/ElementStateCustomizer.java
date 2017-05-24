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
package ${package}.read;

import ${package}.CrudService;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${packageName}rev160918.${classNamePrefix}StateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${packageName}rev160918.${packageName}params.Element;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${packageName}rev160918.${packageName}params.ElementBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${packageName}rev160918.${packageName}params.ElementKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Reader for {@link Element} list node from our YANG model.
 */
public final class ElementStateCustomizer implements
    InitializingListReaderCustomizer<Element, ElementKey, ElementBuilder> {

    private final CrudService<Element> crudService;

    public ElementStateCustomizer(final CrudService<Element> crudService) {
        this.crudService = crudService;
    }

    @Nonnull
    @Override
    public List<ElementKey> getAllIds(@Nonnull final InstanceIdentifier<Element> id, @Nonnull final ReadContext context)
            throws ReadFailedException {
        // perform read operation and extract keys from data
        return crudService.readAll()
                .stream()
                .map(a -> new ElementKey(a.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Element> readData) {
        // merge children data to parent builder
        // used by infrastructure to merge data loaded in separated customizers
        ((${classNamePrefix}StateBuilder) builder).setElement(readData);
    }

    @Nonnull
    @Override
    public ElementBuilder getBuilder(@Nonnull final InstanceIdentifier<Element> id) {
        // return new builder for this data node
        return new ElementBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Element> id,
                                      @Nonnull final ElementBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        // this stage is used after reading all ids by getAllIds,to read specific details about data

        // perform read of details of data specified by key of Element in id
        final Element data = crudService.readSpecific(id);

        // and sets it to builder
        builder.setId(data.getId());
        builder.setKey(data.getKey());
        builder.setDescription(data.getDescription());
    }
    /**
     *
     * Initialize configuration data based on operational data.
     * <p/>
     * Very useful when a plugin is initiated but the underlying layer already contains some operation state.
     * Deriving the configuration from existing operational state enables reconciliation in case when
     * Honeycomb's persistence is not available to do the work for us.
     */
    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<Element> id,
                                                  @Nonnull final Element readValue,
                                                  @Nonnull final ReadContext ctx) {
        return Initialized.create(id, readValue);
    }
}
