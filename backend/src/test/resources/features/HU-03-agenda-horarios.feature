# language: es
@HU-03
Característica: Definir agenda y horarios disponibles
  Como proveedor de servicios
  Quiero definir los bloques de horario en los que mis servicios están disponibles
  Para que los usuarios solo puedan reservar en los espacios que yo habilito

  Antecedentes:
    Dado que soy un proveedor con una agenda activa

  @positivo @CP-01 @P2
  Escenario: Definir un bloque de disponibilidad válido
    Cuando defino un bloque el día 1 de "08:00:00" a "12:00:00" con turnos de 30 minutos
    Entonces la respuesta tiene el código HTTP 200
    Y el mensaje de la respuesta es "Bloque de disponibilidad creado exitosamente."
    Y la respuesta muestra el bloque del día 1 de "08:00:00" a "12:00:00"
    Y mi agenda tiene 1 bloque guardado

  @excepcional @CP-02 @RSK-02 @P1
  Esquema del escenario: Bloque que se solapa con uno existente
    Dado mi agenda tiene un bloque el día 1 de "10:00:00" a "12:00:00"
    Cuando defino un bloque el día 1 de "<inicio>" a "<fin>" con turnos de 30 minutos
    Entonces la respuesta tiene el código HTTP 400
    Y el mensaje de la respuesta es "El horario ya tiene disponibilidad definida. No se pueden solapar bloques."
    Y mi agenda tiene 1 bloque guardado

    Ejemplos: formas de solapamiento y valores límite que sí se cruzan
      | inicio   | fin      | caso                                |
      | 11:00:00 | 13:00:00 | se cruzan en parte                  |
      | 10:30:00 | 11:30:00 | el nuevo queda dentro del existente |
      | 09:00:00 | 13:00:00 | el existente queda dentro del nuevo |
      | 10:00:00 | 12:00:00 | mismo rango                         |
      | 11:59:00 | 12:30:00 | se cruzan un minuto al final        |
      | 08:00:00 | 10:01:00 | se cruzan un minuto al inicio       |

  @positivo @CP-15 @RSK-02 @P1
  Esquema del escenario: Bloques que se tocan o están separados sí se pueden crear
    Dado mi agenda tiene un bloque el día 1 de "10:00:00" a "12:00:00"
    Cuando defino un bloque el día <día> de "<inicio>" a "<fin>" con turnos de 30 minutos
    Entonces la respuesta tiene el código HTTP 200
    Y mi agenda tiene 2 bloques guardados

    Ejemplos: valores límite del contacto entre bloques
      | día | inicio   | fin      | caso                                |
      | 1   | 12:00:00 | 13:00:00 | empieza justo cuando termina el otro |
      | 1   | 12:01:00 | 13:00:00 | queda un minuto libre               |
      | 1   | 08:00:00 | 10:00:00 | termina justo cuando empieza el otro |
      | 1   | 13:00:00 | 14:00:00 | separados                           |
      | 2   | 10:00:00 | 12:00:00 | mismo rango en otro día             |

  @excepcional @CP-04 @RSK-12 @P3
  Esquema del escenario: Hora de fin igual o anterior a la hora de inicio
    Cuando defino un bloque el día 1 de "<inicio>" a "<fin>" con turnos de 30 minutos
    Entonces la respuesta tiene el código HTTP 400
    Y el mensaje de la respuesta es "El rango de horas no es válido. La hora de fin debe ser posterior a la de inicio."
    Y mi agenda tiene 0 bloques guardados

    Ejemplos:
      | inicio   | fin      |
      | 14:00:00 | 14:00:00 |
      | 10:00:00 | 09:59:00 |

  @positivo @CP-04 @P3
  Escenario: Bloque de un minuto, valor límite válido del rango
    Cuando defino un bloque el día 1 de "10:00:00" a "10:01:00" con turnos de 1 minutos
    Entonces la respuesta tiene el código HTTP 200
    Y mi agenda tiene 1 bloque guardado

  @excepcional @CP-01 @RSK-10 @P2
  Escenario: Bloque sin hora de fin
    Cuando defino un bloque el día 1 desde las "08:00:00" sin hora de fin
    Entonces la respuesta tiene el código HTTP 400
    Y la respuesta indica un error en el campo "horaFin"
    Y mi agenda tiene 0 bloques guardados

  @excepcional @CP-HOR-07 @P3
  Esquema del escenario: Día de la semana y duración fuera de rango
    Cuando defino un bloque el día <día> de "08:00:00" a "12:00:00" con turnos de <duración> minutos
    Entonces la respuesta tiene el código HTTP 400
    Y la respuesta indica un error en el campo "<campo>"
    Y mi agenda tiene 0 bloques guardados

    Ejemplos:
      | día | duración | campo           |
      | -1  | 30       | diaSemana       |
      | 7   | 30       | diaSemana       |
      | 1   | 0        | duracionSlotMin |

  @excepcional @CP-HOR-06 @P3
  Escenario: Agenda inexistente
    Cuando defino un bloque válido en la agenda 9999
    Entonces la respuesta tiene el código HTTP 404
    Y el mensaje de la respuesta es "La agenda con ID 9999 no existe."
