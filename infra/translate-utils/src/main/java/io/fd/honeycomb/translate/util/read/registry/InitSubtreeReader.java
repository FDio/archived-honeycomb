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

package io.fd.honeycomb.translate.util.read.registry;

import io.fd.honeycomb.translate.read.InitFailedException;
import io.fd.honeycomb.translate.read.InitListReader;
import io.fd.honeycomb.translate.read.InitReader;
import io.fd.honeycomb.translate.read.ListReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.Reader;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

class InitSubtreeReader<O extends DataObject, B extends Builder<O>>
        extends SubtreeReader<O, B>
        implements InitReader<O, B> {

    private InitSubtreeReader(final InitReader<O, B> delegate,
                              final Set<InstanceIdentifier<?>> handledTypes) {
        super(delegate, handledTypes);
    }

    @Override
    public void init(final DataBroker broker, final InstanceIdentifier<O> id, final ReadContext ctx) throws InitFailedException {
        ((InitReader<O, B>) delegate).init(broker, id, ctx);
    }

    /**
     * Wrap a Reader as an initializing subtree Reader.
     */
    static <D extends DataObject, B extends Builder<D>> Reader<D, B> createForReader(@Nonnull final Set<InstanceIdentifier<?>> handledChildren,
                                                                                     @Nonnull final Reader<D, B> reader) {
        return (reader instanceof ListReader)
                ? new InitSubtreeListReader<>((InitListReader) reader, handledChildren)
                : new InitSubtreeReader<>(((InitReader<D, B>) reader), handledChildren);
    }

    private static class InitSubtreeListReader<D extends DataObject & Identifiable<K>, B extends Builder<D>, K extends Identifier<D>>
            extends SubtreeListReader<D, B, K>
            implements InitListReader<D, K, B> {

        InitSubtreeListReader(final InitListReader<D, K, B> delegate,
                              final Set<InstanceIdentifier<?>> handledTypes) {
            super(delegate, handledTypes);
        }

        @Override
        public void init(final DataBroker broker, final InstanceIdentifier<D> id, final ReadContext ctx) throws InitFailedException {
            ((InitListReader<D, K, B>) delegate).init(broker, id, ctx);
        }
    }
}
