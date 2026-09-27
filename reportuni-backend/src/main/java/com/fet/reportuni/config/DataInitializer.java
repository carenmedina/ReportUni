package com.fet.reportuni.config;

import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        String passwordHash = passwordEncoder.encode("ReportUni2026");

        usuarioRepository.save(Usuario.builder()
                .nombreCompleto("Estudiante Uno")
                .correoInstitucional("estudiante1@fet.edu.co")
                .passwordHash(passwordHash)
                .rol(Rol.ESTUDIANTE)
                .activo(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nombreCompleto("Estudiante Dos")
                .correoInstitucional("estudiante2@fet.edu.co")
                .passwordHash(passwordHash)
                .rol(Rol.ESTUDIANTE)
                .activo(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nombreCompleto("Administrador ReportUni")
                .correoInstitucional("admin@fet.edu.co")
                .passwordHash(passwordHash)
                .rol(Rol.ADMINISTRADOR)
                .activo(true)
                .build());

        System.out.println(">> DataInitializer: usuarios de prueba creados (password: ReportUni2026)");
    }
}
