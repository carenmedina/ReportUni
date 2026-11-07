package com.fet.reportuni.service;

import com.fet.reportuni.dto.ConteoEstadistica;
import com.fet.reportuni.dto.EstadisticasResponse;
import com.fet.reportuni.model.Bloque;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.Reporte;
import com.fet.reportuni.model.TipoDano;
import com.fet.reportuni.repository.HistorialReporteRepository;
import com.fet.reportuni.repository.ReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EstadisticasService {

    private final ReporteRepository reporteRepository;
    private final HistorialReporteRepository historialRepository;

    public EstadisticasService(ReporteRepository reporteRepository, HistorialReporteRepository historialRepository) {
        this.reporteRepository = reporteRepository;
        this.historialRepository = historialRepository;
    }

    public static class RangoInvalidoException extends RuntimeException {
        public RangoInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    /** Los reportes se filtran por fecha de creacion; desde y hasta son inclusivos y opcionales. */
    @Transactional(readOnly = true)
    public EstadisticasResponse calcular(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new RangoInvalidoException("La fecha inicial no puede ser posterior a la fecha final.");
        }

        LocalDateTime inicio = desde == null ? null : desde.atStartOfDay();
        LocalDateTime fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay();

        List<Reporte> reportes = reporteRepository.findAll().stream()
                .filter(r -> dentroDelRango(r.getFechaCreacion(), inicio, fin))
                .toList();

        List<ConteoEstadistica> porTipo = ranking(reportes, Reporte::getTipoDano, TipoDano.values());
        List<ConteoEstadistica> porBloque = ranking(reportes, Reporte::getBloque, Bloque.values());
        List<ConteoEstadistica> porPrioridad = enOrdenFijo(reportes, Reporte::getPrioridadEstimada,
                new PrioridadReporte[]{PrioridadReporte.ALTA, PrioridadReporte.MEDIA, PrioridadReporte.BAJA});

        double[] resolucion = tiempoDeResolucion(reportes);
        Double promedio = resolucion[1] == 0 ? null : Math.round(resolucion[0] / resolucion[1] * 10) / 10.0;

        return new EstadisticasResponse(reportes.size(), porTipo, porBloque, porPrioridad,
                promedio, (long) resolucion[1]);
    }

    private boolean dentroDelRango(LocalDateTime fecha, LocalDateTime inicio, LocalDateTime fin) {
        if (fecha == null) {
            return inicio == null && fin == null;
        }
        return (inicio == null || !fecha.isBefore(inicio)) && (fin == null || fecha.isBefore(fin));
    }

    private <E extends Enum<E>> List<ConteoEstadistica> ranking(List<Reporte> reportes,
                                                                Function<Reporte, E> campo, E[] valores) {
        return enOrdenFijo(reportes, campo, valores).stream()
                .sorted(Comparator.comparingLong(ConteoEstadistica::total).reversed())
                .toList();
    }

    private <E extends Enum<E>> List<ConteoEstadistica> enOrdenFijo(List<Reporte> reportes,
                                                                    Function<Reporte, E> campo, E[] valores) {
        Map<E, Long> conteos = reportes.stream()
                .collect(Collectors.groupingBy(campo, Collectors.counting()));
        return java.util.Arrays.stream(valores)
                .map(v -> new ConteoEstadistica(v.name(), conteos.getOrDefault(v, 0L)))
                .toList();
    }

    /** Devuelve {suma de dias, cantidad de reportes resueltos medidos}. */
    private double[] tiempoDeResolucion(List<Reporte> reportes) {
        List<Reporte> resueltos = reportes.stream()
                .filter(r -> r.getEstado() == EstadoReporte.RESUELTO)
                .toList();
        if (resueltos.isEmpty()) {
            return new double[]{0, 0};
        }

        Map<Long, LocalDateTime> fechasResolucion = new HashMap<>();
        for (Object[] fila : historialRepository.ultimasResoluciones(
                resueltos.stream().map(Reporte::getId).toList())) {
            fechasResolucion.put((Long) fila[0], (LocalDateTime) fila[1]);
        }

        double totalDias = 0;
        int medidos = 0;
        for (Reporte reporte : resueltos) {
            LocalDateTime resuelto = fechasResolucion.getOrDefault(reporte.getId(), reporte.getFechaActualizacion());
            LocalDateTime creado = reporte.getFechaCreacion();
            if (resuelto == null || creado == null || resuelto.isBefore(creado)) {
                continue;
            }
            totalDias += Duration.between(creado, resuelto).toMinutes() / 1440.0;
            medidos++;
        }
        return new double[]{totalDias, medidos};
    }
}