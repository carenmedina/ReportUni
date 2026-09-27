package com.fet.reportuni.dto;

public record LoginResponse(
        String token,
        Long usuarioId,
        String nombreCompleto,
        String correoInstitucional,
        String rol
) {
}
