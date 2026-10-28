package com.fet.reportuni.controller;

import com.fet.reportuni.dto.AdminReporteItem;
import com.fet.reportuni.model.*;
import com.fet.reportuni.service.AdminReporteService;
import com.fet.reportuni.service.AlmacenamientoFotosService;
import com.fet.reportuni.service.ReporteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminReporteGestionControllerTest {

    private static final String NOTA = "Se contactó al técnico, visita el jueves";

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
    void cambiaElEstadoConNota() throws Exception {
        when(servicio.cambiarEstado(eq(admin), eq(7L), eq(EstadoReporte.EN_PROCESO), eq(NOTA), any()))
                .thenReturn(item);

        mockMvc.perform(multipart("/api/admin/reportes/7/estado")
                        .param("estado", "EN_PROCESO")
                        .param("nota", NOTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("0007"))
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"));
    }

    @Test
    void resolverConFotosDeEvidencia() throws Exception {
        when(servicio.cambiarEstado(eq(admin), eq(7L), eq(EstadoReporte.RESUELTO), eq(NOTA), any()))
                .thenReturn(item);

        mockMvc.perform(multipart("/api/admin/reportes/7/estado")
                        .file(new MockMultipartFile("fotos", "arreglo.jpg", "image/jpeg", new byte[]{1, 2}))
                        .param("estado", "RESUELTO")
                        .param("nota", NOTA))
                .andExpect(status().isOk());
    }

    @Test
    void resolverSinFotosDevuelve400ConMensaje() throws Exception {
        when(servicio.cambiarEstado(eq(admin), eq(7L), eq(EstadoReporte.RESUELTO), eq(NOTA), any()))
                .thenThrow(new AlmacenamientoFotosService.FotoInvalidaException(
                        "Para marcar el reporte como resuelto adjunta al menos una fotografía del arreglo."));

        mockMvc.perform(multipart("/api/admin/reportes/7/estado")
                        .param("estado", "RESUELTO")
                        .param("nota", NOTA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje")
                        .value("Para marcar el reporte como resuelto adjunta al menos una fotografía del arreglo."));
    }

    @Test
    void estadoInexistenteDevuelve400() throws Exception {
        mockMvc.perform(multipart("/api/admin/reportes/7/estado")
                        .param("estado", "INVENTADO")
                        .param("nota", NOTA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El valor enviado no es válido."));
    }

    @Test
    void sinEstadoDevuelve400ConMensaje() throws Exception {
        when(servicio.cambiarEstado(eq(admin), eq(7L), isNull(), any(), any()))
                .thenThrow(new AdminReporteService.CambioInvalidoException("Debes indicar el nuevo estado."));

        mockMvc.perform(multipart("/api/admin/reportes/7/estado").param("nota", NOTA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debes indicar el nuevo estado."));
    }

    @Test
    void notaInvalidaDevuelve400() throws Exception {
        when(servicio.cambiarEstado(eq(admin), eq(7L), eq(EstadoReporte.EN_PROCESO), eq("corta"), any()))
                .thenThrow(new AdminReporteService.CambioInvalidoException(
                        "Describe qué se está haciendo para atender el reporte (entre 10 y 1000 caracteres)."));

        mockMvc.perform(multipart("/api/admin/reportes/7/estado")
                        .param("estado", "EN_PROCESO")
                        .param("nota", "corta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "Describe qué se está haciendo para atender el reporte (entre 10 y 1000 caracteres)."));
    }

    @Test
    void reporteInexistenteDevuelve404() throws Exception {
        when(servicio.cambiarEstado(eq(admin), eq(99L), eq(EstadoReporte.RESUELTO), eq(NOTA), any()))
                .thenThrow(new ReporteService.ReporteNoEncontradoException("No encontramos ese reporte."));

        mockMvc.perform(multipart("/api/admin/reportes/99/estado")
                        .param("estado", "RESUELTO")
                        .param("nota", NOTA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos ese reporte."));
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
    void sinPrioridadDevuelve400ConMensaje() throws Exception {
        mockMvc.perform(put("/api/admin/reportes/7/prioridad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debes indicar la nueva prioridad."));
    }

    @Test
    void asignaElReporte() throws Exception {
        when(servicio.asignar(eq(admin), eq(7L), eq(AreaAsignada.SOPORTE_TECNICO), isNull())).thenReturn(item);

        mockMvc.perform(put("/api/admin/reportes/7/asignacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"area\":\"SOPORTE_TECNICO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("0007"));
    }

    @Test
    void asignarSinEquipoDevuelve400() throws Exception {
        mockMvc.perform(put("/api/admin/reportes/7/asignacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Selecciona a quién se asigna el reporte."));
    }

    @Test
    void asignarAEquipoInexistenteDevuelve400() throws Exception {
        mockMvc.perform(put("/api/admin/reportes/7/asignacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"area\":\"INVENTADO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El valor enviado no es válido."));
    }

    @Test
    void agregaUnaActualizacion() throws Exception {
        when(servicio.agregarActualizacion(eq(admin), eq(7L), eq(NOTA), any())).thenReturn(item);

        mockMvc.perform(multipart("/api/admin/reportes/7/actualizaciones")
                        .file(new MockMultipartFile("fotos", "repuesto.png", "image/png", new byte[]{1}))
                        .param("nota", NOTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("0007"));
    }

    @Test
    void actualizacionSinNotaDevuelve400() throws Exception {
        when(servicio.agregarActualizacion(eq(admin), eq(7L), isNull(), any()))
                .thenThrow(new AdminReporteService.CambioInvalidoException(
                        "Describe la novedad del proceso (entre 10 y 1000 caracteres)."));

        mockMvc.perform(multipart("/api/admin/reportes/7/actualizaciones"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Describe la novedad del proceso (entre 10 y 1000 caracteres)."));
    }
}