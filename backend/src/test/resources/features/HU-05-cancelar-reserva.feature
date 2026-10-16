# language: es
@HU-05
Característica: Cancelar una reserva
  Como usuario de la plataforma
  Quiero cancelar una reserva que ya no necesito
  Para liberar el horario y permitir que otros usuarios puedan reservarlo

  Antecedentes:
    Dado que existe un horario habilitado de "08:00:00" a "12:00:00"
    Y existe el usuario "Ana"
    Y existe el usuario "Luis"
    Y que "Ana" tiene una reserva activa en ese horario a las "09:00:00"

  @positivo @CP-09 @RSK-05 @RSK-11 @P1
  Escenario: Cancelación exitosa libera el horario y conserva el historial
    Cuando "Ana" cancela su reserva
    Entonces la respuesta tiene el código HTTP 200
    Y el mensaje de la respuesta es "Reserva cancelada correctamente. El horario queda disponible para otros usuarios."
    Y la reserva de "Ana" queda en estado "cancelada"
    Cuando "Luis" reserva ese horario a las "09:00:00"
    Entonces la respuesta tiene el código HTTP 201

  @excepcional @CP-10 @RSK-09 @P2
  Escenario: Cancelar una reserva ya cancelada
    Dado la reserva de "Ana" está "cancelada"
    Cuando "Ana" cancela su reserva
    Entonces la respuesta tiene el código HTTP 400
    Y el mensaje de la respuesta es "La reserva ya está cancelada."
    Y la reserva de "Ana" queda en estado "cancelada"

  @excepcional @CP-11 @CP-17 @RSK-03 @P1
  Escenario: Cancelar la reserva de otro usuario cambiando el identificador en la solicitud
    Cuando "Luis" cancela la reserva de "Ana" indicando su propio identificador
    Entonces la respuesta tiene el código HTTP 403
    Y el mensaje de la respuesta es "No puedes cancelar una reserva que no te pertenece."
    Y la reserva de "Ana" queda en estado "activa"

  @excepcional @CP-12 @RSK-09 @P2
  Escenario: Cancelar una reserva que ya fue completada
    Dado la reserva de "Ana" está "completada"
    Cuando "Ana" cancela su reserva
    Entonces la respuesta tiene el código HTTP 400
    Y el mensaje de la respuesta es "La reserva ya fue completada y no puede cancelarse."
    Y la reserva de "Ana" queda en estado "completada"

  @excepcional @CP-CAN-05 @P3
  Escenario: Cancelar una reserva inexistente
    Cuando "Ana" cancela la reserva inexistente 9999
    Entonces la respuesta tiene el código HTTP 404
    Y el mensaje de la respuesta es "La reserva no existe."

  @excepcional @CP-CAN-06 @P3
  Escenario: Solicitud de cancelación sin usuario
    Cuando se intenta cancelar la reserva de "Ana" sin indicar el usuario
    Entonces la respuesta tiene el código HTTP 400
    Y la reserva de "Ana" queda en estado "activa"

  # DEF-01 abierto: hoy el backend acepta la cancelación sin sesión porque el usuario
  # viaja como parámetro. El escenario describe el comportamiento esperado y se excluye
  # del pipeline con @pendiente hasta que se corrija.
  @excepcional @CP-18 @RSK-03 @P1 @pendiente @DEF-01
  Escenario: Cancelar una reserva sin haber iniciado sesión
    Cuando "Ana" cancela su reserva
    Entonces la respuesta tiene el código HTTP 401
    Y la reserva de "Ana" queda en estado "activa"
