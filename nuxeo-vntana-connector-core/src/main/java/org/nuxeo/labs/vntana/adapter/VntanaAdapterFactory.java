package org.nuxeo.labs.vntana.adapter;

import static org.nuxeo.labs.vntana.adapter.VntanaAdapter.VNTANA_FACET;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.adapter.DocumentAdapterFactory;

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
