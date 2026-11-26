package pe.upc.peaceapp.incident.evidence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "incident.evidence.storage.account=stmedia",
        "incident.evidence.storage.key=dW5pdC10ZXN0LXN0b3JhZ2Uta2V5LTAxMjM0NTY3ODk="
})
@AutoConfigureMockMvc
class EvidenceApiTest {

    @Autowired MockMvc mvc;

    final UUID user = UUID.randomUUID();

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder asCitizen(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header("X-User-Id", user).header("X-User-Role", "CIUDADANO").contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void issuesUploadUrlForAuthenticatedUser() throws Exception {
        mvc.perform(asCitizen(post("/api/v1/evidences/upload-url")).content("{\"sizeBytes\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value(containsString("evidences-raw")))
                .andExpect(jsonPath("$.evidenceUrl").value(containsString("evidences-sanitized")))
                .andExpect(jsonPath("$.evidenceUrl").value(not(containsString("sig="))));
    }

    @Test
    void rejectsOversizedUploads() throws Exception {
        mvc.perform(asCitizen(post("/api/v1/evidences/upload-url")).content("{\"sizeBytes\":999999999}"))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/evidences/upload-url").contentType(MediaType.APPLICATION_JSON).content("{\"sizeBytes\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsEvidenceThatBypassedTheSanitizer() throws Exception {
        mvc.perform(asCitizen(post("/api/v1/incidents")).content(incident("https://res.cloudinary.com/x/photo.jpg")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptsSanitizedEvidenceAndReturnsSignedReadUrl() throws Exception {
        String evidenceUrl = "https://stmedia.blob.core.windows.net/evidences-sanitized/" + UUID.randomUUID() + ".jpg";
        String id = com.jayway.jsonpath.JsonPath.read(
                mvc.perform(asCitizen(post("/api/v1/incidents")).content(incident(evidenceUrl)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.evidences[0].url").value(containsString("sig=")))
                        .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(asCitizen(get("/api/v1/incidents/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidences[0].url").value(containsString(evidenceUrl)));
    }

    private static String incident(String url) {
        return "{\"type\":\"ROBO\",\"title\":\"Con foto\",\"latitude\":-12.05,\"longitude\":-77.04,"
                + "\"evidences\":[{\"mediaType\":\"FOTO\",\"url\":\"%s\",\"sizeBytes\":1000}]}".formatted(url);
    }
}
