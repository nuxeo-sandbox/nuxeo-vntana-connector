package org.nuxeo.labs.vntana.service;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.labs.vntana.client.ApiClient;
import org.nuxeo.labs.vntana.client.model.GetUserClientOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.GetUserOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.ProductGetResponseModel;

import java.util.List;
import java.util.Map;

public interface VntanaService {

    /**
     *
     * @return the API client object
     */
    ApiClient getClient();

    /**
     *
     * @return
     */
    List<GetUserOrganizationsResponseModel> getOrganizations();

    /**
     *
     * @return
     */
    List<GetUserClientOrganizationsResponseModel> getClients(String organizationID);

    /**
     *
     * @param organizationId
     * @param clientId
     * @param productId
     * @return
     */
    ProductGetResponseModel getProduct(String organizationId, String clientId, String productId);

    /**
     *
     * @param organizationId
     * @return
     */
    List<Map<String, String>> getPipelines(String organizationId);

    /**
     *
     * @param organizationId
     * @return
     */
    String getPipelineUUID(String organizationId, String name);


    /**
     *
     * @param doc
     */
    void publishModel(DocumentModel doc);


    /**
     *
     * @param doc
     * @param organizationUUID
     * @param clientUUID
     */
    void publishModel(DocumentModel doc, String organizationUUID, String clientUUID);


}
