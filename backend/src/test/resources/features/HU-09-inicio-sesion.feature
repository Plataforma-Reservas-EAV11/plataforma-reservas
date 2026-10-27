# language: es
@HU-09
Característica: Iniciar sesión en la plataforma
  Como usuario registrado
  Quiero iniciar sesión con mi correo y contraseña
  Para acceder a las funciones de mi perfil

  Antecedentes:
    Dado que existe una cuenta con el correo "ana@example.com" y la contraseña "clave-segura-1"

  @positivo @CP-LOG-01 @P1
  Escenario: Inicio de sesión exitoso sin distinguir mayúsculas en el correo
    Cuando inicio sesión con el correo "ANA@Example.com" y la contraseña "clave-segura-1"
    Entonces la respuesta tiene el código HTTP 200
    Y el mensaje de la respuesta es "Inicio de sesión exitoso."
    Y la respuesta contiene los datos públicos del usuario "ana@example.com"

  @excepcional @CP-LOG-02 @P1
  Esquema del escenario: Credenciales incorrectas reciben la misma respuesta genérica
    Cuando inicio sesión con el correo "<correo>" y la contraseña "<contraseña>"
    Entonces la respuesta tiene el código HTTP 401
    Y el código de error es "INVALID_CREDENTIALS"
    Y el mensaje de la respuesta es "El correo electrónico o la contraseña no son correctos."

    Ejemplos:
      | correo            | contraseña      |
      | ana@example.com   | clave-incorrect |
      | nadie@example.com | clave-segura-1  |

  # DEF-15 corregido: el correo se recorta antes de validarlo, así que los espacios
  # al inicio o al final ya no provocan un 400.
  @positivo @CP-LOG-04 @P3 @DEF-15
  Escenario: Correo con espacios al inicio o al final
    Cuando inicio sesión con el correo " ana@example.com " y la contraseña "clave-segura-1"
    Entonces la respuesta tiene el código HTTP 200

  @excepcional @CP-LOG-03 @P1
  Escenario: Solicitud de inicio de sesión sin token CSRF
    Cuando inicio sesión sin token CSRF con el correo "ana@example.com" y la contraseña "clave-segura-1"
    Entonces la respuesta tiene el código HTTP 403
    Y el código de error es "CSRF_INVALID"

  @excepcional @CP-LOG-05 @P2 @DEF-13
  Escenario: La cuenta se bloquea tras cinco intentos fallidos aunque luego se use la contraseña correcta
    Cuando intento iniciar sesión 5 veces con el correo "ana@example.com" y una contraseña incorrecta
    Y inicio sesión con el correo "ana@example.com" y la contraseña "clave-segura-1"
    Entonces la respuesta tiene el código HTTP 423
    Y el código de error es "ACCOUNT_LOCKED"

  @positivo @CP-LOG-06 @P2 @DEF-13
  Escenario: Cuatro intentos fallidos no bloquean la cuenta
    Cuando intento iniciar sesión 4 veces con el correo "ana@example.com" y una contraseña incorrecta
    Y inicio sesión con el correo "ana@example.com" y la contraseña "clave-segura-1"
    Entonces la respuesta tiene el código HTTP 200

  @positivo @CP-LOG-07 @P2 @DEF-13
  Escenario: Un inicio de sesión exitoso reinicia el contador de intentos fallidos
    Cuando intento iniciar sesión 4 veces con el correo "ana@example.com" y una contraseña incorrecta
    Y inicio sesión con el correo "ana@example.com" y la contraseña "clave-segura-1"
    Y intento iniciar sesión 4 veces con el correo "ana@example.com" y una contraseña incorrecta
    Y inicio sesión con el correo "ana@example.com" y la contraseña "clave-segura-1"
    Entonces la respuesta tiene el código HTTP 200

  @positivo @CP-LOG-08 @P2 @DEF-13
  Escenario: Cuando vence el bloqueo la cuenta vuelve a iniciar sesión
    Cuando intento iniciar sesión 5 veces con el correo "ana@example.com" y una contraseña incorrecta
    Y el bloqueo de la cuenta "ana@example.com" ya venció
    Y inicio sesión con el correo "ana@example.com" y la contraseña "clave-segura-1"
    Entonces la respuesta tiene el código HTTP 200
