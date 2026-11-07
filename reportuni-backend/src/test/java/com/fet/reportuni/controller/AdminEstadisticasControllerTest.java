package com.fet.reportuni.controller;

import com.fet.reportuni.dto.ConteoEstadistica;
import com.fet.reportuni.dto.EstadisticasResponse;
import com.fet.reportuni.service.EstadisticasService;
import com.fet.reportuni.service.EstadisticasService.RangoInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminEstadisticasControllerTest {

    @Mock
    private EstadisticasService servicio;

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminEstadisticasController(servicio))
                .setControllerAdvice(new AdminEstadisticasExceptionHandler())
                .build();
    }

    private EstadisticasResponse respuesta() {
        return new EstadisticasResponse(3,
                List.of(new ConteoEstadistica("FUGA_AGUA", 2), new ConteoEstadistica("AUDIOVISUAL", 1)),
                List.of(new ConteoEstadistica("BLOQUE_1", 2)),
                List.of(new ConteoEstadistica("ALTA", 1), new ConteoEstadistica("MEDIA", 1), new ConteoEstadistica("BAJA", 1)),
                1.5, 2);
    }

    @Test
    void devuelveLasEstadisticasSinFiltro() throws Exception {
        when(servicio.calcular(null, null)).thenReturn(respuesta());

        mockMvc.perform(get("/api/admin/estadisticas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReportes").value(3))
                .andExpect(jsonPath("$.porTipo[0].codigo").value("FUGA_AGUA"))
                .andExpect(jsonPath("$.porTipo[0].total").value(2))
                .andExpect(jsonPath("$.porBloque[0].codigo").value("BLOQUE_1"))
                .andExpect(jsonPath("$.porPrioridad[2].codigo").value("BAJA"))
                .andExpect(jsonPath("$.promedioResolucionDias").value(1.5))
                .andExpect(jsonPath("$.reportesResueltosMedidos").value(2));
    }

    @Test
    void pasaLasFechasAlServicio() throws Exception {
        when(servicio.calcular(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).thenReturn(respuesta());

        mockMvc.perform(get("/api/admin/estadisticas").param("desde", "2026-10-01").param("hasta", "2026-10-31"))
                .andExpect(status().isOk());

        verify(servicio).calcular(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    }

    @Test
    void unaFechaConFormatoMaloRespondeBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/estadisticas").param("desde", "ayer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La fecha enviada no es válida."));
    }

    @Test
    void unRangoInvertidoRespondeBadRequestConElMensaje() throws Exception {
        when(servicio.calcular(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1)))
                .thenThrow(new RangoInvalidoException("La fecha inicial no puede ser posterior a la fecha final."));

        mockMvc.perform(get("/api/admin/estadisticas").param("desde", "2026-10-10").param("hasta", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La fecha inicial no puede ser posterior a la fecha final."));
    }
}