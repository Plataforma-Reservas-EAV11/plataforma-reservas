# language: es
@HU-11
Característica: Gestionar recursos del proveedor
  Como proveedor
  Quiero administrar los recursos que utilizo para prestar mis servicios
  Para controlar la capacidad disponible para reservas

  @positivo @P1
  Escenario: Registrar y consultar recursos propios
    Dado que el proveedor tiene un recurso de prueba
    Cuando el proveedor registra el recurso "Sala 2" de tipo "consultorio"
    Entonces la respuesta tiene el código HTTP 201
    Y el recurso queda registrado y activo
    Cuando el proveedor consulta sus recursos
    Entonces la respuesta tiene el código HTTP 200
    Y el proveedor ve únicamente sus recursos
    Cuando el proveedor consulta el recurso registrado
    Entonces el recurso consultado pertenece al proveedor autenticado

  @excepcional @P1
  Escenario: Un proveedor no puede consultar ni modificar recursos ajenos
    Dado que el proveedor tiene un recurso de prueba
    Y que existe un recurso de otro proveedor
    Cuando otro proveedor consulta ese recurso
    Entonces la respuesta tiene el código HTTP 404
    Cuando otro proveedor intenta actualizar ese recurso
    Entonces la respuesta tiene el código HTTP 404

  @positivo @P1
  Escenario: Actualizar, desactivar y volver a activar un recurso propio
    Dado que el proveedor tiene un recurso de prueba
    Cuando el proveedor actualiza el recurso a "Sala renovada"
    Entonces la respuesta tiene el código HTTP 200
    Y los datos del recurso quedan actualizados
    Cuando el proveedor desactiva el recurso
    Entonces la respuesta tiene el código HTTP 200
    Cuando el proveedor activa el recurso
    Entonces la respuesta tiene el código HTTP 200
    Y el recurso queda registrado y activo

  @positivo @P1
  Escenario: Asociar y retirar un recurso de un servicio propio
    Dado que el proveedor tiene un recurso de prueba
    Cuando el proveedor registra el recurso "Sala asociable" de tipo "consultorio"
    Entonces la respuesta tiene el código HTTP 201
    Cuando el proveedor asocia el recurso al servicio de prueba
    Entonces la respuesta tiene el código HTTP 204
    Y el servicio tiene el recurso asociado
    Cuando el proveedor retira el recurso del servicio de prueba
    Entonces la respuesta tiene el código HTTP 204
    Y el recurso deja de estar asociado al servicio

  @excepcional @P1
  Escenario: Rechazar asociar un recurso propio a un servicio de otro proveedor
    Dado que el proveedor tiene un recurso de prueba
    Y que existe un servicio de otro proveedor
    Cuando el proveedor intenta asociar el recurso al servicio ajeno
    Entonces la respuesta tiene el código HTTP 404

  @positivo @P1
  Escenario: Desactivar un recurso no modifica las reservas históricas
    Dado que existe un horario habilitado de "08:00:00" a "12:00:00"
    Y existe el usuario "Ana"
    Y que "Ana" tiene una reserva activa en ese horario a las "09:00:00"
    Cuando el proveedor desactiva el recurso con una reserva histórica
    Entonces la respuesta tiene el código HTTP 200
    Y el recurso queda desactivado sin borrar sus datos
    Y la reserva histórica sigue activa

  @excepcional @P1
  Escenario: Un recurso desactivado no se asigna a nuevas reservas
    Dado que existe un horario habilitado de "08:00:00" a "12:00:00"
    Y existe el usuario "Ana"
    Cuando el proveedor desactiva el recurso
    Entonces la respuesta tiene el código HTTP 200
    Cuando "Ana" reserva ese horario a las "09:00:00"
    Entonces la respuesta tiene el código HTTP 409
    Y el mensaje de la respuesta es "El servicio aún no tiene recursos disponibles para reservar."
