package com.fet.reportuni.repository;

import com.fet.reportuni.model.HistorialReporte;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistorialReporteRepository extends JpaRepository<HistorialReporte, Long> {

    @EntityGraph(attributePaths = {"realizadoPor", "fotos"})
    List<HistorialReporte> findByReporteIdOrderByFechaDescIdDesc(Long reporteId);

    @EntityGraph(attributePaths = "fotos")
    Optional<HistorialReporte> findByIdAndReporteId(Long id, Long reporteId);
}