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

package io.fd.honeycomb.infra.distro.netconf

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.inject.Inject
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import io.fd.honeycomb.infra.distro.ProviderTrait
import io.fd.honeycomb.infra.distro.cfgattrs.HoneycombConfiguration
import io.netty.channel.nio.NioEventLoopGroup

@Slf4j
@ToString
class NettyThreadGroupProvider extends ProviderTrait<NioEventLoopGroup> {

    @Inject
    HoneycombConfiguration cfgAttributes

    @Override
    def create() {
        new NioEventLoopGroup(cfgAttributes.netconfNettyThreads,
            new ThreadFactoryBuilder().setNameFormat("netconf-netty-%d").build())
    }
}
