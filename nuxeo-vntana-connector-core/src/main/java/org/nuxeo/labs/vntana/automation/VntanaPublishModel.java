package org.nuxeo.labs.vntana.automation;

import org.apache.commons.lang3.StringUtils;
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
@Operation(id = VntanaPublishModel.ID, category = VntanaAutomation.CAT, label = "Vnatana Publish Model", description = "Publish a 3D model to Vntana")
public class VntanaPublishModel {

    public static final String ID = VntanaAutomation.CAT + ".PublishModel";

    @Context
    protected CoreSession session;

    @Context
    protected VntanaService vntanaService;

    @Param(name = "organizationUUID", required = false)
    protected String organizationUUID;

    @Param(name = "clientUUID", required = false)
    protected String clientUUID;

    @Param(name = "save", required = false)
    protected boolean save = false;

    @OperationMethod
    public DocumentModel run(DocumentModel input) {
        if (!StringUtils.isBlank(organizationUUID) && !StringUtils.isBlank(clientUUID)) {
            input = vntanaService.publishModel(input, organizationUUID, clientUUID, null);
        } else {
            input = vntanaService.publishModel(input);
        }

        if (save) {
            input = session.saveDocument(input);
        }

        return input;
    }
}
