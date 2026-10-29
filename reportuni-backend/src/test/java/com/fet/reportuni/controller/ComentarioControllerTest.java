package com.fet.reportuni.controller;

import com.fet.reportuni.dto.ComentarioResponse;
import com.fet.reportuni.dto.NotificacionResponse;
import com.fet.reportuni.dto.TotalNoLeidasResponse;
import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.service.ComentarioService;
import com.fet.reportuni.service.ReporteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ComentarioControllerTest {

    @Mock
    private ComentarioService servicio;

    private MockMvc mockMvc;
    private Usuario estudiante;

    @BeforeEach
    void preparar() {
        estudiante = Usuario.builder().id(1L).nombreCompleto("Estudiante Uno")
                .correoInstitucional("estudiante1@fet.edu.co").passwordHash("x").rol(Rol.ESTUDIANTE).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(estudiante, null, List.of()));

        mockMvc = MockMvcBuilders.standaloneSetup(new ComentarioController(servicio))
                .setControllerAdvice(new ComentarioExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private ComentarioResponse respuesta(String texto, boolean propio) {
        return new ComentarioResponse(1L, 7L, texto, "Admin Uno", Rol.ADMINISTRADOR, propio,
                LocalDateTime.of(2026, 10, 21, 10, 0));
    }

    @Test
    void listaLosComentariosDelReporte() throws Exception {
        when(servicio.listar(estudiante, 7L)).thenReturn(List.of(respuesta("Hola", false)));

        mockMvc.perform(get("/api/reportes/7/comentarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].texto").value("Hola"))
                .andExpect(jsonPath("$[0].autor").value("Admin Uno"))
                .andExpect(jsonPath("$[0].rol").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$[0].propio").value(false));
    }

    @Test
    void listarUnReporteAjenoDaNoEncontrado() throws Exception {
        when(servicio.listar(estudiante, 7L))
                .thenThrow(new ReporteService.ReporteNoEncontradoException("No encontramos ese reporte."));

        mockMvc.perform(get("/api/reportes/7/comentarios"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No encontramos ese reporte."));
    }

    @Test
    void creaUnComentario() throws Exception {
        when(servicio.comentar(estudiante, 7L, "Gracias")).thenReturn(respuesta("Gracias", true));

        mockMvc.perform(post("/api/reportes/7/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Gracias\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.texto").value("Gracias"))
                .andExpect(jsonPath("$.propio").value(true));
    }

    @Test
    void unComentarioInvalidoDa400ConElMensaje() throws Exception {
        when(servicio.comentar(estudiante, 7L, " "))
                .thenThrow(new ComentarioService.ComentarioInvalidoException("Escribe un comentario antes de enviarlo."));

        mockMvc.perform(post("/api/reportes/7/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Escribe un comentario antes de enviarlo."));
    }

    @Test
    void uncuerpoMalFormadoDa400() throws Exception {
        mockMvc.perform(post("/api/reportes/7/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void listaLosAvisosDeComentarios() throws Exception {
        when(servicio.misAvisos(estudiante)).thenReturn(List.of(new NotificacionResponse(
                3L, 7L, "0007", "Admin Uno comentó en tu reporte #0007: «Hola»", false,
                LocalDateTime.of(2026, 10, 22, 8, 0))));

        mockMvc.perform(get("/api/notificaciones/comentarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("0007"))
                .andExpect(jsonPath("$[0].leida").value(false));
    }

    @Test
    void devuelveElTotalDeAvisosSinLeer() throws Exception {
        when(servicio.totalNoLeidos(estudiante)).thenReturn(new TotalNoLeidasResponse(2));

        mockMvc.perform(get("/api/notificaciones/comentarios/no-leidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void marcaTodosLosAvisosComoLeidos() throws Exception {
        mockMvc.perform(put("/api/notificaciones/comentarios/leidas"))
                .andExpect(status().isNoContent());

        verify(servicio).marcarTodosLeidos(estudiante);
    }
}