# language: es
@transversal
Característica: Pruebas de humo y manejo de errores de la API
  Como equipo de QA
  Quiero comprobar que el servicio responde y que los errores no exponen detalles internos
  Para decidir si vale la pena continuar con la ejecución y proteger la información del sistema

  @humo @SM-01 @P1
  Escenario: El servicio y la base de datos están disponibles
    Cuando consulto el estado de salud del servicio
    Entonces la respuesta tiene el código HTTP 200
    Y el estado del servicio es "UP"

  @excepcional @seguridad @DEF-05 @P2
  Escenario: Una ruta inexistente responde 404 en JSON sin exponer la ruta interna
    Cuando consulto la ruta inexistente "/api/no-existe"
    Entonces la respuesta tiene el código HTTP 404
    Y el código de error es "HTTP_404"
    Y el mensaje de la respuesta es "El recurso solicitado no existe."

  @excepcional @seguridad @DEF-05 @P2
  Esquema del escenario: Un cuerpo JSON malformado responde 400 en todos los endpoints
    Dado que inicié sesión con una cuenta de usuario
    Cuando envío a "<ruta>" un cuerpo JSON malformado
    Entonces la respuesta tiene el código HTTP 400
    Y el código de error es "MALFORMED_REQUEST"
    Y la respuesta no contiene el texto "Exception"

    Ejemplos:
      | ruta                       |
      | /api/reservas              |
      | /api/agendas/1/horarios    |
      | /api/v1/auth/register      |
      | /api/v1/providers/register |
