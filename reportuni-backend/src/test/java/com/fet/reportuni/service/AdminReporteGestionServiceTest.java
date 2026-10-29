package com.fet.reportuni.service;

import com.fet.reportuni.dto.AdminReporteItem;
import com.fet.reportuni.model.*;
import com.fet.reportuni.repository.ReporteRepository;
import com.fet.reportuni.service.AlmacenamientoFotosService.FotoInvalidaException;
import com.fet.reportuni.service.AlmacenamientoFotosService.FotoValidada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReporteGestionServiceTest {

    private static final String NOTA = "Se contactó al técnico, visita el jueves";

    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private SeguimientoService seguimiento;
    @Mock
    private AlmacenamientoFotosService almacenamiento;

    private AdminReporteService servicio;
    private Reporte reporte;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        servicio = new AdminReporteService(reporteRepository, seguimiento, almacenamiento);

        Usuario estudiante = Usuario.builder().id(1L).nombreCompleto("Estudiante Uno")
                .correoInstitucional("estudiante1@fet.edu.co").passwordHash("x").rol(Rol.ESTUDIANTE).build();
        admin = Usuario.builder().id(9L).nombreCompleto("Admin")
                .correoInstitucional("admin@fet.edu.co").passwordHash("x").rol(Rol.ADMINISTRADOR).build();

        reporte = Reporte.builder().id(7L).usuario(estudiante).tipoDano(TipoDano.FUGA_AGUA)
                .bloque(Bloque.BLOQUE_1).espacioEspecifico("Salon 204")
                .prioridadEstimada(PrioridadReporte.MEDIA).estado(EstadoReporte.PENDIENTE)
                .descripcion("Gotera").fechaCreacion(LocalDateTime.of(2026, 10, 20, 9, 0)).build();
    }

    private FotoValidada fotoValida() {
        return new FotoValidada(new MockMultipartFile("fotos", "arreglo.jpg", "image/jpeg", new byte[]{1}),
                "image/jpeg", "jpg");
    }

    @Test
    void cambiaElEstadoGuardaLaNotaYNotifica() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));
        when(almacenamiento.validarEvidencia(null, false)).thenReturn(List.of());

        AdminReporteItem item = servicio.cambiarEstado(admin, 7L, EstadoReporte.EN_PROCESO, "  " + NOTA + "  ", null);

        assertEquals(EstadoReporte.EN_PROCESO, item.estado());
        assertEquals(EstadoReporte.EN_PROCESO, reporte.getEstado());
        assertNotNull(reporte.getFechaActualizacion());
        assertEquals("Estudiante Uno", item.reportadoPor());
        verify(reporteRepository).save(reporte);
        verify(seguimiento).registrarCambioEstado(
                reporte, admin, EstadoReporte.PENDIENTE, EstadoReporte.EN_PROCESO, NOTA, List.of());
    }

    @Test
    void resolverExigeFotosDeEvidencia() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));
        when(almacenamiento.validarEvidencia(null, true))
                .thenThrow(new FotoInvalidaException("Para marcar el reporte como resuelto adjunta al menos una fotografía del arreglo."));

        FotoInvalidaException error = assertThrows(FotoInvalidaException.class,
                () -> servicio.cambiarEstado(admin, 7L, EstadoReporte.RESUELTO, NOTA, null));

        assertTrue(error.getMessage().contains("fotografía del arreglo"));
        assertEquals(EstadoReporte.PENDIENTE, reporte.getEstado());
        verify(reporteRepository, never()).save(any());
        verifyNoInteractions(seguimiento);
    }

    @Test
    void resolverConFotosLasPasaAlHistorial() {
        List<MultipartFile> archivos = List.of(new MockMultipartFile("fotos", "arreglo.jpg", "image/jpeg", new byte[]{1}));
        FotoValidada foto = fotoValida();
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));
        when(almacenamiento.validarEvidencia(archivos, true)).thenReturn(List.of(foto));

        AdminReporteItem item = servicio.cambiarEstado(admin, 7L, EstadoReporte.RESUELTO, NOTA, archivos);

        assertEquals(EstadoReporte.RESUELTO, item.estado());
        verify(seguimiento).registrarCambioEstado(
                reporte, admin, EstadoReporte.PENDIENTE, EstadoReporte.RESUELTO, NOTA, List.of(foto));
    }

    @Test
    void permiteReabrirUnReporteResuelto() {
        reporte.setEstado(EstadoReporte.RESUELTO);
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));
        when(almacenamiento.validarEvidencia(null, false)).thenReturn(List.of());

        AdminReporteItem item = servicio.cambiarEstado(admin, 7L, EstadoReporte.EN_PROCESO, NOTA, null);

        assertEquals(EstadoReporte.EN_PROCESO, item.estado());
    }

    @Test
    void rechazaUnaNotaCorta() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        AdminReporteService.CambioInvalidoException error = assertThrows(
                AdminReporteService.CambioInvalidoException.class,
                () -> servicio.cambiarEstado(admin, 7L, EstadoReporte.EN_PROCESO, "corta", null));

        assertEquals("Describe qué se está haciendo para atender el reporte (entre 10 y 1000 caracteres).",
                error.getMessage());
        verify(reporteRepository, never()).save(any());
        verifyNoInteractions(seguimiento, almacenamiento);
    }

    @Test
    void rechazaNotaVaciaYNotaDemasiadoLarga() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        assertThrows(AdminReporteService.CambioInvalidoException.class,
                () -> servicio.cambiarEstado(admin, 7L, EstadoReporte.RESUELTO, null, null));
        assertThrows(AdminReporteService.CambioInvalidoException.class,
                () -> servicio.cambiarEstado(admin, 7L, EstadoReporte.RESUELTO, "x".repeat(1001), null));
    }

    @Test
    void rechazaCambiarAlMismoEstado() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        AdminReporteService.CambioInvalidoException error = assertThrows(
                AdminReporteService.CambioInvalidoException.class,
                () -> servicio.cambiarEstado(admin, 7L, EstadoReporte.PENDIENTE, NOTA, null));

        assertEquals("El reporte ya está en estado Pendiente.", error.getMessage());
        verify(reporteRepository, never()).save(any());
        verifyNoInteractions(seguimiento);
    }

    @Test
    void rechazaCuandoNoSeIndicaElEstado() {
        AdminReporteService.CambioInvalidoException error = assertThrows(
                AdminReporteService.CambioInvalidoException.class,
                () -> servicio.cambiarEstado(admin, 7L, null, NOTA, null));

        assertEquals("Debes indicar el nuevo estado.", error.getMessage());
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
    void asignaElReporteAUnEquipo() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        servicio.asignar(admin, 7L, AreaAsignada.SOPORTE_TECNICO, "   ");

        assertEquals(AreaAsignada.SOPORTE_TECNICO, reporte.getAsignadoA());
        assertNotNull(reporte.getFechaActualizacion());
        verify(reporteRepository).save(reporte);
        verify(seguimiento).registrarAsignacion(reporte, admin, null, AreaAsignada.SOPORTE_TECNICO, null);
    }

    @Test
    void reasignaYGuardaLaNota() {
        reporte.setAsignadoA(AreaAsignada.MANTENIMIENTO);
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        servicio.asignar(admin, 7L, AreaAsignada.ELECTRICO, "Requiere revisión eléctrica");

        verify(seguimiento).registrarAsignacion(
                reporte, admin, AreaAsignada.MANTENIMIENTO, AreaAsignada.ELECTRICO, "Requiere revisión eléctrica");
    }

    @Test
    void rechazaAsignarAlMismoEquipoOSinEquipo() {
        reporte.setAsignadoA(AreaAsignada.AUDIOVISUALES);
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        AdminReporteService.CambioInvalidoException error = assertThrows(
                AdminReporteService.CambioInvalidoException.class,
                () -> servicio.asignar(admin, 7L, AreaAsignada.AUDIOVISUALES, null));
        assertEquals("El reporte ya está asignado a Audiovisuales.", error.getMessage());

        assertThrows(AdminReporteService.CambioInvalidoException.class,
                () -> servicio.asignar(admin, 7L, null, null));
        verifyNoInteractions(seguimiento);
    }

    @Test
    void rechazaUnaNotaDeAsignacionMuyCorta() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        assertThrows(AdminReporteService.CambioInvalidoException.class,
                () -> servicio.asignar(admin, 7L, AreaAsignada.MANTENIMIENTO, "corta"));
        verify(reporteRepository, never()).save(any());
    }

    @Test
    void agregaUnaActualizacionSinCambiarElEstado() {
        FotoValidada foto = fotoValida();
        List<MultipartFile> archivos = List.of(new MockMultipartFile("fotos", "repuesto.jpg", "image/jpeg", new byte[]{1}));
        reporte.setEstado(EstadoReporte.EN_PROCESO);
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));
        when(almacenamiento.validarEvidencia(archivos, false)).thenReturn(List.of(foto));

        servicio.agregarActualizacion(admin, 7L, "Se solicitó un repuesto, tarda 2 días hábiles", archivos);

        assertEquals(EstadoReporte.EN_PROCESO, reporte.getEstado());
        assertNotNull(reporte.getFechaActualizacion());
        verify(seguimiento).registrarActualizacion(
                reporte, admin, "Se solicitó un repuesto, tarda 2 días hábiles", List.of(foto));
    }

    @Test
    void laActualizacionExigeUnaNota() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        assertThrows(AdminReporteService.CambioInvalidoException.class,
                () -> servicio.agregarActualizacion(admin, 7L, "ok", null));
        verifyNoInteractions(seguimiento);
    }

    @Test
    void reporteInexistenteDaNoEncontrado() {
        when(reporteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReporteService.ReporteNoEncontradoException.class,
                () -> servicio.cambiarEstado(admin, 99L, EstadoReporte.RESUELTO, NOTA, null));
        assertThrows(ReporteService.ReporteNoEncontradoException.class,
                () -> servicio.cambiarPrioridad(admin, 99L, PrioridadReporte.BAJA));
        assertThrows(ReporteService.ReporteNoEncontradoException.class,
                () -> servicio.asignar(admin, 99L, AreaAsignada.MANTENIMIENTO, null));
        assertThrows(ReporteService.ReporteNoEncontradoException.class,
                () -> servicio.agregarActualizacion(admin, 99L, NOTA, null));
    }
}