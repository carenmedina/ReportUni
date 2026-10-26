package com.fet.reportuni.controller;

import com.fet.reportuni.dto.HistorialItemResponse;
import com.fet.reportuni.dto.NotificacionResponse;
import com.fet.reportuni.dto.TotalNoLeidasResponse;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.service.SeguimientoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Semana 8: historial de un reporte y notificaciones del usuario autenticado.
 * Todas las rutas exigen sesion (SecurityConfig: anyRequest().authenticated()).
 */
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