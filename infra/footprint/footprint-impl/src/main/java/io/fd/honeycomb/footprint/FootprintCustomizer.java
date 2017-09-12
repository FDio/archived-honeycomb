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

package io.fd.honeycomb.footprint;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.footprint.rev170830.MemoryFootprintState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.footprint.rev170830.MemoryFootprintStateBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FootprintCustomizer implements ReaderCustomizer<MemoryFootprintState, MemoryFootprintStateBuilder> {

    private final FootprintReader footprintReader;

    public FootprintCustomizer(@Nonnull final FootprintReader footprintReader) {
        this.footprintReader = footprintReader;
    }

    @Nonnull
    @Override
    public MemoryFootprintStateBuilder getBuilder(@Nonnull final InstanceIdentifier<MemoryFootprintState> id) {
        return new MemoryFootprintStateBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<MemoryFootprintState> id,
                                      @Nonnull final MemoryFootprintStateBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        builder.setFootprint((long) footprintReader.readCurrentFootprint()).setPid((long) footprintReader.getPid());
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final MemoryFootprintState readValue) {
        //NOOP
    }
}
