package org.nuxeo.labs.vntana.service;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.labs.vntana.client.ApiClient;
import org.nuxeo.labs.vntana.client.ApiException;
import org.nuxeo.labs.vntana.client.api.OperationsAboutClientsApi;
import org.nuxeo.labs.vntana.client.api.OperationsAboutOrganizationsApi;
import org.nuxeo.labs.vntana.client.api.OperationsAboutPipelinesApi;
import org.nuxeo.labs.vntana.client.api.OperationsAboutProductsApi;
import org.nuxeo.labs.vntana.client.model.AdminCommonProductCreateRequest;
import org.nuxeo.labs.vntana.client.model.ClientOrganizationResultResponseOk;
import org.nuxeo.labs.vntana.client.model.GetUserClientOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.GetUserOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.PipelinesGetPipelinesResultResponseOk;
import org.nuxeo.labs.vntana.client.model.ProductCreateResponseModel;
import org.nuxeo.labs.vntana.client.model.ProductCreateResultResponseOk;
import org.nuxeo.labs.vntana.client.model.ProductGetResponseModel;
import org.nuxeo.labs.vntana.client.model.ProductGetResultResponseOk;
import org.nuxeo.labs.vntana.client.model.UserOrganizationResultResponseOk;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VntanaServiceImpl extends DefaultComponent implements VntanaService {

    public static final String VNTANA_API_TOKEN = "vntana.api.token";
    public static final String VNTANA_DEFAULT_ORGANIZATION_UUID = "vntana.api.default.organization";
    public static final String VNTANA_DEFAULT_CLIENT_UUID = "vntana.api.default.organization";

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    public static final String X_AUTH_TOKEN = "x-auth-token";

    /**
     * volatile on purpose to allow for the double-checked locking idiom
     */
    protected volatile ApiClient client;

    protected String authToken;

    public ApiClient getClient() {
        // thread safe lazy initialization of the google vision client
        // see https://en.wikipedia.org/wiki/Double-checked_locking
        ApiClient result = client;
        if (result == null) {
            synchronized (this) {
                result = client;
                if (result == null) {
                    result = client = new ApiClient();
                    client.getJSON().setOffsetDateTimeFormat(
                            new DateTimeFormatterBuilder().appendPattern("uuuu-MM-dd'T'HH:mm:ss")
                                    .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                                    .toFormatter()
                                    .withZone(ZoneOffset.UTC));
                    String apiToken = Framework.getProperty(VNTANA_API_TOKEN);
                    try {
                        Request request = new Request.Builder()
                                .url("https://api-platform.vntana.com/v1/auth/login/token")
                                .post(RequestBody.create(String.format("{\n\"personal-access-token\": \"%s\"\n}", apiToken), JSON))
                                .build();
                        Response response = client.getHttpClient().newCall(request).execute();
                        if (response.isSuccessful()) {
                            authToken = "Bearer " + response.header(X_AUTH_TOKEN);
                        } else {
                            throw new NuxeoException("Could not initialize the vntana client");
                        }
                    } catch (IOException e) {
                        throw new NuxeoException("Could not initialize the vntana client", e);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public List<GetUserOrganizationsResponseModel> getOrganizations() {
        try {
            UserOrganizationResultResponseOk response =
                    new OperationsAboutOrganizationsApi(getClient()).getUserOrganizationsUsingGET(authToken);
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse().getGrid();
            } else {
                throw new NuxeoException("Could not get the list of organizations");
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not get the list of organizations", e);
        }
    }

    @Override
    public List<GetUserClientOrganizationsResponseModel> getClients(String organizationID) {
        try {
            String organizationToken = getOrganizationToken(organizationID);
            ClientOrganizationResultResponseOk response =
                    new OperationsAboutClientsApi(getClient()).getClientOrganizationsUsingGET(organizationToken);

            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse().getGrid();
            } else {
                throw new NuxeoException("Could not get the list of clients");
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not get the list of clients", e);
        }
    }

    @Override
    public ProductGetResponseModel getProduct(String organizationId, String clientId, String productId) {
        String organizationToken = getOrganizationToken(organizationId);
        try {
            ProductGetResultResponseOk response = new OperationsAboutProductsApi(getClient()).getByUuidUsingGET4(organizationToken, productId);
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse();
            } else {
                throw new NuxeoException("Could not fetch product: " + productId);
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not fetch product: " + productId, e);
        }
    }

    @Override
    public List<Map<String, String>> getPipelines(String organizationId) {
        String organizationToken = getOrganizationToken(organizationId);
        try {
            PipelinesGetPipelinesResultResponseOk response = new OperationsAboutPipelinesApi(getClient()).getPipelinesUsingGET(organizationToken);
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse().getPipelines();
            } else {
                throw new NuxeoException("Could not fetch pipelines for org: " + organizationId);
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not fetch pipelines for org: " + organizationId, e);
        }
    }

    @Override
    public String getPipelineUUID(String organizationId, String name) {
        List<Map<String, String>> pipelines = getPipelines(organizationId);
        Optional<Map<String, String>> pipeline = pipelines.stream().filter(item -> name.equals(item.get("name"))).findFirst();
        if (pipeline.isPresent()) {
            return pipeline.get().get("uuid");
        } else {
            throw new NuxeoException(String.format("No pipeline found for %s %s", organizationId, name));
        }
    }

    @Override
    public void publishModel(DocumentModel doc) {
        publishModel(doc, Framework.getProperty(VNTANA_DEFAULT_ORGANIZATION_UUID), Framework.getProperty(VNTANA_DEFAULT_CLIENT_UUID));
    }

    @Override
    public void publishModel(DocumentModel doc, String organizationUUID, String clientUUID) {
        String pipelineUUID = getPipelineUUID(organizationUUID, "Convert Only");
        AdminCommonProductCreateRequest productCreateRequest = new AdminCommonProductCreateRequest();
        productCreateRequest.setClientUuid(clientUUID);
        productCreateRequest.setName((String) doc.getPropertyValue("dc:title"));
        productCreateRequest.setAutoPublish(false);
        productCreateRequest.setPipelineUuid(pipelineUUID);

        Map<String, String> compression = new HashMap<>();
        compression.put("enabled","true");
        Map<String, Map<String,String>> modelOpsParameters = new HashMap<>();
        modelOpsParameters.put("DRACO_COMPRESSION",compression);

        productCreateRequest.setModelOpsParameters(modelOpsParameters);

        try {
            String organizationToken = getOrganizationToken(organizationUUID);
            ProductCreateResultResponseOk response = new OperationsAboutProductsApi(getClient()).createUsingPOST5(organizationToken, productCreateRequest);
            if (Boolean.TRUE.equals(response.getSuccess())) {
                ProductCreateResponseModel product = response.getResponse();
                doc.addFacet("Vnatana");
                doc.setPropertyValue("vntana:organizationuuid", organizationUUID);
                doc.setPropertyValue("vntana:clientuuid", clientUUID);
                doc.setPropertyValue("vntana:productuuid", product.getUuid());
            } else {
                throw new NuxeoException("Could not create product");
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not create product", e);
        }
    }

    protected String getOrganizationToken(String organizationId) {
        try {
            ApiClient client = getClient();
            Request request = new Request.Builder()
                    .url("https://api-platform.vntana.com/v1/auth/refresh-token")
                    .header(X_AUTH_TOKEN, authToken)
                    .header("organizationUuid", organizationId)
                    .method("POST", RequestBody.create(new byte[]{}))
                    .build();
            Response authResponse = client.getHttpClient().newCall(request).execute();

            if (!authResponse.isSuccessful()) {
                throw new NuxeoException("Could not authenticate for organization " + organizationId);
            }

            return "Bearer " + authResponse.header(X_AUTH_TOKEN);

        } catch (IOException e) {
            throw new NuxeoException("Could not authenticate for organization " + organizationId, e);
        }
    }
}
