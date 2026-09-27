package com.fet.reportuni.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El correo institucional es obligatorio")
        @Email(message = "Ingresa un correo institucional válido")
        String correoInstitucional,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
