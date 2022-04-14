package org.nuxeo.labs.vntana.automation;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.labs.vntana.client.model.Model;
import org.nuxeo.labs.vntana.service.VntanaService;

/**
 *
 */
@Operation(id = VntanaDownloadModel.ID, category = VntanaAutomation.CAT, label = "Vntana Download Model", description = "Download a 3D model from Vntana")
public class VntanaDownloadModel {

    public static final String ID = VntanaAutomation.CAT + ".DownloadModel";

    @Context
    protected VntanaService vntanaService;

    @Param(name = "format", required = false, values = { "GLB", "USDZ", "OPTIMIZED" })
    protected String format = Model.ConversionFormatEnum.GLB.getValue();

    @OperationMethod
    public Blob run(DocumentModel input) {
        return vntanaService.download(input, Model.ConversionFormatEnum.fromValue(format));
    }
}
