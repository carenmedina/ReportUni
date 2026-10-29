package com.fet.reportuni.repository;

import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoInstitucionalIgnoreCase(String correoInstitucional);

    boolean existsByCorreoInstitucionalIgnoreCase(String correoInstitucional);

    List<Usuario> findByRolAndActivoTrue(Rol rol);
}