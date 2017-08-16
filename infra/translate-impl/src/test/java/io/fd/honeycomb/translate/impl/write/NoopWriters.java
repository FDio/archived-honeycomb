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

package io.fd.honeycomb.translate.impl.write;

import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class NoopWriters {

    private NoopWriters() {
    }

    public static class NonDirectUpdateWriterCustomizer implements WriterCustomizer<DataObject> {

        @Override
        public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<DataObject> id,
                                           @Nonnull final DataObject dataAfter,
                                           @Nonnull final WriteContext writeContext)
                throws WriteFailedException {
            // NOOP
        }

        @Override
        public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<DataObject> id,
                                            @Nonnull final DataObject dataBefore,
                                            @Nonnull final WriteContext writeContext)
                throws WriteFailedException {
            // NOOP
        }
    }

    public static class DirectUpdateWriterCustomizer extends NonDirectUpdateWriterCustomizer {

        @Override
        public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<DataObject> id,
                                            @Nonnull final DataObject dataBefore, @Nonnull final DataObject dataAfter,
                                            @Nonnull final WriteContext writeContext) throws WriteFailedException {
            // is direct support
        }
    }

    public static class ParentImplDirectUpdateWriterCustomizer extends DirectUpdateWriterCustomizer {
        // parent impls directly
    }
}
