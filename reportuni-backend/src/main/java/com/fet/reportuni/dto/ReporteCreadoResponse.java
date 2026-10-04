package com.fet.reportuni.dto;

import com.fet.reportuni.model.EstadoReporte;

import java.time.LocalDateTime;

public record ReporteCreadoResponse(
        Long id,
        String codigo,
        EstadoReporte estado,
        LocalDateTime fechaCreacion,
        int totalFotos
) {
}