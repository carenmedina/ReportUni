package com.fet.reportuni.dto;

import com.fet.reportuni.model.EstadoReporte;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(
        @NotNull(message = "Debes indicar el nuevo estado.")
        EstadoReporte estado
) {
}