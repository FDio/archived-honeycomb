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

package io.fd.honeycomb.infra.bgp.distro;

import static org.junit.Assert.assertTrue;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.fd.honeycomb.infra.distro.Main;
import io.fd.honeycomb.infra.distro.activation.ActivationModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private static final String BGP_HOST_ADDRESS = "127.0.0.1";
    private static final int HTTP_PORT = 8182;
    private static final String UNAME = "admin";
    private static final String PASSWORD = "admin";

    private static final Logger LOG = LoggerFactory.getLogger(BgpDistributionTest.class);
    private static final String CERT_PASSWORD = "testing";
    private static final int HELLO_WAIT = 2500;

    private static final int BGP_MSG_TYPE_OFFSET = 18; // 16 (MARKER) + 2 (LENGTH);
    private static final byte BGP_OPEN_MSG_TYPE = 1;
    private static final int BGP_PORT = 1790;

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
        Main.init(new ActivationModule());
        LOG.info("Testing Honeycomb BGP distribution");
        assertBgp();
    }

    private void assertBgp() throws Exception {
        // Wait until BGP server is started
        Thread.sleep(HELLO_WAIT);

        configureBgpPeers();

        assertBgpOpenIsSent("127.0.0.2");
        assertBgpOpenIsSent("127.0.0.3");
    }

    private void configureBgpPeers() throws UnirestException, IOException {
        final String url =
            "http://" + BGP_HOST_ADDRESS + ":" + HTTP_PORT
                + "/restconf/config/openconfig-network-instance:network-instances/network-instance/global-bgp/"
                + "openconfig-network-instance:protocols/protocol/openconfig-policy-types:BGP/hc-bgp-instance/"
                + "bgp/bgp-openconfig-extensions:neighbors";

        final String request =
            new String(Files.readAllBytes(Paths.get("src/test/resources/bgp-peers.json")), Charsets.UTF_8);
        final HttpResponse<String> response =
            Unirest.put(url)
                .basicAuth(UNAME, PASSWORD)
                .header("Content-Type", "application/json")
                .body(request)
                .asString();

        assertSuccessStatus(response);
    }

    private void assertBgpOpenIsSent(final String peerAddress) throws IOException, InterruptedException {
        final InetAddress bgpHost = InetAddress.getByName(BGP_HOST_ADDRESS);
        final InetAddress bgpPeerAddress = InetAddress.getByName(peerAddress);
        try (final Socket localhost = new Socket(bgpHost, BGP_PORT, bgpPeerAddress, 0);
             final InputStream inputStream = localhost.getInputStream()) {
            // Wait until bgp message is sent
            Thread.sleep(HELLO_WAIT);

            final byte[] msg = readMessage(inputStream);
            LOG.info("Received BGP message: {}", msg);

            Assert.assertEquals(BGP_OPEN_MSG_TYPE, msg[BGP_MSG_TYPE_OFFSET]);
        }
    }

    private byte[] readMessage(final InputStream inputStream) throws IOException {
        final int available = inputStream.available();
        final byte[] msg = new byte[available];
        ByteStreams.read(inputStream, msg, 0, available);
        return msg;
    }

    private void assertSuccessStatus(final HttpResponse<String> jsonNodeHttpResponse) {
        assertTrue(jsonNodeHttpResponse.getStatus() >= 200);
        assertTrue(jsonNodeHttpResponse.getStatus() < 400);
    }
}