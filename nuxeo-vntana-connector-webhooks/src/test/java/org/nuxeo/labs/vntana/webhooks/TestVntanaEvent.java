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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.labs.vntana.webhooks.events.VntanaEvent;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestVntanaEvent {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testParsing() throws IOException {
        File src = FileUtils.getResourceFileFromContext("files/sample_event.json");
        String json;
        try (InputStream in = new FileInputStream(src)) {
            json = IOUtils.toString(in, StandardCharsets.UTF_8);
        }
        VntanaEvent event = objectMapper.readValue(json, VntanaEvent.class);
        Assert.assertNotNull(event);
        Assert.assertEquals("product.added",event.getEvent());
        VntanaEvent.Product product = event.getProduct();
        Assert.assertNotNull(product);
        Assert.assertEquals("1234",product.getUuid());
        Assert.assertEquals("TestWebhooks",product.getName());
        Assert.assertEquals("DRAFT",product.getStatus());
    }

}
