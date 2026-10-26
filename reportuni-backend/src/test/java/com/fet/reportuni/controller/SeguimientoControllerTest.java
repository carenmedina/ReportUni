package com.fet.reportuni.controller;

import com.fet.reportuni.dto.HistorialItemResponse;
import com.fet.reportuni.dto.NotificacionResponse;
import com.fet.reportuni.dto.TotalNoLeidasResponse;
import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.TipoCambio;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.service.ReporteService;
import com.fet.reportuni.service.SeguimientoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SeguimientoControllerTest {

    @Mock
    private SeguimientoService servicio;

    private MockMvc mockMvc;
    private Usuario estudiante;

    @BeforeEach
    void preparar() {
        estudiante = Usuario.builder().id(1L).nombreCompleto("Estudiante Uno")
                .correoInstitucional("estudiante1@fet.edu.co").passwordHash("x").rol(Rol.ESTUDIANTE).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(estudiante, null, List.of()));

        mockMvc = MockMvcBuilders.standaloneSetup(new SeguimientoController(servicio))
                .setControllerAdvice(new SeguimientoExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devuelveElHistorial() throws Exception {
        when(servicio.historial(estudiante, 7L)).thenReturn(List.of(new HistorialItemResponse(
                1L, TipoCambio.ESTADO, "PENDIENTE", "EN_PROCESO", "Admin", LocalDateTime.of(2026, 10, 21, 10, 0))));

        mockMvc.perform(get("/api/reportes/7/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("ESTADO"))
                .andExpect(jsonPath("$[0].valorNuevo").value("EN_PROCESO"))
                .andExpect(jsonPath("$[0].realizadoPor").value("Admin"));
    }

    @Test
    void historialDeReporteAjenoDevuelve404() throws Exception {
        when(servicio.historial(estudiante, 8L))
                .thenThrow(new ReporteService.ReporteNoEncontradoException("No encontramos ese reporte."));

        mockMvc.perform(get("/api/reportes/8/historial"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos ese reporte."));
    }

    @Test
    void listaLasNotificaciones() throws Exception {
        when(servicio.misNotificaciones(estudiante)).thenReturn(List.of(new NotificacionResponse(
                3L, 7L, "0007", "Tu reporte #0007 cambió a estado «Resuelto».", false,
                LocalDateTime.of(2026, 10, 22, 8, 0))));

        mockMvc.perform(get("/api/notificaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("0007"))
                .andExpect(jsonPath("$[0].leida").value(false));
    }

    @Test
    void devuelveElTotalDeNoLeidas() throws Exception {
        when(servicio.totalNoLeidas(estudiante)).thenReturn(new TotalNoLeidasResponse(2));

        mockMvc.perform(get("/api/notificaciones/no-leidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void marcaUnaComoLeida() throws Exception {
        mockMvc.perform(put("/api/notificaciones/3/leida"))
                .andExpect(status().isNoContent());

        verify(servicio).marcarLeida(estudiante, 3L);
    }

    @Test
    void marcarNotificacionAjenaDevuelve404() throws Exception {
        doThrow(new ReporteService.ReporteNoEncontradoException("No encontramos esa notificación."))
                .when(servicio).marcarLeida(estudiante, 5L);

        mockMvc.perform(put("/api/notificaciones/5/leida"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos esa notificación."));
    }

    @Test
    void marcaTodasComoLeidas() throws Exception {
        mockMvc.perform(put("/api/notificaciones/leidas"))
                .andExpect(status().isNoContent());

        verify(servicio).marcarTodasLeidas(estudiante);
    }
}