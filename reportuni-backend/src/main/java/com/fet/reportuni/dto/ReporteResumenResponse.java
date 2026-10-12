package com.fet.reportuni.dto;

import com.fet.reportuni.model.Bloque;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.TipoDano;

import java.time.LocalDateTime;

public record ReporteResumenResponse(
        Long id,
        String codigo,
        TipoDano tipoDano,
        Bloque bloque,
        String espacioEspecifico,
        PrioridadReporte prioridadEstimada,
        EstadoReporte estado,
        LocalDateTime fechaCreacion,
        int totalFotos
) {
}