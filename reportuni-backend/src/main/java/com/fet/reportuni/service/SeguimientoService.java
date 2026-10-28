package com.fet.reportuni.service;

import com.fet.reportuni.dto.HistorialItemResponse;
import com.fet.reportuni.dto.NotificacionResponse;
import com.fet.reportuni.dto.TotalNoLeidasResponse;
import com.fet.reportuni.model.AreaAsignada;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.HistorialFoto;
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
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SeguimientoService {

    private final HistorialReporteRepository historialRepository;
    private final NotificacionRepository notificacionRepository;
    private final ReporteRepository reporteRepository;
    private final AlmacenamientoFotosService almacenamientoFotos;
    private final ConsultaFotosService consultaFotos;

    public SeguimientoService(HistorialReporteRepository historialRepository,
                              NotificacionRepository notificacionRepository,
                              ReporteRepository reporteRepository,
                              AlmacenamientoFotosService almacenamientoFotos,
                              ConsultaFotosService consultaFotos) {
        this.historialRepository = historialRepository;
        this.notificacionRepository = notificacionRepository;
        this.reporteRepository = reporteRepository;
        this.almacenamientoFotos = almacenamientoFotos;
        this.consultaFotos = consultaFotos;
    }

    @Transactional
    public void registrarCambioEstado(Reporte reporte, Usuario actor, EstadoReporte anterior,
                                      EstadoReporte nuevo, String nota,
                                      List<AlmacenamientoFotosService.FotoValidada> fotos) {
        guardarHistorial(reporte, actor, TipoCambio.ESTADO, anterior.name(), nuevo.name(), nota, fotos);
        notificarAlDueno(reporte, actor,
                "Tu reporte #" + codigo(reporte.getId()) + " cambió a estado «" + etiqueta(nuevo) + "».");
    }

    @Transactional
    public void registrarCambioPrioridad(Reporte reporte, Usuario actor,
                                         PrioridadReporte anterior, PrioridadReporte nueva) {
        guardarHistorial(reporte, actor, TipoCambio.PRIORIDAD, anterior.name(), nueva.name(), null, List.of());
        notificarAlDueno(reporte, actor,
                "La prioridad de tu reporte #" + codigo(reporte.getId()) + " ahora es «" + etiqueta(nueva) + "».");
    }

    @Transactional
    public void registrarAsignacion(Reporte reporte, Usuario actor, AreaAsignada anterior,
                                    AreaAsignada nueva, String nota) {
        guardarHistorial(reporte, actor, TipoCambio.ASIGNACION,
                anterior == null ? "" : anterior.name(), nueva.name(), nota, List.of());
        notificarAlDueno(reporte, actor,
                "Tu reporte #" + codigo(reporte.getId()) + " fue asignado a " + nueva.getEtiqueta() + ".");
    }

    @Transactional
    public void registrarActualizacion(Reporte reporte, Usuario actor, String nota,
                                       List<AlmacenamientoFotosService.FotoValidada> fotos) {
        guardarHistorial(reporte, actor, TipoCambio.ACTUALIZACION, "", "", nota, fotos);
        notificarAlDueno(reporte, actor,
                "Hay una nueva actualización en tu reporte #" + codigo(reporte.getId()) + ".");
    }

    private void guardarHistorial(Reporte reporte, Usuario actor, TipoCambio tipo, String anterior,
                                  String nuevo, String nota,
                                  List<AlmacenamientoFotosService.FotoValidada> fotos) {
        HistorialReporte registro = HistorialReporte.builder()
                .reporte(reporte)
                .tipo(tipo)
                .valorAnterior(anterior)
                .valorNuevo(nuevo)
                .nota(nota)
                .realizadoPor(actor)
                .build();

        List<String> guardados = new ArrayList<>();
        try {
            int orden = 1;
            for (AlmacenamientoFotosService.FotoValidada foto : fotos) {
                String nombreArchivo = almacenamientoFotos.guardar(reporte.getId(), foto);
                guardados.add(nombreArchivo);
                registro.agregarFoto(HistorialFoto.builder()
                        .nombreOriginal(nombreOriginal(foto))
                        .nombreArchivo(nombreArchivo)
                        .tipoContenido(foto.tipoContenido())
                        .tamanoBytes(foto.archivo().getSize())
                        .orden(orden++)
                        .build());
            }
            historialRepository.save(registro);
        } catch (RuntimeException ex) {
            guardados.forEach(nombre -> almacenamientoFotos.eliminarArchivo(reporte.getId(), nombre));
            throw ex;
        }
    }

    private String nombreOriginal(AlmacenamientoFotosService.FotoValidada foto) {
        String nombre = foto.archivo().getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            return "foto";
        }
        String soloNombre = nombre.replace('\\', '/');
        soloNombre = soloNombre.substring(soloNombre.lastIndexOf('/') + 1);
        return soloNombre.length() > 255 ? soloNombre.substring(0, 255) : soloNombre;
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

    @Transactional(readOnly = true)
    public List<HistorialItemResponse> historial(Usuario usuario, Long reporteId) {
        verificarAcceso(usuario, reporteId);

        return historialRepository.findByReporteIdOrderByFechaDescIdDesc(reporteId).stream()
                .map(h -> new HistorialItemResponse(
                        h.getId(),
                        h.getTipo(),
                        h.getValorAnterior(),
                        h.getValorNuevo(),
                        h.getNota(),
                        h.getRealizadoPor().getNombreCompleto(),
                        h.getFecha(),
                        h.getFotos().stream()
                                .map(f -> new HistorialItemResponse.FotoHistorialInfo(
                                        f.getId(), f.getNombreOriginal(), f.getTipoContenido(), f.getTamanoBytes()))
                                .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReporteService.FotoDescargable fotoHistorial(Usuario usuario, Long reporteId,
                                                        Long historialId, Long fotoId) {
        verificarAcceso(usuario, reporteId);

        HistorialReporte registro = historialRepository.findByIdAndReporteId(historialId, reporteId)
                .orElseThrow(() -> new ReporteService.ReporteNoEncontradoException("No encontramos esa fotografía."));

        HistorialFoto foto = registro.getFotos().stream()
                .filter(f -> f.getId().equals(fotoId))
                .findFirst()
                .orElseThrow(() -> new ReporteService.ReporteNoEncontradoException("No encontramos esa fotografía."));

        Resource recurso = consultaFotos.cargar(reporteId, foto.getNombreArchivo())
                .orElseThrow(() -> new ReporteService.ReporteNoEncontradoException("La fotografía ya no está disponible."));

        return new ReporteService.FotoDescargable(recurso, foto.getTipoContenido());
    }

    private void verificarAcceso(Usuario usuario, Long reporteId) {
        boolean puedeVerlo = usuario.getRol() == Rol.ADMINISTRADOR
                ? reporteRepository.existsById(reporteId)
                : reporteRepository.existsByIdAndUsuarioId(reporteId, usuario.getId());

        if (!puedeVerlo) {
            // igual que en el detalle: un estudiante no se entera de que el reporte existe
            throw new ReporteService.ReporteNoEncontradoException("No encontramos ese reporte.");
        }
    }

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