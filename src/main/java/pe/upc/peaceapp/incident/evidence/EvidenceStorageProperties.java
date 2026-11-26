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
        Duration readTtl) {

    public EvidenceStorageProperties {
        rawContainer = rawContainer == null ? "evidences-raw" : rawContainer;
        sanitizedContainer = sanitizedContainer == null ? "evidences-sanitized" : sanitizedContainer;
        uploadTtl = uploadTtl == null ? Duration.ofMinutes(10) : uploadTtl;
        readTtl = readTtl == null ? Duration.ofHours(1) : readTtl;
    }

    public boolean configured() {
        return account != null && !account.isBlank() && key != null && !key.isBlank();
    }
}
