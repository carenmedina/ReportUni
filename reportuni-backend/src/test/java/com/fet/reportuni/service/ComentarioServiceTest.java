package com.fet.reportuni.service;

import com.fet.reportuni.dto.ComentarioResponse;
import com.fet.reportuni.dto.NotificacionResponse;
import com.fet.reportuni.model.*;
import com.fet.reportuni.repository.ComentarioRepository;
import com.fet.reportuni.repository.NotificacionRepository;
import com.fet.reportuni.repository.ReporteRepository;
import com.fet.reportuni.repository.UsuarioRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComentarioServiceTest {

    @Mock
    private ComentarioRepository comentarioRepository;
    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    private ComentarioService servicio;
    private Usuario estudiante;
    private Usuario otroEstudiante;
    private Usuario admin;
    private Usuario otroAdmin;
    private Reporte reporte;

    @BeforeEach
    void preparar() {
        servicio = new ComentarioService(comentarioRepository, reporteRepository, notificacionRepository,
                usuarioRepository);

        estudiante = Usuario.builder().id(1L).nombreCompleto("Estudiante Uno")
                .correoInstitucional("estudiante1@fet.edu.co").passwordHash("x").rol(Rol.ESTUDIANTE).build();
        otroEstudiante = Usuario.builder().id(2L).nombreCompleto("Estudiante Dos")
                .correoInstitucional("estudiante2@fet.edu.co").passwordHash("x").rol(Rol.ESTUDIANTE).build();
        admin = Usuario.builder().id(9L).nombreCompleto("Admin Uno")
                .correoInstitucional("admin@fet.edu.co").passwordHash("x").rol(Rol.ADMINISTRADOR).build();
        otroAdmin = Usuario.builder().id(10L).nombreCompleto("Admin Dos")
                .correoInstitucional("admin2@fet.edu.co").passwordHash("x").rol(Rol.ADMINISTRADOR).build();

        reporte = Reporte.builder().id(7L).usuario(estudiante).tipoDano(TipoDano.FUGA_AGUA)
                .bloque(Bloque.BLOQUE_1).espacioEspecifico("Salon 204")
                .prioridadEstimada(PrioridadReporte.MEDIA).estado(EstadoReporte.PENDIENTE)
                .descripcion("Gotera").fechaCreacion(LocalDateTime.of(2026, 10, 20, 9, 0)).build();
    }

    private void guardarDevuelveElMismo() {
        when(comentarioRepository.save(any(Comentario.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void elAdministradorComentaYElEstudianteRecibeElAviso() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));
        guardarDevuelveElMismo();

        ComentarioResponse respuesta = servicio.comentar(admin, 7L, "  Revisaremos mañana el salón  ");

        assertEquals("Revisaremos mañana el salón", respuesta.texto());
        assertEquals("Admin Uno", respuesta.autor());
        assertTrue(respuesta.propio());
        assertEquals(Rol.ADMINISTRADOR, respuesta.rol());

        ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(aviso.capture());
        assertEquals(estudiante, aviso.getValue().getUsuario());
        assertEquals(7L, aviso.getValue().getReporteId());
        assertTrue(aviso.getValue().isComentario());
        assertTrue(aviso.getValue().getMensaje().startsWith("Admin Uno comentó en tu reporte #0007"));
        assertTrue(aviso.getValue().getMensaje().contains("Revisaremos mañana el salón"));
    }

    @Test
    void elEstudianteComentaYSeAvisaATodosLosAdministradores() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));
        when(usuarioRepository.findByRolAndActivoTrue(Rol.ADMINISTRADOR)).thenReturn(List.of(admin, otroAdmin));
        guardarDevuelveElMismo();

        servicio.comentar(estudiante, 7L, "¿Ya hay novedades?");

        ArgumentCaptor<Notificacion> avisos = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, times(2)).save(avisos.capture());
        assertEquals(List.of(admin, otroAdmin), avisos.getAllValues().stream().map(Notificacion::getUsuario).toList());
        assertTrue(avisos.getAllValues().stream().allMatch(Notificacion::isComentario));
        assertTrue(avisos.getAllValues().get(0).getMensaje().startsWith("Estudiante Uno comentó en el reporte #0007"));
    }

    @Test
    void sinAdministradoresActivosNoSeCreanAvisos() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));
        when(usuarioRepository.findByRolAndActivoTrue(Rol.ADMINISTRADOR)).thenReturn(List.of());
        guardarDevuelveElMismo();

        servicio.comentar(estudiante, 7L, "Hola");

        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void rechazaUnComentarioVacio() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        ComentarioService.ComentarioInvalidoException ex = assertThrows(
                ComentarioService.ComentarioInvalidoException.class, () -> servicio.comentar(estudiante, 7L, "   "));

        assertEquals("Escribe un comentario antes de enviarlo.", ex.getMessage());
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    void rechazaUnComentarioNulo() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        assertThrows(ComentarioService.ComentarioInvalidoException.class,
                () -> servicio.comentar(estudiante, 7L, null));
    }

    @Test
    void rechazaUnComentarioMuyLargo() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        ComentarioService.ComentarioInvalidoException ex = assertThrows(
                ComentarioService.ComentarioInvalidoException.class,
                () -> servicio.comentar(estudiante, 7L, "a".repeat(1001)));

        assertTrue(ex.getMessage().contains("1000"));
    }

    @Test
    void aceptaUnComentarioDeExactamenteMilCaracteres() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));
        when(usuarioRepository.findByRolAndActivoTrue(Rol.ADMINISTRADOR)).thenReturn(List.of(admin));
        guardarDevuelveElMismo();

        ComentarioResponse respuesta = servicio.comentar(estudiante, 7L, "a".repeat(1000));

        assertEquals(1000, respuesta.texto().length());
        ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(aviso.capture());
        assertTrue(aviso.getValue().getMensaje().length() <= 255);
    }

    @Test
    void unEstudianteNoPuedeComentarElReporteDeOtro() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        assertThrows(ReporteService.ReporteNoEncontradoException.class,
                () -> servicio.comentar(otroEstudiante, 7L, "Hola"));
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    void comentarUnReporteInexistenteDaNoEncontrado() {
        when(reporteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReporteService.ReporteNoEncontradoException.class,
                () -> servicio.comentar(admin, 99L, "Hola"));
    }

    @Test
    void listaLosComentariosMarcandoCuales_SonPropios_YLeyendoLosAvisos() {
        Comentario deAdmin = Comentario.builder().id(1L).reporte(reporte).autor(admin).texto("Hola")
                .fechaCreacion(LocalDateTime.of(2026, 10, 21, 10, 0)).build();
        Comentario propio = Comentario.builder().id(2L).reporte(reporte).autor(estudiante).texto("Gracias")
                .fechaCreacion(LocalDateTime.of(2026, 10, 21, 11, 0)).build();
        Notificacion aviso = Notificacion.builder().id(5L).usuario(estudiante).reporteId(7L).mensaje("x")
                .comentario(true).build();

        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));
        when(notificacionRepository.findByUsuarioIdAndReporteIdAndComentarioTrueAndLeidaFalse(1L, 7L))
                .thenReturn(List.of(aviso));
        when(comentarioRepository.findByReporteIdOrderByFechaCreacionAscIdAsc(7L))
                .thenReturn(List.of(deAdmin, propio));

        List<ComentarioResponse> lista = servicio.listar(estudiante, 7L);

        assertEquals(2, lista.size());
        assertFalse(lista.get(0).propio());
        assertTrue(lista.get(1).propio());
        assertTrue(aviso.isLeida());
        verify(notificacionRepository).saveAll(List.of(aviso));
    }

    @Test
    void listarSinAvisosPendientesNoEscribeNada() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));
        when(notificacionRepository.findByUsuarioIdAndReporteIdAndComentarioTrueAndLeidaFalse(9L, 7L))
                .thenReturn(List.of());
        when(comentarioRepository.findByReporteIdOrderByFechaCreacionAscIdAsc(7L)).thenReturn(List.of());

        assertTrue(servicio.listar(admin, 7L).isEmpty());
        verify(notificacionRepository, never()).saveAll(any());
    }

    @Test
    void unEstudianteNoPuedeVerLosComentariosDeOtro() {
        when(reporteRepository.findById(7L)).thenReturn(Optional.of(reporte));

        assertThrows(ReporteService.ReporteNoEncontradoException.class,
                () -> servicio.listar(otroEstudiante, 7L));
        verify(comentarioRepository, never()).findByReporteIdOrderByFechaCreacionAscIdAsc(any());
    }

    @Test
    void listaLosAvisosDeComentariosConSuCodigo() {
        Notificacion n = Notificacion.builder().id(3L).usuario(estudiante).reporteId(7L)
                .mensaje("Admin Uno comentó en tu reporte #0007: «Hola»").comentario(true)
                .fechaCreacion(LocalDateTime.of(2026, 10, 22, 8, 0)).build();
        when(notificacionRepository.findTop30ByUsuarioIdAndComentarioTrueOrderByFechaCreacionDescIdDesc(1L))
                .thenReturn(List.of(n));

        List<NotificacionResponse> lista = servicio.misAvisos(estudiante);

        assertEquals(1, lista.size());
        assertEquals("0007", lista.get(0).codigo());
        assertEquals(7L, lista.get(0).reporteId());
    }

    @Test
    void cuentaLosAvisosDeComentariosNoLeidos() {
        when(notificacionRepository.countByUsuarioIdAndComentarioTrueAndLeidaFalse(1L)).thenReturn(3L);

        assertEquals(3L, servicio.totalNoLeidos(estudiante).total());
    }

    @Test
    void marcaTodosLosAvisosDeComentariosComoLeidos() {
        Notificacion a = Notificacion.builder().id(1L).usuario(estudiante).reporteId(7L).mensaje("a")
                .comentario(true).build();
        when(notificacionRepository.findByUsuarioIdAndComentarioTrueAndLeidaFalse(1L)).thenReturn(List.of(a));

        servicio.marcarTodosLeidos(estudiante);

        assertTrue(a.isLeida());
        verify(notificacionRepository).saveAll(List.of(a));
    }
}