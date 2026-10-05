package com.fet.reportuni.dto;

import com.fet.reportuni.model.Bloque;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.TipoDano;
import com.fet.reportuni.model.TipoDano;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearReporteRequest(
        @NotNull(message = "Selecciona el tipo de daño")
        TipoDano tipoDano,

        PrioridadReporte prioridadEstimada,

        @NotNull(message = "Selecciona el bloque o edificio")
        Bloque bloque,

        @NotBlank(message = "Indica el espacio específico (ej: Salón 204)")
        @Size(max = 120, message = "El espacio específico no puede superar 120 caracteres")
        String espacioEspecifico,

        @NotBlank(message = "Describe el daño")
        @Size(min = 10, max = 1000, message = "La descripción debe tener entre 10 y 1000 caracteres")
        String descripcion,

        @DecimalMin(value = "-90.0", message = "La latitud no es válida")
        @DecimalMax(value = "90.0", message = "La latitud no es válida")
        Double latitud,

        @DecimalMin(value = "-180.0", message = "La longitud no es válida")
        @DecimalMax(value = "180.0", message = "La longitud no es válida")
        Double longitud,

        Boolean notificarPorCorreo
) {
}