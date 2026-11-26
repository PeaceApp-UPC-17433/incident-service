package pe.upc.peaceapp.incident.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import pe.upc.peaceapp.incident.IncidentServiceApplication;
import pe.upc.peaceapp.incident.domain.repository.IncidentRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CucumberContextConfiguration
@SpringBootTest(classes = IncidentServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IncidentSteps {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private IncidentRepository repository;

    @Before
    public void cleanDatabase() {
        repository.deleteAll();
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, UUID> ids = new HashMap<>();
    private final Map<String, String> roles = new HashMap<>();
    private final Map<String, String> incidents = new HashMap<>();
    private HttpResponse<String> response;

    @Dado("que existen los siguientes usuarios")
    public void usuarios(DataTable table) {
        ids.clear();
        roles.clear();
        incidents.clear();
        table.asMaps().forEach(row -> {
            ids.put(row.get("nombre"), UUID.randomUUID());
            roles.put(row.get("nombre"), row.get("rol"));
        });
    }

    @Dado("que {word} registró un incidente de tipo {string} titulado {string}")
    public void registroPrevio(String user, String type, String title) throws Exception {
        response = send("POST", "/api/v1/incidents", user, incidentBody(type, title, -12.0464, -77.0428, ""));
        assertEquals(201, response.statusCode());
        incidents.put(title, mapper.readTree(response.body()).get("id").asText());
    }

    @Dado("que {word} descartó el incidente {string}")
    public void descartado(String user, String title) throws Exception {
        response = send("PUT", "/api/v1/incidents/" + incidents.get(title) + "/status", user, "{\"status\":\"DESCARTADO\"}");
        assertEquals(200, response.statusCode());
    }

    @Cuando("{word} registra un incidente con los siguientes datos")
    public void registra(String user, DataTable table) throws Exception {
        Map<String, String> row = table.asMaps().get(0);
        response = send("POST", "/api/v1/incidents", user, incidentBody(row.get("tipo"), row.get("título"),
                Double.parseDouble(row.get("latitud")), Double.parseDouble(row.get("longitud")), ""));
    }

    @Cuando("{word} registra un incidente de tipo {string} con la evidencia")
    public void registraConEvidencia(String user, String type, DataTable table) throws Exception {
        Map<String, String> row = table.asMaps().get(0);
        String evidence = ",\"evidences\":[{\"mediaType\":\"%s\",\"url\":\"%s\",\"sizeBytes\":%s}]"
                .formatted(row.get("formato"), row.get("url"), row.get("bytes"));
        response = send("POST", "/api/v1/incidents", user, incidentBody(type, "Con evidencia", -12.05, -77.04, evidence));
    }

    @Cuando("un visitante sin sesión intenta registrar un incidente")
    public void visitante() throws Exception {
        response = send("POST", "/api/v1/incidents", null, incidentBody("ROBO", "Anónimo", -12.0, -77.0, ""));
    }

    @Cuando("{word} consulta el incidente {string}")
    public void consulta(String user, String title) throws Exception {
        response = send("GET", "/api/v1/incidents/" + incidents.get(title), user, null);
    }

    @Cuando("{word} consulta un incidente que no existe")
    public void consultaInexistente(String user) throws Exception {
        response = send("GET", "/api/v1/incidents/" + UUID.randomUUID(), user, null);
    }

    @Cuando("{word} consulta su historial de reportes")
    public void historial(String user) throws Exception {
        response = send("GET", "/api/v1/incidents/mine", user, null);
    }

    @Cuando("{word} cambia el título del incidente {string} a {string}")
    public void edita(String user, String title, String newTitle) throws Exception {
        response = send("PUT", "/api/v1/incidents/" + incidents.get(title), user, "{\"title\":\"%s\"}".formatted(newTitle));
    }

    @Cuando("{word} elimina el incidente {string}")
    public void elimina(String user, String title) throws Exception {
        response = send("DELETE", "/api/v1/incidents/" + incidents.get(title), user, null);
    }

    @Cuando("{word} cambia el estado del incidente {string} a {string}")
    public void cambiaEstado(String user, String title, String status) throws Exception {
        response = send("PUT", "/api/v1/incidents/" + incidents.get(title) + "/status", user, "{\"status\":\"%s\"}".formatted(status));
    }

    @Cuando("{word} consulta el mapa del área {string}")
    public void mapa(String user, String area) throws Exception {
        String[] b = area.split(",");
        response = send("GET", "/api/v1/incidents/map?minLat=%s&maxLat=%s&minLng=%s&maxLng=%s".formatted(b[0], b[1], b[2], b[3]), user, null);
    }

    @Cuando("{word} consulta las estadísticas")
    public void estadisticas(String user) throws Exception {
        response = send("GET", "/api/v1/incidents/stats", user, null);
    }

    @Entonces("el sistema responde con código {int}")
    public void codigo(int status) {
        assertEquals(status, response.statusCode(), response.body());
    }

    @Y("la severidad del incidente es {string}")
    public void severidad(String severity) throws Exception {
        assertEquals(severity, json().get("severity").asText());
    }

    @Y("el estado del incidente es {string}")
    public void estado(String status) throws Exception {
        assertEquals(status, json().get("status").asText());
    }

    @Y("el título del incidente es {string}")
    public void titulo(String title) throws Exception {
        assertEquals(title, json().get("title").asText());
    }

    @Y("el incidente tiene {int} evidencia(s)")
    public void evidencias(int count) throws Exception {
        assertEquals(count, json().get("evidences").size());
    }

    @Y("la lista contiene {int} incidente(s)")
    public void lista(int count) throws Exception {
        assertEquals(count, json().size());
    }

    @Y("el mapa contiene {int} punto(s)")
    public void puntos(int count) throws Exception {
        assertEquals(count, json().size());
    }

    private JsonNode json() throws Exception {
        return mapper.readTree(response.body());
    }

    private static String incidentBody(String type, String title, double lat, double lng, String extra) {
        return "{\"type\":\"%s\",\"title\":\"%s\",\"latitude\":%s,\"longitude\":%s%s}".formatted(type, title, lat, lng, extra);
    }

    private HttpResponse<String> send(String method, String path, String user, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (user != null) {
            request.header("X-User-Id", ids.get(user).toString()).header("X-User-Role", roles.get(user));
        }
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> result = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (method.equals("POST") && result.statusCode() == 201 && body != null && path.equals("/api/v1/incidents")) {
            JsonNode node = mapper.readTree(result.body());
            incidents.put(node.get("title").asText(), node.get("id").asText());
        }
        return result;
    }
}
