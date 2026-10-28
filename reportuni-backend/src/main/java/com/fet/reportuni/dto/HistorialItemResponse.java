package com.fet.reportuni.dto;

import com.fet.reportuni.model.TipoCambio;

import java.time.LocalDateTime;
import java.util.List;

public record HistorialItemResponse(
        Long id,
        TipoCambio tipo,
        String valorAnterior,
        String valorNuevo,
        String nota,
        String realizadoPor,
        LocalDateTime fecha,
        List<FotoHistorialInfo> fotos
) {

    public record FotoHistorialInfo(Long id, String nombreOriginal, String tipoContenido, long tamanoBytes) {
    }
}