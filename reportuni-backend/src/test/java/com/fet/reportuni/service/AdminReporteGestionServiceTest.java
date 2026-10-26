package com.fet.reportuni.service;

import com.fet.reportuni.dto.AdminReporteItem;
import com.fet.reportuni.model.*;
import com.fet.reportuni.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReporteGestionServiceTest {

    @Mock
    private ReporteRepository reporteRepository;

    @Mock
    private SeguimientoService seguimiento;

    private AdminReporteService servicio;
    private Reporte reporte;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        servicio = new AdminReporteService(reporteRepository, seguimiento);

        Usuario estudiante = Usuario.builder().id(1L).nombreCompleto("Estudiante Uno")
                .correoInstitucional("estudiante1@fet.edu.co").passwordHash("x").rol(Rol.ESTUDIANTE).build();

        admin = Usuario.builder().id(9L).nombreCompleto("Admin")
                .correoInstitucional("admin@fet.edu.co").passwordHash("x").rol(Rol.ADMINISTRADOR).build();

        reporte = Reporte.builder().id(7L).usuario(estudiante).tipoDano(TipoDano.FUGA_AGUA)
                .bloque(Bloque.BLOQUE_A).espacioEspecifico("Salon 204")
                .prioridadEstimada(PrioridadReporte.MEDIA).estado(EstadoReporte.PENDIENTE)
                .descripcion("Gotera").fechaCreacion(LocalDateTime.of(2026, 10, 20, 9, 0)).build();
    }

    @Test
    void cambiaElEstadoYLoGuarda() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        AdminReporteItem item = servicio.cambiarEstado(admin, 7L, EstadoReporte.EN_PROCESO);

        assertEquals(EstadoReporte.EN_PROCESO, item.estado());
        assertEquals(EstadoReporte.EN_PROCESO, reporte.getEstado());
        assertEquals("Estudiante Uno", item.reportadoPor());
        verify(reporteRepository).save(reporte);
        verify(seguimiento).registrarCambioEstado(reporte, admin, EstadoReporte.PENDIENTE, EstadoReporte.EN_PROCESO);
    }

    @Test
    void permiteReabrirUnReporteResuelto() {
        reporte.setEstado(EstadoReporte.RESUELTO);
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        AdminReporteItem item = servicio.cambiarEstado(admin, 7L, EstadoReporte.EN_PROCESO);

        assertEquals(EstadoReporte.EN_PROCESO, item.estado());
    }

    @Test
    void rechazaCambiarAlMismoEstado() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        AdminReporteService.CambioInvalidoException error = assertThrows(
                AdminReporteService.CambioInvalidoException.class,
                () -> servicio.cambiarEstado(admin, 7L, EstadoReporte.PENDIENTE));

        assertEquals("El reporte ya está en estado Pendiente.", error.getMessage());
        verify(reporteRepository, never()).save(any());
        verifyNoInteractions(seguimiento);
    }

    @Test
    void cambiaLaPrioridad() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        AdminReporteItem item = servicio.cambiarPrioridad(admin, 7L, PrioridadReporte.ALTA);

        assertEquals(PrioridadReporte.ALTA, item.prioridadEstimada());
        assertEquals(PrioridadReporte.ALTA, reporte.getPrioridadEstimada());
        verify(reporteRepository).save(reporte);
        verify(seguimiento).registrarCambioPrioridad(reporte, admin, PrioridadReporte.MEDIA, PrioridadReporte.ALTA);
    }

    @Test
    void rechazaLaMismaPrioridad() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        assertThrows(AdminReporteService.CambioInvalidoException.class,
                () -> servicio.cambiarPrioridad(admin, 7L, PrioridadReporte.MEDIA));
    }

    @Test
    void reporteInexistenteDaNoEncontrado() {
        when(reporteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReporteService.ReporteNoEncontradoException.class,
                () -> servicio.cambiarEstado(admin, 99L, EstadoReporte.RESUELTO));
        assertThrows(ReporteService.ReporteNoEncontradoException.class,
                () -> servicio.cambiarPrioridad(admin, 99L, PrioridadReporte.BAJA));
    }
}