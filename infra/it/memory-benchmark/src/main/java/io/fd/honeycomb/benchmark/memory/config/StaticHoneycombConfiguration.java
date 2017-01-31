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
package io.fd.honeycomb.benchmark.memory.config;

import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Static configuration to prevent injecting of properties from json fles
 */
public class StaticHoneycombConfiguration extends HoneycombConfiguration implements Closeable {

    public StaticHoneycombConfiguration(@Nonnull final String persistConfigPath, @Nonnull final String persistContextPath) {
        this.peristConfigPath = persistConfigPath;
        this.peristContextPath = persistContextPath;

        this.username = "admin";
        this.password = "admin";
        this.notificationServiceQueueDepth = 1;
        this.restconfBindingAddress = Optional.of("/restconf");
        this.restconfPort = Optional.of(8187);
        this.restconfBindingAddress = Optional.of("127.0.0.1");
        this.persistedConfigRestorationType = "Merge";
        this.persistedContextRestorationType = "Merge";
        this.restconfWebsocketPort = Optional.of(7890);
    }

    @Override
    public boolean isConfigPersistenceEnabled() {
        return false;
    }

    @Override
    public boolean isRestconfHttpsEnabled() {
        return false;
    }

    @Override
    public boolean isRestconfHttpEnabled() {
        return true;
    }

    @Override
    public boolean isRestconfEnabled() {
        return true;
    }

    @Override
    public boolean isNetconfTcpEnabled() {
        return false;
    }

    @Override
    public boolean isContextPersistenceEnabled() {
        return false;
    }

    @Override
    public boolean isNetconfEnabled() {
        return false;
    }

    @Override
    public boolean isNetconfSshEnabled() {
        return false;
    }

    @Override
    public void close() throws IOException {
        if (Files.exists(Paths.get(peristConfigPath))) {
            Files.delete(Paths.get(peristConfigPath));
        }

        if (Files.exists(Paths.get(peristContextPath))) {
            Files.delete(Paths.get(peristContextPath));
        }
    }
}
