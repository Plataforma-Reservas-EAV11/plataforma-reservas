# language: es
@HU-consultar-historial-reservas
Característica: Consultar el historial de mis reservas
  Como usuario de la plataforma
  Quiero consultar el historial de mis reservas con su información y estado
  Para tener trazabilidad de los servicios que he reservado, cancelado o completado

  @positivo @P1
  Escenario: Consulta del historial con reservas registradas
    Dado que existe el usuario "Ana"
    Y que existe un horario habilitado de "08:00:00" a "12:00:00"
    Y "Ana" tiene una reserva en el historial a las "09:00:00" con estado "activa"
    Y "Ana" tiene una reserva en el historial a las "10:00:00" con estado "cancelada"
    Cuando "Ana" consulta su historial de reservas
    Entonces la respuesta tiene el código HTTP 200
    Y la lista del historial contiene 2 reservas
    Y la reserva de las "09:00:00" muestra el servicio "Corte", la fecha del horario y el estado "activa"
    Y la reserva de las "10:00:00" muestra el servicio "Corte", la fecha del horario y el estado "cancelada"

  @positivo @P1
  Escenario: Reservas con estados distintos
    Dado que existe el usuario "Ana"
    Y que existe un horario habilitado de "08:00:00" a "12:00:00"
    Y "Ana" tiene una reserva en el historial a las "09:00:00" con estado "activa"
    Y "Ana" tiene una reserva en el historial a las "10:00:00" con estado "cancelada"
    Y "Ana" tiene una reserva en el historial a las "11:00:00" con estado "completada"
    Cuando "Ana" consulta su historial de reservas
    Entonces la respuesta tiene el código HTTP 200
    Y la reserva de las "09:00:00" aparece con estado "activa"
    Y la reserva de las "10:00:00" aparece con estado "cancelada"
    Y la reserva de las "11:00:00" aparece con estado "completada"

  @positivo @P2
  Escenario: Usuario sin reservas
    Dado que existe el usuario "Ana"
    Y que existe el usuario "Luis"
    Y que existe un horario habilitado de "08:00:00" a "12:00:00"
    Y "Luis" tiene una reserva en el historial a las "09:00:00" con estado "activa"
    Cuando "Ana" consulta su historial de reservas
    Entonces la respuesta tiene el código HTTP 200
    Y la lista del historial contiene 0 reservas
    Y veo un mensaje indicando que aún no tengo reservas
    Cuando "Ana" consulta su historial enviando el identificador de "Luis"
    Entonces la respuesta tiene el código HTTP 200
    Y la lista del historial contiene 0 reservas
    Y veo un mensaje indicando que aún no tengo reservas
    Cuando "Luis" consulta su historial de reservas
    Entonces la respuesta tiene el código HTTP 200
    Y la lista del historial contiene 1 reservas
    Y la reserva de las "09:00:00" aparece con estado "activa"
    Y "Ana" tiene una reserva en el historial a las "10:00:00" con estado "cancelada"
    Cuando "Ana" consulta su historial enviando el identificador de "Luis"
    Entonces la respuesta tiene el código HTTP 200
    Y la lista del historial contiene 1 reservas
    Y la reserva de las "10:00:00" aparece con estado "cancelada"
    Cuando consulto el historial sin iniciar sesión
    Entonces la respuesta tiene el código HTTP 401

  @positivo @P1
  Escenario: Consulta del detalle de una reserva
    Dado que existe el usuario "Ana"
    Y que existe un horario habilitado de "08:00:00" a "12:00:00"
    Y "Ana" tiene una reserva en el historial a las "09:00:00" con estado "completada"
    Cuando "Ana" consulta el detalle de esa reserva
    Entonces la respuesta tiene el código HTTP 200
    Y el detalle muestra el servicio "Corte", la fecha del horario, el horario "09:00:00", el estado "completada" y el usuario "Ana"
