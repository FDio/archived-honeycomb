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

package io.fd.honeycomb.infra.distro.initializer;

import com.google.inject.Inject;
import io.fd.honeycomb.data.init.RestoringInitializer;
import io.fd.honeycomb.infra.distro.ProviderTrait;
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration;
import java.nio.file.Paths;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.core.api.model.SchemaService;

public abstract class PersistedFileInitializerProvider extends ProviderTrait<RestoringInitializer> {

    @Inject
    private SchemaService schemaService;
    @Inject
    protected HoneycombConfiguration cfgAttributes;
    @Inject
    private DOMDataBroker domDataBroker;

    @Override
    public RestoringInitializer create() {
        return new RestoringInitializer(schemaService, Paths.get(getPersistPath()), domDataBroker,
                RestoringInitializer.RestorationType.valueOf(getRestorationType()), getDataStoreType());
    }

    public abstract String getPersistPath();

    public abstract LogicalDatastoreType getDataStoreType();

    public abstract String getRestorationType();

    public static class PersistedContextInitializerProvider extends PersistedFileInitializerProvider {
        public String getPersistPath() {
            return cfgAttributes.peristContextPath;
        }

        public LogicalDatastoreType getDataStoreType() {
            return LogicalDatastoreType.OPERATIONAL;
        }

        public String getRestorationType() {
            return cfgAttributes.persistedContextRestorationType;
        }

    }

    public static class PersistedConfigInitializerProvider extends PersistedFileInitializerProvider {
        public String getPersistPath() {
            return cfgAttributes.peristConfigPath;
        }

        public LogicalDatastoreType getDataStoreType() {
            return LogicalDatastoreType.CONFIGURATION;
        }

        public String getRestorationType() {
            return cfgAttributes.persistedConfigRestorationType;
        }

    }
}
