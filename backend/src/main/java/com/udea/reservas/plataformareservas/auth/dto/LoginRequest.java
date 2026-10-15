package com.udea.reservas.plataformareservas.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El correo electrónico no es válido.")
    @Size(max = 150, message = "El correo electrónico no puede superar los 150 caracteres.")
    String email,

    @NotBlank(message = "La contraseña es obligatoria.")
    String password
) {}
