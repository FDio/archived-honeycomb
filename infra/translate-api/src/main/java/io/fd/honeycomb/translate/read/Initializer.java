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

package io.fd.honeycomb.translate.read;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Generic initializer.
 * <p/>
 * Capable of invoking 0..n edits as a result of existing operational data.
 */
@Beta
public interface Initializer<O extends DataObject> {

    /**
     * Transform operational data located under provided (keyed) id.
     *
     * @param broker DataBroker that accepts the resulting config data
     * @param id InstanceIdentifier of the operational data to initialize from
     * @param ctx Standard read context to assist during initialization e.g. caching data between customizers
     */
    void init(@Nonnull DataBroker broker, @Nonnull InstanceIdentifier<O> id, @Nonnull ReadContext ctx)
            throws InitFailedException;
}
