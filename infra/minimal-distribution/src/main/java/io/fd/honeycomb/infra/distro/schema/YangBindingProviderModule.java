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

package io.fd.honeycomb.infra.distro.schema;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load all YangModelBindingProvider classes from classpath.
 * <p/>
 * Relying on /META-INF/services/ metadata.
 */
public class YangBindingProviderModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(YangBindingProviderModule.class);

    private static final String YANG_BA_PROVIDER_PATH = "META-INF/services/" + YangModelBindingProvider.class.getName();

    protected void configure() {
        final Multibinder<YangModelBindingProvider> binder =
                Multibinder.newSetBinder(binder(), YangModelBindingProvider.class);
        final List<URL> resources;
        try {
            resources = Collections.list(getClass().getClassLoader().getResources(YANG_BA_PROVIDER_PATH));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load binding providers from path: " + YANG_BA_PROVIDER_PATH, e);
        }
        LOG.debug("ModuleProviders found at {}", resources);
        resources.stream()
                .map(YangBindingProviderModule::urlToString)
                .flatMap(content -> Lists.newArrayList(content.split("\n")).stream())
                .filter(line -> !Strings.isNullOrEmpty(line.trim()))
                .map(YangBindingProviderModule::loadClass)
                .forEach(providerClass -> {
                    LOG.debug("ModuleProvider found for {}", providerClass);
                    binder.addBinding().to((Class<? extends YangModelBindingProvider>) providerClass)
                            .in(Singleton.class);
                });
    }

    private static Class<?> loadClass(@Nonnull final String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to load class: " + className, e);
        }
    }

    private static String urlToString(@Nonnull final URL url) {
        try {
            return Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read resource from: " + url, e);
        }
    }
}
