package com.fet.reportuni.dto;

import java.util.List;

public record CargaEstudiantesResponse(
        int filasLeidas,
        int creados,
        int actualizados,
        int rechazados,
        List<ErrorFila> errores
) {

    public record ErrorFila(int fila, String correo, String motivo) {
    }
}