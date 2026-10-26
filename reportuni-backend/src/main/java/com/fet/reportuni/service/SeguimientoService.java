package com.fet.reportuni.service;

import com.fet.reportuni.dto.HistorialItemResponse;
import com.fet.reportuni.dto.NotificacionResponse;
import com.fet.reportuni.dto.TotalNoLeidasResponse;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.HistorialReporte;
import com.fet.reportuni.model.Notificacion;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.Reporte;
import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.TipoCambio;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.repository.HistorialReporteRepository;
import com.fet.reportuni.repository.NotificacionRepository;
import com.fet.reportuni.repository.ReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Semana 8: historial de cambios de un reporte y notificaciones dentro de la aplicacion.
 * Cada vez que el administrador cambia el estado o la prioridad se guarda un registro en el
 * historial y se avisa al estudiante dueno del reporte.
 */
@Service
public class SeguimientoService {

    private final HistorialReporteRepository historialRepository;
    private final NotificacionRepository notificacionRepository;
    private final ReporteRepository reporteRepository;

    public SeguimientoService(HistorialReporteRepository historialRepository,
                              NotificacionRepository notificacionRepository,
                              ReporteRepository reporteRepository) {
        this.historialRepository = historialRepository;
        this.notificacionRepository = notificacionRepository;
        this.reporteRepository = reporteRepository;
    }

    // ---------- registro de cambios (lo llama AdminReporteService) ----------

    @Transactional
    public void registrarCambioEstado(Reporte reporte, Usuario actor,
                                      EstadoReporte anterior, EstadoReporte nuevo) {
        guardarHistorial(reporte, actor, TipoCambio.ESTADO, anterior.name(), nuevo.name());
        notificarAlDueno(reporte, actor,
                "Tu reporte #" + codigo(reporte.getId()) + " cambió a estado «" + etiqueta(nuevo) + "».");
    }

    @Transactional
    public void registrarCambioPrioridad(Reporte reporte, Usuario actor,
                                         PrioridadReporte anterior, PrioridadReporte nueva) {
        guardarHistorial(reporte, actor, TipoCambio.PRIORIDAD, anterior.name(), nueva.name());
        notificarAlDueno(reporte, actor,
                "La prioridad de tu reporte #" + codigo(reporte.getId()) + " ahora es «" + etiqueta(nueva) + "».");
    }

    private void guardarHistorial(Reporte reporte, Usuario actor, TipoCambio tipo, String anterior, String nuevo) {
        historialRepository.save(HistorialReporte.builder()
                .reporte(reporte)
                .tipo(tipo)
                .valorAnterior(anterior)
                .valorNuevo(nuevo)
                .realizadoPor(actor)
                .build());
    }

    private void notificarAlDueno(Reporte reporte, Usuario actor, String mensaje) {
        Usuario dueno = reporte.getUsuario();
        if (dueno.getId().equals(actor.getId())) {
            return; // nadie se notifica a si mismo
        }
        notificacionRepository.save(Notificacion.builder()
                .usuario(dueno)
                .reporteId(reporte.getId())
                .mensaje(mensaje)
                .build());
    }

    // ---------- consulta del historial ----------

    @Transactional(readOnly = true)
    public List<HistorialItemResponse> historial(Usuario usuario, Long reporteId) {
        boolean puedeVerlo = usuario.getRol() == Rol.ADMINISTRADOR
                ? reporteRepository.existsById(reporteId)
                : reporteRepository.existsByIdAndUsuarioId(reporteId, usuario.getId());

        if (!puedeVerlo) {
            // igual que en el detalle: un estudiante no se entera de que el reporte existe
            throw new ReporteService.ReporteNoEncontradoException("No encontramos ese reporte.");
        }

        return historialRepository.findByReporteIdOrderByFechaDescIdDesc(reporteId).stream()
                .map(h -> new HistorialItemResponse(
                        h.getId(),
                        h.getTipo(),
                        h.getValorAnterior(),
                        h.getValorNuevo(),
                        h.getRealizadoPor().getNombreCompleto(),
                        h.getFecha()))
                .toList();
    }

    // ---------- notificaciones del usuario ----------

    @Transactional(readOnly = true)
    public List<NotificacionResponse> misNotificaciones(Usuario usuario) {
        return notificacionRepository.findTop30ByUsuarioIdOrderByFechaCreacionDescIdDesc(usuario.getId())
                .stream()
                .map(n -> new NotificacionResponse(
                        n.getId(),
                        n.getReporteId(),
                        codigo(n.getReporteId()),
                        n.getMensaje(),
                        n.isLeida(),
                        n.getFechaCreacion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TotalNoLeidasResponse totalNoLeidas(Usuario usuario) {
        return new TotalNoLeidasResponse(notificacionRepository.countByUsuarioIdAndLeidaFalse(usuario.getId()));
    }

    @Transactional
    public void marcarLeida(Usuario usuario, Long notificacionId) {
        Notificacion notificacion = notificacionRepository.findByIdAndUsuarioId(notificacionId, usuario.getId())
                .orElseThrow(() -> new ReporteService.ReporteNoEncontradoException("No encontramos esa notificación."));
        notificacion.setLeida(true);
        notificacionRepository.save(notificacion);
    }

    @Transactional
    public void marcarTodasLeidas(Usuario usuario) {
        List<Notificacion> pendientes = notificacionRepository.findByUsuarioIdAndLeidaFalse(usuario.getId());
        pendientes.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(pendientes);
    }

    // ---------- utilidades ----------

    private String codigo(Long reporteId) {
        return String.format("%04d", reporteId);
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
}