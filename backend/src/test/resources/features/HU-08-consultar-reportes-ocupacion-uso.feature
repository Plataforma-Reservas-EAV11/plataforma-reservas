# language: es
@HU-consultar-reportes-ocupacion-uso
Característica: Consultar reportes de ocupación y uso
  Como responsable del negocio
  Quiero consultar reportes de ocupación y uso de los servicios en un periodo
  Para visibilizar el comportamiento de las reservas y apoyar la gestión de agendas y servicios

  @positivo @P1
  Escenario: Consulta del reporte con reservas en el periodo
    Dado que existen reservas registradas en la plataforma
    Cuando consulto el reporte de un periodo determinado
    Entonces la respuesta tiene el código HTTP 200
    Y el reporte muestra 1 reserva realizada
    Y el reporte muestra la ocupación del servicio "Corte" con 1 reserva
    Y el reporte muestra el uso del recurso "Silla 1" con 1 reserva

  @positivo @P2
  @pendiente
  Escenario: Periodo sin reservas
    Dado que no hay reservas registradas en el periodo consultado
    Cuando consulto el reporte de ese periodo
    Entonces la respuesta tiene el código HTTP 200
    Y el reporte muestra 0 reservas realizadas
    Y el reporte muestra los valores de ocupación y uso en cero
    Y el reporte indica que no hay reservas en el periodo seleccionado

  @excepcional @P2
  Escenario: Rango de fechas inválido
    Dado que estoy configurando el periodo del reporte
    Cuando ingreso una fecha de inicio posterior a la fecha de fin
    Entonces la respuesta tiene el código HTTP 400
    Y el mensaje de la respuesta es "El rango de fechas no es válido."
    Y no se genera el reporte

  @positivo @P1
  Escenario: Reporte limitado al periodo seleccionado
    Dado que existen reservas dentro y fuera del periodo consultado
    Cuando consulto el reporte de un periodo específico
    Entonces la respuesta tiene el código HTTP 200
    Y el reporte muestra 1 reserva realizada
    Y el reporte solo incluye el servicio "Corte" con 1 reserva
    Y el reporte solo incluye el recurso "Silla 1" con 1 reserva
