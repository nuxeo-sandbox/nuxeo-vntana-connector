package org.nuxeo.labs.vntana.automation;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.labs.vntana.service.VntanaService;

/**
 *
 */
@Operation(id = VntanaDownloadModelThumbnail.ID, category = VntanaAutomation.CAT, label = "Vntana Download Model Thumbnail", description = "Download a 3D model Thumbnail from Vntana")
public class VntanaDownloadModelThumbnail {

    public static final String ID = VntanaAutomation.CAT + ".DownloadThumbnail";

    @Context
    protected VntanaService vntanaService;

    @OperationMethod
    public Blob run(DocumentModel input) {
        return vntanaService.thumbnail(input);
    }
}
