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

package io.fd.honeycomb.infra.distro.netconf;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.infra.distro.ProviderTrait;
import java.lang.reflect.Constructor;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.netconf.mapping.api.NetconfOperationServiceFactoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class NetconfMonitoringMapperProvider extends ProviderTrait<NetconfOperationServiceFactory> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfMonitoringMapperProvider.class);

    @Inject
    @Named(NetconfModule.HONEYCOMB_NETCONF)
    private BindingAwareBroker bindingAwareBroker;
    @Inject
    private NetconfOperationServiceFactoryListener aggregator;
    @Inject
    private NetconfMonitoringService monitoringService;

    @Override
    protected NetconfOperationServiceFactory create() {
        try {
            final Class<?> monitoringWriterCls = Class.forName(
                    "org.opendaylight.controller.config.yang.netconf.mdsal.monitoring.MonitoringToMdsalWriter");
            Constructor<?> declaredConstructor =
                    monitoringWriterCls.getDeclaredConstructor(NetconfMonitoringService.class);
            declaredConstructor.setAccessible(true);
            final BindingAwareProvider writer = (BindingAwareProvider) declaredConstructor.newInstance(monitoringService);
            bindingAwareBroker.registerProvider(writer);

            final Class<?> moduleClass = Class.forName(
                    "org.opendaylight.controller.config.yang.netconf.mdsal.monitoring.NetconfMdsalMonitoringMapperModule");
            final Class<?> monitoringMapperCls = Class.forName(
                    "org.opendaylight.controller.config.yang.netconf.mdsal.monitoring.NetconfMdsalMonitoringMapperModule$MdsalMonitoringMapper");
            declaredConstructor =
                    monitoringMapperCls.getDeclaredConstructor(NetconfMonitoringService.class);
            declaredConstructor.setAccessible(true);
            final NetconfOperationService mdSalMonitoringMapper =
                    (NetconfOperationService) declaredConstructor.newInstance(monitoringService);

            final Class<?> monitoringMpperFactory = Class.forName(
                    "org.opendaylight.controller.config.yang.netconf.mdsal.monitoring.NetconfMdsalMonitoringMapperModule$MdSalMonitoringMapperFactory");
            declaredConstructor =
                    monitoringMpperFactory.getDeclaredConstructor(NetconfOperationService.class, moduleClass, monitoringWriterCls);
            declaredConstructor.setAccessible(true);
            // The second argument is null, it should be the parent cfg-subsystem module class instance, that we dont have
            // it's used only during close so dont close the factory using its close() method
            final NetconfOperationServiceFactory mdSalMonitoringMapperFactory =
                    (NetconfOperationServiceFactory) declaredConstructor.newInstance(mdSalMonitoringMapper, null, writer);
            aggregator.onAddNetconfOperationServiceFactory(mdSalMonitoringMapperFactory);
            return mdSalMonitoringMapperFactory;
        } catch (final ReflectiveOperationException e) {
            final String msg = "Unable to instantiate operation service factory using reflection";
            LOG.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }
}
