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

package io.fd.honeycomb.translate.spi.read;

import io.fd.honeycomb.translate.read.ReadContext;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * ReadCustomizers which want to participate in the initializing process need to implement this interface.
 *
 * It is triggered after Honeycomb initializes the plugins to give them a change
 * reconcile(put data in HC in sync with underlying layer) with the underlying layer.
 */
public interface InitializingCustomizer<O extends DataObject> {

    /**
     * Transform Operational data into Config data.
     *
     * @param id InstanceIdentifier of operational data being initialized
     * @param readValue Operational data being initialized(converted into config)
     * @param ctx Standard read context to assist during initialization e.g. caching data between customizers
     *
     * @return Initialized, config data and its identifier
     */
    @Nonnull
    Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<O> id,
                                           @Nonnull O readValue,
                                           @Nonnull ReadContext ctx);
}
