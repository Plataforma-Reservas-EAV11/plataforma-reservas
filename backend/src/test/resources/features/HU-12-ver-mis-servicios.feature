# language: es
@HU-12
Característica: Ver mis servicios como proveedor
  Como proveedor de servicios
  Quiero ver los servicios que he creado
  Para saber qué ofrezco actualmente en la plataforma

  @positivo @P1
  Escenario: Ver lista de mis servicios
    Dado que existe el proveedor "Proveedor lista" con correo "lista@test.com"
    Y que el proveedor tiene el servicio "Corte" registrado
    Y que el proveedor tiene el servicio "Tinte" registrado
    Cuando consulto mis servicios
    Entonces la respuesta tiene el código HTTP 200
    Y la lista contiene 2 servicios
    Y el servicio "Corte" aparece en la lista con duración 30 minutos y estado "activo"
    Y el servicio "Tinte" aparece en la lista con duración 30 minutos y estado "activo"

  @positivo @P1
  Escenario: Proveedor sin servicios
    Dado que existe el proveedor "Proveedor vacio" con correo "vacio@test.com"
    Cuando consulto mis servicios
    Entonces la respuesta tiene el código HTTP 200
    Y la lista contiene 0 servicios
    Y el mensaje de la respuesta es "Aún no tienes servicios registrados."

  @positivo @P2
  Escenario: No se muestran los servicios de otro proveedor
    Dado que existe otro proveedor "Proveedor ajeno" con correo "ajeno-lista@test.com" que tiene el servicio "Ajeno"
    Y que existe el proveedor "Proveedor propio" con correo "propio-lista@test.com"
    Y que el proveedor tiene el servicio "Propio" registrado
    Cuando consulto mis servicios
    Entonces la lista contiene 1 servicios
    Y el servicio "Propio" aparece en la lista con duración 30 minutos y estado "activo"
    Y el servicio "Ajeno" no aparece en la lista

  @positivo @P2
  Escenario: No se muestran los servicios eliminados
    Dado que existe el proveedor "Proveedor eliminados" con correo "eliminados@test.com"
    Y que el proveedor tiene el servicio "Corte" registrado
    Y que el proveedor tiene el servicio "Viejo" registrado
    Cuando elimino el servicio "Viejo"
    Y consulto mis servicios
    Entonces la lista contiene 1 servicios
    Y el servicio "Viejo" no aparece en la lista

  @excepcional @P1
  Escenario: Un usuario sin sesión no puede ver servicios
    Cuando intento consultar mis servicios sin iniciar sesión
    Entonces la respuesta tiene el código HTTP 401

  @excepcional @P1
  Escenario: Un cliente autenticado no puede ver servicios de proveedor
    Dado que existe el cliente "Cliente lista" con correo "cliente-lista@test.com"
    Cuando consulto mis servicios
    Entonces la respuesta tiene el código HTTP 403
