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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.cache.Cache;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.labs.vntana.adapter.VntanaAdapter;
import org.nuxeo.labs.vntana.adapter.VntanaProductReference;
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
import org.nuxeo.labs.vntana.client.model.ClientGetResultResponseOk;
import org.nuxeo.labs.vntana.client.model.ClientOrganizationResultResponseOk;
import org.nuxeo.labs.vntana.client.model.GCloudStorageResourceCreateSignUrlSessionResponseOk;
import org.nuxeo.labs.vntana.client.model.GetClientOrganizationResponseModel;
import org.nuxeo.labs.vntana.client.model.GetOrganizationByUuidResponseModel;
import org.nuxeo.labs.vntana.client.model.GetUserClientOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.GetUserOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.Model;
import org.nuxeo.labs.vntana.client.model.ModelOpsParameters;
import org.nuxeo.labs.vntana.client.model.OrganizationGetResultResponseOk;
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

    public static final String VNTANA_API_TOKEN = "vntana.api.key";

    public static final String VNTANA_DEFAULT_ORGANIZATION_UUID = "vntana.api.default.organization";

    public static final String VNTANA_DEFAULT_CLIENT_UUID = "vntana.api.default.client";

    public static final String VNTANA_DEFAULT_PUBLISH_LIVE = "vntana.api.default.publish.live";

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static final String X_AUTH_TOKEN = "x-auth-token";

    public static final String VNTANA_TOKEN_CACHE_NAME = "vnatana_token_cache";

    public static final String VNTANA_DOCUMENT_FILTER_ID = "vntanaDocumentFilter";

    /**
     * volatile on purpose to allow for the double-checked locking idiom
     */
    protected volatile ApiClient client;

    public ApiClient getApiClient() {
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
    public void setApiClient(ApiClient client) {
        this.client = client;
    }

    @Override
    public ModelOpsParameters getDefaultModelOpsParameters() {
        return new ModelOpsParameters()
                .setDracoCompression(true)
                .setOptimizationDesiredOuput("AUTO")
                .setOptimizationObstructedGeometry(false)
                .setTextureLosslessCompression(false)
                .setTextureAgression(4)
                .setTextureMaxDimension(4096);
    }

    @Override
    public List<GetUserOrganizationsResponseModel> getOrganizations() {
        String apiToken = getApiToken();
        try {
            UserOrganizationResultResponseOk response = new OperationsAboutOrganizationsApi(
                    getApiClient()).getUserOrganizationsUsingGET(apiToken);
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
    public GetOrganizationByUuidResponseModel getOrganization(String organizationUUID) {
        String organizationToken = getOrganizationToken(organizationUUID);
        try {
            OrganizationGetResultResponseOk response = new OperationsAboutOrganizationsApi(
                    getApiClient()).getCurrentOrganizationUsingGET(organizationToken);
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse();
            } else {
                throw new NuxeoException("Could not get organization " + organizationUUID);
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not get organization " + organizationUUID, e);
        }
    }

    @Override
    public List<GetUserClientOrganizationsResponseModel> getClients(String organizationID) {
        try {
            String organizationToken = getOrganizationToken(organizationID);
            ClientOrganizationResultResponseOk response = new OperationsAboutClientsApi(
                    getApiClient()).getClientOrganizationsUsingGET(organizationToken);

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
    public GetClientOrganizationResponseModel getClient(String organizationUUID, String clientUUID) {
        try {
            String organizationToken = getOrganizationToken(organizationUUID);
            ClientGetResultResponseOk response = new OperationsAboutClientsApi(getApiClient()).getByUuidUsingGET(
                    organizationToken, clientUUID);

            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse();
            } else {
                throw new NuxeoException("Could not get client " + clientUUID);
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not get client " + clientUUID, e);
        }
    }

    @Override
    public ProductGetResponseModel getProduct(VntanaProductReference productRef) {
        String organizationToken = getOrganizationToken(productRef.getOrganizationUUID());
        try {
            ProductGetResultResponseOk response = new OperationsAboutProductsApi(getApiClient()).getByUuidUsingGET3(
                    organizationToken, productRef.getProductUUID());
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse();
            } else {
                throw new NuxeoException("Could not fetch product: " + productRef.getProductUUID());
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not fetch product: " + productRef.getProductUUID(), e);
        }
    }

    @Override
    public List<Map<String, String>> getPipelines(String organizationId) {
        String organizationToken = getOrganizationToken(organizationId);
        try {
            PipelinesGetPipelinesResultResponseOk response = new OperationsAboutPipelinesApi(
                    getApiClient()).getPipelinesUsingGET(organizationToken);
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
                                                    String pipelineUUID, ModelOpsParameters parameters,
                                                    Map<String,String> attributes) {
        AdminCommonProductCreateRequest productCreateRequest = new AdminCommonProductCreateRequest();
        productCreateRequest.setClientUuid(clientUUID);
        productCreateRequest.setName(name);
        productCreateRequest.setAutoPublish(false);
        productCreateRequest.setPipelineUuid(pipelineUUID);
        productCreateRequest.setModelOpsParameters(parameters.toMap());
        productCreateRequest.setAttributes(attributes);

        try {
            String organizationToken = getOrganizationToken(organizationUUID);
            ProductCreateResultResponseOk response = new OperationsAboutProductsApi(getApiClient()).createUsingPOST4(
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

    public boolean upload(VntanaProductReference productRef, Blob blob) throws ApiException, IOException {
        String organizationToken = getOrganizationToken(productRef.getOrganizationUUID());
        String location;

        AdminCommonGCloudStorageProductAssetUploadSignUrlSessionRequest urlRequest = new AdminCommonGCloudStorageProductAssetUploadSignUrlSessionRequest();
        urlRequest.setClientUuid(productRef.getClientUUID());
        urlRequest.setProductUuid(productRef.getProductUUID());
        AdminCommonGCloudStorageResourceSettingsModel resourceSettings = new AdminCommonGCloudStorageResourceSettingsModel();
        resourceSettings.setContentType(blob.getMimeType());
        resourceSettings.setOriginalName(blob.getFilename());
        resourceSettings.setOriginalSize(blob.getLength());
        urlRequest.setResourceSettings(resourceSettings);
        GCloudStorageResourceCreateSignUrlSessionResponseOk urlResponse = new OperationsAboutFilesUploadApi(
                getApiClient()).createClientProductAssetUploadSignUrlSessionUsingPOST(organizationToken, urlRequest);
        location = urlResponse.getResponse().getLocation();

        ApiClient client = getApiClient();
        try (CloseableFile cfile = blob.getCloseableFile()) {
            Request uploadRequest = new Request.Builder().url(location)
                    .header(X_AUTH_TOKEN, organizationToken)
                    .put(RequestBody.create(cfile.getFile(),
                            MediaType.get(blob.getMimeType())))
                    .build();
            try (Response response = client.getHttpClient().newCall(uploadRequest).execute()) {
                return response.isSuccessful();
            }
        }
    }

    public ProductDeleteResponseModel deleteProduct(String organizationUUID, String productUUID) {
        AdminCommonProductDeleteRequest productDeleteRequest = new AdminCommonProductDeleteRequest();
        productDeleteRequest.addUuidsItem(productUUID);
        try {
            String organizationToken = getOrganizationToken(organizationUUID);
            ProductDeleteResultResponseOk response = new OperationsAboutProductsApi(getApiClient()).deleteUsingDELETE4(
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
    public boolean documentIsSupported(DocumentModel doc) {
        ActionManager actionService = Framework.getService(ActionManager.class);
        ActionContext actionContext = new ELActionContext();
        actionContext.setCurrentDocument(doc);
        return actionService.checkFilter(VNTANA_DOCUMENT_FILTER_ID, actionContext);
    }

    @Override
    public DocumentModel publishModel(DocumentModel doc) {
        return publishModel(doc,
                Framework.getProperty(VNTANA_DEFAULT_ORGANIZATION_UUID),
                Framework.getProperty(VNTANA_DEFAULT_CLIENT_UUID),
                Boolean.getBoolean(Framework.getProperty(VNTANA_DEFAULT_PUBLISH_LIVE,"false")),
                getDefaultModelOpsParameters());
    }

    @Override
    public DocumentModel publishModel(DocumentModel doc, String organizationUUID, String clientUUID, boolean autoLive, ModelOpsParameters parameters) {
        if (!doc.hasFacet(VNTANA_FACET)) {
            doc.addFacet(VNTANA_FACET);
        }
        GetOrganizationByUuidResponseModel organization = getOrganization(organizationUUID);
        GetClientOrganizationResponseModel client = getClient(organizationUUID, clientUUID);
        String pipelineUUID = getPipelineUUID(organizationUUID, "Convert Only");

        Map<String,String> attributes = new HashMap<>();
        attributes.put("NuxeoUuid",doc.getId());

        ProductCreateResponseModel product = createProduct(
                (String) doc.getPropertyValue("dc:title"), organizationUUID,
                clientUUID, pipelineUUID, parameters != null ? parameters : getDefaultModelOpsParameters(), attributes);
        String productId = product.getUuid();
        VntanaAdapter vntanaAdapter = doc.getAdapter(VntanaAdapter.class);
        vntanaAdapter.setOrganizationUUID(organizationUUID)
                     .setOrganizationSlug(organization.getSlug())
                     .setClientUUID(clientUUID)
                     .setClientSlug(client.getClientSlug())
                     .setProductUUID(productId);
        try {
            Blob blob = vntanaAdapter.getOriginalBlob();
            if (upload(vntanaAdapter, blob)) {
                vntanaAdapter.setSourceDigest(blob.getDigest()).setUploadSuccessful();
                updateModelRemoteProcessingStatus(vntanaAdapter);
            } else {
                vntanaAdapter.setUploadFailed();
            }
        } catch (IOException | ApiException e) {
            vntanaAdapter.setUploadFailed();
        }
        return vntanaAdapter.getDoc();
    }

    @Override
    public DocumentModel updateModelRemoteProcessingStatus(DocumentModel doc) {
        VntanaAdapter adapter = doc.getAdapter(VntanaAdapter.class);
        updateModelRemoteProcessingStatus(adapter);
        return adapter.getDoc();
    }

    public void updateModelRemoteProcessingStatus(VntanaAdapter adapter) {
        ProductGetResponseModel model = getProduct(adapter);
        adapter.setStatus(model.getStatus().getValue());
        adapter.setConversionStatus(model.getConversionStatus().getValue());
    }

    @Override
    public DocumentModel updateModel(DocumentModel doc) {
        VntanaAdapter vntanaAdapter = doc.getAdapter(VntanaAdapter.class);
        Blob blob = vntanaAdapter.getOriginalBlob();
        if (blob.getDigest().equals(vntanaAdapter.getSourceDigest()) && vntanaAdapter.isUploaded()) {
            return doc;
        }
        try {
            if (upload(vntanaAdapter, blob)) {
                vntanaAdapter.setSourceDigest(blob.getDigest()).setUploadSuccessful();
                updateModelRemoteProcessingStatus(vntanaAdapter);
            }
        } catch (IOException | ApiException e) {
            vntanaAdapter.setUploadFailed();
        }
        return vntanaAdapter.getDoc();
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
                return downloadFile(response);
            } else {
                throw new NuxeoException("Failed to download");
            }
        } catch (IOException | ApiException e) {
            throw new NuxeoException("Failed to download", e);
        }
    }

    @Override
    public Blob thumbnail(DocumentModel doc) {
        VntanaAdapter adapter = getAdapter(doc);
        ProductGetResponseModel productModel = getProduct(doc.getAdapter(VntanaAdapter.class));
        String thumbnailBlobId = productModel.getAsset().getThumbnailBlobId();
        String organizationToken = getOrganizationToken(adapter.getOrganizationUUID());
        try (Response response = new OperationsAboutProductsApi().loadProductThumbnailResourceUsingGETCall(
                        organizationToken, adapter.getClientUUID(), adapter.getProductUUID(),thumbnailBlobId, null,null,null).execute()) {
            if (response.isSuccessful()) {
                return downloadFile(response);
            } else {
                throw new NuxeoException("Failed to download");
            }
        } catch (IOException | ApiException e) {
            throw new NuxeoException("Failed to download", e);
        }
    }

    protected String getApiToken() {
        String apiKey = Framework.getProperty(VNTANA_API_TOKEN);
        if (StringUtils.isBlank(apiKey)) {
            throw new NuxeoException("Vntana API key is not set");
        }
        String apiToken = getTokenFromCache(apiKey);
        if (apiToken != null) {
            return apiToken;
        }
        try {
            Request request = new Request.Builder().url("https://api-platform.vntana.com/v1/auth/login/token")
                                                   .post(RequestBody.create(String.format(
                                                           "{\n\"personal-access-token\": \"%s\"\n}", apiKey), JSON))
                                                   .build();
            try (Response response = getApiClient().getHttpClient().newCall(request).execute()) {
                if (response.isSuccessful()) {
                    apiToken = "Bearer " + response.header(X_AUTH_TOKEN);
                    putTokenInCache(apiKey, apiToken);
                    return apiToken;
                } else {
                    throw new NuxeoException("Could not initialize the vntana client");
                }
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
            ApiClient client = getApiClient();
            Request request = new Request.Builder().url("https://api-platform.vntana.com/v1/auth/refresh-token")
                                                   .header(X_AUTH_TOKEN, apiToken)
                                                   .header("organizationUuid", organizationId)
                                                   .method("POST", RequestBody.create(new byte[] {}))
                                                   .build();
            try (Response authResponse = client.getHttpClient().newCall(request).execute()) {
                if (!authResponse.isSuccessful()) {
                    throw new NuxeoException("Could not authenticate for organization " + organizationId);
                }

                orgToken = "Bearer " + authResponse.header(X_AUTH_TOKEN);
                putTokenInCache(organizationId, orgToken);
                return orgToken;
            }
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

    protected VntanaAdapter getAdapter(DocumentModel doc) {
        return doc.getAdapter(VntanaAdapter.class);
    }

    protected Blob downloadFile(Response response) throws ApiException {
        File file = getApiClient().downloadFileFromResponse(response);
        String contentDisposition = response.header("Content-Disposition");
        String filename = null;
        if (StringUtils.isNotBlank(contentDisposition)) {
            Pattern pattern = Pattern.compile("filename=['\"]?([^'\";]+)['\"]?");
            Matcher matcher = pattern.matcher(contentDisposition);
            if (matcher.find()) {
                filename = getApiClient().sanitizeFilename(matcher.group(1));
            }
        }
        Blob blob = new FileBlob(file, response.headers().get("content-type"));
        if (StringUtils.isNotBlank(filename)) {
            blob.setFilename(filename);
        }
        return blob;
    }

}
