package com.fet.reportuni.dto;

import com.fet.reportuni.model.Rol;

import java.time.LocalDateTime;

public record ComentarioResponse(
        Long id,
        Long reporteId,
        String texto,
        String autor,
        Rol rol,
        boolean propio,
        LocalDateTime fecha
) {
}