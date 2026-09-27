package com.fet.reportuni.service;

import com.fet.reportuni.dto.LoginRequest;
import com.fet.reportuni.dto.LoginResponse;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.repository.UsuarioRepository;
import com.fet.reportuni.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dominioInstitucional;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.auth.dominio-institucional}") String dominioInstitucional
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dominioInstitucional = dominioInstitucional;
    }

    public static class CredencialesInvalidasException extends RuntimeException {
        public CredencialesInvalidasException(String mensaje) {
            super(mensaje);
        }
    }

    public LoginResponse login(LoginRequest request) {
        String correo = request.correoInstitucional().trim();

        if (!correo.toLowerCase().endsWith("@" + dominioInstitucional.toLowerCase())) {
            throw new CredencialesInvalidasException(
                    "Debes ingresar con tu correo institucional (@" + dominioInstitucional + ")");
        }

        Usuario usuario = usuarioRepository.findByCorreoInstitucionalIgnoreCase(correo)
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos"));

        if (!usuario.isActivo()) {
            throw new CredencialesInvalidasException("El usuario se encuentra inactivo");
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos");
        }

        String token = jwtService.generarToken(
                usuario.getCorreoInstitucional(),
                Map.of("rol", usuario.getRol().name(), "nombre", usuario.getNombreCompleto())
        );

        return new LoginResponse(
                token,
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreoInstitucional(),
                usuario.getRol().name()
        );
    }
}
