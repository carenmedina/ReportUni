package com.fet.reportuni.dto;

import com.fet.reportuni.model.Bloque;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.TipoDano;

import java.time.LocalDateTime;

public record AdminReporteItem(
        Long id,
        String codigo,
        TipoDano tipoDano,
        Bloque bloque,
        String espacioEspecifico,
        PrioridadReporte prioridadEstimada,
        EstadoReporte estado,
        String reportadoPor,
        String reportadoPorCorreo,
        LocalDateTime fechaCreacion
) {
}