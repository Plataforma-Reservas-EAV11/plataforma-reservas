# language: es
@HU-10
Característica: Crear mis servicios
  Como proveedor de servicios
  Quiero registrar los servicios que ofrezco
  Para que los usuarios conozcan mi oferta

  @positivo @P1
  Escenario: Crear un servicio con datos válidos
    Dado que existe el proveedor "Proveedor de prueba" con correo "proveedor-servicio@test.com"
    Y que el proveedor tiene el recurso "Sala de corte"
    Cuando creo un servicio con nombre "Corte", descripción "Corte de cabello", duración 30 minutos
    Entonces la respuesta tiene el código HTTP 201
    Y el mensaje de la respuesta es "Servicio creado exitosamente."
    Y el servicio "Corte" queda guardado para el proveedor autenticado
    Cuando consulto mis servicios
    Entonces la lista contiene 1 servicios
    Y el servicio "Corte" aparece en la lista con duración 30 minutos y estado "activo"
    Y el servicio "Corte" aparece en el catálogo para los usuarios

  @positivo @P1
  Escenario: Asociar un recurso compatible al crear el servicio
    Dado que existe el proveedor "Proveedor con recurso" con correo "proveedor-recurso@test.com"
    Y que el proveedor tiene el recurso "Consultorio 1"
    Cuando creo un servicio usando ese recurso
    Entonces la respuesta tiene el código HTTP 201
    Y el recurso queda asociado al servicio creado

  @excepcional @P1
  Esquema del escenario: Rechazar nombres duplicados para un proveedor
    Dado que existe el proveedor "Proveedor duplicados" con correo "duplicados@test.com"
    Y que el proveedor tiene el recurso "Sala de corte"
    Cuando creo dos servicios con nombres "Corte" y "<segundo>"
    Entonces la respuesta tiene el código HTTP 409
    Y el código de error es "DUPLICATE_SERVICE_NAME"
    Y el mensaje de la respuesta es "Ya existe un servicio con ese nombre para este proveedor."
    Y el proveedor autenticado tiene 1 servicios guardados
    Y existen 1 servicios, 1 agendas y 1 asociaciones guardadas

    Ejemplos:
      | segundo |
      | Corte   |
      | corte   |

  @excepcional @P1
  Escenario: Ignorar espacios externos al comparar nombres
    Dado que existe el proveedor "Proveedor espacios" con correo "espacios@test.com"
    Y que el proveedor tiene el recurso "Sala de corte"
    Cuando creo dos servicios con nombres "Corte" y " Corte "
    Entonces la respuesta tiene el código HTTP 409
    Y el código de error es "DUPLICATE_SERVICE_NAME"
    Y el mensaje de la respuesta es "Ya existe un servicio con ese nombre para este proveedor."
    Y existen 1 servicios, 1 agendas y 1 asociaciones guardadas

  @positivo @P1
  Escenario: Proveedores diferentes pueden usar el mismo nombre
    Dado que existe el proveedor "Proveedor uno" con correo "uno@test.com"
    Y que el proveedor tiene el recurso "Sala uno"
    Cuando creo un servicio con nombre "Corte", descripción "Corte", duración 30 minutos
    Entonces la respuesta tiene el código HTTP 201
    Dado que existe el proveedor "Proveedor dos" con correo "dos@test.com"
    Y que el proveedor tiene el recurso "Sala dos"
    Cuando creo un servicio con nombre "Corte", descripción "Corte", duración 30 minutos
    Entonces la respuesta tiene el código HTTP 201
    Y existen 2 servicios, 2 agendas y 2 asociaciones guardadas

  @positivo @P1
  Escenario: Un proveedor puede crear servicios con nombres diferentes
    Dado que existe el proveedor "Proveedor nombres" con correo "nombres@test.com"
    Y que el proveedor tiene el recurso "Sala"
    Cuando creo un servicio con nombre "Corte", descripción "Corte", duración 30 minutos
    Entonces la respuesta tiene el código HTTP 201
    Cuando creo un servicio con nombre "Manicure", descripción "Manicure", duración 30 minutos
    Entonces la respuesta tiene el código HTTP 201
    Y existen 2 servicios, 2 agendas y 2 asociaciones guardadas

  @positivo @P1
  Escenario: Se puede reutilizar el nombre de un servicio eliminado
    Dado que existe el proveedor "Proveedor reutiliza" con correo "reutiliza@test.com"
    Y que el proveedor tiene el recurso "Sala"
    Cuando creo un servicio con nombre "Corte", descripción "Corte", duración 30 minutos
    Entonces la respuesta tiene el código HTTP 201
    Cuando elimino el servicio "Corte"
    Entonces la respuesta tiene el código HTTP 200
    Cuando creo un servicio con nombre " corte ", descripción "Corte", duración 30 minutos
    Entonces la respuesta tiene el código HTTP 201
    Y existen 2 servicios, 2 agendas y 2 asociaciones guardadas

  @excepcional @P1
  Escenario: PostgreSQL impide insertar nombres activos duplicados
    Dado que existe el proveedor "Proveedor índice" con correo "indice@test.com"
    Y que el proveedor tiene el recurso "Sala"
    Cuando creo un servicio con nombre "Corte", descripción "Corte", duración 30 minutos
    Entonces la respuesta tiene el código HTTP 201
    Y la base de datos rechaza otro servicio activo llamado " corte "
    Y existen 1 servicios, 1 agendas y 1 asociaciones guardadas

  @excepcional @P1
  Escenario: Solicitudes simultáneas no crean servicios duplicados
    Dado que existe el proveedor "Proveedor concurrente" con correo "concurrente@test.com"
    Y que el proveedor tiene el recurso "Sala"
    Cuando creo simultáneamente dos servicios llamados "Corte"
    Entonces una creación tiene éxito y la otra se rechaza por duplicado
    Y existen 1 servicios, 1 agendas y 1 asociaciones guardadas

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
  Esquema del escenario: Rechazar campos obligatorios vacíos sin persistir
    Dado que existe el proveedor "Proveedor inválido" con correo "proveedor-invalido@test.com"
    Y que el proveedor tiene el recurso "Sala válida"
    Cuando intento crear un servicio dejando el campo "<campo>" sin completar
    Entonces la respuesta tiene el código HTTP 400
    Y el código de error es "VALIDATION_ERROR"
    Y la respuesta indica un error en el campo "<campo>"
    Y el mensaje indica que el campo "<campo>" es obligatorio
    Y existen 0 servicios guardados
    Y existen 0 servicios, 0 agendas y 0 asociaciones guardadas

    Ejemplos:
      | campo       |
      | nombre      |
      | descripcion |
      | duracion    |
