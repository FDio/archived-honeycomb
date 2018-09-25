/*
 * Copyright (c) 2015, 2017 Cisco and/or its affiliates.
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

package io.fd.honeycomb.impl;

import com.google.common.base.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.dom.api.DOMMountPointListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Empty implementation of DOMMountPointService. HC does not support mountpoints, but Restconf requires
 * DOMMountPointService implementation to be present.
 */
public class EmptyDomMountService implements DOMMountPointService {
    @Override
    public Optional<DOMMountPoint> getMountPoint(final YangInstanceIdentifier yangInstanceIdentifier) {
        return Optional.absent();
    }

    @Override
    public DOMMountPointBuilder createMountPoint(final YangInstanceIdentifier yangInstanceIdentifier) {
        throw new UnsupportedOperationException("No mountpoint support");
    }

    @Override
    public ListenerRegistration<DOMMountPointListener> registerProvisionListener(final DOMMountPointListener listener) {
        return new ListenerRegistration<DOMMountPointListener>() {
            @Override
            public void close() {
                // Noop
            }

            @Nonnull
            @Override
            public DOMMountPointListener getInstance() {
                return listener;
            }
        };
    }
}
