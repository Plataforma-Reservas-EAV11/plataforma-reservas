package com.udea.reservas.plataformareservas.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record LoginRequest(
    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El correo electrónico no es válido.")
    @Size(max = 150, message = "El correo electrónico no puede superar los 150 caracteres.")
    String email,

    @NotBlank(message = "La contraseña es obligatoria.")
    String password
) {

    // DEF-15: el correo se normaliza al construir la solicitud, antes de que se
    // evalúen las validaciones, para que " Ana@Example.com " sea un correo válido.
    public LoginRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
