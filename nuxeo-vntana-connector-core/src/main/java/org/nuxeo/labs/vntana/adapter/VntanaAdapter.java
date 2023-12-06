package org.nuxeo.labs.vntana.adapter;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

/**
 *
 */
public class VntanaAdapter implements VntanaProductReference {

    public static final String RENDITION_PREFIX = "//rendition:";

    public static final String VNTANA_FACET = "Vntana";

    public static final String VNTANA_ORG_UUID_PROPERTY = "vntana:organization_uuid";

    public static final String VNTANA_ORG_SLUG_PROPERTY = "vntana:organization_slug";

    public static final String VNTANA_CLIENT_UUID_PROPERTY = "vntana:client_uuid";

    public static final String VNTANA_CLIENT_SLUG_PROPERTY = "vntana:client_slug";

    public static final String VNTANA_PRODUCT_PROPERTY = "vntana:product_uuid";

    public static final String VNTANA_STATUS_PROPERTY = "vntana:status";

    public static final String VNTANA_CONVERSION_STATUS_PROPERTY = "vntana:conversion_status";

    public static final String VNTANA_SOURCE_DIGEST_PROPERTY = "vntana:source_digest";

    public static final String VNTANA_UPLOAD_STATUS_PROPERTY = "vntana:upload_status";

    protected DocumentModel doc;

    public VntanaAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    public DocumentModel getDoc() {
        return doc;
    }

    public String getOrganizationUUID() {
        return (String) doc.getPropertyValue(VNTANA_ORG_UUID_PROPERTY);
    }

    public VntanaAdapter setOrganizationUUID(String organizationUUID) {
        doc.setPropertyValue(VNTANA_ORG_UUID_PROPERTY, organizationUUID);
        return this;
    }

    public String getOrganizationSlug() {
        return (String) doc.getPropertyValue(VNTANA_ORG_SLUG_PROPERTY);
    }

    public VntanaAdapter setOrganizationSlug(String organizationSlug) {
        doc.setPropertyValue(VNTANA_ORG_SLUG_PROPERTY, organizationSlug);
        return this;
    }

    public String getClientUUID() {
        return (String) doc.getPropertyValue(VNTANA_CLIENT_UUID_PROPERTY);
    }

    public VntanaAdapter setClientUUID(String clientUUID) {
        doc.setPropertyValue(VNTANA_CLIENT_UUID_PROPERTY, clientUUID);
        return this;
    }

    public String getClientSlug() {
        return (String) doc.getPropertyValue(VNTANA_CLIENT_SLUG_PROPERTY);
    }

    public VntanaAdapter setClientSlug(String clientSlug) {
        doc.setPropertyValue(VNTANA_CLIENT_SLUG_PROPERTY, clientSlug);
        return this;
    }

    public String getProductUUID() {
        return (String) doc.getPropertyValue(VNTANA_PRODUCT_PROPERTY);
    }

    public VntanaAdapter setProductUUID(String productUUID) {
        doc.setPropertyValue(VNTANA_PRODUCT_PROPERTY, productUUID);
        return this;
    }

    public String getStatus() {
        return (String) doc.getPropertyValue(VNTANA_STATUS_PROPERTY);
    }

    public VntanaAdapter setStatus(String status) {
        doc.setPropertyValue(VNTANA_STATUS_PROPERTY, status);
        return this;
    }

    public String getConversionStatus() {
        return (String) doc.getPropertyValue(VNTANA_CONVERSION_STATUS_PROPERTY);
    }

    public VntanaAdapter setConversionStatus(String status) {
        doc.setPropertyValue(VNTANA_CONVERSION_STATUS_PROPERTY, status);
        return this;
    }

    public String getSourceDigest() {
        return (String) doc.getPropertyValue(VNTANA_SOURCE_DIGEST_PROPERTY);
    }

    public VntanaAdapter setSourceDigest(String digest) {
        doc.setPropertyValue(VNTANA_SOURCE_DIGEST_PROPERTY, digest);
        return this;
    }

    public String getUploadStatus() {
        return (String) doc.getPropertyValue(VNTANA_UPLOAD_STATUS_PROPERTY);
    }

    public VntanaAdapter setUploadStatus(String status) {
        doc.setPropertyValue(VNTANA_UPLOAD_STATUS_PROPERTY, status);
        return this;
    }

    public boolean isUploaded() {
        return UploadStatusEnum.SUCCESS.getValue().equals(doc.getPropertyValue(VNTANA_UPLOAD_STATUS_PROPERTY));
    }

    public VntanaAdapter setUploadSuccessful() {
        setUploadStatus(UploadStatusEnum.SUCCESS.value);
        setConversionStatus("PENDING");
        return this;
    }

    public boolean isNotUploaded() {
        return UploadStatusEnum.FAILED.getValue().equals(doc.getPropertyValue(VNTANA_UPLOAD_STATUS_PROPERTY));
    }

    public VntanaAdapter setUploadFailed() {
        setUploadStatus(UploadStatusEnum.FAILED.value);
        return this;
    }

    public Blob getOriginalBlob() {
        String fileProperty = Framework.getProperty(String.format("vntana.%s.file.xpath", doc.getType().toLowerCase()),
                "file:content");
        if (fileProperty.startsWith(RENDITION_PREFIX)) {
            String renditionName = fileProperty.substring(RENDITION_PREFIX.length());
            RenditionService renditionService = Framework.getService(RenditionService.class);
            Blob blob = renditionService.getRendition(doc, renditionName).getBlob();
            if (blob.getDigest() == null) {
                try (InputStream in = blob.getStream()) {
                    String digest = new DigestUtils(MD5).digestAsHex(in);
                    blob.setDigest(digest);
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }
            }
            return blob;
        } else {
            return (Blob) doc.getPropertyValue(fileProperty);
        }
    }

    public VntanaAdapter save() {
        CoreSession session = doc.getCoreSession();
        doc = session.saveDocument(doc);
        return this;
    }

    public enum UploadStatusEnum {
        SUCCESS("SUCCESS"), FAILED("FAILED");

        private final String value;

        UploadStatusEnum(String value) {
            this.value = value;
        }

        public static UploadStatusEnum fromValue(String value) {
            for (UploadStatusEnum b : UploadStatusEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

}
