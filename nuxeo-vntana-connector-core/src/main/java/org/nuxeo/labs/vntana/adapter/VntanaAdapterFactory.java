package org.nuxeo.labs.vntana.adapter;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

import static org.nuxeo.labs.vntana.adapter.VntanaAdapter.VNTANA_FACET;

public class VntanaAdapterFactory implements DocumentAdapterFactory {

    @Override
    public Object getAdapter(DocumentModel doc, Class<?> itf) {
        if (doc.hasFacet(VNTANA_FACET)) {
            return new VntanaAdapter(doc);
        } else {
            return null;
        }
    }
}
