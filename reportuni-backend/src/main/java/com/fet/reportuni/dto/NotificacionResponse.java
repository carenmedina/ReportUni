package com.fet.reportuni.dto;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long id,
        Long reporteId,
        String codigo,
        String mensaje,
        boolean leida,
        LocalDateTime fechaCreacion
) {
}