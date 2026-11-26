package pe.upc.peaceapp.incident.api.error;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    // Hallazgo de ZAP (A10:2025): un NUL (%00) en el texto de búsqueda llegaba a PostgreSQL, que lo rechaza con
    // SQLSTATE 22021, y la excepción salía como 500. Es una entrada inválida del cliente: 400 sin detalles internos.
    @Test
    void aNulCharacterRejectedByTheDatabaseIsABadRequest() {
        var ex = new DataIntegrityViolationException("JDBC exception executing SQL [select i1_0.id ... like ?]",
                new SQLException("ERROR: invalid byte sequence for encoding \"UTF8\": 0x00", "22021"));

        var problem = handler.handleDataIntegrity(ex);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).doesNotContain("select", "UTF8", "0x00");
    }

    @Test
    void anyOtherIntegrityErrorStaysAServerErrorWithoutInternalDetails() {
        var ex = new DataIntegrityViolationException("could not execute statement [insert into incidents ...]",
                new SQLException("null value in column violates not-null constraint", "23502"));

        var problem = handler.handleDataIntegrity(ex);

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).doesNotContain("insert", "null value");
    }

    @Test
    void anIntegrityErrorWithoutSqlCauseIsAServerError() {
        var problem = handler.handleDataIntegrity(new DataIntegrityViolationException("sin causa"));

        assertThat(problem.getStatus()).isEqualTo(500);
    }
}
