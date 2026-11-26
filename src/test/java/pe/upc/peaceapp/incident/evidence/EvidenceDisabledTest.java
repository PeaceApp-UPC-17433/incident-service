package pe.upc.peaceapp.incident.evidence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EvidenceDisabledTest {

    @Autowired MockMvc mvc;

    @Test
    void uploadUrlIsNotImplementedWithoutStorage() throws Exception {
        mvc.perform(post("/api/v1/evidences/upload-url").header("X-User-Id", UUID.randomUUID())
                        .header("X-User-Role", "CIUDADANO").contentType(MediaType.APPLICATION_JSON).content("{\"sizeBytes\":1}"))
                .andExpect(status().isNotImplemented());
    }
}
