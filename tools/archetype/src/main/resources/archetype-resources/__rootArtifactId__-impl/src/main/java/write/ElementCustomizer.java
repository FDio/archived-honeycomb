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

#macro( dotted $str )
#foreach( $word in $rootArtifactId.split('-') )$word.#end
#end
#set( $packageName = "#dotted( $rootArtifactId )" )
package ${package}.write;

import ${package}.CrudService;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${packageName}rev160918.${packageName}params.Element;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.${packageName}rev160918.${packageName}params.ElementKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Writer for {@link Element} list node from our YANG model.
 */
public final class ElementCustomizer implements ListWriterCustomizer<Element, ElementKey> {

    private final CrudService<Element> crudService;

    public ElementCustomizer(@Nonnull final CrudService<Element> crudService) {
        this.crudService = crudService;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Element> id, @Nonnull final Element dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        //perform write of data,or throw exception
        //invoked by PUT operation,if provided data doesn't exist in Config data
        crudService.writeData(id, dataAfter);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Element> id,
                                        @Nonnull final Element dataBefore,
                                        @Nonnull final Element dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        //perform update of data,or throw exception
        //invoked by PUT operation,if provided data does exist in Config data
        crudService.updateData(id, dataBefore, dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Element> id,
                                        @Nonnull final Element dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        //perform delete of data,or throw exception
        //invoked by DELETE operation
        crudService.deleteData(id, dataBefore);
    }
}
