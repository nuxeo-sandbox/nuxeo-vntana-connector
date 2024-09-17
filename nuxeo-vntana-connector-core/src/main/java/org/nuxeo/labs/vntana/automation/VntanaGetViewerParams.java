package org.nuxeo.labs.vntana.automation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.labs.vntana.adapter.VntanaAdapter;
import org.nuxeo.labs.vntana.service.VntanaService;

import java.util.HashMap;
import java.util.Map;

@Operation(id = VntanaGetViewerParams.ID, category = VntanaAutomation.CAT, label = "Vntana Get Viewer Params", description = "Get Params for the Vntana viewer")
public class VntanaGetViewerParams {

    public static final String ID = VntanaAutomation.CAT + ".GeViewerParams";

    @Context
    protected VntanaService vntanaService;

    @OperationMethod
    public Blob run(DocumentModel input) throws JsonProcessingException {
        VntanaAdapter adapter = input.getAdapter(VntanaAdapter.class);
        String token = vntanaService.getAccessToken(input);

        Map<String,String> map = new HashMap<>();
        map.put("token", token);
        map.put("productUuid",adapter.getProductUUID());
        map.put("organizationSlug", adapter.getOrganizationSlug());
        map.put("clientSlug", adapter.getClientSlug());

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonStr = objectMapper.writeValueAsString(map);

        return new StringBlob(jsonStr,"application/json");
    }
}
