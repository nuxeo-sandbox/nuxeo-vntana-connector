package org.nuxeo.labs.vntana.adapter;

import org.apache.commons.codec.digest.DigestUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;

/**
 *
 */
public class VntanaAdapter implements VntanaProductReference {

    public static final String RENDITION_PREFIX = "//rendition:";

    public static final String VNTANA_FACET = "Vntana";

    public static final String VNTANA_ORG_PROPERTY = "vntana:organizationuuid";

    public static final String VNTANA_CLIENT_PROPERTY = "vntana:clientuuid";

    public static final String VNTANA_PRODUCT_PROPERTY = "vntana:productuuid";

    public static final String VNTANA_STATUS_PROPERTY = "vntana:status";

    public static final String VNTANA_STATUS_UPLOADED = "uploaded";

    public static final String VNTANA_STATUS_FAILED_UPLOAD = "failed_upload";

    public static final String VNTANA_STATUS_PROCESSED = "processed";

    protected DocumentModel doc;

    public VntanaAdapter(DocumentModel doc) {
        this.doc = doc;
    }

    public String getOrganizationUUID() {
        return (String) doc.getPropertyValue(VNTANA_ORG_PROPERTY);
    }

    public VntanaAdapter setOrganizationUUID(String organizationUUID) {
        doc.setPropertyValue(VNTANA_ORG_PROPERTY, organizationUUID);
        return this;
    }

    public String getClientUUID() {
        return (String) doc.getPropertyValue(VNTANA_CLIENT_PROPERTY);
    }

    public VntanaAdapter setClientUUID(String clientUUID) {
        doc.setPropertyValue(VNTANA_CLIENT_PROPERTY, clientUUID);
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

    public VntanaAdapter setUploadedStatus() {
        doc.setPropertyValue(VNTANA_STATUS_PROPERTY, VNTANA_STATUS_UPLOADED);
        return this;
    }

    public boolean isUploaded() {
        return VNTANA_STATUS_UPLOADED.equals(doc.getPropertyValue(VNTANA_STATUS_PROPERTY));
    }

    public VntanaAdapter setFailedUploadStatus() {
        doc.setPropertyValue(VNTANA_STATUS_PROPERTY, VNTANA_STATUS_FAILED_UPLOAD);
        return this;
    }

    public boolean isFailedUpload() {
        return VNTANA_STATUS_FAILED_UPLOAD.equals(doc.getPropertyValue(VNTANA_STATUS_PROPERTY));
    }

    public boolean isProcessed() {
        return VNTANA_STATUS_PROCESSED.equals(doc.getPropertyValue(VNTANA_STATUS_PROPERTY));
    }

    public VntanaAdapter setProcessedStatus() {
        doc.setPropertyValue(VNTANA_STATUS_PROPERTY, VNTANA_STATUS_PROCESSED);
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

}
