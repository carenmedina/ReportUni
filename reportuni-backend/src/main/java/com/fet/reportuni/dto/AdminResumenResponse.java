package com.fet.reportuni.dto;

import java.util.List;

public record AdminResumenResponse(
        long total,
        long pendientes,
        long enProceso,
        long resueltos,
        long altaPrioridadPendientes,
        List<AdminReporteItem> recientes
) {
}