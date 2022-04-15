package org.nuxeo.labs.vntana.service;

import static org.nuxeo.labs.vntana.adapter.VntanaAdapter.VNTANA_FACET;

import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.labs.vntana.adapter.VntanaAdapter;
import org.nuxeo.labs.vntana.client.ApiClient;
import org.nuxeo.labs.vntana.client.ApiException;
import org.nuxeo.labs.vntana.client.api.OperationsAboutClientsApi;
import org.nuxeo.labs.vntana.client.api.OperationsAboutFilesUploadApi;
import org.nuxeo.labs.vntana.client.api.OperationsAboutOrganizationsApi;
import org.nuxeo.labs.vntana.client.api.OperationsAboutPipelinesApi;
import org.nuxeo.labs.vntana.client.api.OperationsAboutProductsApi;
import org.nuxeo.labs.vntana.client.model.AdminCommonGCloudStorageProductAssetUploadSignUrlSessionRequest;
import org.nuxeo.labs.vntana.client.model.AdminCommonGCloudStorageResourceSettingsModel;
import org.nuxeo.labs.vntana.client.model.AdminCommonProductCreateRequest;
import org.nuxeo.labs.vntana.client.model.AdminCommonProductDeleteRequest;
import org.nuxeo.labs.vntana.client.model.ClientOrganizationResultResponseOk;
import org.nuxeo.labs.vntana.client.model.GCloudStorageResourceCreateSignUrlSessionResponseOk;
import org.nuxeo.labs.vntana.client.model.GetUserClientOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.GetUserOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.Model;
import org.nuxeo.labs.vntana.client.model.PipelinesGetPipelinesResultResponseOk;
import org.nuxeo.labs.vntana.client.model.ProductCreateResponseModel;
import org.nuxeo.labs.vntana.client.model.ProductCreateResultResponseOk;
import org.nuxeo.labs.vntana.client.model.ProductDeleteResponseModel;
import org.nuxeo.labs.vntana.client.model.ProductDeleteResultResponseOk;
import org.nuxeo.labs.vntana.client.model.ProductGetResponseModel;
import org.nuxeo.labs.vntana.client.model.ProductGetResultResponseOk;
import org.nuxeo.labs.vntana.client.model.UserOrganizationResultResponseOk;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VntanaServiceImpl extends DefaultComponent implements VntanaService {

    public static final String VNTANA_API_TOKEN = "vntana.api.token";

    public static final String VNTANA_DEFAULT_ORGANIZATION_UUID = "vntana.api.default.organization";

    public static final String VNTANA_DEFAULT_CLIENT_UUID = "vntana.api.default.client";

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static final String X_AUTH_TOKEN = "x-auth-token";

    public static final String VNTANA_TOKEN_CACHE_NAME = "vnatana_token_cache";

    /**
     * volatile on purpose to allow for the double-checked locking idiom
     */
    protected volatile ApiClient client;

    public ApiClient getClient() {
        // thread safe lazy initialization of the google vision client
        // see https://en.wikipedia.org/wiki/Double-checked_locking
        ApiClient result = client;
        if (result == null) {
            synchronized (this) {
                result = client;
                if (result == null) {
                    result = client = new ApiClient();
                    client.getJSON()
                          .setOffsetDateTimeFormat(
                                  new DateTimeFormatterBuilder().appendPattern("uuuu-MM-dd'T'HH:mm:ss")
                                                                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                                                                .toFormatter()
                                                                .withZone(ZoneOffset.UTC));
                    client.addDefaultHeader("Origin", "www.nuxeo.com");
                }
            }
        }
        return result;
    }

    @Override
    public void setClient(ApiClient client) {
        this.client = client;
    }

    @Override
    public List<GetUserOrganizationsResponseModel> getOrganizations() {
        String apiToken = getApiToken();
        try {
            UserOrganizationResultResponseOk response = new OperationsAboutOrganizationsApi(
                    getClient()).getUserOrganizationsUsingGET(apiToken);
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
            ClientOrganizationResultResponseOk response = new OperationsAboutClientsApi(
                    getClient()).getClientOrganizationsUsingGET(organizationToken);

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
            ProductGetResultResponseOk response = new OperationsAboutProductsApi(getClient()).getByUuidUsingGET4(
                    organizationToken, productId);
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
            PipelinesGetPipelinesResultResponseOk response = new OperationsAboutPipelinesApi(
                    getClient()).getPipelinesUsingGET(organizationToken);
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
        Optional<Map<String, String>> pipeline = pipelines.stream()
                                                          .filter(item -> name.equals(item.get("name")))
                                                          .findFirst();
        if (pipeline.isPresent()) {
            return pipeline.get().get("uuid");
        } else {
            throw new NuxeoException(String.format("No pipeline found for %s %s", organizationId, name));
        }
    }

    @Override
    public ProductCreateResponseModel createProduct(String name, String organizationUUID, String clientUUID,
            String pipelineUUID) {
        AdminCommonProductCreateRequest productCreateRequest = new AdminCommonProductCreateRequest();
        productCreateRequest.setClientUuid(clientUUID);
        productCreateRequest.setName(name);
        productCreateRequest.setAutoPublish(false);
        productCreateRequest.setPipelineUuid(pipelineUUID);

        Map<String, String> compression = new HashMap<>();
        compression.put("enabled", "true");
        Map<String, Map<String, String>> modelOpsParameters = new HashMap<>();
        modelOpsParameters.put("DRACO_COMPRESSION", compression);

        productCreateRequest.setModelOpsParameters(modelOpsParameters);

        try {
            String organizationToken = getOrganizationToken(organizationUUID);
            ProductCreateResultResponseOk response = new OperationsAboutProductsApi(getClient()).createUsingPOST5(
                    organizationToken, productCreateRequest);
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse();
            } else {
                throw new NuxeoException("Could not create product");
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not create product", e);
        }
    }

    public boolean upload(String organizationId, String clientId, String productId, Blob blob)
            throws ApiException, IOException {
        String organizationToken = getOrganizationToken(organizationId);
        String location;

        AdminCommonGCloudStorageProductAssetUploadSignUrlSessionRequest urlRequest = new AdminCommonGCloudStorageProductAssetUploadSignUrlSessionRequest();
        urlRequest.setClientUuid(clientId);
        urlRequest.setProductUuid(productId);
        AdminCommonGCloudStorageResourceSettingsModel resourceSettings = new AdminCommonGCloudStorageResourceSettingsModel();
        resourceSettings.setContentType(blob.getMimeType());
        resourceSettings.setOriginalName(blob.getFilename());
        resourceSettings.setOriginalSize(blob.getLength());
        urlRequest.setResourceSettings(resourceSettings);
        GCloudStorageResourceCreateSignUrlSessionResponseOk urlResponse = new OperationsAboutFilesUploadApi(
                getClient()).createClientProductAssetUploadSignUrlSessionUsingPOST(organizationToken, urlRequest);
        location = urlResponse.getResponse().getLocation();

        ApiClient client = getClient();
        Request uploadRequest = new Request.Builder().url(location)
                                                     .header(X_AUTH_TOKEN, organizationToken)
                                                     .put(RequestBody.create(blob.getFile(),
                                                             MediaType.get(blob.getMimeType())))
                                                     .build();
        return client.getHttpClient().newCall(uploadRequest).execute().isSuccessful();
    }

    public ProductDeleteResponseModel deleteProduct(String organizationUUID, String productUUID) {
        AdminCommonProductDeleteRequest productDeleteRequest = new AdminCommonProductDeleteRequest();
        productDeleteRequest.addUuidsItem(productUUID);
        try {
            String organizationToken = getOrganizationToken(organizationUUID);
            ProductDeleteResultResponseOk response = new OperationsAboutProductsApi(getClient()).deleteUsingDELETE4(
                    organizationToken, productDeleteRequest);
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse();
            } else {
                throw new NuxeoException("Could not delete product " + productUUID);
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not delete product " + productUUID, e);
        }
    }

    @Override
    public DocumentModel publishModel(DocumentModel doc) {
        return publishModel(doc, Framework.getProperty(VNTANA_DEFAULT_ORGANIZATION_UUID),
                Framework.getProperty(VNTANA_DEFAULT_CLIENT_UUID));
    }

    @Override
    public DocumentModel publishModel(DocumentModel doc, String organizationUUID, String clientUUID) {
        String pipelineUUID = getPipelineUUID(organizationUUID, "Convert Only");
        ProductCreateResponseModel product = createProduct((String) doc.getPropertyValue("dc:title"), organizationUUID,
                clientUUID, pipelineUUID);
        String productId = product.getUuid();
        if (!doc.hasFacet(VNTANA_FACET)) {
            doc.addFacet(VNTANA_FACET);
        }
        VntanaAdapter vntanaAdapter = doc.getAdapter(VntanaAdapter.class);
        vntanaAdapter.setOrganizationUUID(organizationUUID).setClientUUID(clientUUID).setProductUUID(productId);
        try {
            if (upload(organizationUUID, clientUUID, productId, vntanaAdapter.getOriginalBlob())) {
                vntanaAdapter.setUploadedStatus();
            } else {
                vntanaAdapter.setFailedUploadStatus();
            }
        } catch (IOException | ApiException e) {
            vntanaAdapter.setFailedUploadStatus();
        }
        return doc;
    }

    @Override
    public DocumentModel unpublishModel(DocumentModel doc) {
        VntanaAdapter adapter = doc.getAdapter(VntanaAdapter.class);
        deleteProduct(adapter.getOrganizationUUID(), adapter.getProductUUID());
        doc.removeFacet(VNTANA_FACET);
        return doc;
    }

    @Override
    public Blob download(DocumentModel doc, Model.ConversionFormatEnum format) {
        VntanaAdapter adapter = doc.getAdapter(VntanaAdapter.class);
        String organizationToken = getOrganizationToken(adapter.getOrganizationUUID());

        try (Response response = new OperationsAboutProductsApi().loadProductAssetModelResourceUsingGETCall(
                organizationToken, adapter.getClientUUID(), format.getValue(), adapter.getProductUUID(), null)
                                                                 .execute()) {
            if (response.isSuccessful()) {
                File file = getClient().downloadFileFromResponse(response);
                Blob blob = new FileBlob(file,response.headers().get("content-type"));
                return blob;
            } else {
                throw new NuxeoException("Failed to download");
            }
        } catch (IOException | ApiException e) {
            throw new NuxeoException("Failed to download", e);
        }
    }

    protected String getApiToken() {
        String apiKey = Framework.getProperty(VNTANA_API_TOKEN);
        String apiToken = getTokenFromCache(apiKey);
        if (apiToken != null) {
            return apiToken;
        }
        try {
            Request request = new Request.Builder().url("https://api-platform.vntana.com/v1/auth/login/token")
                                                   .post(RequestBody.create(String.format(
                                                           "{\n\"personal-access-token\": \"%s\"\n}", apiKey), JSON))
                                                   .build();
            Response response = getClient().getHttpClient().newCall(request).execute();
            if (response.isSuccessful()) {
                apiToken = "Bearer " + response.header(X_AUTH_TOKEN);
                putTokenInCache(apiKey, apiToken);
                return apiToken;
            } else {
                throw new NuxeoException("Could not initialize the vntana client");
            }
        } catch (IOException e) {
            throw new NuxeoException("Could not initialize the vntana client", e);
        }
    }

    protected String getOrganizationToken(String organizationId) {
        String orgToken = getTokenFromCache(organizationId);
        if (orgToken != null) {
            return orgToken;
        }

        String apiToken = getApiToken();

        try {
            ApiClient client = getClient();
            Request request = new Request.Builder().url("https://api-platform.vntana.com/v1/auth/refresh-token")
                                                   .header(X_AUTH_TOKEN, apiToken)
                                                   .header("organizationUuid", organizationId)
                                                   .method("POST", RequestBody.create(new byte[] {}))
                                                   .build();
            Response authResponse = client.getHttpClient().newCall(request).execute();

            if (!authResponse.isSuccessful()) {
                throw new NuxeoException("Could not authenticate for organization " + organizationId);
            }

            orgToken = "Bearer " + authResponse.header(X_AUTH_TOKEN);
            putTokenInCache(organizationId, orgToken);
            return orgToken;

        } catch (IOException e) {
            throw new NuxeoException("Could not authenticate for organization " + organizationId, e);
        }
    }

    protected String getTokenFromCache(String key) {
        CacheService cacheService = Framework.getService(CacheService.class);
        Cache cache = cacheService.getCache(VNTANA_TOKEN_CACHE_NAME);
        return (String) cache.get(key);
    }

    protected void putTokenInCache(String key, String token) {
        CacheService cacheService = Framework.getService(CacheService.class);
        Cache cache = cacheService.getCache(VNTANA_TOKEN_CACHE_NAME);
        cache.put(key, token);
    }

}
