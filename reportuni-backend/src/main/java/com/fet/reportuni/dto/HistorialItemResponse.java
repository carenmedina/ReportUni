package com.fet.reportuni.dto;

import com.fet.reportuni.model.TipoCambio;

import java.time.LocalDateTime;

public record HistorialItemResponse(
        Long id,
        TipoCambio tipo,
        String valorAnterior,
        String valorNuevo,
        String realizadoPor,
        LocalDateTime fecha
) {
}