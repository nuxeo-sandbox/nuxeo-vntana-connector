package org.nuxeo.labs.vntana.service;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.labs.vntana.adapter.VntanaProductReference;
import org.nuxeo.labs.vntana.client.ApiClient;
import org.nuxeo.labs.vntana.client.model.GetClientOrganizationResponseModel;
import org.nuxeo.labs.vntana.client.model.GetOrganizationByUuidResponseModel;
import org.nuxeo.labs.vntana.client.model.GetUserClientOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.GetUserOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.Model;
import org.nuxeo.labs.vntana.client.model.ProductCreateResponseModel;
import org.nuxeo.labs.vntana.client.model.ProductGetResponseModel;

public interface VntanaService {

    /**
     * @return the API client object
     */
    ApiClient getApiClient();

    /**
     * @param client
     */
    void setApiClient(ApiClient client);

    /**
     * @return
     */
    List<GetUserOrganizationsResponseModel> getOrganizations();

    GetOrganizationByUuidResponseModel getOrganization(String organizationUUID);

    /**
     * @return
     */
    List<GetUserClientOrganizationsResponseModel> getClients(String organizationID);

    GetClientOrganizationResponseModel getClient(String organizationUUID, String ClientUUID);

    ProductGetResponseModel getProduct(VntanaProductReference ref);

    /**
     * @param organizationId
     * @return
     */
    List<Map<String, String>> getPipelines(String organizationId);

    /**
     * @param organizationId
     * @return
     */
    String getPipelineUUID(String organizationId, String name);

    ProductCreateResponseModel createProduct(String name, String organizationUUID, String clientUUID,
            String pipelineUUID);

    boolean documentIsSupported(DocumentModel doc);

    /**
     * @param doc
     * @return
     */
    DocumentModel publishModel(DocumentModel doc);

    /**
     * @param doc
     * @param organizationUUID
     * @param clientUUID
     * @return
     */
    DocumentModel publishModel(DocumentModel doc, String organizationUUID, String clientUUID);

    DocumentModel updateModelRemoteProcessingStatus(DocumentModel doc);

    DocumentModel updateModel(DocumentModel doc);

    DocumentModel unpublishModel(DocumentModel doc);

    Blob download(DocumentModel doc, Model.ConversionFormatEnum format);

}
