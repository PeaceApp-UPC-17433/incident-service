package pe.upc.peaceapp.incident.evidence;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "incident.evidence.storage")
public record EvidenceStorageProperties(
        String account,
        String key,
        String rawContainer,
        String sanitizedContainer,
        Duration uploadTtl,
        Duration readTtl,
        String endpoint) {

    public EvidenceStorageProperties {
        rawContainer = rawContainer == null ? "evidences-raw" : rawContainer;
        sanitizedContainer = sanitizedContainer == null ? "evidences-sanitized" : sanitizedContainer;
        uploadTtl = uploadTtl == null ? Duration.ofMinutes(10) : uploadTtl;
        readTtl = readTtl == null ? Duration.ofHours(1) : readTtl;
        // Endpoint del Blob service. Por defecto el de Azure; se puede apuntar a Azurite en pruebas.
        endpoint = endpoint == null || endpoint.isBlank()
                ? "https://" + account + ".blob.core.windows.net"
                : endpoint.replaceAll("/+$", "");
    }

    public boolean configured() {
        return account != null && !account.isBlank() && key != null && !key.isBlank();
    }
}
