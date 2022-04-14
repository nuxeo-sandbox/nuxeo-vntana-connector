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
@Operation(id = VntanaUnpublishModel.ID, category = VntanaAutomation.CAT, label = "Vnatana Unpublish Model", description = "Unpublish Model From Vntana")
public class VntanaUnpublishModel {

    public static final String ID = VntanaAutomation.CAT + ".UnpublishModel";

    @Context
    protected CoreSession session;

    @Context
    protected VntanaService vntanaService;

    @Param(name = "save", required = false)
    protected boolean save = false;

    @OperationMethod
    public DocumentModel run(DocumentModel input) {
        input = vntanaService.unpublishModel(input);

        if (save) {
            input = session.saveDocument(input);
        }

        return input;
    }
}
