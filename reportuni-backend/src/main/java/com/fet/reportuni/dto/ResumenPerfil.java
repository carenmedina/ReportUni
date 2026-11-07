package com.fet.reportuni.dto;

public record ResumenPerfil(
        Long total,
        Long pendientes,
        Long enProceso,
        Long resueltos,
        Long gestionados
) {
}