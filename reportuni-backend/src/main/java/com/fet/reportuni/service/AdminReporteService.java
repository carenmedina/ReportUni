package com.fet.reportuni.service;

import com.fet.reportuni.dto.AdminReporteItem;
import com.fet.reportuni.dto.AdminResumenResponse;
import com.fet.reportuni.model.AreaAsignada;
import com.fet.reportuni.model.Bloque;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.Reporte;
import com.fet.reportuni.model.TipoDano;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.repository.ReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AdminReporteService {

    private static final int CANTIDAD_RECIENTES = 5;

    private static final int NOTA_MINIMA = 10;
    private static final int NOTA_MAXIMA = 1000;

    private final ReporteRepository reporteRepository;
    private final SeguimientoService seguimiento;
    private final AlmacenamientoFotosService almacenamientoFotos;

    public AdminReporteService(ReporteRepository reporteRepository,
                               SeguimientoService seguimiento,
                               AlmacenamientoFotosService almacenamientoFotos) {
        this.reporteRepository = reporteRepository;
        this.seguimiento = seguimiento;
        this.almacenamientoFotos = almacenamientoFotos;
    }

    public static class CambioInvalidoException extends RuntimeException {
        public CambioInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    @Transactional
    public AdminReporteItem cambiarEstado(Usuario actor, Long id, EstadoReporte nuevoEstado,
                                          String nota, List<MultipartFile> archivos) {
        if (nuevoEstado == null) {
            throw new CambioInvalidoException("Debes indicar el nuevo estado.");
        }

        Reporte reporte = buscar(id);

        if (reporte.getEstado() == nuevoEstado) {
            throw new CambioInvalidoException(
                    "El reporte ya está en estado " + etiqueta(nuevoEstado) + ".");
        }

        String explicacion = validarNota(nota, mensajeNotaEstado(nuevoEstado));
        List<AlmacenamientoFotosService.FotoValidada> fotos =
                almacenamientoFotos.validarEvidencia(archivos, nuevoEstado == EstadoReporte.RESUELTO);

        EstadoReporte anterior = reporte.getEstado();
        reporte.setEstado(nuevoEstado);
        reporte.setFechaActualizacion(LocalDateTime.now());
        reporteRepository.save(reporte);
        seguimiento.registrarCambioEstado(reporte, actor, anterior, nuevoEstado, explicacion, fotos);
        return aItem(reporte);
    }

    @Transactional
    public AdminReporteItem asignar(Usuario actor, Long id, AreaAsignada area, String nota) {
        if (area == null) {
            throw new CambioInvalidoException("Selecciona a quién se asigna el reporte.");
        }

        Reporte reporte = buscar(id);

        if (reporte.getAsignadoA() == area) {
            throw new CambioInvalidoException("El reporte ya está asignado a " + area.getEtiqueta() + ".");
        }

        String explicacion = null;
        if (nota != null && !nota.isBlank()) {
            explicacion = validarNota(nota, "La nota debe tener entre " + NOTA_MINIMA + " y "
                    + NOTA_MAXIMA + " caracteres.");
        }

        AreaAsignada anterior = reporte.getAsignadoA();
        reporte.setAsignadoA(area);
        reporte.setFechaActualizacion(LocalDateTime.now());
        reporteRepository.save(reporte);
        seguimiento.registrarAsignacion(reporte, actor, anterior, area, explicacion);
        return aItem(reporte);
    }

    @Transactional
    public AdminReporteItem agregarActualizacion(Usuario actor, Long id, String nota,
                                                 List<MultipartFile> archivos) {
        Reporte reporte = buscar(id);

        String explicacion = validarNota(nota, "Describe la novedad del proceso (entre "
                + NOTA_MINIMA + " y " + NOTA_MAXIMA + " caracteres).");
        List<AlmacenamientoFotosService.FotoValidada> fotos =
                almacenamientoFotos.validarEvidencia(archivos, false);

        reporte.setFechaActualizacion(LocalDateTime.now());
        reporteRepository.save(reporte);
        seguimiento.registrarActualizacion(reporte, actor, explicacion, fotos);
        return aItem(reporte);
    }

    private String validarNota(String nota, String mensaje) {
        String limpia = nota == null ? "" : nota.trim();
        if (limpia.length() < NOTA_MINIMA || limpia.length() > NOTA_MAXIMA) {
            throw new CambioInvalidoException(mensaje);
        }
        return limpia;
    }

    private String mensajeNotaEstado(EstadoReporte estado) {
        String rango = " (entre " + NOTA_MINIMA + " y " + NOTA_MAXIMA + " caracteres).";
        return switch (estado) {
            case EN_PROCESO -> "Describe qué se está haciendo para atender el reporte" + rango;
            case RESUELTO -> "Describe qué se hizo para resolver el reporte" + rango;
            case PENDIENTE -> "Indica el motivo para devolver el reporte a Pendiente" + rango;
        };
    }

    @Transactional
    public AdminReporteItem cambiarPrioridad(Usuario actor, Long id, PrioridadReporte nuevaPrioridad) {
        Reporte reporte = buscar(id);

        if (reporte.getPrioridadEstimada() == nuevaPrioridad) {
            throw new CambioInvalidoException(
                    "El reporte ya tiene prioridad " + etiqueta(nuevaPrioridad) + ".");
        }

        PrioridadReporte anterior = reporte.getPrioridadEstimada();
        reporte.setPrioridadEstimada(nuevaPrioridad);
        reporte.setFechaActualizacion(LocalDateTime.now());
        reporteRepository.save(reporte);
        seguimiento.registrarCambioPrioridad(reporte, actor, anterior, nuevaPrioridad);
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