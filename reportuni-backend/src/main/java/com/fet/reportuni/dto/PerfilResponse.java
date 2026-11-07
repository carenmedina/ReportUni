package com.fet.reportuni.dto;

public record PerfilResponse(
        String nombreCompleto,
        String correoInstitucional,
        String rol,
        boolean tieneFoto,
        ResumenPerfil resumen
) {
}