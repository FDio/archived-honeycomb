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

import java.nio.file.Path;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangModuleWhitelistWriter extends JAXBContextHolder {

    private static final Logger LOG = LoggerFactory.getLogger(YangModuleWhitelistWriter.class);

    public YangModuleWhitelistWriter() {
        super(YangModuleWhitelist.class);
    }

    /**
     * Output serialized whitelist on specified path
     *
     * @param whitelist whitelist configuration
     * @param outPath   output path
     */
    public void write(@Nonnull final YangModuleWhitelist whitelist,
                      @Nonnull final Path outPath,
                      final boolean formatOutput) {
        Objects.requireNonNull(whitelist, "Cannot white null whitelist");
        // mashaller is not synchronized and lightweight, best practice is to create it per request(as opose to ctx,
        // that should be created just once)
        final Marshaller marshaller = createMarshaller(getCtx());
        setupPrettyPrint(marshaller, formatOutput);
        whiteWhitelist(whitelist, outPath, marshaller);
    }

    private static void whiteWhitelist(final YangModuleWhitelist whitelist, final Path outPath,
                                       final Marshaller marshaller) {
        try {
            LOG.debug("Writing whitelist {} to file {}", whitelist, outPath);
            marshaller.marshal(whitelist, outPath.toFile());
            LOG.debug("Whitelist successfully written to file {}", outPath);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to write whitelist", e);
        }
    }

    private static void setupPrettyPrint(final Marshaller marshaller, final boolean value) {
        try {
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, value);
        } catch (PropertyException e) {
            throw new IllegalStateException("Unable to setup pretty print");
        }
    }

    private static Marshaller createMarshaller(final JAXBContext ctx) {
        try {
            return ctx.createMarshaller();
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create marshaller", e);
        }
    }
}
