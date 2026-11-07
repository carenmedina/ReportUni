package com.fet.reportuni.controller;

import com.fet.reportuni.dto.CargaEstudiantesResponse;
import com.fet.reportuni.dto.CargaEstudiantesResponse.ErrorFila;
import com.fet.reportuni.service.CargaEstudiantesService;
import com.fet.reportuni.service.CargaEstudiantesService.CargaInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminEstudianteControllerTest {

    @Mock
    private CargaEstudiantesService servicio;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminEstudianteController(servicio))
                .setControllerAdvice(new AdminEstudianteExceptionHandler())
                .build();
    }

    @Test
    void devuelveElResumenDeLaCarga() throws Exception {
        when(servicio.procesar(any(InputStream.class))).thenReturn(new CargaEstudiantesResponse(
                3, 1, 1, 1, List.of(new ErrorFila(4, "x@fet.edu.co", "El correo no tiene un formato válido."))));

        MockMultipartFile archivo = new MockMultipartFile("archivo", "estudiantes.csv", "text/csv",
                "documento;nombre;correo\n1;a;b".getBytes());

        mockMvc.perform(multipart("/api/admin/estudiantes/carga").file(archivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filasLeidas").value(3))
                .andExpect(jsonPath("$.creados").value(1))
                .andExpect(jsonPath("$.actualizados").value(1))
                .andExpect(jsonPath("$.rechazados").value(1))
                .andExpect(jsonPath("$.errores[0].fila").value(4))
                .andExpect(jsonPath("$.errores[0].motivo").value("El correo no tiene un formato válido."));
    }

    @Test
    void respondeBadRequestConArchivoVacio() throws Exception {
        MockMultipartFile vacio = new MockMultipartFile("archivo", "estudiantes.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/admin/estudiantes/carga").file(vacio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Selecciona un archivo CSV con los estudiantes."));
    }

    @Test
    void respondeBadRequestSiNoSeEnviaArchivo() throws Exception {
        mockMvc.perform(multipart("/api/admin/estudiantes/carga"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Selecciona un archivo CSV con los estudiantes."));
    }

    @Test
    void trasladaElMensajeDelServicioCuandoElArchivoEsInvalido() throws Exception {
        when(servicio.procesar(any(InputStream.class)))
                .thenThrow(new CargaInvalidaException("La primera fila debe tener los encabezados: documento, nombre y correo."));

        MockMultipartFile archivo = new MockMultipartFile("archivo", "e.csv", "text/csv", "a,b".getBytes());

        mockMvc.perform(multipart("/api/admin/estudiantes/carga").file(archivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La primera fila debe tener los encabezados: documento, nombre y correo."));
    }
}