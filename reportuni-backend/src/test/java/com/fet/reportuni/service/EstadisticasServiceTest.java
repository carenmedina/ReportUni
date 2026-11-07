package com.fet.reportuni.service;

import com.fet.reportuni.dto.EstadisticasResponse;
import com.fet.reportuni.model.Bloque;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.Reporte;
import com.fet.reportuni.model.TipoDano;
import com.fet.reportuni.repository.HistorialReporteRepository;
import com.fet.reportuni.repository.ReporteRepository;
import com.fet.reportuni.service.EstadisticasService.RangoInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstadisticasServiceTest {

    @Mock
    private ReporteRepository reporteRepository;

    @Mock
    private HistorialReporteRepository historialRepository;

    private EstadisticasService servicio;

    private Reporte r1;
    private Reporte r2;
    private Reporte r3;

    @BeforeEach
    void preparar() {
        servicio = new EstadisticasService(reporteRepository, historialRepository);

        r1 = reporte(1L, TipoDano.FUGA_AGUA, Bloque.BLOQUE_1, PrioridadReporte.ALTA,
                EstadoReporte.RESUELTO, LocalDateTime.of(2026, 10, 1, 10, 0));
        r2 = reporte(2L, TipoDano.FUGA_AGUA, Bloque.BLOQUE_1, PrioridadReporte.MEDIA,
                EstadoReporte.PENDIENTE, LocalDateTime.of(2026, 10, 5, 9, 0));
        r3 = reporte(3L, TipoDano.AUDIOVISUAL, Bloque.ZONAS_COMUNES, PrioridadReporte.BAJA,
                EstadoReporte.RESUELTO, LocalDateTime.of(2026, 10, 10, 0, 0));

        lenient().when(reporteRepository.findAll()).thenReturn(List.of(r1, r2, r3));
    }

    private Reporte reporte(Long id, TipoDano tipo, Bloque bloque, PrioridadReporte prioridad,
                            EstadoReporte estado, LocalDateTime creado) {
        return Reporte.builder().id(id).tipoDano(tipo).bloque(bloque).espacioEspecifico("Salon")
                .prioridadEstimada(prioridad).estado(estado).descripcion("x").fechaCreacion(creado).build();
    }

    private void resoluciones() {
        when(historialRepository.ultimasResoluciones(any())).thenReturn(List.of(
                new Object[]{1L, LocalDateTime.of(2026, 10, 3, 10, 0)},
                new Object[]{3L, LocalDateTime.of(2026, 10, 11, 0, 0)}));
    }

    @Test
    void ordenaLosTiposYLosBloquesDelMasAlMenosReportado() {
        resoluciones();

        EstadisticasResponse r = servicio.calcular(null, null);

        assertEquals(3, r.totalReportes());
        assertEquals("FUGA_AGUA", r.porTipo().get(0).codigo());
        assertEquals(2, r.porTipo().get(0).total());
        assertEquals("AUDIOVISUAL", r.porTipo().get(1).codigo());
        assertEquals(1, r.porTipo().get(1).total());
        assertEquals(6, r.porTipo().size());
        assertEquals(0, r.porTipo().get(5).total());

        assertEquals("BLOQUE_1", r.porBloque().get(0).codigo());
        assertEquals(2, r.porBloque().get(0).total());
        assertEquals("ZONAS_COMUNES", r.porBloque().get(1).codigo());
        assertEquals(4, r.porBloque().size());
    }

    @Test
    void laPrioridadSaleSiempreEnElOrdenAltaMediaBaja() {
        resoluciones();

        EstadisticasResponse r = servicio.calcular(null, null);

        assertEquals(List.of("ALTA", "MEDIA", "BAJA"), r.porPrioridad().stream().map(c -> c.codigo()).toList());
        assertEquals(List.of(1L, 1L, 1L), r.porPrioridad().stream().map(c -> c.total()).toList());
    }

    @Test
    void calculaElPromedioDeResolucionEnDias() {
        resoluciones();

        EstadisticasResponse r = servicio.calcular(null, null);

        assertEquals(1.5, r.promedioResolucionDias());
        assertEquals(2, r.reportesResueltosMedidos());
    }

    @Test
    void sinHistorialUsaLaFechaDeActualizacionDelReporte() {
        r1.setFechaActualizacion(LocalDateTime.of(2026, 10, 4, 10, 0));
        r3.setFechaActualizacion(null);
        when(historialRepository.ultimasResoluciones(any())).thenReturn(List.of());

        EstadisticasResponse r = servicio.calcular(null, null);

        assertEquals(3.0, r.promedioResolucionDias());
        assertEquals(1, r.reportesResueltosMedidos());
    }

    @Test
    void sinReportesResueltosNoHayPromedio() {
        EstadisticasResponse r = servicio.calcular(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9));

        assertEquals(1, r.totalReportes());
        assertNull(r.promedioResolucionDias());
        assertEquals(0, r.reportesResueltosMedidos());
        verify(historialRepository, never()).ultimasResoluciones(any());
    }

    @Test
    void elFiltroPorFechasEsInclusivoEnAmbosExtremos() {
        resoluciones();

        EstadisticasResponse r = servicio.calcular(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 10));

        assertEquals(2, r.totalReportes());
        assertEquals(1, r.reportesResueltosMedidos());
    }

    @Test
    void sinReportesEnElPeriodoTodoQuedaEnCero() {
        EstadisticasResponse r = servicio.calcular(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

        assertEquals(0, r.totalReportes());
        assertEquals(6, r.porTipo().size());
        assertEquals(0, r.porTipo().get(0).total());
        assertNull(r.promedioResolucionDias());
    }

    @Test
    void rechazaUnRangoConLaFechaInicialPosteriorALaFinal() {
        assertThrows(RangoInvalidoException.class,
                () -> servicio.calcular(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1)));
    }
}