package com.fet.reportuni.service;

import com.fet.reportuni.dto.CambiarPasswordRequest;
import com.fet.reportuni.dto.PerfilResponse;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.repository.HistorialReporteRepository;
import com.fet.reportuni.repository.ReporteRepository;
import com.fet.reportuni.repository.UsuarioRepository;
import com.fet.reportuni.service.PerfilService.PerfilInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private HistorialReporteRepository historialRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @TempDir
    Path carpeta;

    private PerfilService servicio;
    private Usuario estudiante;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        servicio = new PerfilService(usuarioRepository, reporteRepository, historialRepository,
                passwordEncoder, carpeta.toString());
        estudiante = Usuario.builder().id(1L).nombreCompleto("Estudiante Uno")
                .correoInstitucional("estudiante1@fet.edu.co").passwordHash("hash-actual").rol(Rol.ESTUDIANTE).build();
        admin = Usuario.builder().id(9L).nombreCompleto("Admin Uno")
                .correoInstitucional("admin@fet.edu.co").passwordHash("x").rol(Rol.ADMINISTRADOR).build();
    }

    @Test
    void elEstudianteVeSusReportesPorEstado() {
        when(reporteRepository.countByUsuarioId(1L)).thenReturn(6L);
        when(reporteRepository.countByUsuarioIdAndEstado(1L, EstadoReporte.PENDIENTE)).thenReturn(3L);
        when(reporteRepository.countByUsuarioIdAndEstado(1L, EstadoReporte.EN_PROCESO)).thenReturn(2L);
        when(reporteRepository.countByUsuarioIdAndEstado(1L, EstadoReporte.RESUELTO)).thenReturn(1L);

        PerfilResponse perfil = servicio.obtener(estudiante);

        assertEquals("Estudiante Uno", perfil.nombreCompleto());
        assertEquals("estudiante1@fet.edu.co", perfil.correoInstitucional());
        assertEquals("ESTUDIANTE", perfil.rol());
        assertFalse(perfil.tieneFoto());
        assertEquals(6L, perfil.resumen().total());
        assertEquals(3L, perfil.resumen().pendientes());
        assertEquals(2L, perfil.resumen().enProceso());
        assertEquals(1L, perfil.resumen().resueltos());
        assertNull(perfil.resumen().gestionados());
    }

    @Test
    void elAdministradorVeCuantosReportesHaGestionado() {
        when(historialRepository.contarReportesGestionados(9L)).thenReturn(14L);

        PerfilResponse perfil = servicio.obtener(admin);

        assertEquals("ADMINISTRADOR", perfil.rol());
        assertEquals(14L, perfil.resumen().gestionados());
        assertNull(perfil.resumen().total());
    }

    @Test
    void guardaLaFotoPngYLaPuedeLeer() throws IOException {
        when(reporteRepository.countByUsuarioId(1L)).thenReturn(0L);

        PerfilResponse perfil = servicio.subirFoto(estudiante,
                new MockMultipartFile("foto", "yo.png", "image/png", PNG));

        assertTrue(perfil.tieneFoto());
        assertNotNull(estudiante.getFotoPerfil());
        assertTrue(estudiante.getFotoPerfil().endsWith(".png"));
        assertTrue(Files.exists(carpeta.resolve(estudiante.getFotoPerfil())));
        verify(usuarioRepository).save(estudiante);

        PerfilService.FotoPerfil foto = servicio.foto(estudiante).orElseThrow();
        assertEquals("image/png", foto.tipoContenido());
    }

    @Test
    void alCambiarLaFotoBorraLaAnterior() throws IOException {
        servicio.subirFoto(estudiante, new MockMultipartFile("foto", "a.jpg", "image/jpeg", JPG));
        String primera = estudiante.getFotoPerfil();

        servicio.subirFoto(estudiante, new MockMultipartFile("foto", "b.png", "image/png", PNG));

        assertFalse(Files.exists(carpeta.resolve(primera)));
        assertTrue(Files.exists(carpeta.resolve(estudiante.getFotoPerfil())));
    }

    @Test
    void rechazaArchivosQueNoSonImagenesVaciosOMuyGrandes() {
        assertThrows(PerfilInvalidoException.class, () -> servicio.subirFoto(estudiante,
                new MockMultipartFile("foto", "falso.png", "image/png", "texto".getBytes())));
        assertThrows(PerfilInvalidoException.class, () -> servicio.subirFoto(estudiante,
                new MockMultipartFile("foto", "vacia.png", "image/png", new byte[0])));

        byte[] enorme = new byte[(int) PerfilService.MAX_BYTES_FOTO + 1];
        System.arraycopy(PNG, 0, enorme, 0, PNG.length);
        PerfilInvalidoException ex = assertThrows(PerfilInvalidoException.class, () -> servicio.subirFoto(estudiante,
                new MockMultipartFile("foto", "grande.png", "image/png", enorme)));
        assertTrue(ex.getMessage().contains("5 MB"));
        assertNull(estudiante.getFotoPerfil());
        verify(usuarioRepository, never()).save(estudiante);
    }

    @Test
    void quitarLaFotoBorraElArchivoYLimpiaElCampo() {
        servicio.subirFoto(estudiante, new MockMultipartFile("foto", "a.jpg", "image/jpeg", JPG));
        String nombre = estudiante.getFotoPerfil();

        PerfilResponse perfil = servicio.eliminarFoto(estudiante);

        assertFalse(perfil.tieneFoto());
        assertNull(estudiante.getFotoPerfil());
        assertFalse(Files.exists(carpeta.resolve(nombre)));
        assertTrue(servicio.foto(estudiante).isEmpty());
    }

    @Test
    void sinFotoNoHayNadaQueDevolver() {
        assertTrue(servicio.foto(estudiante).isEmpty());
    }

    @Test
    void cambiaLaContrasenaCuandoTodoEsCorrecto() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(passwordEncoder.matches("1075000001", "hash-actual")).thenReturn(true);
        when(passwordEncoder.encode("NuevaClave2026")).thenReturn("hash-nuevo");

        servicio.cambiarPassword(estudiante, new CambiarPasswordRequest("1075000001", "NuevaClave2026", "NuevaClave2026"));

        assertEquals("hash-nuevo", estudiante.getPasswordHash());
        verify(usuarioRepository).save(estudiante);
    }

    @Test
    void noCambiaLaContrasenaSiLaActualEsIncorrecta() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(passwordEncoder.matches("mala", "hash-actual")).thenReturn(false);

        PerfilInvalidoException ex = assertThrows(PerfilInvalidoException.class, () ->
                servicio.cambiarPassword(estudiante, new CambiarPasswordRequest("mala", "NuevaClave2026", "NuevaClave2026")));

        assertEquals("La contraseña actual no es correcta.", ex.getMessage());
        assertEquals("hash-actual", estudiante.getPasswordHash());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void validaLongitudConfirmacionYQueSeaDistintaALaActual() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertTrue(assertThrows(PerfilInvalidoException.class, () ->
                servicio.cambiarPassword(estudiante, new CambiarPasswordRequest("1075000001", "corta", "corta")))
                .getMessage().contains("entre 8 y 64"));

        assertTrue(assertThrows(PerfilInvalidoException.class, () ->
                servicio.cambiarPassword(estudiante, new CambiarPasswordRequest("1075000001", "NuevaClave2026", "OtraClave2026")))
                .getMessage().contains("confirmación"));

        assertTrue(assertThrows(PerfilInvalidoException.class, () ->
                servicio.cambiarPassword(estudiante, new CambiarPasswordRequest("1075000001", "1075000001", "1075000001")))
                .getMessage().contains("diferente"));

        verify(usuarioRepository, never()).save(estudiante);
    }
}