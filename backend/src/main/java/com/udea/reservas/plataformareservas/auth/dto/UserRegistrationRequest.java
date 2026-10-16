package com.udea.reservas.plataformareservas.auth.dto;

import com.udea.reservas.plataformareservas.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegistrationRequest(
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres.")
    String name,

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El correo electrónico no es válido.")
    @Size(max = 150, message = "El correo electrónico no puede superar los 150 caracteres.")
    String email,

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
    @MaxUtf8Bytes(value = 72, message = "La contraseña es demasiado larga.")
    String password,

    @NotBlank(message = "La confirmación de contraseña es obligatoria.")
    String confirmPassword
) {}
