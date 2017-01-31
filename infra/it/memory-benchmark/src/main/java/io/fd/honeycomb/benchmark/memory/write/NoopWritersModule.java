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
package io.fd.honeycomb.benchmark.memory.write;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.fd.honeycomb.translate.write.WriterFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.memory.benchmark.rev161204.ConfigData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.memory.benchmark.rev161204.config.data.ConfigList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NoopWritersModule extends AbstractModule {
    @Override
    protected void configure() {

        final Multibinder<WriterFactory> writeBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writeBinder.addBinding().toInstance(registry -> {
            // Add noop writers for all data written in this benchmark

            registry.add(new NoopWriter<>(InstanceIdentifier.create(ConfigData.class)));
            registry.add(new NoopWriter<>(InstanceIdentifier.create(ConfigData.class).child(ConfigList.class)));
        });
    }

}
