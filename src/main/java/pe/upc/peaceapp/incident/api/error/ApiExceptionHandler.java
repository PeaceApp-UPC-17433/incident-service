package pe.upc.peaceapp.incident.api.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

/** CA-07: formato estandar de error (RFC 7807 Problem Details) en todas las APIs. */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    /** SQLSTATE character_not_in_repertoire: PostgreSQL no admite el caracter NUL en un texto. */
    private static final String INVALID_CHARACTER = "22021";

    @ExceptionHandler(IncidentNotFoundException.class)
    public ProblemDetail handleNotFound(IncidentNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EditWindowClosedException.class)
    public ProblemDetail handleEditWindowClosed(EditWindowClosedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EvidenceTooLargeException.class)
    public ProblemDetail handleEvidenceTooLarge(EvidenceTooLargeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ProblemDetail handleForbidden(ForbiddenOperationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidStatusTransitionException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * PostgreSQL rechaza el caracter NUL en un texto (SQLSTATE 22021). Llega desde el cliente, por ejemplo un
     * "%00" en el texto de búsqueda, así que es una petición inválida. Cualquier otro error de integridad sigue
     * siendo un 500, pero sin exponer el SQL.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        if (INVALID_CHARACTER.equals(sqlState(ex))) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "El texto contiene caracteres no permitidos");
        }
        log.error("Error de integridad de datos", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo completar la operacion");
    }

    private static String sqlState(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) return sql.getSQLState();
        }
        return null;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Uno o mas campos no son validos");
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList());
        return problem;
    }
}
