# language: es
Característica: Registro, consulta y moderación de incidentes
  Como ciudadano
  Quiero reportar y consultar incidentes de seguridad
  Para que la comunidad conozca las zonas de riesgo

  Antecedentes:
    Dado que existen los siguientes usuarios
      | nombre | rol         |
      | ana    | CIUDADANO   |
      | luis   | CIUDADANO   |
      | marta  | MODERADOR   |

  Escenario: Un ciudadano registra un incidente
    Cuando ana registra un incidente con los siguientes datos
      | tipo | título          | latitud  | longitud |
      | ROBO | Robo en el parque | -12.0464 | -77.0428 |
    Entonces el sistema responde con código 201
    Y el estado del incidente es "PENDIENTE_VALIDACION"
    Y la severidad del incidente es "HIGH"

  Esquema del escenario: La severidad se deriva del tipo de incidente
    Cuando ana registra un incidente con los siguientes datos
      | tipo   | título   | latitud  | longitud |
      | <tipo> | Reporte  | -12.0464 | -77.0428 |
    Entonces el sistema responde con código 201
    Y la severidad del incidente es "<severidad>"

    Ejemplos:
      | tipo               | severidad |
      | ROBO               | HIGH      |
      | AGRESION           | HIGH      |
      | HURTO              | MEDIUM    |
      | ACCIDENTE_TRANSITO | MEDIUM    |
      | VANDALISMO         | LOW       |
      | OTRO               | LOW       |

  Esquema del escenario: Se rechazan coordenadas fuera de rango
    Cuando ana registra un incidente con los siguientes datos
      | tipo | título   | latitud   | longitud   |
      | ROBO | Inválido | <latitud> | <longitud> |
    Entonces el sistema responde con código 400

    Ejemplos:
      | latitud | longitud |
      | 91      | -77.0    |
      | -91     | -77.0    |
      | -12.0   | 181      |
      | -12.0   | -181     |

  Escenario: Un visitante sin sesión no puede registrar incidentes
    Cuando un visitante sin sesión intenta registrar un incidente
    Entonces el sistema responde con código 403

  Escenario: Se adjunta una evidencia válida
    Cuando ana registra un incidente de tipo "ROBO" con la evidencia
      | formato | url                                            | bytes   |
      | FOTO    | https://evidencias.peaceapp.pe/sanitized/a.jpg | 1048576 |
    Entonces el sistema responde con código 201
    Y el incidente tiene 1 evidencia

  Escenario: Se rechaza una evidencia que supera el tamaño permitido
    Cuando ana registra un incidente de tipo "ROBO" con la evidencia
      | formato | url                                            | bytes    |
      | VIDEO   | https://evidencias.peaceapp.pe/sanitized/b.mp4 | 30000000 |
    Entonces el sistema responde con código 413

  Escenario: Se consulta el detalle de un incidente
    Dado que ana registró un incidente de tipo "ROBO" titulado "Robo en la avenida"
    Cuando luis consulta el incidente "Robo en la avenida"
    Entonces el sistema responde con código 200
    Y el título del incidente es "Robo en la avenida"

  Escenario: Un incidente inexistente responde no encontrado
    Cuando ana consulta un incidente que no existe
    Entonces el sistema responde con código 404

  Escenario: El historial solo muestra los reportes propios
    Dado que ana registró un incidente de tipo "ROBO" titulado "Primero de Ana"
    Y que ana registró un incidente de tipo "HURTO" titulado "Segundo de Ana"
    Y que luis registró un incidente de tipo "VANDALISMO" titulado "Único de Luis"
    Cuando ana consulta su historial de reportes
    Entonces el sistema responde con código 200
    Y la lista contiene 2 incidentes

  Escenario: El autor edita su incidente dentro de la ventana de edición
    Dado que ana registró un incidente de tipo "ROBO" titulado "Título original"
    Cuando ana cambia el título del incidente "Título original" a "Título corregido"
    Entonces el sistema responde con código 200
    Y el título del incidente es "Título corregido"

  Escenario: Otro usuario no puede editar un incidente ajeno
    Dado que ana registró un incidente de tipo "ROBO" titulado "De Ana"
    Cuando luis cambia el título del incidente "De Ana" a "Intento ajeno"
    Entonces el sistema responde con código 403

  Escenario: El autor elimina su incidente y deja de existir
    Dado que ana registró un incidente de tipo "HURTO" titulado "Para borrar"
    Cuando ana elimina el incidente "Para borrar"
    Entonces el sistema responde con código 204
    Cuando ana consulta el incidente "Para borrar"
    Entonces el sistema responde con código 404

  Escenario: Un moderador valida un incidente
    Dado que ana registró un incidente de tipo "ROBO" titulado "A moderar"
    Cuando marta cambia el estado del incidente "A moderar" a "VALIDADO"
    Entonces el sistema responde con código 200
    Y el estado del incidente es "VALIDADO"

  Escenario: Un ciudadano no puede cambiar el estado de un incidente
    Dado que ana registró un incidente de tipo "ROBO" titulado "Sin permiso"
    Cuando luis cambia el estado del incidente "Sin permiso" a "VALIDADO"
    Entonces el sistema responde con código 403

  Escenario: Un incidente descartado no puede volver a validarse
    Dado que ana registró un incidente de tipo "ROBO" titulado "Descartado"
    Y que marta descartó el incidente "Descartado"
    Cuando marta cambia el estado del incidente "Descartado" a "VALIDADO"
    Entonces el sistema responde con código 422

  Escenario: El mapa solo muestra los incidentes del área consultada
    Dado que ana registró un incidente de tipo "ROBO" titulado "Dentro del área"
    Cuando luis consulta el mapa del área "-12.10,-12.00,-77.10,-77.00"
    Entonces el sistema responde con código 200
    Y el mapa contiene 1 punto
    Cuando luis consulta el mapa del área "10.0,11.0,10.0,11.0"
    Entonces el sistema responde con código 200
    Y el mapa contiene 0 puntos

  Escenario: Las estadísticas son solo para el personal de moderación
    Cuando ana consulta las estadísticas
    Entonces el sistema responde con código 403
    Cuando marta consulta las estadísticas
    Entonces el sistema responde con código 200
