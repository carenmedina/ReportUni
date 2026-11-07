package com.fet.reportuni.repository;

import com.fet.reportuni.model.HistorialReporte;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HistorialReporteRepository extends JpaRepository<HistorialReporte, Long> {

    @EntityGraph(attributePaths = {"realizadoPor", "fotos"})
    List<HistorialReporte> findByReporteIdOrderByFechaDescIdDesc(Long reporteId);

    @EntityGraph(attributePaths = "fotos")
    Optional<HistorialReporte> findByIdAndReporteId(Long id, Long reporteId);

    @Query("select count(distinct h.reporte.id) from HistorialReporte h where h.realizadoPor.id = :usuarioId")
    long contarReportesGestionados(@Param("usuarioId") Long usuarioId);

    @Query("select h.reporte.id, max(h.fecha) from HistorialReporte h "
            + "where h.tipo = com.fet.reportuni.model.TipoCambio.ESTADO and h.valorNuevo = 'RESUELTO' "
            + "and h.reporte.id in :ids group by h.reporte.id")
    List<Object[]> ultimasResoluciones(@Param("ids") Collection<Long> ids);
}