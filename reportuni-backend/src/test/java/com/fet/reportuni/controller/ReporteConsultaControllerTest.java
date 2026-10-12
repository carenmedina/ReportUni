package com.fet.reportuni.controller;

import com.fet.reportuni.dto.ReporteDetalleResponse;
import com.fet.reportuni.dto.ReporteResumenResponse;
import com.fet.reportuni.model.*;
import com.fet.reportuni.service.ReporteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReporteConsultaControllerTest {

    @Mock
    private ReporteService reporteService;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        Usuario estudiante = Usuario.builder().id(1L).nombreCompleto("Estudiante Uno")
                .correoInstitucional("e1@fet.edu.co").passwordHash("x").rol(Rol.ESTUDIANTE).build();
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

    @Test
    void listaMisReportes() throws Exception {
        when(reporteService.listarMios(any())).thenReturn(List.of(new ReporteResumenResponse(
                12L, "0012", TipoDano.FUGA_AGUA, Bloque.BLOQUE_A, "Salon 204",
                PrioridadReporte.ALTA, EstadoReporte.PENDIENTE, LocalDateTime.of(2026, 10, 5, 9, 30), 2)));

        mockMvc.perform(get("/api/reportes/mis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("0012"))
                .andExpect(jsonPath("$[0].tipoDano").value("FUGA_AGUA"))
                .andExpect(jsonPath("$[0].totalFotos").value(2));
    }

    @Test
    void devuelveElDetalleConSusFotos() throws Exception {
        when(reporteService.detalle(any(), eq(12L))).thenReturn(new ReporteDetalleResponse(
                12L, "0012", TipoDano.FUGA_AGUA, Bloque.BLOQUE_A, "Salon 204", PrioridadReporte.ALTA,
                EstadoReporte.PENDIENTE, "Fuga junto a la ventana", 6.2, -75.5, true, "Estudiante Uno",
                LocalDateTime.of(2026, 10, 5, 9, 30),
                List.of(new ReporteDetalleResponse.FotoInfo(70L, "dano.jpg", "image/jpeg", 1234, 1))));

        mockMvc.perform(get("/api/reportes/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Fuga junto a la ventana"))
                .andExpect(jsonPath("$.fotos[0].id").value(70));
    }

    @Test
    void respondeNotFoundCuandoElReporteNoEsDelUsuario() throws Exception {
        when(reporteService.detalle(any(), eq(99L)))
                .thenThrow(new ReporteService.ReporteNoEncontradoException("No encontramos ese reporte."));

        mockMvc.perform(get("/api/reportes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos ese reporte."));
    }

    @Test
    void entregaLaFotoConSuTipoDeContenido() throws Exception {
        when(reporteService.foto(any(), eq(12L), eq(70L))).thenReturn(
                new ReporteService.FotoDescargable(new ByteArrayResource(new byte[]{1, 2, 3}), "image/jpeg"));

        mockMvc.perform(get("/api/reportes/12/fotos/70"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }
}