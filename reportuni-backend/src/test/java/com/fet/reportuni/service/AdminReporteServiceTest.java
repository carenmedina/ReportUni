package com.fet.reportuni.service;

import com.fet.reportuni.dto.AdminReporteItem;
import com.fet.reportuni.dto.AdminResumenResponse;
import com.fet.reportuni.model.*;
import com.fet.reportuni.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReporteServiceTest {

    @Mock
    private ReporteRepository reporteRepository;

    private AdminReporteService servicio;
    private List<Reporte> todos;

    @BeforeEach
    void preparar() {
        servicio = new AdminReporteService(reporteRepository);

        Usuario uno = Usuario.builder().id(1L).nombreCompleto("Estudiante Uno")
                .correoInstitucional("estudiante1@fet.edu.co").passwordHash("x").rol(Rol.ESTUDIANTE).build();
        Usuario dos = Usuario.builder().id(2L).nombreCompleto("Estudiante Dos")
                .correoInstitucional("estudiante2@fet.edu.co").passwordHash("x").rol(Rol.ESTUDIANTE).build();

        // ya vienen ordenados del mas reciente al mas antiguo, como los devuelve el repositorio
        todos = List.of(
                reporte(3L, dos, TipoDano.AUDIOVISUAL, Bloque.BLOQUE_C, "Auditorio",
                        PrioridadReporte.MEDIA, EstadoReporte.EN_PROCESO, "Proyector sin imagen", 3),
                reporte(2L, uno, TipoDano.FUGA_AGUA, Bloque.BLOQUE_A, "Salon 204",
                        PrioridadReporte.ALTA, EstadoReporte.PENDIENTE, "Gotera en el techo", 2),
                reporte(1L, uno, TipoDano.MOBILIARIO, Bloque.BIBLIOTECA, "Sala de lectura",
                        PrioridadReporte.BAJA, EstadoReporte.RESUELTO, "Silla rota", 1)
        );
    }

    private Reporte reporte(Long id, Usuario usuario, TipoDano tipo, Bloque bloque, String espacio,
                            PrioridadReporte prioridad, EstadoReporte estado, String descripcion, int dia) {
        return Reporte.builder().id(id).usuario(usuario).tipoDano(tipo).bloque(bloque)
                .espacioEspecifico(espacio).prioridadEstimada(prioridad).estado(estado)
                .descripcion(descripcion).fechaCreacion(LocalDateTime.of(2026, 10, dia, 9, 0)).build();
    }

    @Test
    void resumenCuentaPorEstadoYTrae5Recientes() {
        when(reporteRepository.countByEstado(EstadoReporte.PENDIENTE)).thenReturn(4L);
        when(reporteRepository.countByEstado(EstadoReporte.EN_PROCESO)).thenReturn(2L);
        when(reporteRepository.countByEstado(EstadoReporte.RESUELTO)).thenReturn(1L);
        when(reporteRepository.countByEstadoAndPrioridadEstimada(EstadoReporte.PENDIENTE, PrioridadReporte.ALTA))
                .thenReturn(3L);
        when(reporteRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(todos);

        AdminResumenResponse resumen = servicio.resumen();

        assertEquals(7L, resumen.total());
        assertEquals(4L, resumen.pendientes());
        assertEquals(2L, resumen.enProceso());
        assertEquals(1L, resumen.resueltos());
        assertEquals(3L, resumen.altaPrioridadPendientes());
        assertEquals(3, resumen.recientes().size());
        assertEquals("0003", resumen.recientes().get(0).codigo());
    }

    @Test
    void listarSinFiltrosDevuelveTodosConQuienReporto() {
        when(reporteRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(todos);

        List<AdminReporteItem> lista = servicio.listar(null, null, null, null, null);

        assertEquals(3, lista.size());
        assertEquals("Estudiante Dos", lista.get(0).reportadoPor());
        assertEquals("estudiante2@fet.edu.co", lista.get(0).reportadoPorCorreo());
    }

    @Test
    void filtraPorEstadoYPrioridad() {
        when(reporteRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(todos);

        List<AdminReporteItem> porEstado = servicio.listar(EstadoReporte.RESUELTO, null, null, null, null);
        List<AdminReporteItem> porPrioridad = servicio.listar(null, null, PrioridadReporte.ALTA, null, null);

        assertEquals(1, porEstado.size());
        assertEquals("0001", porEstado.get(0).codigo());
        assertEquals(1, porPrioridad.size());
        assertEquals("0002", porPrioridad.get(0).codigo());
    }

    @Test
    void filtraPorTipoYBloque() {
        when(reporteRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(todos);

        assertEquals(1, servicio.listar(null, TipoDano.AUDIOVISUAL, null, null, null).size());
        assertEquals(1, servicio.listar(null, null, null, Bloque.BIBLIOTECA, null).size());
        assertTrue(servicio.listar(null, TipoDano.ASEO, null, null, null).isEmpty());
    }

    @Test
    void buscaPorCodigoEspacioDescripcionYAutor() {
        when(reporteRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(todos);

        assertEquals("0002", servicio.listar(null, null, null, null, "#0002").get(0).codigo());
        assertEquals("0003", servicio.listar(null, null, null, null, "auditorio").get(0).codigo());
        assertEquals("0002", servicio.listar(null, null, null, null, "GOTERA").get(0).codigo());
        assertEquals(2, servicio.listar(null, null, null, null, "estudiante uno").size());
        assertEquals(2, servicio.listar(null, null, null, null, "estudiante1@fet").size());
    }

    @Test
    void combinaFiltrosYBusquedaVaciaNoFiltra() {
        when(reporteRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(todos);

        assertEquals(3, servicio.listar(null, null, null, null, "   ").size());
        assertEquals(1, servicio.listar(EstadoReporte.PENDIENTE, null, null, Bloque.BLOQUE_A, "salon").size());
        assertTrue(servicio.listar(EstadoReporte.RESUELTO, null, null, Bloque.BLOQUE_A, null).isEmpty());
    }
}