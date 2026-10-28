package com.fet.reportuni.repository;

import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.Reporte;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    @EntityGraph(attributePaths = "fotos")
    List<Reporte> findConFotosByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    @EntityGraph(attributePaths = "fotos")
    Optional<Reporte> findConFotosById(Long id);

    @EntityGraph(attributePaths = "fotos")
    Optional<Reporte> findConFotosByIdAndUsuarioId(Long id, Long usuarioId);

    @EntityGraph(attributePaths = "usuario")
    List<Reporte> findAllByOrderByFechaCreacionDesc();

    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);

    long countByEstado(EstadoReporte estado);

    long countByEstadoAndPrioridadEstimada(EstadoReporte estado, PrioridadReporte prioridad);
}