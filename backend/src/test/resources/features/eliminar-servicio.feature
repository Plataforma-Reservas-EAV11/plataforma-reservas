# language: es
 @eliminar-servicio
Característica: Eliminar un servicio
  Como proveedor de servicios
  Quiero eliminar los servicios que ya no deseo ofrecer
  Para mantener actualizada mi oferta de servicios

  @positivo @P1
  Escenario: Eliminar un servicio sin reservas activas
    Dado que existe el proveedor "Proveedor eliminar" con correo "eliminar@test.com"
    Y que el proveedor tiene el servicio "Corte" registrado
    Cuando elimino el servicio "Corte"
    Entonces la respuesta tiene el código HTTP 200
    Y el servicio "Corte" está "eliminado"
    Y el proveedor autenticado tiene 0 servicios activos
    Y el proveedor autenticado tiene 1 servicios guardados

  @positivo @P1
  Escenario: Eliminar un servicio con reservas canceladas no modifica las reservas
    Dado que existe el proveedor "Proveedor historial" con correo "historial@test.com"
    Y que el proveedor tiene el servicio "Corte" registrado
    Y que el servicio "Corte" tiene una reserva "cancelada"
    Cuando elimino el servicio "Corte"
    Entonces la respuesta tiene el código HTTP 200
    Y el servicio "Corte" está "eliminado"
    Y la reserva de "Cliente reservante" sigue en estado "cancelada"
    Y el servicio "Corte" no aparece en el catálogo
    Y la disponibilidad del servicio "Corte" no se puede consultar
    Y el servicio "Corte" no admite nuevas reservas

  @excepcional @P1
  Escenario: Rechazar eliminar un servicio con reservas activas
    Dado que existe el proveedor "Proveedor reservas" con correo "reservas@test.com"
    Y que el proveedor tiene el servicio "Corte" registrado
    Y que el servicio "Corte" tiene una reserva "activa"
    Cuando elimino el servicio "Corte"
    Entonces la respuesta tiene el código HTTP 409
    Y el código de error es "SERVICE_HAS_ACTIVE_RESERVATIONS"
    Y el servicio "Corte" está "activo"
    Y la reserva de "Cliente reservante" sigue en estado "activa"

  @excepcional @P1
  Escenario: No eliminar el servicio de otro proveedor
    Dado que existe otro proveedor "Proveedor ajeno" con correo "ajeno@test.com" que tiene el servicio "Ajeno"
    Y que existe el proveedor "Proveedor propio" con correo "propio@test.com"
    Cuando elimino el servicio "Ajeno"
    Entonces la respuesta tiene el código HTTP 404
    Y el código de error es "SERVICE_NOT_FOUND"
    Y el servicio "Ajeno" está "activo"

  @excepcional @P1
  Escenario: Eliminar un servicio inexistente
    Dado que existe el proveedor "Proveedor inexistente" con correo "inexistente@test.com"
    Cuando intento eliminar el servicio inexistente 9999
    Entonces la respuesta tiene el código HTTP 404
    Y el código de error es "SERVICE_NOT_FOUND"

  @excepcional @P1
  Escenario: Un servicio ya eliminado no se puede eliminar de nuevo
    Dado que existe el proveedor "Proveedor doble" con correo "doble@test.com"
    Y que el proveedor tiene el servicio "Corte" registrado
    Cuando elimino el servicio "Corte"
    Y elimino el servicio "Corte"
    Entonces la respuesta tiene el código HTTP 404
    Y el código de error es "SERVICE_NOT_FOUND"

  @excepcional @P1
  Escenario: Un usuario sin sesión no puede eliminar servicios
    Dado que existe otro proveedor "Proveedor sin sesion" con correo "sin-sesion@test.com" que tiene el servicio "Corte"
    Cuando intento eliminar el servicio "Corte" sin iniciar sesión
    Entonces la respuesta tiene el código HTTP 401
    Y el servicio "Corte" está "activo"

  @excepcional @P1
  Escenario: Un cliente autenticado no puede eliminar servicios
    Dado que existe otro proveedor "Proveedor del cliente" con correo "del-cliente@test.com" que tiene el servicio "Corte"
    Y que existe el cliente "Cliente eliminar" con correo "cliente-eliminar@test.com"
    Cuando elimino el servicio "Corte"
    Entonces la respuesta tiene el código HTTP 403
    Y el servicio "Corte" está "activo"
