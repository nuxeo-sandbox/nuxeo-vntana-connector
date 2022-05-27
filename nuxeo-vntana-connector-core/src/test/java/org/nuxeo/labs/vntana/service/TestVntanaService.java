package org.nuxeo.labs.vntana.service;

import static org.junit.Assert.assertNotNull;
import static org.nuxeo.labs.vntana.adapter.VntanaAdapter.VNTANA_FACET;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.labs.vntana.VntanaTestFeature;
import org.nuxeo.labs.vntana.adapter.VntanaAdapter;
import org.nuxeo.labs.vntana.client.ApiClient;
import org.nuxeo.labs.vntana.client.model.GetClientOrganizationResponseModel;
import org.nuxeo.labs.vntana.client.model.GetOrganizationByUuidResponseModel;
import org.nuxeo.labs.vntana.client.model.GetUserClientOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.GetUserOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.Model;
import org.nuxeo.labs.vntana.client.model.ModelOpsParameters;
import org.nuxeo.labs.vntana.client.model.ProductGetResponseModel;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ VntanaTestFeature.class })

public class TestVntanaService {

    @Inject
    protected CoreSession session;

    @Inject
    protected VntanaService vntanaservice;

    @Inject
    protected VntanaTestFeature vntanaTestFeature;

    @Test
    public void testService() {
        assertNotNull(vntanaservice);
    }

    @Test
    public void testGetApiClient() {
        ApiClient client = vntanaservice.getApiClient();
        Assert.assertNotNull(client);
    }

    @Test
    public void testGetOrganizations() {
        List<GetUserOrganizationsResponseModel> organizations = vntanaservice.getOrganizations();
        Assert.assertNotNull(organizations);
    }

    @Test
    public void testGetOrganization() {
        GetOrganizationByUuidResponseModel organization = vntanaservice.getOrganization(vntanaTestFeature.getDefaultOrg());
        Assert.assertEquals(vntanaTestFeature.getDefaultOrg(),organization.getUuid());
    }

    @Test
    public void testGetClients() {
        List<GetUserClientOrganizationsResponseModel> clients = vntanaservice.getClients(
                vntanaTestFeature.getDefaultOrg());
        Assert.assertNotNull(clients);
    }

    @Test
    public void testGetClient() {
        GetClientOrganizationResponseModel client = vntanaservice.getClient(
                vntanaTestFeature.getDefaultOrg(), vntanaTestFeature.getDefaultClient());
        Assert.assertEquals(vntanaTestFeature.getDefaultClient(),client.getClientUuid());
    }

    @Test
    public void testGetProduct() {
        ProductGetResponseModel product = vntanaservice.getProduct(vntanaTestFeature.getDefaultProductAsRef());
        Assert.assertNotNull(product);
    }

    @Test
    public void testDocumentIsSupported() {
        DocumentModel model = vntanaTestFeature.getTestDocument(session);
        Assert.assertTrue(vntanaservice.documentIsSupported(model));
    }

    @Test
    public void testPublishModel() {
        DocumentModel model = vntanaTestFeature.getTestDocument(session);
        vntanaservice.publishModel(model);
        VntanaAdapter adapter = model.getAdapter(VntanaAdapter.class);
        Assert.assertNotNull(adapter);
        Assert.assertTrue(adapter.isUploaded());
    }

    @Test
    public void testUnpublishModel() {
        String pipelineUUID = vntanaservice.getPipelineUUID(vntanaTestFeature.getDefaultOrg(), "Convert Only");
        String productUUID = vntanaservice.createProduct(
                "TestDelete", vntanaTestFeature.getDefaultOrg(),
                vntanaTestFeature.getDefaultClient(), pipelineUUID,
                new ModelOpsParameters(), new HashMap<>()
        ).getUuid();

        DocumentModel model = vntanaTestFeature.getTestDocument(session);
        model.addFacet(VNTANA_FACET);
        VntanaAdapter adapter = model.getAdapter(VntanaAdapter.class);
        adapter.setOrganizationUUID(vntanaTestFeature.getDefaultOrg())
               .setClientUUID(vntanaTestFeature.getDefaultClient())
               .setProductUUID(productUUID);

        model = vntanaservice.unpublishModel(model);
        Assert.assertFalse(model.hasFacet(VNTANA_FACET));
    }

    @Test
    public void testDownloadModel() {
        DocumentModel model = vntanaTestFeature.getDefaultProductAsDocument(session);
        Blob blob = vntanaservice.download(model, Model.ConversionFormatEnum.GLB);
        Assert.assertNotNull(blob);
        Assert.assertEquals("model/gltf-binary",blob.getMimeType());
        Assert.assertEquals("box.glb",blob.getFilename());
        Assert.assertTrue(blob.getLength() > 0);
    }

    @Test
    public void testDownloadModelThumbnail() {
        DocumentModel model = vntanaTestFeature.getDefaultProductAsDocument(session);
        Blob blob = vntanaservice.thumbnail(model);
        Assert.assertNotNull(blob);
        Assert.assertEquals("image/png",blob.getMimeType());
        Assert.assertNotNull(blob.getFilename());
        Assert.assertTrue(blob.getLength() > 0);
    }

}
