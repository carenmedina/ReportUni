package com.fet.reportuni.controller;

import com.fet.reportuni.dto.AdminReporteItem;
import com.fet.reportuni.dto.AdminResumenResponse;
import com.fet.reportuni.dto.AsignarRequest;
import com.fet.reportuni.dto.CambiarPrioridadRequest;
import com.fet.reportuni.model.Bloque;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.TipoDano;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.service.AdminReporteService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/{id}/estado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminReporteItem cambiarEstado(
            @AuthenticationPrincipal Usuario actor,
            @PathVariable("id") Long id,
            @RequestParam(value = "estado", required = false) EstadoReporte estado,
            @RequestParam(value = "nota", required = false) String nota,
            @RequestParam(value = "fotos", required = false) List<MultipartFile> fotos
    ) {
        return adminReporteService.cambiarEstado(actor, id, estado, nota, fotos);
    }

    @PutMapping("/{id}/asignacion")
    public AdminReporteItem asignar(
            @AuthenticationPrincipal Usuario actor,
            @PathVariable("id") Long id,
            @Valid @RequestBody AsignarRequest datos
    ) {
        return adminReporteService.asignar(actor, id, datos.area(), datos.nota());
    }

    @PostMapping(value = "/{id}/actualizaciones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminReporteItem agregarActualizacion(
            @AuthenticationPrincipal Usuario actor,
            @PathVariable("id") Long id,
            @RequestParam(value = "nota", required = false) String nota,
            @RequestParam(value = "fotos", required = false) List<MultipartFile> fotos
    ) {
        return adminReporteService.agregarActualizacion(actor, id, nota, fotos);
    }

    @PutMapping("/{id}/prioridad")
    public AdminReporteItem cambiarPrioridad(
            @AuthenticationPrincipal Usuario actor,
            @PathVariable("id") Long id,
            @Valid @RequestBody CambiarPrioridadRequest datos
    ) {
        return adminReporteService.cambiarPrioridad(actor, id, datos.prioridad());
    }
}