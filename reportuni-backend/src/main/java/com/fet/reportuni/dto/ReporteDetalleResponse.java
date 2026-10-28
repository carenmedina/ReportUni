package com.fet.reportuni.dto;

import com.fet.reportuni.model.AreaAsignada;
import com.fet.reportuni.model.Bloque;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.TipoDano;

import java.time.LocalDateTime;
import java.util.List;

public record ReporteDetalleResponse(
        Long id,
        String codigo,
        TipoDano tipoDano,
        Bloque bloque,
        String espacioEspecifico,
        PrioridadReporte prioridadEstimada,
        EstadoReporte estado,
        String descripcion,
        Double latitud,
        Double longitud,
        boolean notificarPorCorreo,
        String reportadoPor,
        LocalDateTime fechaCreacion,
        AreaAsignada asignadoA,
        LocalDateTime ultimaActualizacion,
        List<FotoInfo> fotos
) {

    public record FotoInfo(Long id, String nombreOriginal, String tipoContenido, long tamanoBytes, int orden) {
    }
}