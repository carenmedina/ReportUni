package com.fet.reportuni.controller;

import com.fet.reportuni.dto.AdminReporteItem;
import com.fet.reportuni.dto.AdminResumenResponse;
import com.fet.reportuni.model.*;
import com.fet.reportuni.service.AdminReporteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminReporteControllerTest {

    @Mock
    private AdminReporteService servicio;

    private MockMvc mockMvc;

    private final AdminReporteItem item = new AdminReporteItem(
            12L, "0012", TipoDano.FUGA_AGUA, Bloque.BLOQUE_A, "Salon 204", PrioridadReporte.ALTA,
            EstadoReporte.PENDIENTE, "Estudiante Uno", "estudiante1@fet.edu.co",
            LocalDateTime.of(2026, 10, 5, 9, 30));

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminReporteController(servicio)).build();
    }

    @Test
    void devuelveElResumen() throws Exception {
        when(servicio.resumen()).thenReturn(new AdminResumenResponse(5, 3, 1, 1, 2, List.of(item)));

        mockMvc.perform(get("/api/admin/reportes/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.pendientes").value(3))
                .andExpect(jsonPath("$.altaPrioridadPendientes").value(2))
                .andExpect(jsonPath("$.recientes[0].reportadoPor").value("Estudiante Uno"));
    }

    @Test
    void listaTodosLosReportesConFiltros() throws Exception {
        when(servicio.listar(EstadoReporte.PENDIENTE, null, PrioridadReporte.ALTA, null, "salon"))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/admin/reportes")
                        .param("estado", "PENDIENTE")
                        .param("prioridad", "ALTA")
                        .param("q", "salon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("0012"))
                .andExpect(jsonPath("$[0].reportadoPorCorreo").value("estudiante1@fet.edu.co"));
    }

    @Test
    void sinParametrosNoAplicaFiltros() throws Exception {
        when(servicio.listar(isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(item));

        mockMvc.perform(get("/api/admin/reportes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void estadoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/admin/reportes").param("estado", "INVENTADO"))
                .andExpect(status().isBadRequest());
    }
}