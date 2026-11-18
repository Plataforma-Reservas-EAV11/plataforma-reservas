# language: es
@HU-ver-mis-reservas-activas
Característica: Ver mis reservas activas
  Como usuario de la plataforma
  Quiero ver las reservas que he creado con su estado actual
  Para saber qué citas tengo pendientes sin revisar todo mi historial

  @positivo @P1
  Escenario: Ver lista de reservas activas
    Dado que existe el usuario "Ana"
    Y que existe un horario habilitado de "08:00:00" a "12:00:00"
    Y que "Ana" tiene una reserva activa en ese horario a las "09:00:00"
    Cuando "Ana" consulta sus reservas activas
    Entonces la respuesta tiene el código HTTP 200
    Y la lista de reservas activas contiene 1 reservas
    Y la reserva de las "09:00:00" muestra el servicio "Corte", la fecha del horario, el horario y el estado "activa"

  @positivo @P1
  Esquema del escenario: Reserva que deja de estar activa
    Dado que existe el usuario "Ana"
    Y que existe un horario habilitado de "08:00:00" a "12:00:00"
    Y que "Ana" tiene una reserva activa en ese horario a las "09:00:00"
    Y la reserva de "Ana" está "<estado>"
    Cuando "Ana" consulta sus reservas activas
    Entonces la respuesta tiene el código HTTP 200
    Y la lista de reservas activas contiene 0 reservas

    Ejemplos:
      | estado     |
      | cancelada  |
      | completada |

  @positivo @P2
  @pendiente
  Escenario: Usuario sin reservas activas
    Dado que existe el usuario "Ana"
    Cuando "Ana" consulta sus reservas activas
    Entonces la respuesta tiene el código HTTP 200
    Y la lista de reservas activas contiene 0 reservas
    Y veo un mensaje indicando que aún no tengo reservas activas
