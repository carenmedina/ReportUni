package com.fet.reportuni.controller;

import com.fet.reportuni.dto.NavegacionResponse;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.service.NavegacionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone la navegacion base para el usuario autenticado. El filtro JWT
 * coloca el Usuario ya resuelto en el contexto de seguridad, por lo que
 * aqui simplemente se usa @AuthenticationPrincipal para obtenerlo.
 */
@RestController
@RequestMapping("/api/navegacion")
public class NavegacionController {

    private final NavegacionService navegacionService;

    public NavegacionController(NavegacionService navegacionService) {
        this.navegacionService = navegacionService;
    }

    @GetMapping("/menu")
    public NavegacionResponse menu(@AuthenticationPrincipal Usuario usuario) {
        return navegacionService.obtenerMenu(usuario);
    }
}
