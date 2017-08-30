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

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

abstract class JAXBContextHolder {
    private final JAXBContext ctx;

    protected JAXBContextHolder(@Nonnull final Class<?>... ctxClasses) {
        try {
            ctx = JAXBContext.newInstance(ctxClasses);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create JAXB context", e);
        }
    }

    protected JAXBContext getCtx() {
        return ctx;
    }
}
