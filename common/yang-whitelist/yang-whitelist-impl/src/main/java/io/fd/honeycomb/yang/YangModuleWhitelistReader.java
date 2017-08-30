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

package io.fd.honeycomb.yang;

import static java.lang.String.format;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class YangModuleWhitelistReader extends JAXBContextHolder {

    public YangModuleWhitelistReader() {
        super(YangModuleWhitelist.class);
    }

    @Nonnull
    public YangModuleWhitelist read(@Nonnull final Path path) {
        final Unmarshaller unmarshaller = createUnmarshaller();
        return YangModuleWhitelist.class.cast(readWhitelist(path, unmarshaller));
    }

    private static Object readWhitelist(final Path path, final Unmarshaller unmarshaller) {
        try {
            return unmarshaller.unmarshal(path.toFile());
        } catch (JAXBException e) {
            throw new IllegalStateException(format("Unable to read whitelist from %s", path), e);
        }
    }

    private Unmarshaller createUnmarshaller() {
        try {
            return getCtx().createUnmarshaller();
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create unmarshaller", e);
        }
    }
}
