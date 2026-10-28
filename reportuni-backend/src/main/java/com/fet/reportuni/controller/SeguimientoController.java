package com.fet.reportuni.controller;

import com.fet.reportuni.dto.HistorialItemResponse;
import com.fet.reportuni.dto.NotificacionResponse;
import com.fet.reportuni.dto.TotalNoLeidasResponse;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.service.ReporteService;
import com.fet.reportuni.service.SeguimientoService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SeguimientoController {

    private final SeguimientoService seguimientoService;

    public SeguimientoController(SeguimientoService seguimientoService) {
        this.seguimientoService = seguimientoService;
    }

    @GetMapping("/reportes/{id}/historial")
    public List<HistorialItemResponse> historial(@AuthenticationPrincipal Usuario usuario,
                                                 @PathVariable("id") Long id) {
        return seguimientoService.historial(usuario, id);
    }

    @GetMapping("/reportes/{id}/historial/{historialId}/fotos/{fotoId}")
    public ResponseEntity<Resource> fotoHistorial(@AuthenticationPrincipal Usuario usuario,
                                                  @PathVariable("id") Long id,
                                                  @PathVariable("historialId") Long historialId,
                                                  @PathVariable("fotoId") Long fotoId) {
        ReporteService.FotoDescargable foto = seguimientoService.fotoHistorial(usuario, id, historialId, fotoId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipoContenido()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(foto.recurso());
    }

    @GetMapping("/notificaciones")
    public List<NotificacionResponse> notificaciones(@AuthenticationPrincipal Usuario usuario) {
        return seguimientoService.misNotificaciones(usuario);
    }

    @GetMapping("/notificaciones/no-leidas")
    public TotalNoLeidasResponse noLeidas(@AuthenticationPrincipal Usuario usuario) {
        return seguimientoService.totalNoLeidas(usuario);
    }

    @PutMapping("/notificaciones/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@AuthenticationPrincipal Usuario usuario,
                                            @PathVariable("id") Long id) {
        seguimientoService.marcarLeida(usuario, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/notificaciones/leidas")
    public ResponseEntity<Void> marcarTodasLeidas(@AuthenticationPrincipal Usuario usuario) {
        seguimientoService.marcarTodasLeidas(usuario);
        return ResponseEntity.noContent().build();
    }
}