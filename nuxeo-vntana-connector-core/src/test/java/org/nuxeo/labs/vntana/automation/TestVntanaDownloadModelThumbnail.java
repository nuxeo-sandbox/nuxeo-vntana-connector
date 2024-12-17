package org.nuxeo.labs.vntana.automation;

import jakarta.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.labs.vntana.VntanaTestFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(VntanaTestFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestVntanaDownloadModelThumbnail {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Inject
    protected VntanaTestFeature vntanaTestFeature;

    @Test
    public void shouldCallTheOperation() throws OperationException {
        DocumentModel model = vntanaTestFeature.getDefaultProductAsDocument(session);
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(model);
        Blob blob = (Blob) automationService.run(ctx, VntanaDownloadModelThumbnail.ID);
        Assert.assertNotNull(blob);
        Assert.assertEquals("image/png",blob.getMimeType());
        Assert.assertTrue(blob.getLength() > 0);
    }
}
