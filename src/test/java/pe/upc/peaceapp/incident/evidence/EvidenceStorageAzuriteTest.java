package pe.upc.peaceapp.incident.evidence;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integración con Azure Blob Storage (4.4.2) contra Azurite, el emulador oficial.
 * <p>
 * {@link EvidenceStorageTest} solo revisa el texto del SAS; aquí se comprueba que el servicio de almacenamiento
 * de verdad aplica lo que el SAS promete. El navegador del vecino recibe la URL de subida, así que cualquier
 * permiso de más en ella es un permiso que tiene un atacante.
 */
@Testcontainers
class EvidenceStorageAzuriteTest {

    // Cuenta y clave públicas de desarrollo de Azurite (documentadas por Microsoft; no son secretos).
    static final String ACCOUNT = "devstoreaccount1";
    static final String KEY = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
    static final byte[] JPEG = "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8);

    @Container
    static final GenericContainer<?> AZURITE = new GenericContainer<>(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.35.0"))
            .withCommand("azurite-blob", "--blobHost", "0.0.0.0", "--skipApiVersionCheck", "--loose")
            .withExposedPorts(10000);

    static final HttpClient HTTP = HttpClient.newHttpClient();
    static String endpoint;
    static BlobServiceClient admin;

    @BeforeAll
    static void containers() {
        endpoint = "http://" + AZURITE.getHost() + ":" + AZURITE.getMappedPort(10000) + "/" + ACCOUNT;
        admin = new BlobServiceClientBuilder().endpoint(endpoint).credential(new StorageSharedKeyCredential(ACCOUNT, KEY)).buildClient();
        admin.createBlobContainerIfNotExists("evidences-raw");
        admin.createBlobContainerIfNotExists("evidences-sanitized");
    }

    static EvidenceStorage storage(Duration uploadTtl) {
        return new EvidenceStorage(new EvidenceStorageProperties(ACCOUNT, KEY, null, null, uploadTtl, Duration.ofMinutes(5), endpoint));
    }

    static int send(String method, String url, byte[] body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10));
        if ("PUT".equals(method)) {
            builder.header("x-ms-blob-type", "BlockBlob").header("Content-Type", "image/jpeg")
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    static String blobName(String url) {
        String path = URI.create(url).getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    static String query(String url) {
        return url.substring(url.indexOf('?') + 1);
    }

    @Test
    void elSasDeSubidaPermiteSubirPeroNoLeerNiBorrar() throws Exception {
        var target = storage(Duration.ofMinutes(5)).newUploadTarget();

        assertThat(send("PUT", target.uploadUrl(), JPEG)).isEqualTo(201);
        assertThat(admin.getBlobContainerClient("evidences-raw").getBlobClient(blobName(target.uploadUrl())).exists()).isTrue();

        assertThat(send("GET", target.uploadUrl(), null)).isEqualTo(403);
        assertThat(send("DELETE", target.uploadUrl(), null)).isEqualTo(403);
    }

    @Test
    void elSasDeSubidaNoSirveParaOtroBlobNiParaSaltarseElSaneamiento() throws Exception {
        var target = storage(Duration.ofMinutes(5)).newUploadTarget();
        String sas = query(target.uploadUrl());

        // Mismo SAS sobre otro nombre: la firma está atada al blob (sr=b).
        assertThat(send("PUT", endpoint + "/evidences-raw/otro-blob?" + sas, JPEG)).isEqualTo(403);
        // Escribir directo en el contenedor saneado evitaría la Function que limpia metadatos EXIF.
        assertThat(send("PUT", endpoint + "/evidences-sanitized/" + blobName(target.uploadUrl()) + ".jpg?" + sas, JPEG)).isEqualTo(403);
    }

    @Test
    void elSasDeLecturaPermiteLeerLaEvidenciaSaneadaPeroNoModificarla() throws Exception {
        var storage = storage(Duration.ofMinutes(5));
        var target = storage.newUploadTarget();
        admin.getBlobContainerClient("evidences-sanitized").getBlobClient(blobName(target.evidenceUrl()))
                .upload(new ByteArrayInputStream(JPEG), JPEG.length, true);

        String readUrl = storage.signedReadUrl(target.evidenceUrl());
        var response = HTTP.send(HttpRequest.newBuilder(URI.create(readUrl)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(JPEG);

        assertThat(send("PUT", readUrl, "otra-imagen".getBytes(StandardCharsets.UTF_8))).isEqualTo(403);
        assertThat(send("DELETE", readUrl, null)).isEqualTo(403);
    }

    @Test
    void sinSasLosContenedoresNoSonPublicos() throws Exception {
        var storage = storage(Duration.ofMinutes(5));
        var target = storage.newUploadTarget();
        admin.getBlobContainerClient("evidences-sanitized").getBlobClient(blobName(target.evidenceUrl()))
                .upload(new ByteArrayInputStream(JPEG), JPEG.length, true);

        assertThat(send("GET", target.evidenceUrl(), null)).isIn(403, 404);
    }

    @Test
    void unSasVencidoOAlteradoEsRechazado() throws Exception {
        var expired = storage(Duration.ofMinutes(-5)).newUploadTarget();
        assertThat(send("PUT", expired.uploadUrl(), JPEG)).isEqualTo(403);

        var target = storage(Duration.ofMinutes(5)).newUploadTarget();
        // Pedir más permisos en la URL invalida la firma: sp=cw -> sp=racwd.
        String escalated = target.uploadUrl().replace("sp=cw", "sp=racwd");
        assertThat(escalated).isNotEqualTo(target.uploadUrl());
        assertThat(send("GET", escalated, null)).isEqualTo(403);
    }
}
