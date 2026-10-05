package com.fet.reportuni.controller;

import com.fet.reportuni.dto.CrearReporteRequest;
import com.fet.reportuni.dto.ReporteCreadoResponse;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.service.ReporteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReporteCreadoResponse> crear(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @ModelAttribute CrearReporteRequest datos,
            @RequestParam(value = "fotos", required = false) List<MultipartFile> fotos
    ) {
        ReporteCreadoResponse respuesta = reporteService.crear(usuario, datos, fotos);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}