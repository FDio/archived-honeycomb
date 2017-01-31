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

package io.fd.honeycomb.management.jmx;

import javax.annotation.Nonnull;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;

public interface JMXBeanProvider {

    /**
     * Provides JMX connector to specified url
     */
    default JMXConnector getConnector(@Nonnull final JMXServiceURL url) {
        try {
            return JMXConnectorFactory.connect(url);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create JMX connector", e);
        }
    }

    /**
     * Requests specified jxm bean from provided connector
     */
    default Object getJMXAttribute(@Nonnull final JMXConnector connector, @Nonnull final String beanType,
                                   @Nonnull final String beanName) {
        try {
            return connector.getMBeanServerConnection().getAttribute(new ObjectName(beanType), beanName);
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("Unable to query mbean of type %s, name %s", beanType, beanName), e);
        }
    }
}
