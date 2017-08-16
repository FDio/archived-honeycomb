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

package io.fd.honeycomb.translate.util.write;

import io.fd.honeycomb.translate.TranslationException;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.registry.WriterRegistry;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Empty registry that does not perform any changes. Can be used in data layer, if we want to disable passing data to
 * translation layer.
 */
public class NoopWriterRegistry implements WriterRegistry, AutoCloseable {

    @Override
    public void processModifications(@Nonnull final DataObjectUpdates updates,
                                     @Nonnull final WriteContext ctx) throws TranslationException {
        // NOOP
    }

    @Override
    public boolean writerSupportsUpdate(@Nonnull final InstanceIdentifier<?> type) {
        // returns true to make higher level performance better(does not have to break updates to delete+create pairs)
        return true;
    }

    @Override
    public void close() throws Exception {
        // NOOP
    }
}
