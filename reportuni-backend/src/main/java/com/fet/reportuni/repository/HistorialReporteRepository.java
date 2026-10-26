package com.fet.reportuni.repository;

import com.fet.reportuni.model.HistorialReporte;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialReporteRepository extends JpaRepository<HistorialReporte, Long> {

    @EntityGraph(attributePaths = "realizadoPor")
    List<HistorialReporte> findByReporteIdOrderByFechaDescIdDesc(Long reporteId);
}