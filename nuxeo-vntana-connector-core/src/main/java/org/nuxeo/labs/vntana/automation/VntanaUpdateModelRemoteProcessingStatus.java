package org.nuxeo.labs.vntana.automation;

import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.labs.vntana.service.VntanaService;

/**
 *
 */
@Operation(id = VntanaUpdateModelRemoteProcessingStatus.ID, category = VntanaAutomation.CAT, label = "Vnatana Update Model Remote Processing Status", description = "Update Model Remote Processing Status")
public class VntanaUpdateModelRemoteProcessingStatus {

    public static final String ID = VntanaAutomation.CAT + ".UpdateModelRemoteProcessingStatus";

    @Context
    protected CoreSession session;

    @Context
    protected VntanaService vntanaService;

    @Param(name = "save", required = false)
    protected boolean save = false;

    @OperationMethod
    public DocumentModel run(DocumentModel input) {
        input = vntanaService.updateModelRemoteProcessingStatus(input);

        if (save) {
            input = session.saveDocument(input);
        }

        return input;
    }
}
