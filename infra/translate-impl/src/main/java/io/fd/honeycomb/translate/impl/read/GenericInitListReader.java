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

package io.fd.honeycomb.translate.impl.read;

import static io.fd.honeycomb.translate.impl.read.GenericInitReader.writeInit;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.read.InitFailedException;
import io.fd.honeycomb.translate.read.InitListReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GenericInitListReader<O extends DataObject & Identifiable<K>, K extends Identifier<O>, B extends Builder<O>>
        extends GenericListReader<O, K, B>
        implements InitListReader<O, K, B> {

    private static final Logger LOG = LoggerFactory.getLogger(GenericInitListReader.class);

    public GenericInitListReader(@Nonnull final InstanceIdentifier<O> id,
                                 @Nonnull final InitializingListReaderCustomizer<O, K, B> customizer) {
        super(id, customizer);
    }

    @Override
    public void init(final DataBroker broker, final InstanceIdentifier<O> id, final ReadContext ctx)
            throws InitFailedException {
        LOG.debug("{}: Initializing current: {}", this, id);

        try {
            for (K k : getAllIds(id, ctx)) {
                initSingle(broker, RWUtils.replaceLastInId(id, RWUtils.getCurrentIdItem(id, k)), ctx);
            }
        } catch (ReadFailedException e) {
            LOG.warn("{}: Failed to initialize current, unable to read: {}", this, id, e);
            throw new InitFailedException(e.getFailedId(), e);
        }
    }

    private void initSingle(final DataBroker broker, final InstanceIdentifier<O> id, final ReadContext ctx)
            throws InitFailedException {
        LOG.debug("{}: Initializing current: {}", this, id);

        try {
            final Optional<O> operational = readCurrent(id, ctx);
            if (operational.isPresent()) {
                final Initialized<? extends DataObject> init =
                        ((InitializingListReaderCustomizer<O, K, B>) customizer).init(id, operational.get(), ctx);
                LOG.debug("{}: Writing init config : {} at: {}", GenericInitListReader.this, init.getData(), init.getId());
                writeInit(broker, init);
            }
        } catch (ReadFailedException e) {
            LOG.warn("{}: Failed to initialize current, unable to read: {}", this, id, e);
            throw new InitFailedException(e.getFailedId(), e);
        }
    }
}
