package com.fet.reportuni.dto;

import com.fet.reportuni.model.PrioridadReporte;
import jakarta.validation.constraints.NotNull;

public record CambiarPrioridadRequest(
        @NotNull(message = "Debes indicar la nueva prioridad.")
        PrioridadReporte prioridad
) {
}