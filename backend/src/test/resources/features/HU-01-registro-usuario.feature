# language: es
@HU-01
Característica: Registrarse en la plataforma
  Como usuario de la plataforma
  Quiero crear una cuenta con mi nombre, correo y contraseña
  Para poder reservar servicios

  @positivo @CP-REG-01 @P1
  Escenario: Registro exitoso con datos válidos
    Dado que no existe una cuenta con el correo "ana@example.com"
    Cuando me registro con nombre "Ana Pérez", correo "ANA@Example.com", contraseña "clave-segura-1" y confirmación "clave-segura-1"
    Entonces la respuesta tiene el código HTTP 201
    Y el mensaje de la respuesta es "Registro exitoso. Tu cuenta fue creada."
    Y la respuesta no contiene el texto "clave-segura-1"
    Y la cuenta "ana@example.com" queda guardada con la contraseña cifrada y el rol "usuario"

  @excepcional @CP-REG-02 @P1
  Escenario: Correo ya registrado, sin distinguir mayúsculas
    Dado que existe una cuenta con el correo "ana@example.com"
    Cuando me registro con nombre "Ana Pérez", correo "Ana@Example.com", contraseña "clave-segura-1" y confirmación "clave-segura-1"
    Entonces la respuesta tiene el código HTTP 409
    Y el mensaje de la respuesta es "El correo electrónico ya está registrado."
    Y existen 1 cuentas registradas

  @excepcional @CP-REG-03 @P2
  Esquema del escenario: Datos obligatorios o inválidos
    Cuando me registro con nombre "<nombre>", correo "<correo>", contraseña "<contraseña>" y confirmación "<confirmación>"
    Entonces la respuesta tiene el código HTTP 400
    Y la respuesta indica un error en el campo "<campo>"
    Y existen 0 cuentas registradas

    Ejemplos:
      | nombre    | correo          | contraseña     | confirmación   | campo           |
      |           | ana@example.com | clave-segura-1 | clave-segura-1 | name            |
      | Ana Pérez | no-es-un-correo | clave-segura-1 | clave-segura-1 | email           |
      | Ana Pérez | ana@example.com |                |                | password        |
      | Ana Pérez | ana@example.com | clave-segura-1 |                | confirmPassword |
      | Ana Pérez | ana@example.com | corta1         | corta1         | password        |

  @excepcional @CP-REG-04 @P2
  Escenario: Contraseña y confirmación distintas
    Cuando me registro con nombre "Ana Pérez", correo "ana@example.com", contraseña "clave-segura-1" y confirmación "otra-clave-2"
    Entonces la respuesta tiene el código HTTP 400
    Y el mensaje de la respuesta es "Las contraseñas no coinciden."
    Y existen 0 cuentas registradas

  @excepcional @CP-REG-05 @P2 @DEF-04
  Escenario: Contraseña que supera los 72 bytes que admite BCrypt
    Cuando me registro con una contraseña formada por 40 veces el carácter "ñ"
    Entonces la respuesta tiene el código HTTP 400
    Y la respuesta indica un error en el campo "password"
    Y la respuesta no contiene el texto "bytes"
    Y existen 0 cuentas registradas
