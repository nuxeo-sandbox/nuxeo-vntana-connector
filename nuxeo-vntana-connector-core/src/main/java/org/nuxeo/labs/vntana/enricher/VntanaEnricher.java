package org.nuxeo.labs.vntana.enricher;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.labs.vntana.service.VntanaService;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Enrich {@link nuxeo.ecm.core.api.DocumentModel} Json.
 * <p>
 * Format is:
 * </p>
 * 
 * <pre>
 * {@code
 * {
 *   ...
 *   "contextParameters": {
*     "vntana": { ... }
 *   }
 * }}
 * </pre>
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class VntanaEnricher extends AbstractJsonEnricher<DocumentModel> {

    public static final String NAME = "vntana";

    public VntanaEnricher() {
        super(NAME);
    }

    @Override
    public void write(JsonGenerator jg, DocumentModel obj) throws IOException {
        // How to instanciate a Session if `obj` is a DocumentModel
        // try (SessionWrapper wrapper = ctx.getSession(obj)) {
        // CoreSession session = wrapper.getSession();
        // ...
        // }
        try (RenderingContext.SessionWrapper wrapper = ctx.getSession(obj)) {
            if (!wrapper.getSession().exists(obj.getRef())) {
                return;
            }
            VntanaService service = Framework.getService(VntanaService.class);
            jg.writeFieldName(NAME);
            jg.writeStartObject();
            jg.writeBooleanField("isSupported", service.documentIsSupported(obj));
            jg.writeEndObject();
        }
    }
}
