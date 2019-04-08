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

package io.fd.honeycomb.infra.distro.data;

import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreConfigProperties;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreFactory;

public final class DataStoreProvider extends ProviderTrait<InMemoryDOMDataStore> {

    @Inject
    private DOMSchemaService schemaService;
    private String name;

    public DataStoreProvider(final String name) {
        this.name = name;
    }

    @Override
    protected InMemoryDOMDataStore create() {
        return InMemoryDOMDataStoreFactory
                .create(name, InMemoryDOMDataStoreConfigProperties.getDefault(), schemaService);
    }
}
