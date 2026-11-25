package pe.upc.peaceapp.incident.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pe.upc.peaceapp.incident.domain.repository.IncidentRepository;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentQueryApiTest {
    @Autowired MockMvc mvc;
    @Autowired IncidentRepository repository;

    final UUID author = UUID.randomUUID();
    final UUID other = UUID.randomUUID();

    @BeforeEach
    void seed() throws Exception {
        repository.deleteAll();
        create(author, "ROBO", "Robo en el parque", -12.0464, -77.0428, false);
        create(author, "VANDALISMO", "Pintas en la pared", -12.0470, -77.0430, false);
        create(other, "ACCIDENTE_TRANSITO", "Choque en la avenida", -12.0900, -77.0900, true);
    }

    private void create(UUID user, String type, String title, double lat, double lng, boolean anonymous) throws Exception {
        mvc.perform(post("/api/v1/incidents").header("X-User-Id", user).header("X-User-Role", "CIUDADANO")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"%s\",\"title\":\"%s\",\"latitude\":%s,\"longitude\":%s,\"anonymous\":%s}"
                                .formatted(type, title, lat, lng, anonymous)))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder as(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, UUID user, String role) {
        return builder.header("X-User-Id", user).header("X-User-Role", role);
    }

    @Test
    void listIsPagedAndFiltersByTypeSeverityAndText() throws Exception {
        mvc.perform(as(get("/api/v1/incidents"), author, "CIUDADANO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(as(get("/api/v1/incidents").param("type", "ROBO"), author, "CIUDADANO"))
                .andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.content[0].severity").value("HIGH"));
        mvc.perform(as(get("/api/v1/incidents").param("severity", "LOW"), author, "CIUDADANO"))
                .andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.content[0].type").value("VANDALISMO"));
        mvc.perform(as(get("/api/v1/incidents").param("q", "avenida"), author, "CIUDADANO"))
                .andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(as(get("/api/v1/incidents").param("page", "0").param("size", "2"), author, "CIUDADANO"))
                .andExpect(jsonPath("$.content.length()").value(2)).andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void mineOnlyReturnsOwnAndAnonymousAuthorIsHidden() throws Exception {
        mvc.perform(as(get("/api/v1/incidents").param("mine", "true"), author, "CIUDADANO"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(as(get("/api/v1/incidents").param("type", "ACCIDENTE_TRANSITO"), author, "CIUDADANO"))
                .andExpect(jsonPath("$.content[0].authorId").doesNotExist());
    }

    @Test
    void mapReturnsPointsInsideTheBoundingBoxOnly() throws Exception {
        mvc.perform(as(get("/api/v1/incidents/map").param("minLat", "-12.05").param("maxLat", "-12.04")
                        .param("minLng", "-77.05").param("maxLng", "-77.03"), author, "CIUDADANO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void heatmapGroupsIncidentsByCell() throws Exception {
        mvc.perform(as(get("/api/v1/incidents/heatmap").param("minLat", "-12.2").param("maxLat", "-12.0")
                        .param("minLng", "-77.2").param("maxLng", "-77.0").param("cellSize", "0.05"), author, "CIUDADANO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].count").exists()).andExpect(jsonPath("$[0].risk").exists());
    }

    @Test
    void onlyModeratorsChangeStatusAndTransitionsAreValidated() throws Exception {
        var id = repository.findAll().get(0).getId();
        mvc.perform(as(put("/api/v1/incidents/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"VALIDADO\"}"), author, "CIUDADANO")).andExpect(status().isForbidden());
        mvc.perform(as(put("/api/v1/incidents/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DESCARTADO\"}"), other, "MODERADOR")).andExpect(status().isOk());
        mvc.perform(as(put("/api/v1/incidents/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"VALIDADO\"}"), other, "ADMINISTRADOR")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void statsAreOnlyForModeratorsAndCountByStatus() throws Exception {
        mvc.perform(as(get("/api/v1/incidents/stats"), author, "CIUDADANO")).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/v1/incidents/stats"), author, "MODERADOR")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.byStatus.PENDIENTE_VALIDACION").value(3))
                .andExpect(jsonPath("$.byStatus.VALIDADO").value(0));
    }

    @Test
    void futureOccurrenceDateIsRejected() throws Exception {
        mvc.perform(as(post("/api/v1/incidents").contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"ROBO\",\"latitude\":-12.0,\"longitude\":-77.0,\"occurredAt\":\"2999-01-01T00:00:00Z\"}"),
                author, "CIUDADANO")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidCoordinatesAreRejected() throws Exception {
        mvc.perform(as(post("/api/v1/incidents").contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"ROBO\",\"latitude\":120,\"longitude\":-77.0}"), author, "CIUDADANO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/v1/incidents")).andExpect(status().is4xxClientError());
    }
}
