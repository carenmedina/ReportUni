package com.fet.reportuni.controller;

import com.fet.reportuni.dto.CrearReporteRequest;
import com.fet.reportuni.dto.ReporteCreadoResponse;
import com.fet.reportuni.dto.ReporteDetalleResponse;
import com.fet.reportuni.dto.ReporteResumenResponse;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.service.ReporteService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
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

    @GetMapping("/mis")
    public List<ReporteResumenResponse> listarMios(@AuthenticationPrincipal Usuario usuario) {
        return reporteService.listarMios(usuario);
    }

    @GetMapping("/{id}")
    public ReporteDetalleResponse detalle(@AuthenticationPrincipal Usuario usuario, @PathVariable("id") Long id) {
        return reporteService.detalle(usuario, id);
    }

    @GetMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<Resource> foto(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable("id") Long id,
            @PathVariable("fotoId") Long fotoId
    ) {
        ReporteService.FotoDescargable foto = reporteService.foto(usuario, id, fotoId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipoContenido()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(foto.recurso());
    }
}