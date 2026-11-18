# language: es
@HU-ver-servicios-disponibles
Característica: Ver servicios disponibles
  Como usuario de la plataforma
  Quiero ver los servicios que ofrecen los proveedores y los horarios que tienen libres
  Para elegir el servicio y el horario que necesito antes de reservar

  @positivo @P1
  Escenario: Ver la lista de proveedores
    Dado que estoy autenticado como usuario
    Y que hay 3 proveedores registrados
    Cuando accedo a la sección de servicios
    Entonces la respuesta tiene el código HTTP 200
    Y veo 1 proveedores en la página
    Y el proveedor "Proveedor catálogo 1 negocio" aparece en la lista
    Y la paginación indica que hay más de una página
    Cuando avanzo a la siguiente página de proveedores
    Entonces veo 1 proveedores en la página
    Y el proveedor "Proveedor catálogo 2 negocio" aparece en la lista

  @positivo @P1
  Escenario: Ver servicios de un proveedor
    Dado que estoy autenticado como usuario
    Y que existe el proveedor "Proveedor servicios" con correo "servicios@test.com"
    Y que el proveedor "Proveedor servicios" ofrece el servicio "Corte"
    Cuando selecciono el proveedor "Proveedor servicios"
    Entonces la respuesta tiene el código HTTP 200
    Y veo 1 servicios del proveedor
    Y el servicio "Corte" muestra duración 30 minutos y proveedor "Proveedor servicios"

  @positivo @P1
  Escenario: Ver horarios disponibles de un servicio
    Dado que estoy autenticado como usuario
    Y que existe el proveedor "Proveedor horarios" con correo "horarios@test.com"
    Y que el proveedor "Proveedor horarios" ofrece el servicio "Corte"
    Y que el servicio "Corte" tiene horarios habilitados
    Y que el servicio "Corte" tiene un horario que ya pasó
    Y que uno de los horarios del servicio "Corte" ya está reservado
    Cuando selecciono el servicio "Corte"
    Entonces la respuesta tiene el código HTTP 200
    Y veo horarios futuros y libres del servicio "Corte"

  @positivo @P2
  @pendiente
  Escenario: Proveedor sin servicios registrados
    Dado que estoy autenticado como usuario
    Y que el proveedor "Proveedor vacío" no tiene servicios registrados
    Cuando selecciono el proveedor "Proveedor vacío"
    Entonces la respuesta tiene el código HTTP 200
    Y veo 0 servicios del proveedor
    Y la respuesta indica que el proveedor no tiene servicios disponibles

  @positivo @P2
  @pendiente
  Escenario: No hay proveedores registrados
    Dado que estoy autenticado como usuario
    Cuando accedo a la sección de servicios
    Entonces la respuesta tiene el código HTTP 200
    Y veo 0 proveedores en la página
    Y la respuesta indica que no hay proveedores disponibles
