package com.fet.reportuni.dto;

import java.util.List;

public record EstadisticasResponse(
        long totalReportes,
        List<ConteoEstadistica> porTipo,
        List<ConteoEstadistica> porBloque,
        List<ConteoEstadistica> porPrioridad,
        Double promedioResolucionDias,
        long reportesResueltosMedidos
) {
}