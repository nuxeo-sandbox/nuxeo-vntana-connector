package org.nuxeo.labs.vntana.service;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.labs.vntana.adapter.VntanaProductReference;
import org.nuxeo.labs.vntana.client.ApiClient;
import org.nuxeo.labs.vntana.client.model.GetUserClientOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.GetUserOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.Model;
import org.nuxeo.labs.vntana.client.model.ProductCreateResponseModel;
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
     * @param client
     */
    void setClient(ApiClient client);

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
    ProductGetResponseModel getProduct(VntanaProductReference ref);

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

    ProductCreateResponseModel createProduct(String name, String organizationUUID, String clientUUID, String pipelineUUID);

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
