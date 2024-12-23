/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Michael Vachette
 */

package org.nuxeo.labs.vntana.webhooks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.http.test.CloseableHttpResponse;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.labs.vntana.webhooks.endpoint.VntanaWebhookEndpoint;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.labs.vntana.webhooks.endpoint.VntanaWebhookEndpoint.HEADER_TIMESTAMP;
import static org.nuxeo.labs.vntana.webhooks.endpoint.VntanaWebhookEndpoint.HEADER_VNTANA_SIGNATURE;
import static org.nuxeo.labs.vntana.webhooks.endpoint.VntanaWebhookEndpoint.VNTANA_EVENT;
import static org.nuxeo.labs.vntana.webhooks.endpoint.VntanaWebhookEndpoint.VNTANA_WEBHOOK_SECRET_PROPERTY;

@RunWith(FeaturesRunner.class)
@Features({ WebEngineFeature.class, CoreFeature.class })
@Deploy("org.nuxeo.labs.vntana.nuxeo-vntana-connector-webhooks")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestWebhookEndpoint {

    private static final String CONTENT_TYPE = "application/json";

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.builder()
            .url(() -> servletContainerFeature.getHttpUrl() + "/vntana/event")
            .build();

    @Test
    public void shouldFireEvent() throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        Framework.getProperties().setProperty(VNTANA_WEBHOOK_SECRET_PROPERTY,"the-secret");

        try (CapturingEventListener listener = new CapturingEventListener(VNTANA_EVENT)) {
            ArrayNode arrayNode = new ArrayNode(JsonNodeFactory.instance);
            arrayNode.add(new ObjectNode(JsonNodeFactory.instance));

            File jsonPayload = FileUtils.getResourceFileFromContext("files/sample_event.json");
            byte[] jsonData = Files.readAllBytes(Paths.get(jsonPayload.toURI()));
            String jsonPost = new String(jsonData, StandardCharsets.UTF_8);

            String timestamp = "123";
            String secret = Framework.getProperty(VNTANA_WEBHOOK_SECRET_PROPERTY);
            String signature = VntanaWebhookEndpoint.computeSignature(secret, timestamp, jsonPost);

            HttpClientTestRule.RequestBuilder request = httpClient.buildPostRequest("")
                    .contentType(CONTENT_TYPE).addHeader(HEADER_TIMESTAMP, timestamp)
                    .addHeader(HEADER_VNTANA_SIGNATURE, signature).entity(jsonPost);

            try (CloseableHttpResponse response = request.execute()) {
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
                assertEquals(1, listener.getCapturedEventCount(VNTANA_EVENT));
            }
        }
    }

    public void noSecretIsInternalError() {
        HttpClientTestRule.RequestBuilder request = httpClient.buildPostRequest("")
                .contentType(CONTENT_TYPE).entity("{}");
        try (CloseableHttpResponse response = request.execute()) {
            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        }
    }

    public void wrongSignatureIsUnauthorized() {
        Framework.getProperties().setProperty(VNTANA_WEBHOOK_SECRET_PROPERTY,"the-secret");

        HttpClientTestRule.RequestBuilder request = httpClient.buildPostRequest("")
                .contentType(CONTENT_TYPE).addHeader(HEADER_TIMESTAMP, "123")
                .addHeader(HEADER_VNTANA_SIGNATURE, "wrong").entity("{}");
        try (CloseableHttpResponse response = request.execute()) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        }
    }

}
