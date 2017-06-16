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

package io.fd.honeycomb.infra.bgp.distro;

import static com.google.common.collect.ImmutableSet.of;

import com.google.common.io.ByteStreams;
import com.google.inject.Module;
import com.mashape.unirest.http.Unirest;
import io.fd.honeycomb.infra.bgp.BgpConfigurationModule;
import io.fd.honeycomb.infra.bgp.BgpExtensionsModule;
import io.fd.honeycomb.infra.bgp.BgpModule;
import io.fd.honeycomb.infra.bgp.BgpReadersModule;
import io.fd.honeycomb.infra.bgp.BgpWritersModule;
import io.fd.honeycomb.infra.distro.cfgattrs.CfgAttrsModule;
import io.fd.honeycomb.infra.distro.data.ConfigAndOperationalPipelineModule;
import io.fd.honeycomb.infra.distro.data.context.ContextPipelineModule;
import io.fd.honeycomb.infra.distro.initializer.InitializerPipelineModule;
import io.fd.honeycomb.infra.distro.netconf.NetconfModule;
import io.fd.honeycomb.infra.distro.netconf.NetconfReadersModule;
import io.fd.honeycomb.infra.distro.restconf.RestconfModule;
import io.fd.honeycomb.infra.distro.schema.SchemaModule;
import io.fd.honeycomb.infra.distro.schema.YangBindingProviderModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Set;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BgpDistributionTest {

    private static final Logger LOG = LoggerFactory.getLogger(BgpDistributionTest.class);
    private static final String CERT_PASSWORD = "testing";
    private static final int HELLO_WAIT = 2500;

    private static final int BGP_MSG_TYPE_OFFSET = 18; // 16 (MARKER) + 2 (LENGTH);
    private static final byte BGP_OPEN_MSG_TYPE = 1;
    private static final int BGP_PORT = 1790;

    public static final Set<Module> BASE_MODULES = of(
            new YangBindingProviderModule(),
            new SchemaModule(),
            new ConfigAndOperationalPipelineModule(),
            new ContextPipelineModule(),
            new InitializerPipelineModule(),
            new NetconfModule(),
            new NetconfReadersModule(),
            new RestconfModule(),
            new CfgAttrsModule(),
            new BgpModule(),
            new BgpExtensionsModule(),
            new BgpReadersModule(),
            new BgpWritersModule(),
            new BgpConfigurationModule());

    @Before
    public void setUp() throws Exception {
        SSLContext sslcontext = SSLContexts.custom()
            .loadTrustMaterial(getClass().getResource("/honeycomb-keystore"),
                CERT_PASSWORD.toCharArray(), new TrustSelfSignedStrategy())
            .build();

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext);
        CloseableHttpClient httpclient = HttpClients.custom()
            .setSSLSocketFactory(sslsf)
            .build();
        Unirest.setHttpClient(httpclient);
    }

    @Test(timeout = 120000)
    public void test() throws Exception {
        io.fd.honeycomb.infra.bgp.distro.Main.init(BASE_MODULES);
        LOG.info("Testing Honeycomb BGP distribution");
        assertBgp();
    }

    private byte[] readMessage(final InputStream inputStream) throws IOException {
        final int available = inputStream.available();
        final byte[] msg = new byte[available];
        ByteStreams.read(inputStream, msg, 0, available);
        return msg;
    }

    private void assertBgp() throws Exception {
        // Wait until BGP server is started
        Thread.sleep(HELLO_WAIT);
        final InetAddress bgpHost = InetAddress.getByName("127.0.0.1");
        final InetAddress bgpPeerAddress = InetAddress.getByName("127.0.0.2");
        try (final Socket localhost = new Socket(bgpHost, BGP_PORT, bgpPeerAddress, 0);
             final InputStream inputStream = localhost.getInputStream()) {
            // Wait until bgp message is sent
            Thread.sleep(HELLO_WAIT);

            final byte[] msg = readMessage(inputStream);
            LOG.info("Received BGP message: {}", msg);

            Assert.assertEquals(BGP_OPEN_MSG_TYPE, msg[BGP_MSG_TYPE_OFFSET]);
        }
    }
}