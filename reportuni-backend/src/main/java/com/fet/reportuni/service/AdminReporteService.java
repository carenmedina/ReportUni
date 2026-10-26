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

    public static class CambioInvalidoException extends RuntimeException {
        public CambioInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    /**
     * Semana 7: el administrador cambia el estado de un reporte. Se permite pasar a cualquier
     * estado distinto del actual (incluido reabrir un reporte resuelto). El historial de cambios
     * y las notificaciones al estudiante se construyen en la Semana 8.
     */
    @Transactional
    public AdminReporteItem cambiarEstado(Long id, EstadoReporte nuevoEstado) {
        Reporte reporte = buscar(id);

        if (reporte.getEstado() == nuevoEstado) {
            throw new CambioInvalidoException(
                    "El reporte ya está en estado " + etiqueta(nuevoEstado) + ".");
        }

        reporte.setEstado(nuevoEstado);
        reporteRepository.save(reporte);
        return aItem(reporte);
    }

    @Transactional
    public AdminReporteItem cambiarPrioridad(Long id, PrioridadReporte nuevaPrioridad) {
        Reporte reporte = buscar(id);

        if (reporte.getPrioridadEstimada() == nuevaPrioridad) {
            throw new CambioInvalidoException(
                    "El reporte ya tiene prioridad " + etiqueta(nuevaPrioridad) + ".");
        }

        reporte.setPrioridadEstimada(nuevaPrioridad);
        reporteRepository.save(reporte);
        return aItem(reporte);
    }

    private Reporte buscar(Long id) {
        return reporteRepository.findById(id)
                .orElseThrow(() -> new ReporteService.ReporteNoEncontradoException("No encontramos ese reporte."));
    }

    private String etiqueta(EstadoReporte estado) {
        return switch (estado) {
            case PENDIENTE -> "Pendiente";
            case EN_PROCESO -> "En proceso";
            case RESUELTO -> "Resuelto";
        };
    }

    private String etiqueta(PrioridadReporte prioridad) {
        return switch (prioridad) {
            case ALTA -> "Alta";
            case MEDIA -> "Media";
            case BAJA -> "Baja";
        };
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