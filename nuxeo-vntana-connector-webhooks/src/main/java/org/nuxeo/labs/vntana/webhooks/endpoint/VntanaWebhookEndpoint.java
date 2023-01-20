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

package org.nuxeo.labs.vntana.webhooks.endpoint;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import static java.util.Collections.singletonMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.buf.HexUtils;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nuxeo.labs.vntana.webhooks.events.VntanaEvent;
import org.nuxeo.runtime.api.Framework;

/**
 * WebEngine module to handle the Vntana webhooks
 */
@Path("/vntana")
@WebObject(type = "vntana")
@Consumes({ MediaType.APPLICATION_JSON })
public class VntanaWebhookEndpoint extends ModuleRoot {

    private static final Logger log = LogManager.getLogger(VntanaWebhookEndpoint.class);

    // The Signature and Timestamp will be used to verify the request was made by VNTANA
    public static final String HEADER_VNTANA_SIGNATURE = "X-VNTANA-SIGNATURE";
    public static final String HEADER_TIMESTAMP = "X-TIMESTAMP";

    public static final String VNTANA_WEBHOOK_SECRET_PROPERTY = "vntana.webhook.secret";

    public static final String ALGORITHM = "HmacSHA256";

    public static final String VNTANA_EVENT = "vntanaEvent";

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    @Path("/event")
    @POST
    public Object doPost(@Context HttpServletRequest request) {
        try {
            String requestBody = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
            // Verify the Signature was sent by VNTANA, return Forbidden response if not
            String timestamp = request.getHeader(HEADER_TIMESTAMP);
            String signature = request.getHeader(HEADER_VNTANA_SIGNATURE);
            String secret = Framework.getProperty(VNTANA_WEBHOOK_SECRET_PROPERTY);
            if (StringUtils.isBlank(secret)) {
                log.debug("Vntana webhook secret is not set");
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            } else if(!isValidSignature(timestamp, requestBody, secret, signature)){
                log.debug("Wrong signature from Vnatana");
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            VntanaEvent event = objectMapper.readValue(requestBody, VntanaEvent.class);
            EventContextImpl ctx = new EventContextImpl();
            Map<String, Serializable> props = singletonMap(VNTANA_EVENT,event);
            ctx.setProperties(props);
            EventService es = Framework.getService(EventService.class);
            es.fireEvent(VNTANA_EVENT, ctx);
        // Handle errors and send correct server responses
        } catch (JsonProcessingException e) {
            log.error("Error processing the event",e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }catch (Exception e){
            log.error("Error processing the event",e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        // If no errors, return OK response
        return Response.status(Response.Status.OK).build();
    }

    // Method to verify the Signature
    public Boolean isValidSignature(String timestamp, String payload, String secret, String signature) throws Exception{
        // The signature is a hashed message consisting of a Timestamp + ‘#’ + the Payload
        String calcSignature = computeSignature(secret, timestamp, payload);
        return calcSignature.equals(signature);
    }
    // Hashing method for reconstructing the Signature
    public static String computeSignature(String secret, String timestamp, String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        String data = String.join("#",timestamp, payload);
        Mac sha256_HMAC = Mac.getInstance(ALGORITHM);
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        sha256_HMAC.init(secret_key);
        return HexUtils.toHexString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

}


