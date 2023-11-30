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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.nuxeo.labs.vntana.client.api.ClientsApi;
import org.nuxeo.labs.vntana.client.api.OrganizationsApi;
import org.nuxeo.labs.vntana.client.api.PipelinesApi;
import org.nuxeo.labs.vntana.client.api.ProductsApi;
import org.nuxeo.labs.vntana.client.api.UploadApi;
import org.nuxeo.labs.vntana.client.model.AdminCommonGCloudStorageProductAssetUploadSignUrlSessionRequest;
import org.nuxeo.labs.vntana.client.model.AdminCommonGCloudStorageResourceSettingsModel;
import org.nuxeo.labs.vntana.client.model.AdminCommonProductCreateRequest;
import org.nuxeo.labs.vntana.client.model.AdminCommonProductHardDeleteRequest;
import org.nuxeo.labs.vntana.client.model.GCloudStorageResourceCreateSignUrlSessionResponse;
import org.nuxeo.labs.vntana.client.model.GetClientOrganizationResponseModel;
import org.nuxeo.labs.vntana.client.model.GetClientOrganizationResultResponse;
import org.nuxeo.labs.vntana.client.model.GetOrganizationByUuidResponseModel;
import org.nuxeo.labs.vntana.client.model.GetOrganizationByUuidResultResponse;
import org.nuxeo.labs.vntana.client.model.GetUserClientOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.GetUserOrganizationsResponseModel;
import org.nuxeo.labs.vntana.client.model.Model;
import org.nuxeo.labs.vntana.client.model.ModelOpsParameters;
import org.nuxeo.labs.vntana.client.model.PipelinesResultResponse;
import org.nuxeo.labs.vntana.client.model.ProductCreateResponseModel;
import org.nuxeo.labs.vntana.client.model.ProductCreateResultResponseOk;
import org.nuxeo.labs.vntana.client.model.ProductGetResponseModel;
import org.nuxeo.labs.vntana.client.model.ProductGetResultResponseOk;
import org.nuxeo.labs.vntana.client.model.ProductHardDeleteResponseModel;
import org.nuxeo.labs.vntana.client.model.ProductHardDeleteResultResponseOk;
import org.nuxeo.labs.vntana.client.model.UserClientOrganizationResponse;
import org.nuxeo.labs.vntana.client.model.UserOrganizationResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VntanaServiceImpl extends DefaultComponent implements VntanaService {

    private static final Logger log = LogManager.getLogger(VntanaServiceImpl.class);

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
                    org.nuxeo.labs.vntana.client.JSON.setOffsetDateTimeFormat(
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

    public ApiClient getApiClient(String token) {
        ApiClient client = getApiClient();
        client.setApiKey(token);
        return client;
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
            UserOrganizationResponse response = new OrganizationsApi(
                    getApiClient(apiToken)).getUserOrganizations();
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse().getGrid();
            } else {
                throw new NuxeoException(String.format("Could not get the list of organizations, %s", response.getErrors()));
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not get the list of organizations", e);
        }
    }

    @Override
    public GetOrganizationByUuidResponseModel getOrganization(String organizationUUID) {
        String organizationToken = getOrganizationToken(organizationUUID);
        try {
            GetOrganizationByUuidResultResponse response = new OrganizationsApi(
                    getApiClient(organizationToken)).getCurrentOrganization();
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse();
            } else {
                throw new NuxeoException(String.format("Could not get organization %s, %s", organizationUUID, response.getErrors()));
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not get organization " + organizationUUID, e);
        }
    }

    @Override
    public List<GetUserClientOrganizationsResponseModel> getClients(String organizationID) {
        try {
            String organizationToken = getOrganizationToken(organizationID);
            UserClientOrganizationResponse response = new ClientsApi(
                    getApiClient(organizationToken)).getClientOrganizations();
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse().getGrid();
            } else {
                throw new NuxeoException(String.format("Could not get the list of clients, %s", response.getErrors()));
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not get the list of clients", e);
        }
    }

    @Override
    public GetClientOrganizationResponseModel getClient(String organizationUUID, String clientUUID) {
        try {
            String organizationToken = getOrganizationToken(organizationUUID);
            GetClientOrganizationResultResponse response = new ClientsApi(getApiClient(organizationToken)).getClient(clientUUID);

            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse();
            } else {
                throw new NuxeoException(String.format("Could not get client %s, %s",clientUUID, response.getErrors()));
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not get client " + clientUUID, e);
        }
    }

    @Override
    public ProductGetResponseModel getProduct(VntanaProductReference productRef) {
        String organizationToken = getOrganizationToken(productRef.getOrganizationUUID());
        try {
            ProductGetResultResponseOk response = new ProductsApi(getApiClient(organizationToken))
                    .getByUuid(productRef.getProductUUID());
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse();
            } else {
                throw new NuxeoException(String.format("Could not fetch product: %s, %s", productRef.getProductUUID(), response.getErrors()));
            }
        } catch (ApiException e) {
            throw new NuxeoException("Could not fetch product: " + productRef.getProductUUID(), e);
        }
    }

    @Override
    public List<Map<String, String>> getPipelines(String organizationId) {
        String organizationToken = getOrganizationToken(organizationId);
        try {
            PipelinesResultResponse response = new PipelinesApi(getApiClient(organizationToken)).getPipelines();
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse().getPipelines();
            } else {
                throw new NuxeoException(String.format("Could not fetch pipelines for org: %s, %s", organizationId, response.getErrors()));
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
        productCreateRequest.setPipelineUuid(pipelineUUID);
        productCreateRequest.setModelOpsParameters(parameters.toMap());
        productCreateRequest.setAttributes(attributes);
        productCreateRequest.setStatus(AdminCommonProductCreateRequest.StatusEnum.LIVE_INTERNAL);
        productCreateRequest.setPublishToStatus(AdminCommonProductCreateRequest.PublishToStatusEnum.LIVE_INTERNAL);

        try {
            String organizationToken = getOrganizationToken(organizationUUID);
            ProductCreateResultResponseOk response = new ProductsApi(getApiClient(organizationToken))
                    .createProduct(productCreateRequest);
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse();
            } else {
                throw new NuxeoException(String.format("Could not create product, %s",response.getErrors())) ;
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
        GCloudStorageResourceCreateSignUrlSessionResponse urlResponse = new UploadApi(
                getApiClient(organizationToken)).createClientProductAssetUploadSignUrlSession(urlRequest);
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

    public ProductHardDeleteResponseModel deleteProduct(String organizationUUID, String productUUID) {
        AdminCommonProductHardDeleteRequest productDeleteRequest = new AdminCommonProductHardDeleteRequest();
        productDeleteRequest.setUuid(productUUID);
        try {
            String organizationToken = getOrganizationToken(organizationUUID);
            ProductHardDeleteResultResponseOk response = new ProductsApi(getApiClient(organizationToken))
                    .hardDelete(productDeleteRequest);
            if (Boolean.TRUE.equals(response.getSuccess())) {
                return response.getResponse();
            } else {
                throw new NuxeoException(String.format("Could not delete product %s, %s",productUUID,response.getErrors())) ;
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

        try (Response response = new ProductsApi(getApiClient(organizationToken)).downloadModelCall(
                adapter.getProductUUID(), format.getValue(), adapter.getClientUUID(), null).execute()) {
            if (response.isSuccessful()) {
                return downloadFile(response);
            } else {
                throw new NuxeoException(String.format("Failed to download format %s for doc %s: %s",format.getValue(), doc.getId(), response));
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

        if (thumbnailBlobId == null) {
            log.debug(String.format("No Thumbnail ID for vntana product %s in doc %s",productModel.getUuid(),doc.getId()));
            return null;
        }

        String organizationToken = getOrganizationToken(adapter.getOrganizationUUID());

        try (Response response = new ProductsApi(getApiClient(organizationToken)).downloadThumbnailCall(
                adapter.getProductUUID(), adapter.getClientUUID(),thumbnailBlobId, null,null,true, null).execute()) {
            if (response.isSuccessful()) {
                return downloadFile(response);
            } else {
                throw new NuxeoException(String.format("Failed to download thumbnail for doc %s: %s", doc.getId(),response));
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
