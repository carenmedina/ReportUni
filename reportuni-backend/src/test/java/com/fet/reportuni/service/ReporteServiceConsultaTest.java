package com.fet.reportuni.service;

import com.fet.reportuni.dto.ReporteDetalleResponse;
import com.fet.reportuni.dto.ReporteResumenResponse;
import com.fet.reportuni.model.*;
import com.fet.reportuni.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceConsultaTest {

    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private AlmacenamientoFotosService almacenamientoFotos;
    @Mock
    private ConsultaFotosService consultaFotos;

    private ReporteService servicio;
    private Usuario estudiante;
    private Usuario administrador;
    private Reporte reporte;

    @BeforeEach
    void preparar() {
        servicio = new ReporteService(reporteRepository, almacenamientoFotos, consultaFotos);

        estudiante = Usuario.builder().id(1L).nombreCompleto("Estudiante Uno")
                .correoInstitucional("e1@fet.edu.co").passwordHash("x").rol(Rol.ESTUDIANTE).build();
        administrador = Usuario.builder().id(9L).nombreCompleto("Admin")
                .correoInstitucional("admin@fet.edu.co").passwordHash("x").rol(Rol.ADMINISTRADOR).build();

        reporte = Reporte.builder()
                .id(12L)
                .usuario(estudiante)
                .tipoDano(TipoDano.FUGA_AGUA)
                .prioridadEstimada(PrioridadReporte.ALTA)
                .bloque(Bloque.BLOQUE_A)
                .espacioEspecifico("Salon 204")
                .descripcion("Fuga junto a la ventana")
                .notificarPorCorreo(true)
                .fechaCreacion(LocalDateTime.of(2026, 10, 5, 9, 30))
                .build();
        reporte.agregarFoto(ReporteFoto.builder().id(70L).nombreOriginal("dano.jpg")
                .nombreArchivo("abc.jpg").tipoContenido("image/jpeg").tamanoBytes(1234).orden(1).build());
    }

    @Test
    void listaSoloLosReportesDelEstudianteConSuCodigo() {
        when(reporteRepository.findConFotosByUsuarioIdOrderByFechaCreacionDesc(1L)).thenReturn(List.of(reporte));

        List<ReporteResumenResponse> lista = servicio.listarMios(estudiante);

        assertEquals(1, lista.size());
        assertEquals("0012", lista.get(0).codigo());
        assertEquals(1, lista.get(0).totalFotos());
        assertEquals(EstadoReporte.PENDIENTE, lista.get(0).estado());
    }

    @Test
    void elEstudianteVeElDetalleDeSuReporte() {
        when(reporteRepository.findConFotosByIdAndUsuarioId(12L, 1L)).thenReturn(Optional.of(reporte));

        ReporteDetalleResponse detalle = servicio.detalle(estudiante, 12L);

        assertEquals("Fuga junto a la ventana", detalle.descripcion());
        assertEquals("Estudiante Uno", detalle.reportadoPor());
        assertEquals(1, detalle.fotos().size());
        assertEquals(70L, detalle.fotos().get(0).id());
    }

    @Test
    void elEstudianteNoPuedeVerElReporteDeOtro() {
        when(reporteRepository.findConFotosByIdAndUsuarioId(12L, 2L)).thenReturn(Optional.empty());
        Usuario otro = Usuario.builder().id(2L).nombreCompleto("Otro").correoInstitucional("o@fet.edu.co")
                .passwordHash("x").rol(Rol.ESTUDIANTE).build();

        assertThrows(ReporteService.ReporteNoEncontradoException.class, () -> servicio.detalle(otro, 12L));
    }

    @Test
    void elAdministradorPuedeVerCualquierReporte() {
        when(reporteRepository.findConFotosById(12L)).thenReturn(Optional.of(reporte));

        ReporteDetalleResponse detalle = servicio.detalle(administrador, 12L);

        assertEquals("0012", detalle.codigo());
        verify(reporteRepository, never()).findConFotosByIdAndUsuarioId(12L, 9L);
    }

    @Test
    void entregaLaFotoDelReporteConSuTipoDeContenido() {
        when(reporteRepository.findConFotosByIdAndUsuarioId(12L, 1L)).thenReturn(Optional.of(reporte));
        ByteArrayResource contenido = new ByteArrayResource(new byte[]{1, 2, 3});
        when(consultaFotos.cargar(12L, "abc.jpg")).thenReturn(Optional.of(contenido));

        ReporteService.FotoDescargable foto = servicio.foto(estudiante, 12L, 70L);

        assertEquals("image/jpeg", foto.tipoContenido());
        assertEquals(contenido, foto.recurso());
    }

    @Test
    void respondeNoEncontradoSiLaFotoNoPerteneceAlReporte() {
        when(reporteRepository.findConFotosByIdAndUsuarioId(12L, 1L)).thenReturn(Optional.of(reporte));

        assertThrows(ReporteService.ReporteNoEncontradoException.class, () -> servicio.foto(estudiante, 12L, 999L));
    }

    @Test
    void respondeNoEncontradoSiElArchivoYaNoExisteEnDisco() {
        when(reporteRepository.findConFotosByIdAndUsuarioId(12L, 1L)).thenReturn(Optional.of(reporte));
        when(consultaFotos.cargar(12L, "abc.jpg")).thenReturn(Optional.empty());

        assertThrows(ReporteService.ReporteNoEncontradoException.class, () -> servicio.foto(estudiante, 12L, 70L));
    }
}