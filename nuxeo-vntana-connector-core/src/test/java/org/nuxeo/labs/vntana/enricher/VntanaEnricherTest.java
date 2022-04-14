package org.nuxeo.labs.vntana.enricher;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonWriter;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.labs.vntana.VntanaTestFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ VntanaTestFeature.class })
public class VntanaEnricherTest extends AbstractJsonWriterTest.Local<DocumentModelJsonWriter, DocumentModel> {

    public VntanaEnricherTest() {
        super(DocumentModelJsonWriter.class, DocumentModel.class);
    }

    @Inject
    private CoreSession session;

    @Inject
    protected VntanaTestFeature vntanaTestFeature;

    @Test
    public void test() throws Exception {
        DocumentModel obj = vntanaTestFeature.getTestDocument(session);
        JsonAssert json = jsonAssert(obj, CtxBuilder.enrich("document", VntanaEnricher.NAME).get());
        json = json.has("contextParameters").isObject();
        json.properties(1);
        json.has(VntanaEnricher.NAME).isObject();
    }
}
