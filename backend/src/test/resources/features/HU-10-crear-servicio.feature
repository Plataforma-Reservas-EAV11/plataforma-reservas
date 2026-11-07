# language: es
@HU-10
Característica: Crear mis servicios
  Como proveedor de servicios
  Quiero registrar los servicios que ofrezco
  Para que los usuarios conozcan mi oferta

  @positivo @P1
  Escenario: Crear un servicio con datos válidos
    Dado que existe el proveedor "Proveedor de prueba" con correo "proveedor-servicio@test.com"
    Cuando creo un servicio con nombre "Corte", descripción "Corte de cabello", duración 30 minutos
    Entonces la respuesta tiene el código HTTP 201
    Y el servicio "Corte" queda guardado para el proveedor autenticado

  @positivo @P1
  Escenario: Asociar un recurso compatible al crear el servicio
    Dado que existe el proveedor "Proveedor con recurso" con correo "proveedor-recurso@test.com"
    Y que el proveedor tiene el recurso "Consultorio 1"
    Cuando creo un servicio usando ese recurso
    Entonces la respuesta tiene el código HTTP 201
    Y el recurso queda asociado al servicio creado

  @positivo @P1
  Escenario: Permitir nombres duplicados para un proveedor
    Dado que existe el proveedor "Proveedor duplicados" con correo "duplicados@test.com"
    Cuando creo dos servicios con el mismo nombre "Corte"
    Entonces la respuesta tiene el código HTTP 201
    Y el proveedor autenticado tiene 2 servicios guardados

  @excepcional @P1
  Escenario: Un usuario sin sesión no puede crear servicios
    Cuando intento crear un servicio sin iniciar sesión
    Entonces la respuesta tiene el código HTTP 401
    Y existen 0 servicios guardados

  @excepcional @P1
  Escenario: Un cliente autenticado no puede crear servicios
    Dado que existe el cliente "Cliente de prueba" con correo "cliente-servicio@test.com"
    Cuando intento crear un servicio
    Entonces la respuesta tiene el código HTTP 403
    Y existen 0 servicios guardados

  @excepcional @P1
  Escenario: Rechazar un proveedor sin perfil asociado
    Dado que existe una cuenta proveedor sin perfil con correo "sin-perfil@test.com"
    Cuando intento crear un servicio
    Entonces la respuesta tiene el código HTTP 404
    Y existen 0 servicios guardados

  @excepcional @P1
  Escenario: Rechazar varios perfiles para el mismo usuario
    Dado que existe una cuenta proveedor con perfiles duplicados y correo "perfiles-duplicados@test.com"
    Cuando intento crear un servicio
    Entonces la respuesta tiene el código HTTP 409
    Y existen 0 servicios guardados

  @excepcional @P1
  Escenario: Exigir un token CSRF válido
    Dado que existe el proveedor "Proveedor CSRF" con correo "proveedor-csrf@test.com"
    Cuando intento crear un servicio sin token CSRF
    Entonces la respuesta tiene el código HTTP 403
    Y existen 0 servicios guardados

  @excepcional @P1
  Escenario: Rechazar datos obligatorios vacíos sin persistir
    Dado que existe el proveedor "Proveedor inválido" con correo "proveedor-invalido@test.com"
    Cuando creo un servicio con campos vacíos
    Entonces la respuesta tiene el código HTTP 400
    Y el código de error es "VALIDATION_ERROR"
    Y existen 0 servicios guardados
