package com.fet.reportuni.controller;

import com.fet.reportuni.dto.AdminReporteItem;
import com.fet.reportuni.dto.AdminResumenResponse;
import com.fet.reportuni.dto.CambiarEstadoRequest;
import com.fet.reportuni.dto.CambiarPrioridadRequest;
import com.fet.reportuni.model.Bloque;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.TipoDano;
import com.fet.reportuni.service.AdminReporteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reportes")
public class AdminReporteController {

    private final AdminReporteService adminReporteService;

    public AdminReporteController(AdminReporteService adminReporteService) {
        this.adminReporteService = adminReporteService;
    }

    @GetMapping("/resumen")
    public AdminResumenResponse resumen() {
        return adminReporteService.resumen();
    }

    @GetMapping
    public List<AdminReporteItem> listar(
            @RequestParam(value = "estado", required = false) EstadoReporte estado,
            @RequestParam(value = "tipoDano", required = false) TipoDano tipoDano,
            @RequestParam(value = "prioridad", required = false) PrioridadReporte prioridad,
            @RequestParam(value = "bloque", required = false) Bloque bloque,
            @RequestParam(value = "q", required = false) String q
    ) {
        return adminReporteService.listar(estado, tipoDano, prioridad, bloque, q);
    }

    @PutMapping("/{id}/estado")
    public AdminReporteItem cambiarEstado(
            @PathVariable("id") Long id,
            @Valid @RequestBody CambiarEstadoRequest datos
    ) {
        return adminReporteService.cambiarEstado(id, datos.estado());
    }

    @PutMapping("/{id}/prioridad")
    public AdminReporteItem cambiarPrioridad(
            @PathVariable("id") Long id,
            @Valid @RequestBody CambiarPrioridadRequest datos
    ) {
        return adminReporteService.cambiarPrioridad(id, datos.prioridad());
    }
}