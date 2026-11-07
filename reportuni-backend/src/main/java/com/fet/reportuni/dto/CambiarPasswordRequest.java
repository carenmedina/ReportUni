package com.fet.reportuni.dto;

import jakarta.validation.constraints.NotBlank;

public record CambiarPasswordRequest(
        @NotBlank(message = "Escribe tu contraseña actual.")
        String actual,

        @NotBlank(message = "Escribe la nueva contraseña.")
        String nueva,

        @NotBlank(message = "Confirma la nueva contraseña.")
        String confirmacion
) {
}