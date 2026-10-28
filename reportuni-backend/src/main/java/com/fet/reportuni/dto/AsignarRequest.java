package com.fet.reportuni.dto;

import com.fet.reportuni.model.AreaAsignada;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AsignarRequest(
        @NotNull(message = "Selecciona a quién se asigna el reporte.")
        AreaAsignada area,

        @Size(max = 1000, message = "La nota no puede superar 1000 caracteres.")
        String nota
) {
}