package com.fet.reportuni.service;

import com.fet.reportuni.dto.CrearReporteRequest;
import com.fet.reportuni.dto.ReporteCreadoResponse;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.Reporte;
import com.fet.reportuni.model.ReporteFoto;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.repository.ReporteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final AlmacenamientoFotosService almacenamientoFotos;

    public ReporteService(ReporteRepository reporteRepository, AlmacenamientoFotosService almacenamientoFotos) {
        this.reporteRepository = reporteRepository;
        this.almacenamientoFotos = almacenamientoFotos;
    }

    public static class ReporteInvalidoException extends RuntimeException {
        public ReporteInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    @Transactional
    public ReporteCreadoResponse crear(Usuario usuario, CrearReporteRequest request, List<MultipartFile> archivos) {
        if ((request.latitud() == null) != (request.longitud() == null)) {
            throw new ReporteInvalidoException("La ubicación GPS debe incluir latitud y longitud.");
        }

        List<AlmacenamientoFotosService.FotoValidada> fotos = almacenamientoFotos.validar(archivos);

        Reporte reporte = Reporte.builder()
                .usuario(usuario)
                .tipoDano(request.tipoDano())
                .prioridadEstimada(request.prioridadEstimada() != null
                        ? request.prioridadEstimada()
                        : PrioridadReporte.MEDIA)
                .bloque(request.bloque())
                .espacioEspecifico(request.espacioEspecifico().trim())
                .descripcion(request.descripcion().trim())
                .latitud(request.latitud())
                .longitud(request.longitud())
                .notificarPorCorreo(request.notificarPorCorreo() == null || request.notificarPorCorreo())
                .build();

        reporteRepository.save(reporte);

        try {
            int orden = 1;
            for (AlmacenamientoFotosService.FotoValidada foto : fotos) {
                String nombreArchivo = almacenamientoFotos.guardar(reporte.getId(), foto);
                reporte.agregarFoto(ReporteFoto.builder()
                        .nombreOriginal(nombreOriginal(foto.archivo()))
                        .nombreArchivo(nombreArchivo)
                        .tipoContenido(foto.tipoContenido())
                        .tamanoBytes(foto.archivo().getSize())
                        .orden(orden++)
                        .build());
            }
        } catch (RuntimeException ex) {
            almacenamientoFotos.eliminarCarpeta(reporte.getId());
            throw ex;
        }

        return new ReporteCreadoResponse(
                reporte.getId(),
                String.format("%04d", reporte.getId()),
                reporte.getEstado(),
                reporte.getFechaCreacion(),
                reporte.getFotos().size()
        );
    }

    private String nombreOriginal(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || nombre.isBlank()) {
            return "foto";
        }
        String soloNombre = nombre.replace('\\', '/');
        soloNombre = soloNombre.substring(soloNombre.lastIndexOf('/') + 1);
        return soloNombre.length() > 255 ? soloNombre.substring(0, 255) : soloNombre;
    }
}