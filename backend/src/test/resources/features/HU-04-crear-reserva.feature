# language: es
@HU-04
Característica: Crear una reserva
  Como usuario de la plataforma
  Quiero crear una reserva para un servicio en un horario habilitado
  Para asegurar mi cita sin conflictos de disponibilidad

  Antecedentes:
    Dado que existe un horario habilitado de "08:00:00" a "12:00:00"
    Y existe el usuario "Ana"
    Y existe el usuario "Luis"

  @positivo @CP-05 @RSK-01 @P1
  Escenario: Reserva exitosa en un horario disponible
    Cuando "Ana" reserva ese horario a las "09:00:00"
    Entonces la respuesta tiene el código HTTP 201
    Y la reserva queda activa para ese horario y fecha
    Y el horario tiene 1 reserva activa en esa fecha

  @excepcional @CP-06 @RSK-01 @P1
  Escenario: Horario que ya está ocupado por otro usuario
    Dado que "Ana" tiene una reserva activa en ese horario a las "09:00:00"
    Cuando "Luis" reserva ese horario a las "09:00:00"
    Entonces la respuesta tiene el código HTTP 409
    Y el mensaje de la respuesta es "El recurso no está disponible en la fecha y hora seleccionadas."
    Y el horario tiene 1 reserva activa en esa fecha

  @excepcional @CP-14 @RSK-01 @P1
  Escenario: El mismo usuario reserva dos veces el mismo horario
    Dado que "Ana" tiene una reserva activa en ese horario a las "09:00:00"
    Cuando "Ana" reserva ese horario a las "09:00:00"
    Entonces la respuesta tiene el código HTTP 409
    Y el horario tiene 1 reserva activa en esa fecha

  @excepcional @CP-13 @RSK-01 @P1 @DEF-02
  Escenario: Dos usuarios reservan el mismo horario al mismo tiempo
    Cuando "Ana" y "Luis" reservan ese horario al mismo tiempo a las "09:00:00"
    Entonces una solicitud responde 201 y la otra responde 409
    Y el horario tiene 1 reserva activa en esa fecha

  @excepcional @CP-07 @RSK-06 @P2
  Escenario: Horario no habilitado por el proveedor
    Dado el proveedor deshabilitó su agenda
    Cuando "Ana" reserva ese horario a las "09:00:00"
    Entonces la respuesta tiene el código HTTP 400
    Y el mensaje de la respuesta es "Este horario no está habilitado por el proveedor."
    Y el horario tiene 0 reservas activas en esa fecha

  @excepcional @CP-07 @RSK-06 @P2
  Esquema del escenario: Hora fuera del bloque habilitado
    Cuando "Ana" reserva ese horario a las "<hora>"
    Entonces la respuesta tiene el código HTTP 400
    Y el mensaje de la respuesta es "La hora seleccionada está fuera del rango de este horario."

    Ejemplos: valores límite del bloque de 08:00 a 12:00
      | hora     |
      | 07:59:00 |
      | 12:00:00 |

  @positivo @CP-RES-03 @P2
  Esquema del escenario: Horas en los límites internos del bloque
    Cuando "Ana" reserva ese horario a las "<hora>"
    Entonces la respuesta tiene el código HTTP 201

    Ejemplos:
      | hora     |
      | 08:00:00 |
      | 11:59:00 |

  @excepcional @CP-RES-07 @RSK-06 @P2 @DEF-03
  Escenario: Fecha que no corresponde al día de la semana del bloque
    Cuando "Ana" reserva ese horario un día de la semana distinto a las "09:00:00"
    Entonces la respuesta tiene el código HTTP 400
    Y el mensaje de la respuesta es "La fecha seleccionada no corresponde al día habilitado para este horario."

  @excepcional @CP-08 @RSK-07 @P2
  Escenario: Fecha que ya pasó
    Cuando "Ana" reserva ese horario para ayer a las "09:00:00"
    Entonces la respuesta tiene el código HTTP 400
    Y el mensaje de la respuesta es "No se puede reservar en una fecha u hora que ya pasó."

  @excepcional @CP-RES-05 @P3
  Escenario: Usuario de la sesión que ya no existe
    Dado que "Ana" inició sesión
    Y la cuenta de "Ana" fue eliminada
    Cuando "Ana" reserva ese horario a las "09:00:00"
    Entonces la respuesta tiene el código HTTP 404
    Y el mensaje de la respuesta es "El usuario no existe."

  @excepcional @CP-RES-06 @P3
  Escenario: Horario inexistente
    Cuando "Ana" reserva el horario inexistente 9999 a las "09:00:00"
    Entonces la respuesta tiene el código HTTP 404
    Y el mensaje de la respuesta es "El horario no existe."

  # DEF-01 corregido: reservar exige sesión iniciada.
  @excepcional @CP-19 @RSK-01 @P1 @DEF-01
  Escenario: Reservar sin haber iniciado sesión
    Cuando intento reservar ese horario a las "09:00:00" sin haber iniciado sesión
    Entonces la respuesta tiene el código HTTP 401
    Y el código de error es "UNAUTHENTICATED"
    Y el horario tiene 0 reservas activas en esa fecha

  @excepcional @CP-05 @RSK-10 @P2
  Escenario: Solicitud sin horario seleccionado ni datos obligatorios
    Cuando envío una solicitud de reserva sin datos
    Entonces la respuesta tiene el código HTTP 400
    Y la respuesta indica un error en el campo "horarioId"
    Y la respuesta indica un error en el campo "fecha"
