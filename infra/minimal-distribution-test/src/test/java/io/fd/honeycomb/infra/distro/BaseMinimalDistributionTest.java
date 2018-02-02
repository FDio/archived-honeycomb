/*
 * Copyright (c) 2016, 2017 Cisco and/or its affiliates.
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

package io.fd.honeycomb.infra.distro;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSubsystem;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.fd.honeycomb.infra.distro.activation.ActivationModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseMinimalDistributionTest {

    private static final Logger LOG = LoggerFactory.getLogger(BaseMinimalDistributionTest.class);

    private static final int HTTP_PORT = 8182;
    private static final int HTTPS_PORT = 8444;
    private static final String UNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String CERT_PASSWORD = "testing";
    private static final int NETCONF_TCP_PORT = 7778;
    private static final int NETCONF_SSH_PORT = 2832;
    private static final String NETCONF_NAMESPACE = "urn:ietf:params:xml:ns:netconf:base:1.0";
    private static final int HELLO_WAIT = 2500;

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

    /**
     * Start base distribution and check all northbound interfaces
     */
    @Test(timeout = 180000)
    public void test() throws Exception {
        Main.init(new ActivationModule());

        LOG.info("Testing Honeycomb base distribution");
        LOG.info("Testing NETCONF TCP");
        assertNetconfTcp();
        LOG.info("Testing NETCONF SSH");
        assertNetconfSsh();
        LOG.info("Testing RESTCONF HTTP");
        assertRestconfHttp();
        LOG.info("Testing RESTCONF HTTPS");
        assertRestconfHttps();
    }

    private void assertNetconfTcp() throws Exception {
        try (final Socket localhost = new Socket("127.0.0.1", NETCONF_TCP_PORT);
             final InputStream inputStream = localhost.getInputStream()) {
            // Wait until hello msg is sent from server
            Thread.sleep(HELLO_WAIT);
            final String helloMessage = inputStreamToString(inputStream);

            LOG.info("NETCONF TCP sent hello: {}", helloMessage);

            assertThat(helloMessage, containsString("hello"));
            assertThat(helloMessage, containsString(NETCONF_NAMESPACE));
        }
    }

    private byte[] readMessage(final InputStream inputStream) throws IOException {
        final int available = inputStream.available();
        final byte[] msg = new byte[available];
        ByteStreams.read(inputStream, msg, 0, available);
        return msg;
    }
    private String inputStreamToString(final InputStream inputStream) throws IOException {
        return new String(readMessage(inputStream), Charsets.UTF_8);
    }

    private void assertNetconfSsh() throws Exception {
        JSch jsch = new JSch();
        final Session session = jsch.getSession(UNAME, "127.0.0.1", NETCONF_SSH_PORT);
        session.setPassword(PASSWORD);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(60000);

        Channel channel = session.openChannel("subsystem");

        ((ChannelSubsystem) channel).setSubsystem("netconf");
        ((ChannelSubsystem) channel).setPty(true);
        final InputStream inputStream = channel.getInputStream();
        channel.connect(60000);

        // Wait until hello msg is sent from server
        Thread.sleep(HELLO_WAIT);
        final String helloMessage = inputStreamToString(inputStream);
        LOG.info("NETCONF SSH sent hello: {}", helloMessage);

        assertThat(helloMessage, containsString("hello"));
        assertThat(helloMessage, containsString(NETCONF_NAMESPACE));

        channel.disconnect();
        session.disconnect();
    }

    private void assertRestconfHttp() throws Exception {
        final String url =
            "http://127.0.0.1:" + HTTP_PORT + "/restconf/operational/ietf-netconf-monitoring:netconf-state";
        LOG.info("RESTCONF HTTP GET to {}", url);
        final HttpResponse<String> jsonNodeHttpResponse = Unirest.get(url)
            .basicAuth(UNAME, PASSWORD)
            .asString();
        LOG.info("RESTCONF HTTP GET to {}, status: {}, data: {}",
            url, jsonNodeHttpResponse.getStatus(), jsonNodeHttpResponse.getBody());

        assertSuccessStatus(jsonNodeHttpResponse);
        assertSuccessResponseForNetconfMonitoring(jsonNodeHttpResponse);
    }

    private void assertRestconfHttps() throws Exception {
        final String url =
            "https://127.0.0.1:" + HTTPS_PORT + "/restconf/operational/ietf-netconf-monitoring:netconf-state";
        LOG.info("RESTCONF HTTPS GET to {}", url);
        final HttpResponse<String> jsonNodeHttpResponse = Unirest.get(url)
            .basicAuth(UNAME, PASSWORD)
            .asString();
        LOG.info("RESTCONF HTTPS GET to {}, status: {}, data: {}",
            url, jsonNodeHttpResponse.getStatus(), jsonNodeHttpResponse.getBody());

        assertSuccessStatus(jsonNodeHttpResponse);
        assertSuccessResponseForNetconfMonitoring(jsonNodeHttpResponse);
    }

    private void assertSuccessResponseForNetconfMonitoring(final HttpResponse<String> jsonNodeHttpResponse) {
        assertThat(jsonNodeHttpResponse.getBody(), containsString("schemas"));
        assertThat(jsonNodeHttpResponse.getBody(), containsString(NETCONF_NAMESPACE));
    }

    private void assertSuccessStatus(final HttpResponse<String> jsonNodeHttpResponse) {
        final int statusCode = jsonNodeHttpResponse.getStatus();
        assertTrue("Expected HTTP status code in range [200, 400), but was: " + statusCode,
            statusCode >= 200 && statusCode < 400);
    }
}