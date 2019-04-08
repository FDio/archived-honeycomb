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

package io.fd.honeycomb.translate.read.registry;


import io.fd.honeycomb.translate.read.InitFailedException;
import io.fd.honeycomb.translate.read.ReadContext;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.api.DataBroker;

/**
 * Root initializer.
 */
public interface InitRegistry {

    /**
     * Perform initialization on top of the data root.
     */
    void initAll(@Nonnull DataBroker broker, @Nonnull ReadContext ctx) throws InitFailedException;
}
