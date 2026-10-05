package com.fet.reportuni.controller;

import com.fet.reportuni.dto.ReporteCreadoResponse;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.service.AlmacenamientoFotosService.FotoInvalidaException;
import com.fet.reportuni.service.ReporteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};

    @Mock
    private ReporteService reporteService;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        Usuario estudiante = Usuario.builder()
                .id(1L)
                .nombreCompleto("Estudiante Uno")
                .correoInstitucional("estudiante1@fet.edu.co")
                .passwordHash("hash")
                .rol(Rol.ESTUDIANTE)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(estudiante, null, List.of()));

        mockMvc = MockMvcBuilders.standaloneSetup(new ReporteController(reporteService))
                .setControllerAdvice(new ReporteExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequestBuilder solicitudValida() {
        return solicitudConTipo("FUGA_AGUA");
    }

    private MockHttpServletRequestBuilder solicitudConTipo(String tipoDano) {
        return multipart("/api/reportes")
                .file(new MockMultipartFile("fotos", "dano.jpg", "image/jpeg", JPG))
                .param("tipoDano", tipoDano)
                .param("prioridadEstimada", "MEDIA")
                .param("bloque", "BLOQUE_A")
                .param("espacioEspecifico", "Salon 204")
                .param("descripcion", "Fuga de agua junto a la ventana del salon")
                .param("notificarPorCorreo", "true");
    }

    @Test
    void creaElReporteYResponde201() throws Exception {
        when(reporteService.crear(any(), any(), any())).thenReturn(
                new ReporteCreadoResponse(12L, "0012", EstadoReporte.PENDIENTE, LocalDateTime.of(2026, 10, 4, 18, 30), 1));

        mockMvc.perform(solicitudValida())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.codigo").value("0012"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.totalFotos").value(1));
    }

    @Test
    void respondeConErroresPorCampoCuandoFaltanDatosObligatorios() throws Exception {
        mockMvc.perform(multipart("/api/reportes").param("descripcion", "corta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.errores.tipoDano").exists())
                .andExpect(jsonPath("$.errores.bloque").exists())
                .andExpect(jsonPath("$.errores.espacioEspecifico").exists())
                .andExpect(jsonPath("$.errores.descripcion").exists());

        verifyNoInteractions(reporteService);
    }

    @Test
    void rechazaUnTipoDeDanoQueNoExiste() throws Exception {
        mockMvc.perform(solicitudConTipo("INVENTADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tipoDano").exists());

        verifyNoInteractions(reporteService);
    }

    @Test
    void rechazaUnaLatitudFueraDeRango() throws Exception {
        mockMvc.perform(solicitudValida().param("latitud", "123.5").param("longitud", "-75.2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.latitud").exists());

        verifyNoInteractions(reporteService);
    }

    @Test
    void traduceLosErroresDeFotosAUn400ConMensaje() throws Exception {
        when(reporteService.crear(any(), any(), any())).thenThrow(new FotoInvalidaException("Mensaje de prueba"));

        mockMvc.perform(solicitudValida())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Mensaje de prueba"));
    }
}