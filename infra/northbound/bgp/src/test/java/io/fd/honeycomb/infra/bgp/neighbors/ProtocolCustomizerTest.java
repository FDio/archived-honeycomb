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

package io.fd.honeycomb.infra.bgp.neighbors;

import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.Protocol;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.network.instance.rev151018.network.instance.top.network.instances.network.instance.protocols.ProtocolBuilder;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.policy.types.rev151009.BGP;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ProtocolCustomizerTest {
    private static final String INSTANCE_NAME = "test-instance";
    private static final InstanceIdentifier<Protocol> ID = InstanceIdentifier.create(Protocol.class);

    @Mock
    private WriteContext ctx;

    private ProtocolCustomizer customizer;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        customizer = new ProtocolCustomizer(INSTANCE_NAME);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        final Protocol protocol = new ProtocolBuilder().setName(INSTANCE_NAME).setIdentifier(BGP.class).build();
        customizer.writeCurrentAttributes(ID, protocol, ctx);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteInvalidProtocolName() throws WriteFailedException {
        final Protocol protocol = new ProtocolBuilder().setName("some-other-name").setIdentifier(BGP.class).build();
        customizer.writeCurrentAttributes(ID, protocol, ctx);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteInvalidProtocolType() throws WriteFailedException {
        final Protocol protocol = new ProtocolBuilder().setName(INSTANCE_NAME).build();
        customizer.writeCurrentAttributes(ID, protocol, ctx);
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdate() throws WriteFailedException {
        customizer.updateCurrentAttributes(ID, mock(Protocol.class), mock(Protocol.class), ctx);
    }

    @Test(expected = WriteFailedException.DeleteFailedException.class)
    public void testDelete() throws WriteFailedException {
        customizer.deleteCurrentAttributes(ID, mock(Protocol.class), ctx);
    }
}