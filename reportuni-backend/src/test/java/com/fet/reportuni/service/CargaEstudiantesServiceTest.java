package com.fet.reportuni.service;

import com.fet.reportuni.dto.CargaEstudiantesResponse;
import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.repository.UsuarioRepository;
import com.fet.reportuni.service.CargaEstudiantesService.CargaInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CargaEstudiantesServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private CargaEstudiantesService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CargaEstudiantesService(usuarioRepository, passwordEncoder);
        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(i -> "hash(" + i.getArgument(0) + ")");
        lenient().when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(anyString())).thenReturn(Optional.empty());
    }

    private CargaEstudiantesResponse cargar(String csv) {
        return servicio.procesar(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void creaEstudiantesConElDocumentoComoContrasenaInicial() {
        CargaEstudiantesResponse r = cargar("documento;nombre;correo\n1075000001;Ana Pérez;ANA.PEREZ@fet.edu.co\n");

        assertEquals(1, r.creados());
        assertEquals(0, r.rechazados());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals("ana.perez@fet.edu.co", guardado.getCorreoInstitucional());
        assertEquals("Ana Pérez", guardado.getNombreCompleto());
        assertEquals("hash(1075000001)", guardado.getPasswordHash());
        assertEquals(Rol.ESTUDIANTE, guardado.getRol());
        assertTrue(guardado.isActivo());
    }

    @Test
    void aceptaComaComoSeparadorYEncabezadosConTildesYOrdenDistinto() {
        CargaEstudiantesResponse r = cargar("Correo Institucional,Nombre Completo,Documento\nluis@fet.edu.co,Luis Gómez,2000123\n");

        assertEquals(1, r.creados());
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("hash(2000123)", captor.getValue().getPasswordHash());
        assertEquals("Luis Gómez", captor.getValue().getNombreCompleto());
    }

    @Test
    void ignoraBomYLineasVacias() {
        byte[] contenido = ("﻿documento;nombre;correo\r\n\r\n1075000001;Ana;ana@fet.edu.co\r\n;;\r\n")
                .getBytes(StandardCharsets.UTF_8);
        CargaEstudiantesResponse r = servicio.procesar(new ByteArrayInputStream(contenido));

        assertEquals(1, r.filasLeidas());
        assertEquals(1, r.creados());
    }

    @Test
    void leeArchivosGuardadosPorExcelEnWindows1252() {
        byte[] contenido = "documento;nombre;correo\n1075000001;José Muñoz;jose@fet.edu.co\n"
                .getBytes(Charset.forName("windows-1252"));
        CargaEstudiantesResponse r = servicio.procesar(new ByteArrayInputStream(contenido));

        assertEquals(1, r.creados());
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("José Muñoz", captor.getValue().getNombreCompleto());
    }

    @Test
    void respetaCamposEntreComillas() {
        CargaEstudiantesResponse r = cargar("documento,nombre,correo\n1075000001,\"Pérez, Ana\",ana@fet.edu.co\n");

        assertEquals(1, r.creados());
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("Pérez, Ana", captor.getValue().getNombreCompleto());
    }

    @Test
    void actualizaElNombreSinTocarLaContrasenaDeUnEstudianteExistente() {
        Usuario existente = Usuario.builder().id(5L).nombreCompleto("Ana P").correoInstitucional("ana@fet.edu.co")
                .passwordHash("hash-que-ya-cambio").rol(Rol.ESTUDIANTE).build();
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase("ana@fet.edu.co")).thenReturn(Optional.of(existente));

        CargaEstudiantesResponse r = cargar("documento;nombre;correo\n1075000001;Ana Pérez;ana@fet.edu.co\n");

        assertEquals(0, r.creados());
        assertEquals(1, r.actualizados());
        assertEquals("Ana Pérez", existente.getNombreCompleto());
        assertEquals("hash-que-ya-cambio", existente.getPasswordHash());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void noPermiteModificarCuentasDeAdministracion() {
        Usuario admin = Usuario.builder().id(1L).nombreCompleto("Admin").correoInstitucional("admin@fet.edu.co")
                .passwordHash("x").rol(Rol.ADMINISTRADOR).build();
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase("admin@fet.edu.co")).thenReturn(Optional.of(admin));

        CargaEstudiantesResponse r = cargar("documento;nombre;correo\n1075000001;Otro Nombre;admin@fet.edu.co\n");

        assertEquals(0, r.actualizados());
        assertEquals(1, r.rechazados());
        assertEquals("Admin", admin.getNombreCompleto());
        verify(usuarioRepository, never()).save(admin);
    }

    @Test
    void rechazaFilasInvalidasYContinuaConLasDemas() {
        CargaEstudiantesResponse r = cargar(String.join("\n",
                "documento;nombre;correo",
                "1075000001;Ana Pérez;ana@fet.edu.co",
                "12;Luis Gómez;luis@fet.edu.co",
                "1075000003;Marta;correo-malo",
                "1075000004;;sin.nombre@fet.edu.co",
                "1075000005;Ana Repetida;ANA@fet.edu.co",
                "1075000006;Pedro Ruiz;pedro@fet.edu.co"));

        assertEquals(6, r.filasLeidas());
        assertEquals(2, r.creados());
        assertEquals(4, r.rechazados());
        assertEquals(3, r.errores().get(0).fila());
        assertEquals(4, r.errores().get(1).fila());
        assertEquals(5, r.errores().get(2).fila());
        assertEquals(6, r.errores().get(3).fila());
        assertTrue(r.errores().get(3).motivo().contains("repetido"));
        verify(usuarioRepository, times(2)).save(org.mockito.ArgumentMatchers.any(Usuario.class));
    }

    @Test
    void soloAceptaCorreosInstitucionales() {
        CargaEstudiantesResponse r = cargar(String.join("\n",
                "documento;nombre;correo",
                "1075000001;Ana Pérez;ana@gmail.com",
                "1075000002;Luis Gómez;luis@FET.EDU.CO",
                "1075000003;Marta Ruiz;marta@fet.edu.co.otro.com"));

        assertEquals(1, r.creados());
        assertEquals(2, r.rechazados());
        assertTrue(r.errores().get(0).motivo().contains("@fet.edu.co"));
        assertEquals(2, r.errores().get(0).fila());
        assertEquals(4, r.errores().get(1).fila());
    }

    @Test
    void exigeLosEncabezadosDocumentoNombreYCorreo() {
        CargaInvalidaException ex = assertThrows(CargaInvalidaException.class,
                () -> cargar("codigo;nombre;correo\n1;Ana;ana@fet.edu.co\n"));
        assertTrue(ex.getMessage().contains("encabezados"));
    }

    @Test
    void rechazaArchivosVaciosOSoloConEncabezados() {
        assertThrows(CargaInvalidaException.class, () -> cargar(""));
        assertThrows(CargaInvalidaException.class, () -> cargar("   \n\n"));
        assertThrows(CargaInvalidaException.class, () -> cargar("documento;nombre;correo\n"));
    }

    @Test
    void rechazaArchivosConMasFilasDeLasPermitidas() {
        StringBuilder csv = new StringBuilder("documento;nombre;correo\n");
        for (int i = 0; i <= CargaEstudiantesService.MAX_FILAS; i++) {
            csv.append("100000").append(i).append(";Estudiante ").append(i).append(";e").append(i).append("@fet.edu.co\n");
        }
        CargaInvalidaException ex = assertThrows(CargaInvalidaException.class, () -> cargar(csv.toString()));
        assertTrue(ex.getMessage().contains("máximo"));
    }

    @Test
    void rechazaArchivosMuyGrandes() {
        byte[] enorme = new byte[CargaEstudiantesService.MAX_BYTES + 10];
        assertThrows(CargaInvalidaException.class, () -> servicio.procesar(new ByteArrayInputStream(enorme)));
    }
}