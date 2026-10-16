# language: es
@HU-02
Característica: Registro como proveedor de servicios
  Como proveedor de servicios
  Quiero crear una cuenta de proveedor
  Para ofrecer mis servicios y administrar mi operación

  @positivo @CP-PRV-01 @P1
  Escenario: Registro exitoso con datos válidos
    Dado que no existe una cuenta con el correo "ana@example.com"
    Cuando me registro como proveedor con nombre "Ana Pérez", negocio "MiNegocio", correo "ana@example.com", contraseña "clave-segura" y confirmación "clave-segura"
    Entonces la respuesta tiene el código HTTP 201
    Y el mensaje de la respuesta es "Registro de proveedor exitoso. Ya puedes iniciar sesión."
    Y la respuesta no contiene el texto "clave-segura"
    Y la cuenta "ana@example.com" queda guardada con la contraseña cifrada y el rol "proveedor"
    Y existe un proveedor con el negocio "MiNegocio"

  @excepcional @CP-PRV-02 @P1
  Escenario: Correo electrónico ya registrado
    Dado que existe una cuenta con el correo "ana@example.com"
    Cuando me registro como proveedor con nombre "Ana Pérez", negocio "MiNegocio", correo "ANA@example.com", contraseña "clave-segura" y confirmación "clave-segura"
    Entonces la respuesta tiene el código HTTP 409
    Y el código de error es "EMAIL_ALREADY_REGISTERED"
    Y la respuesta incluye un identificador de traza
    Y existen 1 cuentas registradas

  @excepcional @CP-PRV-03 @P2
  Escenario: Campos obligatorios vacíos
    Cuando me registro como proveedor con nombre "", negocio "", correo "", contraseña "" y confirmación ""
    Entonces la respuesta tiene el código HTTP 400
    Y el código de error es "VALIDATION_ERROR"
    Y la respuesta indica un error en el campo "businessName"
    Y la respuesta indica un error en el campo "email"
    Y existen 0 cuentas registradas

  @excepcional @CP-PRV-04 @P2
  Escenario: Contraseñas distintas
    Cuando me registro como proveedor con nombre "Ana Pérez", negocio "MiNegocio", correo "ana@example.com", contraseña "clave-segura" y confirmación "otra-clave-1"
    Entonces la respuesta tiene el código HTTP 400
    Y el código de error es "PASSWORD_MISMATCH"
    Y el mensaje de la respuesta es "Las contraseñas no coinciden."
    Y existen 0 cuentas registradas
