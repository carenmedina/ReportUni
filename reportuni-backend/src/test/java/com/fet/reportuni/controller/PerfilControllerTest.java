package com.fet.reportuni.controller;

import com.fet.reportuni.dto.PerfilResponse;
import com.fet.reportuni.dto.ResumenPerfil;
import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.service.PerfilService;
import com.fet.reportuni.service.PerfilService.PerfilInvalidoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PerfilControllerTest {

    @Mock
    private PerfilService servicio;

    private MockMvc mockMvc;
    private Usuario estudiante;

    @BeforeEach
    void preparar() {
        estudiante = Usuario.builder().id(1L).nombreCompleto("Estudiante Uno")
                .correoInstitucional("estudiante1@fet.edu.co").passwordHash("x").rol(Rol.ESTUDIANTE).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(estudiante, null, List.of()));

        mockMvc = MockMvcBuilders.standaloneSetup(new PerfilController(servicio))
                .setControllerAdvice(new PerfilExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private PerfilResponse perfil(boolean conFoto) {
        return new PerfilResponse("Estudiante Uno", "estudiante1@fet.edu.co", "ESTUDIANTE", conFoto,
                new ResumenPerfil(6L, 3L, 2L, 1L, null));
    }

    @Test
    void devuelveElPerfilConSuResumen() throws Exception {
        when(servicio.obtener(estudiante)).thenReturn(perfil(false));

        mockMvc.perform(get("/api/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Estudiante Uno"))
                .andExpect(jsonPath("$.rol").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.tieneFoto").value(false))
                .andExpect(jsonPath("$.resumen.total").value(6))
                .andExpect(jsonPath("$.resumen.pendientes").value(3));
    }

    @Test
    void subeLaFotoDePerfil() throws Exception {
        when(servicio.subirFoto(any(), any())).thenReturn(perfil(true));

        mockMvc.perform(multipart("/api/perfil/foto")
                        .file(new MockMultipartFile("foto", "yo.png", "image/png", new byte[]{1, 2, 3})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneFoto").value(true));
    }

    @Test
    void laFotoInvalidaRespondeBadRequestConElMensaje() throws Exception {
        when(servicio.subirFoto(any(), any()))
                .thenThrow(new PerfilInvalidoException("El archivo no es una imagen JPG o PNG válida."));

        mockMvc.perform(multipart("/api/perfil/foto")
                        .file(new MockMultipartFile("foto", "x.png", "image/png", new byte[]{1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El archivo no es una imagen JPG o PNG válida."));
    }

    @Test
    void sinArchivoRespondeBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/perfil/foto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Selecciona una imagen JPG o PNG."));
    }

    @Test
    void entregaLaFotoConSuTipoDeContenido() throws Exception {
        when(servicio.foto(estudiante)).thenReturn(Optional.of(
                new PerfilService.FotoPerfil(new ByteArrayResource(new byte[]{1, 2, 3}), "image/png")));

        mockMvc.perform(get("/api/perfil/foto"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void sinFotoRespondeNotFound() throws Exception {
        when(servicio.foto(estudiante)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/perfil/foto")).andExpect(status().isNotFound());
    }

    @Test
    void quitaLaFoto() throws Exception {
        when(servicio.eliminarFoto(estudiante)).thenReturn(perfil(false));

        mockMvc.perform(delete("/api/perfil/foto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneFoto").value(false));
    }

    @Test
    void cambiaLaContrasenaYResponde204() throws Exception {
        mockMvc.perform(put("/api/perfil/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"a\",\"nueva\":\"NuevaClave2026\",\"confirmacion\":\"NuevaClave2026\"}"))
                .andExpect(status().isNoContent());

        verify(servicio).cambiarPassword(any(), any());
    }

    @Test
    void laContrasenaActualIncorrectaRespondeBadRequest() throws Exception {
        doThrow(new PerfilInvalidoException("La contraseña actual no es correcta."))
                .when(servicio).cambiarPassword(any(), any());

        mockMvc.perform(put("/api/perfil/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"mala\",\"nueva\":\"NuevaClave2026\",\"confirmacion\":\"NuevaClave2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña actual no es correcta."));
    }

    @Test
    void camposVaciosRespondenBadRequestConElPrimerMensaje() throws Exception {
        mockMvc.perform(put("/api/perfil/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"\",\"nueva\":\"\",\"confirmacion\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
    }
}