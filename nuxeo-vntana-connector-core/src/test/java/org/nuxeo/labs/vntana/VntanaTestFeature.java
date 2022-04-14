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

package org.nuxeo.labs.vntana;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.labs.vntana.adapter.VntanaAdapter;
import org.nuxeo.labs.vntana.adapter.VntanaProductReference;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

import java.io.Serializable;

import static org.nuxeo.labs.vntana.adapter.VntanaAdapter.VNTANA_FACET;
import static org.nuxeo.labs.vntana.service.VntanaServiceImpl.VNTANA_API_TOKEN;
import static org.nuxeo.labs.vntana.service.VntanaServiceImpl.VNTANA_DEFAULT_CLIENT_UUID;
import static org.nuxeo.labs.vntana.service.VntanaServiceImpl.VNTANA_DEFAULT_ORGANIZATION_UUID;

@Features({AutomationFeature.class })
@Deploy({
        "org.nuxeo.labs.vntana.nuxeo-vntana-connector-core",
        "org.nuxeo.ecm.platform.rendition.api",
        "org.nuxeo.ecm.actions"
})
public class VntanaTestFeature implements RunnerFeature {

    @Override
    public void beforeRun(FeaturesRunner runner) {
        Framework.getProperties().setProperty(VNTANA_API_TOKEN,System.getProperty("vntanaApiKey"));
        Framework.getProperties().setProperty(VNTANA_DEFAULT_ORGANIZATION_UUID,getDefaultOrg());
        Framework.getProperties().setProperty(VNTANA_DEFAULT_CLIENT_UUID,getDefaultClient());
    }

    public DocumentModel getTestDocument(CoreSession session) {
        DocumentModel model = session.createDocumentModel(session.getRootDocument().getPathAsString(),"File","File");
        model.setPropertyValue("dc:title","Test From Nuxeo");
        Blob glbBlob = new FileBlob(FileUtils.getResourceFileFromContext("files/box.glb"),"model/gltf-binary");
        model.setPropertyValue("file:content", (Serializable) glbBlob);
        return session.createDocument(model);
    }

    public DocumentModel getDefaultProductAsDocument(CoreSession session) {
        DocumentModel model = session.createDocumentModel(session.getRootDocument().getPathAsString(),"File","File");
        model.setPropertyValue("dc:title","Test From Nuxeo");
        Blob glbBlob = new FileBlob(FileUtils.getResourceFileFromContext("files/box.glb"),"model/gltf-binary");
        model.setPropertyValue("file:content", (Serializable) glbBlob);
        model.addFacet(VNTANA_FACET);
        VntanaAdapter adapter = model.getAdapter(VntanaAdapter.class);
        adapter.setOrganizationUUID(getDefaultOrg()).setClientUUID(getDefaultClient()).setProductUUID(getDefaultProduct());
        return session.createDocument(model);
    }

    public VntanaProductReference getDefaultProductAsRef() {
        return new VntanaProductReference() {
            @Override
            public String getOrganizationUUID() {
                return getDefaultOrg();
            }

            @Override
            public String getClientUUID() {
                return getDefaultClient();
            }

            @Override
            public String getProductUUID() {
                return getDefaultProduct();
            }
        };
    }

    public String getDefaultOrg() {
        return System.getProperty("vntanaOrganizationUUID");
    }

    public String getDefaultClient() {
        return System.getProperty("vntanaClientUUID");
    }

    public String getDefaultProduct() {
        return System.getProperty("vntanaProductUUID");
    }

}
