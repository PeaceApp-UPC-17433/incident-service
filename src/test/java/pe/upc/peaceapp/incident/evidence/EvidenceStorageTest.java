package pe.upc.peaceapp.incident.evidence;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceStorageTest {

    static final String KEY = Base64.getEncoder().encodeToString("unit-test-storage-key-0123456789".getBytes());

    private EvidenceStorage storage() {
        return new EvidenceStorage(new EvidenceStorageProperties("stmedia", KEY, null, null, Duration.ofMinutes(5), null, null));
    }

    @Test
    void isDisabledWithoutCredentials() {
        var disabled = new EvidenceStorage(new EvidenceStorageProperties(null, null, null, null, null, null, null));
        assertFalse(disabled.enabled());
        assertEquals("https://x/y.jpg", disabled.signedReadUrl("https://x/y.jpg"));
    }

    @Test
    void uploadTargetWritesToRawAndPublishesSanitizedUrl() {
        var target = storage().newUploadTarget();
        assertTrue(target.uploadUrl().startsWith("https://stmedia.blob.core.windows.net/evidences-raw/"));
        assertTrue(target.uploadUrl().contains("sig="));
        assertTrue(target.uploadUrl().contains("spr=https"));
        assertTrue(target.evidenceUrl().startsWith("https://stmedia.blob.core.windows.net/evidences-sanitized/"));
        assertTrue(target.evidenceUrl().endsWith(".jpg"));
        assertFalse(target.evidenceUrl().contains("sig="));
    }

    @Test
    void uploadPermissionDoesNotAllowReadOrDelete() {
        String query = storage().newUploadTarget().uploadUrl().split("\\?")[1];
        String permissions = java.util.Arrays.stream(query.split("&"))
                .filter(p -> p.startsWith("sp=")).findFirst().orElseThrow().substring(3);
        assertEquals("cw", permissions);
    }

    @Test
    void onlySanitizedUrlsAreAccepted() {
        var storage = storage();
        assertTrue(storage.isSanitizedUrl(storage.newUploadTarget().evidenceUrl()));
        assertFalse(storage.isSanitizedUrl("https://res.cloudinary.com/x/image.jpg"));
        assertFalse(storage.isSanitizedUrl("https://stmedia.blob.core.windows.net/evidences-raw/abc"));
        assertFalse(storage.isSanitizedUrl(null));
    }

    @Test
    void readUrlIsSignedForReadOnly() {
        var storage = storage();
        String signed = storage.signedReadUrl(storage.newUploadTarget().evidenceUrl());
        assertTrue(signed.contains("sig="));
        assertTrue(signed.contains("sp=r&") || signed.contains("sp=r"));
    }

    @Test
    void foreignUrlsAreReturnedUntouched() {
        assertEquals("https://other/a.jpg", storage().signedReadUrl("https://other/a.jpg"));
    }
}
