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

package io.fd.honeycomb.test.tools;

import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import javax.annotation.Nonnull;

abstract class AbstractYangContextHolder {

    private final SchemaContext schemaContext;
    private final BindingToNormalizedNodeCodec serializer;

    AbstractYangContextHolder(@Nonnull final SchemaContext schemaContext,
                              @Nonnull final BindingToNormalizedNodeCodec serializer){
        this.schemaContext=schemaContext;
        this.serializer=serializer;
    }

    SchemaContext schemaContext() {
        return schemaContext;
    }

    BindingToNormalizedNodeCodec serializer() {
        return serializer;
    }
}
