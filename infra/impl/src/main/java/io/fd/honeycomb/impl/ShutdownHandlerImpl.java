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

package io.fd.honeycomb.impl;

import io.fd.honeycomb.data.init.ShutdownHandler;
import java.util.Deque;
import java.util.LinkedList;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShutdownHandlerImpl implements ShutdownHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownHandlerImpl.class);

    private final Deque<CloseableComponent> components;

    public ShutdownHandlerImpl() {
        components = new LinkedList<>();
    }

    @Override
    public synchronized void register(@Nonnull final String name, @Nonnull final AutoCloseable component) {
        LOG.debug("Registering component {} for proper shutdown", name);
        components.add(new CloseableComponent(name, component));
        LOG.trace("Component {} properly register", name);
    }

    private static final class CloseableComponent {
        private final String name;
        private final AutoCloseable component;

        private CloseableComponent(final String name, final AutoCloseable component) {
            this.name = name;
            this.component = component;
        }

        private String getName() {
            return name;
        }

        private AutoCloseable getComponent() {
            return component;
        }
    }

    @Override
    public void performShutdown() {
        // close components in reverse order that they were registered
        components.descendingIterator().forEachRemaining(closeable -> {
            LOG.info("Closing component {}", closeable.getName());
            try {
                closeable.getComponent().close();
            } catch (Exception e) {
                // We can't do much here, so logging exception and moving to the next closable component
                LOG.warn("Unable to close component {}", closeable.getName(), e);
            }
        });
    }
}
