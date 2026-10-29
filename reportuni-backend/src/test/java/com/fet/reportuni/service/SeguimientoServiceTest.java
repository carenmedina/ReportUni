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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
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
    @Mock
    private AlmacenamientoFotosService almacenamiento;
    @Mock
    private ConsultaFotosService consultaFotos;

    private SeguimientoService servicio;
    private Usuario estudiante;
    private Usuario otroEstudiante;
    private Usuario admin;
    private Reporte reporte;

    @BeforeEach
    void preparar() {
        servicio = new SeguimientoService(historialRepository, notificacionRepository, reporteRepository,
                almacenamiento, consultaFotos);

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
        servicio.registrarCambioEstado(reporte, admin, EstadoReporte.PENDIENTE, EstadoReporte.EN_PROCESO,
                "Se contactó al técnico", List.of());

        ArgumentCaptor<HistorialReporte> historial = ArgumentCaptor.forClass(HistorialReporte.class);
        verify(historialRepository).save(historial.capture());
        assertEquals(TipoCambio.ESTADO, historial.getValue().getTipo());
        assertEquals("PENDIENTE", historial.getValue().getValorAnterior());
        assertEquals("EN_PROCESO", historial.getValue().getValorNuevo());
        assertEquals("Se contactó al técnico", historial.getValue().getNota());
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
        servicio.registrarCambioEstado(reporte, estudiante, EstadoReporte.PENDIENTE, EstadoReporte.RESUELTO,
                "Lo arreglé yo mismo", List.of());

        verify(historialRepository).save(any(HistorialReporte.class));
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void adminVeElHistorialDeCualquierReporte() {
        HistorialReporte h = HistorialReporte.builder().id(1L).reporte(reporte).tipo(TipoCambio.ESTADO)
                .valorAnterior("PENDIENTE").valorNuevo("EN_PROCESO").realizadoPor(admin)
                .nota("Se contactó al técnico")
                .fecha(LocalDateTime.of(2026, 10, 21, 10, 0)).build();
        h.agregarFoto(HistorialFoto.builder().id(5L).nombreOriginal("arreglo.jpg").nombreArchivo("u.jpg")
                .tipoContenido("image/jpeg").tamanoBytes(10).orden(1).build());
        when(reporteRepository.existsById(7L)).thenReturn(true);
        when(historialRepository.findByReporteIdOrderByFechaDescIdDesc(7L)).thenReturn(List.of(h));

        List<HistorialItemResponse> lista = servicio.historial(admin, 7L);

        assertEquals(1, lista.size());
        assertEquals("Admin", lista.get(0).realizadoPor());
        assertEquals(TipoCambio.ESTADO, lista.get(0).tipo());
        assertEquals("EN_PROCESO", lista.get(0).valorNuevo());
        assertEquals("Se contactó al técnico", lista.get(0).nota());
        assertEquals(1, lista.get(0).fotos().size());
        assertEquals("arreglo.jpg", lista.get(0).fotos().get(0).nombreOriginal());
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
        when(notificacionRepository.findTop30ByUsuarioIdAndComentarioFalseOrderByFechaCreacionDescIdDesc(1L)).thenReturn(List.of(n));

        List<NotificacionResponse> lista = servicio.misNotificaciones(estudiante);

        assertEquals(1, lista.size());
        assertEquals("0007", lista.get(0).codigo());
        assertFalse(lista.get(0).leida());
    }

    @Test
    void cuentaLasNoLeidas() {
        when(notificacionRepository.countByUsuarioIdAndComentarioFalseAndLeidaFalse(1L)).thenReturn(4L);

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
        when(notificacionRepository.findByUsuarioIdAndComentarioFalseAndLeidaFalse(1L)).thenReturn(List.of(a, b));

        servicio.marcarTodasLeidas(estudiante);

        assertTrue(a.isLeida());
        assertTrue(b.isLeida());
        verify(notificacionRepository).saveAll(List.of(a, b));
    }

    @Test
    void asignacionGuardaHistorialYNotifica() {
        servicio.registrarAsignacion(reporte, admin, null, AreaAsignada.SOPORTE_TECNICO, "Falla del conector");

        ArgumentCaptor<HistorialReporte> historial = ArgumentCaptor.forClass(HistorialReporte.class);
        verify(historialRepository).save(historial.capture());
        assertEquals(TipoCambio.ASIGNACION, historial.getValue().getTipo());
        assertEquals("", historial.getValue().getValorAnterior());
        assertEquals("SOPORTE_TECNICO", historial.getValue().getValorNuevo());
        assertEquals("Falla del conector", historial.getValue().getNota());

        ArgumentCaptor<Notificacion> notificacion = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(notificacion.capture());
        assertEquals("Tu reporte #0007 fue asignado a Soporte Técnico · Infraestructura.",
                notificacion.getValue().getMensaje());
    }

    @Test
    void reasignacionConservaElEquipoAnterior() {
        servicio.registrarAsignacion(reporte, admin, AreaAsignada.MANTENIMIENTO, AreaAsignada.ELECTRICO, null);

        ArgumentCaptor<HistorialReporte> historial = ArgumentCaptor.forClass(HistorialReporte.class);
        verify(historialRepository).save(historial.capture());
        assertEquals("MANTENIMIENTO", historial.getValue().getValorAnterior());
        assertEquals("ELECTRICO", historial.getValue().getValorNuevo());
    }

    @Test
    void actualizacionGuardaLasFotosYNotifica() {
        MockMultipartFile archivo = new MockMultipartFile("fotos", "repuesto.jpg", "image/jpeg", new byte[]{1, 2, 3});
        AlmacenamientoFotosService.FotoValidada foto =
                new AlmacenamientoFotosService.FotoValidada(archivo, "image/jpeg", "jpg");
        when(almacenamiento.guardar(7L, foto)).thenReturn("uuid-1.jpg");

        servicio.registrarActualizacion(reporte, admin, "Se solicitó un repuesto", List.of(foto));

        ArgumentCaptor<HistorialReporte> historial = ArgumentCaptor.forClass(HistorialReporte.class);
        verify(historialRepository).save(historial.capture());
        assertEquals(TipoCambio.ACTUALIZACION, historial.getValue().getTipo());
        assertEquals("Se solicitó un repuesto", historial.getValue().getNota());
        assertEquals(1, historial.getValue().getFotos().size());
        assertEquals("uuid-1.jpg", historial.getValue().getFotos().get(0).getNombreArchivo());
        assertEquals("repuesto.jpg", historial.getValue().getFotos().get(0).getNombreOriginal());
        assertEquals(3L, historial.getValue().getFotos().get(0).getTamanoBytes());

        ArgumentCaptor<Notificacion> notificacion = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(notificacion.capture());
        assertEquals("Hay una nueva actualización en tu reporte #0007.", notificacion.getValue().getMensaje());
    }

    @Test
    void siFallaElGuardadoSeBorranLasFotosYaCopiadas() {
        AlmacenamientoFotosService.FotoValidada uno = new AlmacenamientoFotosService.FotoValidada(
                new MockMultipartFile("fotos", "a.jpg", "image/jpeg", new byte[]{1}), "image/jpeg", "jpg");
        AlmacenamientoFotosService.FotoValidada dos = new AlmacenamientoFotosService.FotoValidada(
                new MockMultipartFile("fotos", "b.jpg", "image/jpeg", new byte[]{1}), "image/jpeg", "jpg");
        when(almacenamiento.guardar(7L, uno)).thenReturn("uuid-1.jpg");
        when(almacenamiento.guardar(7L, dos))
                .thenThrow(new AlmacenamientoFotosService.AlmacenamientoException("fallo", new RuntimeException()));

        assertThrows(AlmacenamientoFotosService.AlmacenamientoException.class,
                () -> servicio.registrarActualizacion(reporte, admin, "Novedad del proceso", List.of(uno, dos)));

        verify(almacenamiento).eliminarArchivo(7L, "uuid-1.jpg");
        verify(historialRepository, never()).save(any());
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void entregaUnaFotoDelHistorial() {
        HistorialReporte h = HistorialReporte.builder().id(3L).reporte(reporte).tipo(TipoCambio.ESTADO)
                .valorAnterior("EN_PROCESO").valorNuevo("RESUELTO").realizadoPor(admin).build();
        h.agregarFoto(HistorialFoto.builder().id(8L).nombreOriginal("arreglo.jpg").nombreArchivo("u.jpg")
                .tipoContenido("image/jpeg").tamanoBytes(10).orden(1).build());
        when(reporteRepository.existsByIdAndUsuarioId(7L, 1L)).thenReturn(true);
        when(historialRepository.findByIdAndReporteId(3L, 7L)).thenReturn(Optional.of(h));
        when(consultaFotos.cargar(7L, "u.jpg")).thenReturn(Optional.of(new ByteArrayResource(new byte[]{1})));

        ReporteService.FotoDescargable foto = servicio.fotoHistorial(estudiante, 7L, 3L, 8L);

        assertEquals("image/jpeg", foto.tipoContenido());
    }

    @Test
    void noEntregaLaFotoDeUnReporteAjeno() {
        when(reporteRepository.existsByIdAndUsuarioId(7L, 2L)).thenReturn(false);

        assertThrows(ReporteService.ReporteNoEncontradoException.class,
                () -> servicio.fotoHistorial(otroEstudiante, 7L, 3L, 8L));
    }

    @Test
    void fotoInexistenteDaNoEncontrado() {
        HistorialReporte h = HistorialReporte.builder().id(3L).reporte(reporte).tipo(TipoCambio.ESTADO)
                .valorAnterior("A").valorNuevo("B").realizadoPor(admin).build();
        when(reporteRepository.existsById(7L)).thenReturn(true);
        when(historialRepository.findByIdAndReporteId(3L, 7L)).thenReturn(Optional.of(h));

        assertThrows(ReporteService.ReporteNoEncontradoException.class,
                () -> servicio.fotoHistorial(admin, 7L, 3L, 99L));
    }
}