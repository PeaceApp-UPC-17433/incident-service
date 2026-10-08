# incident-service

BC-02 *Incident Reporting* de PeaceApp: ciclo de vida del reporte de incidentes
(creación, categorización, evidencia, edición/eliminación dentro de ventana, historial,
detalle y bitácora de auditoría). Implementa US-04, US-06 a US-10 y US-21 del informe
(ver `PeaceApp-Report`, secciones 1.3.4 y 2.4).

## Requisitos

- Java 21, Maven 3.9+
- Docker (opcional, para levantar junto a Postgres)

## Ejecutar en local

```bash
mvn spring-boot:run
```

Usa H2 en memoria por defecto. Con Postgres real: `docker compose up --build`.
El servicio queda en `http://localhost:8082`. Swagger UI en `/swagger-ui.html`.

Requiere un JWT emitido por `identity-service` en el header `Authorization: Bearer <token>`
(mismo `JWT_SECRET` en ambos servicios — ver nota de seguridad abajo).

## Endpoints

| Método | Ruta | Historia | Descripción |
| --- | --- | --- | --- |
| POST | /api/v1/incidents | US-04, US-06, US-07, US-08 | Registrar un incidente (con evidencia opcional) |
| GET | /api/v1/incidents/{id} | US-21 | Detalle del incidente |
| GET | /api/v1/incidents/mine | US-10 | Historial de reportes propios |
| PUT | /api/v1/incidents/{id} | US-09 | Editar dentro de la ventana de 30 min |
| DELETE | /api/v1/incidents/{id} | US-09 | Eliminar dentro de la ventana de 30 min |

## Limitaciones conocidas (pendientes de diagramas/ADR)

- La validación del JWT usa un secreto HMAC compartido por configuración, como sustituto
  temporal de la propagación de claims por el API Gateway (CA-04, TS-04 aún no existe).
- `US-09` solo verifica la ventana de tiempo; falta bloquear la edición cuando el reporte
  ya recibió validaciones (eso requiere el evento `ReportValidated` de validation-service).
- No se publican los eventos de dominio (`IncidentReported`, `IncidentUpdated`,
  `IncidentDeleted`, `EvidenceAttached`) hacia el broker definido en CA-01, porque ese
  broker todavía no está decidido.
- Los adjuntos de evidencia son solo metadata (URL + tamaño declarado); la subida real a
  un almacenamiento de objetos no está implementada.
