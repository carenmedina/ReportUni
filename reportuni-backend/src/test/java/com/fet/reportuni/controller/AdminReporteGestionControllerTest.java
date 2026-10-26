package com.fet.reportuni.controller;

import com.fet.reportuni.dto.AdminReporteItem;
import com.fet.reportuni.model.*;
import com.fet.reportuni.service.AdminReporteService;
import com.fet.reportuni.service.ReporteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminReporteGestionControllerTest {

    @Mock
    private AdminReporteService servicio;

    private MockMvc mockMvc;

    private final Usuario admin = Usuario.builder().id(9L).nombreCompleto("Admin")
            .correoInstitucional("admin@fet.edu.co").passwordHash("x").rol(Rol.ADMINISTRADOR).build();

    private final AdminReporteItem item = new AdminReporteItem(
            7L, "0007", TipoDano.FUGA_AGUA, Bloque.BLOQUE_A, "Salon 204", PrioridadReporte.MEDIA,
            EstadoReporte.EN_PROCESO, "Estudiante Uno", "estudiante1@fet.edu.co",
            LocalDateTime.of(2026, 10, 20, 9, 0));

    @BeforeEach
    void preparar() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, List.of()));
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminReporteController(servicio))
                .setControllerAdvice(new AdminReporteExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cambiaElEstado() throws Exception {
        when(servicio.cambiarEstado(admin, 7L, EstadoReporte.EN_PROCESO)).thenReturn(item);

        mockMvc.perform(put("/api/admin/reportes/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_PROCESO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("0007"))
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));
    }

    @Test
    void cambiaLaPrioridad() throws Exception {
        when(servicio.cambiarPrioridad(admin, 7L, PrioridadReporte.MEDIA)).thenReturn(item);

        mockMvc.perform(put("/api/admin/reportes/7/prioridad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prioridad\":\"MEDIA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridadEstimada").value("MEDIA"));
    }

    @Test
    void estadoInexistenteDevuelve400() throws Exception {
        mockMvc.perform(put("/api/admin/reportes/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"INVENTADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El valor enviado no es válido."));
    }

    @Test
    void sinEstadoDevuelve400ConMensaje() throws Exception {
        mockMvc.perform(put("/api/admin/reportes/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debes indicar el nuevo estado."));
    }

    @Test
    void mismoEstadoDevuelve400() throws Exception {
        when(servicio.cambiarEstado(admin, 7L, EstadoReporte.PENDIENTE))
                .thenThrow(new AdminReporteService.CambioInvalidoException("El reporte ya está en estado Pendiente."));

        mockMvc.perform(put("/api/admin/reportes/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"PENDIENTE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El reporte ya está en estado Pendiente."));
    }

    @Test
    void reporteInexistenteDevuelve404() throws Exception {
        when(servicio.cambiarEstado(admin, 99L, EstadoReporte.RESUELTO))
                .thenThrow(new ReporteService.ReporteNoEncontradoException("No encontramos ese reporte."));

        mockMvc.perform(put("/api/admin/reportes/99/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RESUELTO\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos ese reporte."));
    }
}