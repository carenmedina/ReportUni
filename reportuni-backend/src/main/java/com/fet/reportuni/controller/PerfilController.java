package com.fet.reportuni.controller;

import com.fet.reportuni.dto.CambiarPasswordRequest;
import com.fet.reportuni.dto.PerfilResponse;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.service.PerfilService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping
    public PerfilResponse obtener(@AuthenticationPrincipal Usuario usuario) {
        return perfilService.obtener(usuario);
    }

    @PostMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PerfilResponse subirFoto(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam("foto") MultipartFile foto
    ) {
        return perfilService.subirFoto(usuario, foto);
    }

    @GetMapping("/foto")
    public ResponseEntity<Resource> foto(@AuthenticationPrincipal Usuario usuario) {
        return perfilService.foto(usuario)
                .map(f -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(f.tipoContenido()))
                        .cacheControl(CacheControl.noCache().cachePrivate())
                        .body(f.recurso()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/foto")
    public PerfilResponse eliminarFoto(@AuthenticationPrincipal Usuario usuario) {
        return perfilService.eliminarFoto(usuario);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> cambiarPassword(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody CambiarPasswordRequest solicitud
    ) {
        perfilService.cambiarPassword(usuario, solicitud);
        return ResponseEntity.noContent().build();
    }
}