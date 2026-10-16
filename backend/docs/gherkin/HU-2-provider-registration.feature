# language: es
Característica: Registro como proveedor de servicios
  Como proveedor de servicios
  Quiero crear una cuenta de proveedor
  Para ofrecer mis servicios y administrar mi operación

  Escenario: Registro exitoso con datos válidos
    Dado que no existe una cuenta con el correo "ana@example.com"
    Cuando envío mis datos de negocio, correo, contraseña y confirmación válidos
    Entonces recibo una respuesta HTTP 201
    Y veo el mensaje "Registro de proveedor exitoso. Ya puedes iniciar sesión."
    Y mi usuario queda registrado con el rol "proveedor"

  Escenario: Correo electrónico ya registrado
    Dado que existe una cuenta con el correo "ana@example.com"
    Cuando intento registrarme con ese correo
    Entonces recibo una respuesta HTTP 409
    Y el código de error es "EMAIL_ALREADY_REGISTERED"
    Y no se crea una cuenta nueva

  Escenario: Campos obligatorios vacíos
    Dado que dejo vacíos uno o más campos obligatorios del formulario
    Cuando envío la solicitud de registro
    Entonces recibo una respuesta HTTP 400
    Y el código de error es "VALIDATION_ERROR"
    Y la respuesta indica los campos que son obligatorios
    Y no se crea una cuenta nueva

  Escenario: Contraseñas distintas
    Dado que ingreso una contraseña y una confirmación diferentes
    Cuando envío la solicitud de registro
    Entonces recibo una respuesta HTTP 400
    Y el código de error es "PASSWORD_MISMATCH"
    Y el mensaje es "Las contraseñas no coinciden."
    Y no se crea una cuenta nueva
