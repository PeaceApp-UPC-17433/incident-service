package pe.upc.peaceapp.incident.evidence;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.upc.peaceapp.incident.api.error.EvidenceTooLargeException;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/evidences")
public class EvidenceController {

    public record UploadRequest(@Positive long sizeBytes) {
    }

    public record UploadResponse(String uploadUrl, String evidenceUrl, OffsetDateTime expiresAt, long maxBytes) {
    }

    private final EvidenceStorage storage;
    private final long maxBytes;

    public EvidenceController(EvidenceStorage storage, @Value("${incident.max-evidence-size-bytes}") long maxBytes) {
        this.storage = storage;
        this.maxBytes = maxBytes;
    }

    @PostMapping("/upload-url")
    public UploadResponse uploadUrl(@Valid @RequestBody UploadRequest request) {
        if (!storage.enabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Evidence storage is not configured");
        }
        if (request.sizeBytes() > maxBytes) {
            throw new EvidenceTooLargeException(maxBytes);
        }
        var target = storage.newUploadTarget();
        return new UploadResponse(target.uploadUrl(), target.evidenceUrl(), target.expiresAt(), maxBytes);
    }
}
