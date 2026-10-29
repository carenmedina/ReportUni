package com.fet.reportuni.controller;

import com.fet.reportuni.dto.ComentarioRequest;
import com.fet.reportuni.dto.ComentarioResponse;
import com.fet.reportuni.dto.NotificacionResponse;
import com.fet.reportuni.dto.TotalNoLeidasResponse;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.service.ComentarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ComentarioController {

    private final ComentarioService comentarioService;

    public ComentarioController(ComentarioService comentarioService) {
        this.comentarioService = comentarioService;
    }

    @GetMapping("/reportes/{id}/comentarios")
    public List<ComentarioResponse> listar(@AuthenticationPrincipal Usuario usuario,
                                           @PathVariable("id") Long id) {
        return comentarioService.listar(usuario, id);
    }

    @PostMapping("/reportes/{id}/comentarios")
    public ResponseEntity<ComentarioResponse> comentar(@AuthenticationPrincipal Usuario usuario,
                                                       @PathVariable("id") Long id,
                                                       @RequestBody ComentarioRequest solicitud) {
        ComentarioResponse creado = comentarioService.comentar(usuario, id, solicitud.texto());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/notificaciones/comentarios")
    public List<NotificacionResponse> avisos(@AuthenticationPrincipal Usuario usuario) {
        return comentarioService.misAvisos(usuario);
    }

    @GetMapping("/notificaciones/comentarios/no-leidas")
    public TotalNoLeidasResponse noLeidos(@AuthenticationPrincipal Usuario usuario) {
        return comentarioService.totalNoLeidos(usuario);
    }

    @PutMapping("/notificaciones/comentarios/leidas")
    public ResponseEntity<Void> marcarTodosLeidos(@AuthenticationPrincipal Usuario usuario) {
        comentarioService.marcarTodosLeidos(usuario);
        return ResponseEntity.noContent().build();
    }
}