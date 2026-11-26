package pe.upc.peaceapp.incident.evidence;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.sas.SasProtocol;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@EnableConfigurationProperties(EvidenceStorageProperties.class)
public class EvidenceStorage {

    public record UploadTarget(String uploadUrl, String evidenceUrl, OffsetDateTime expiresAt) {
    }

    private final EvidenceStorageProperties properties;
    private final StorageSharedKeyCredential credential;

    public EvidenceStorage(EvidenceStorageProperties properties) {
        this.properties = properties;
        this.credential = properties.configured()
                ? new StorageSharedKeyCredential(properties.account(), properties.key())
                : null;
    }

    public boolean enabled() {
        return credential != null;
    }

    public UploadTarget newUploadTarget() {
        String name = UUID.randomUUID().toString();
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(properties.uploadTtl());
        var permission = new BlobSasPermission().setCreatePermission(true).setWritePermission(true);
        String sas = client(properties.rawContainer(), name).generateSas(signature(expiresAt, permission));
        return new UploadTarget(
                blobUrl(properties.rawContainer(), name) + "?" + sas,
                blobUrl(properties.sanitizedContainer(), name + ".jpg"),
                expiresAt);
    }

    public boolean isSanitizedUrl(String url) {
        return url != null && url.startsWith(blobUrl(properties.sanitizedContainer(), ""));
    }

    public String signedReadUrl(String url) {
        if (!enabled() || !isSanitizedUrl(url)) {
            return url;
        }
        String name = url.substring(blobUrl(properties.sanitizedContainer(), "").length());
        var permission = new BlobSasPermission().setReadPermission(true);
        String sas = client(properties.sanitizedContainer(), name)
                .generateSas(signature(OffsetDateTime.now().plus(properties.readTtl()), permission));
        return url + "?" + sas;
    }

    private BlobServiceSasSignatureValues signature(OffsetDateTime expiresAt, BlobSasPermission permission) {
        return new BlobServiceSasSignatureValues(expiresAt, permission).setProtocol(SasProtocol.HTTPS_ONLY);
    }

    private BlobClient client(String container, String name) {
        return new BlobClientBuilder()
                .endpoint("https://" + properties.account() + ".blob.core.windows.net")
                .credential(credential)
                .containerName(container)
                .blobName(name)
                .buildClient();
    }

    private String blobUrl(String container, String name) {
        return "https://" + properties.account() + ".blob.core.windows.net/" + container + "/" + name;
    }
}
