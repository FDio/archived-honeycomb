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
import io.fd.honeycomb.impl.NetconfMonitoringReaderFactory;
import io.fd.honeycomb.infra.distro.ProviderTrait;
import io.fd.honeycomb.translate.read.ReaderFactory;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;


public final class NetconfMonitoringReaderFactoryProvider extends ProviderTrait<ReaderFactory> {

    @Inject
    @Named(NetconfModule.HONEYCOMB_NETCONF)
    private DataBroker netconfDataBroker;

    @Override
    protected NetconfMonitoringReaderFactory create() {
        return new NetconfMonitoringReaderFactory(netconfDataBroker);
    }
}
