package com.udea.reservas.plataformareservas.proveedores.dto;

import com.udea.reservas.plataformareservas.common.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProviderRegistrationRequest(
    @NotBlank(message = "es obligatorio")
    @Size(max = 150, message = "máximo 150 caracteres")
    String name,
    @NotBlank(message = "es obligatorio")
    @Size(max = 150, message = "máximo 150 caracteres")
    String businessName,
    @NotBlank(message = "es obligatorio")
    @Email(message = "no tiene formato de correo válido")
    @Size(max = 150, message = "máximo 150 caracteres")
    String email,
    @NotBlank(message = "es obligatorio")
    @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres")
    @MaxUtf8Bytes(value = 72, message = "es demasiado larga")
    String password,
    @NotBlank(message = "es obligatorio")
    String confirmPassword,
    @Size(max = 30, message = "máximo 30 caracteres")
    String phone
) {}
