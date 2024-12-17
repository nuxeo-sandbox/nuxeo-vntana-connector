package org.nuxeo.labs.vntana.adapter;

import jakarta.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.labs.vntana.nuxeo-vntana-connector-core" })
public class TestVntanaAdapter {
    @Inject
    CoreSession session;

    @Test
    public void shouldCallTheAdapter() {
        DocumentModel doc = session.createDocumentModel("/", "File", "File");
        doc.addFacet("Vntana");
        VntanaAdapter adapter = doc.getAdapter(VntanaAdapter.class);
        Assert.assertNotNull(adapter);
        adapter.setOrganizationUUID("orgUUID");
        Assert.assertEquals("orgUUID",adapter.getOrganizationUUID());
        adapter.setOrganizationSlug("orgSlug");
        Assert.assertEquals("orgSlug",adapter.getOrganizationSlug());
        adapter.setClientUUID("clientUUID");
        Assert.assertEquals("clientUUID",adapter.getClientUUID());
        adapter.setClientSlug("clientSlug");
        Assert.assertEquals("clientSlug",adapter.getClientSlug());
        adapter.setProductUUID("productUUID");
        Assert.assertEquals("productUUID",adapter.getProductUUID());
        adapter.setStatus("status");
        Assert.assertEquals("status",adapter.getStatus());
    }
}
