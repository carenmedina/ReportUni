package com.fet.reportuni.service;

import com.fet.reportuni.dto.HistorialItemResponse;
import com.fet.reportuni.dto.NotificacionResponse;
import com.fet.reportuni.model.*;
import com.fet.reportuni.repository.HistorialReporteRepository;
import com.fet.reportuni.repository.NotificacionRepository;
import com.fet.reportuni.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeguimientoServiceTest {

    @Mock
    private HistorialReporteRepository historialRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private ReporteRepository reporteRepository;

    private SeguimientoService servicio;
    private Usuario estudiante;
    private Usuario otroEstudiante;
    private Usuario admin;
    private Reporte reporte;

    @BeforeEach
    void preparar() {
        servicio = new SeguimientoService(historialRepository, notificacionRepository, reporteRepository);

        estudiante = Usuario.builder().id(1L).nombreCompleto("Estudiante Uno")
                .correoInstitucional("estudiante1@fet.edu.co").passwordHash("x").rol(Rol.ESTUDIANTE).build();
        otroEstudiante = Usuario.builder().id(2L).nombreCompleto("Estudiante Dos")
                .correoInstitucional("estudiante2@fet.edu.co").passwordHash("x").rol(Rol.ESTUDIANTE).build();
        admin = Usuario.builder().id(9L).nombreCompleto("Admin")
                .correoInstitucional("admin@fet.edu.co").passwordHash("x").rol(Rol.ADMINISTRADOR).build();

        reporte = Reporte.builder().id(7L).usuario(estudiante).tipoDano(TipoDano.FUGA_AGUA)
                .bloque(Bloque.BLOQUE_A).espacioEspecifico("Salon 204")
                .prioridadEstimada(PrioridadReporte.MEDIA).estado(EstadoReporte.PENDIENTE)
                .descripcion("Gotera").fechaCreacion(LocalDateTime.of(2026, 10, 20, 9, 0)).build();
    }

    @Test
    void cambioDeEstadoGuardaHistorialYNotificaAlDueno() {
        servicio.registrarCambioEstado(reporte, admin, EstadoReporte.PENDIENTE, EstadoReporte.EN_PROCESO);

        ArgumentCaptor<HistorialReporte> historial = ArgumentCaptor.forClass(HistorialReporte.class);
        verify(historialRepository).save(historial.capture());
        assertEquals(TipoCambio.ESTADO, historial.getValue().getTipo());
        assertEquals("PENDIENTE", historial.getValue().getValorAnterior());
        assertEquals("EN_PROCESO", historial.getValue().getValorNuevo());
        assertEquals(admin, historial.getValue().getRealizadoPor());

        ArgumentCaptor<Notificacion> notificacion = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(notificacion.capture());
        assertEquals(estudiante, notificacion.getValue().getUsuario());
        assertEquals(7L, notificacion.getValue().getReporteId());
        assertEquals("Tu reporte #0007 cambió a estado «En proceso».", notificacion.getValue().getMensaje());
        assertFalse(notificacion.getValue().isLeida());
    }

    @Test
    void cambioDePrioridadGuardaHistorialYNotifica() {
        servicio.registrarCambioPrioridad(reporte, admin, PrioridadReporte.MEDIA, PrioridadReporte.ALTA);

        ArgumentCaptor<HistorialReporte> historial = ArgumentCaptor.forClass(HistorialReporte.class);
        verify(historialRepository).save(historial.capture());
        assertEquals(TipoCambio.PRIORIDAD, historial.getValue().getTipo());
        assertEquals("ALTA", historial.getValue().getValorNuevo());

        ArgumentCaptor<Notificacion> notificacion = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(notificacion.capture());
        assertEquals("La prioridad de tu reporte #0007 ahora es «Alta».", notificacion.getValue().getMensaje());
    }

    @Test
    void noNotificaSiQuienCambiaEsElMismoDueno() {
        servicio.registrarCambioEstado(reporte, estudiante, EstadoReporte.PENDIENTE, EstadoReporte.RESUELTO);

        verify(historialRepository).save(any(HistorialReporte.class));
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void adminVeElHistorialDeCualquierReporte() {
        HistorialReporte h = HistorialReporte.builder().id(1L).reporte(reporte).tipo(TipoCambio.ESTADO)
                .valorAnterior("PENDIENTE").valorNuevo("EN_PROCESO").realizadoPor(admin)
                .fecha(LocalDateTime.of(2026, 10, 21, 10, 0)).build();
        when(reporteRepository.existsById(7L)).thenReturn(true);
        when(historialRepository.findByReporteIdOrderByFechaDescIdDesc(7L)).thenReturn(List.of(h));

        List<HistorialItemResponse> lista = servicio.historial(admin, 7L);

        assertEquals(1, lista.size());
        assertEquals("Admin", lista.get(0).realizadoPor());
        assertEquals(TipoCambio.ESTADO, lista.get(0).tipo());
        assertEquals("EN_PROCESO", lista.get(0).valorNuevo());
    }

    @Test
    void estudianteVeElHistorialDeSuReporte() {
        when(reporteRepository.existsByIdAndUsuarioId(7L, 1L)).thenReturn(true);
        when(historialRepository.findByReporteIdOrderByFechaDescIdDesc(7L)).thenReturn(List.of());

        assertTrue(servicio.historial(estudiante, 7L).isEmpty());
    }

    @Test
    void estudianteNoVeElHistorialDeOtro() {
        when(reporteRepository.existsByIdAndUsuarioId(7L, 2L)).thenReturn(false);

        assertThrows(ReporteService.ReporteNoEncontradoException.class,
                () -> servicio.historial(otroEstudiante, 7L));
        verify(historialRepository, never()).findByReporteIdOrderByFechaDescIdDesc(any());
    }

    @Test
    void listaLasNotificacionesConSuCodigo() {
        Notificacion n = Notificacion.builder().id(3L).usuario(estudiante).reporteId(7L)
                .mensaje("Tu reporte #0007 cambió a estado «Resuelto».")
                .fechaCreacion(LocalDateTime.of(2026, 10, 22, 8, 0)).build();
        when(notificacionRepository.findTop30ByUsuarioIdOrderByFechaCreacionDescIdDesc(1L)).thenReturn(List.of(n));

        List<NotificacionResponse> lista = servicio.misNotificaciones(estudiante);

        assertEquals(1, lista.size());
        assertEquals("0007", lista.get(0).codigo());
        assertFalse(lista.get(0).leida());
    }

    @Test
    void cuentaLasNoLeidas() {
        when(notificacionRepository.countByUsuarioIdAndLeidaFalse(1L)).thenReturn(4L);

        assertEquals(4L, servicio.totalNoLeidas(estudiante).total());
    }

    @Test
    void marcaUnaNotificacionComoLeida() {
        Notificacion n = Notificacion.builder().id(3L).usuario(estudiante).reporteId(7L).mensaje("x").build();
        when(notificacionRepository.findByIdAndUsuarioId(3L, 1L)).thenReturn(Optional.of(n));

        servicio.marcarLeida(estudiante, 3L);

        assertTrue(n.isLeida());
        verify(notificacionRepository).save(n);
    }

    @Test
    void noPuedeMarcarLaNotificacionDeOtro() {
        when(notificacionRepository.findByIdAndUsuarioId(3L, 2L)).thenReturn(Optional.empty());

        assertThrows(ReporteService.ReporteNoEncontradoException.class,
                () -> servicio.marcarLeida(otroEstudiante, 3L));
    }

    @Test
    void marcaTodasComoLeidas() {
        Notificacion a = Notificacion.builder().id(1L).usuario(estudiante).reporteId(7L).mensaje("a").build();
        Notificacion b = Notificacion.builder().id(2L).usuario(estudiante).reporteId(7L).mensaje("b").build();
        when(notificacionRepository.findByUsuarioIdAndLeidaFalse(1L)).thenReturn(List.of(a, b));

        servicio.marcarTodasLeidas(estudiante);

        assertTrue(a.isLeida());
        assertTrue(b.isLeida());
        verify(notificacionRepository).saveAll(List.of(a, b));
    }
}