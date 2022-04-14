package org.nuxeo.labs.vntana.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.labs.vntana.client.ApiClient;
import org.nuxeo.labs.vntana.client.model.GetUserClientOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.GetUserOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.ProductGetResponseModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.nuxeo.labs.vntana.service.VntanaServiceImpl.VNTANA_API_TOKEN;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("org.nuxeo.labs.vntana.nuxeo-vntana-connector-core")
public class TestVntanaService {

    public static final String API_KEY = "";
    @Inject
    protected CoreSession session;

    @Inject
    protected VntanaService vntanaservice;

    @Test
    public void testService() {
        assertNotNull(vntanaservice);
    }

    @Test
    public void testGetClient() {
        Framework.getProperties().put(VNTANA_API_TOKEN,API_KEY);
        ApiClient client = vntanaservice.getClient();
        Assert.assertNotNull(client);
    }

    @Test
    public void testGetOrganizations() {
        Framework.getProperties().put(VNTANA_API_TOKEN,API_KEY);
        List<GetUserOrganizationsResponseModel> organizations = vntanaservice.getOrganizations();
        Assert.assertNotNull(organizations);
    }

    @Test
    public void testGetClients() {
        Framework.getProperties().put(VNTANA_API_TOKEN,API_KEY);
        List<GetUserClientOrganizationsResponseModel> clients = vntanaservice.getClients("ac8ff66b-5a76-46f9-926a-258453c36915");
        Assert.assertNotNull(clients);
    }

    @Test
    public void testGetProduct() {
        Framework.getProperties().put(VNTANA_API_TOKEN,API_KEY);
        ProductGetResponseModel product = vntanaservice.getProduct(
                "ac8ff66b-5a76-46f9-926a-258453c36915",
                "bfe343c8-42fb-4a0d-b210-ba7fa78a2af7",
                "6fb85641-1216-433d-a8bb-7bfe26cb74c0"
                );
        Assert.assertNotNull(product);
    }

    @Test
    public void testPublishModel() {
        Framework.getProperties().put(VNTANA_API_TOKEN, API_KEY);
        DocumentModel model = session.createDocumentModel(session.getRootDocument().getPathAsString(),"File","File");
        model.setPropertyValue("dc:title","testfromnuxeo");
        model = session.createDocument(model);
        vntanaservice.publishModel(
                model,
                "ac8ff66b-5a76-46f9-926a-258453c36915",
                "bfe343c8-42fb-4a0d-b210-ba7fa78a2af7"
        );
        Assert.assertTrue(model.hasFacet("Vntana"));


    }

}
