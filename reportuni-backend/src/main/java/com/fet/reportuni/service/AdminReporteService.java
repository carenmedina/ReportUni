package com.fet.reportuni.service;

import com.fet.reportuni.dto.AdminReporteItem;
import com.fet.reportuni.dto.AdminResumenResponse;
import com.fet.reportuni.model.Bloque;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.Reporte;
import com.fet.reportuni.model.TipoDano;
import com.fet.reportuni.repository.ReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AdminReporteService {

    private static final int CANTIDAD_RECIENTES = 5;

    private final ReporteRepository reporteRepository;

    public AdminReporteService(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    @Transactional(readOnly = true)
    public AdminResumenResponse resumen() {
        long pendientes = reporteRepository.countByEstado(EstadoReporte.PENDIENTE);
        long enProceso = reporteRepository.countByEstado(EstadoReporte.EN_PROCESO);
        long resueltos = reporteRepository.countByEstado(EstadoReporte.RESUELTO);
        long altaPendientes = reporteRepository
                .countByEstadoAndPrioridadEstimada(EstadoReporte.PENDIENTE, PrioridadReporte.ALTA);

        List<AdminReporteItem> recientes = reporteRepository.findAllByOrderByFechaCreacionDesc()
                .stream()
                .limit(CANTIDAD_RECIENTES)
                .map(this::aItem)
                .toList();

        return new AdminResumenResponse(
                pendientes + enProceso + resueltos,
                pendientes,
                enProceso,
                resueltos,
                altaPendientes,
                recientes
        );
    }

    /**
     * Lista todos los reportes (mas recientes primero). Cada filtro es opcional: si llega null o
     * vacio no se aplica. La busqueda de texto compara contra codigo, tipo, espacio, descripcion y
     * nombre o correo de quien reporto.
     */
    @Transactional(readOnly = true)
    public List<AdminReporteItem> listar(EstadoReporte estado, TipoDano tipoDano,
                                         PrioridadReporte prioridad, Bloque bloque, String texto) {
        String busqueda = texto == null ? "" : texto.trim().replace("#", "").toLowerCase(Locale.ROOT);

        return reporteRepository.findAllByOrderByFechaCreacionDesc()
                .stream()
                .filter(r -> estado == null || r.getEstado() == estado)
                .filter(r -> tipoDano == null || r.getTipoDano() == tipoDano)
                .filter(r -> prioridad == null || r.getPrioridadEstimada() == prioridad)
                .filter(r -> bloque == null || r.getBloque() == bloque)
                .filter(r -> busqueda.isEmpty() || coincide(r, busqueda))
                .map(this::aItem)
                .toList();
    }

    private boolean coincide(Reporte r, String busqueda) {
        return codigo(r).contains(busqueda)
                || contiene(r.getTipoDano().name().replace('_', ' '), busqueda)
                || contiene(r.getEspacioEspecifico(), busqueda)
                || contiene(r.getDescripcion(), busqueda)
                || contiene(r.getUsuario().getNombreCompleto(), busqueda)
                || contiene(r.getUsuario().getCorreoInstitucional(), busqueda);
    }

    private boolean contiene(String valor, String busqueda) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(busqueda);
    }

    private AdminReporteItem aItem(Reporte r) {
        return new AdminReporteItem(
                r.getId(),
                codigo(r),
                r.getTipoDano(),
                r.getBloque(),
                r.getEspacioEspecifico(),
                r.getPrioridadEstimada(),
                r.getEstado(),
                r.getUsuario().getNombreCompleto(),
                r.getUsuario().getCorreoInstitucional(),
                r.getFechaCreacion()
        );
    }

    private String codigo(Reporte r) {
        return String.format("%04d", r.getId());
    }
}