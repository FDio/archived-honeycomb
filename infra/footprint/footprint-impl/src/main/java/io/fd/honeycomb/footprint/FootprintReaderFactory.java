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

import com.google.inject.Inject;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.footprint.rev170830.MemoryFootprintState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FootprintReaderFactory implements ReaderFactory {

    @Inject
    private FootprintReader footprintReader;

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        registry.add(new GenericReader(InstanceIdentifier.create(MemoryFootprintState.class),
                new FootprintCustomizer(footprintReader)));
    }
}
