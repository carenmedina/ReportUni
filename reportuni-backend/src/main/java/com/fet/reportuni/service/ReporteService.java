package com.fet.reportuni.service;

import com.fet.reportuni.dto.CrearReporteRequest;
import com.fet.reportuni.dto.ReporteCreadoResponse;
import com.fet.reportuni.dto.ReporteDetalleResponse;
import com.fet.reportuni.dto.ReporteResumenResponse;
import com.fet.reportuni.model.PrioridadReporte;
import com.fet.reportuni.model.Reporte;
import com.fet.reportuni.model.ReporteFoto;
import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.repository.ReporteRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final AlmacenamientoFotosService almacenamientoFotos;
    private final ConsultaFotosService consultaFotos;

    public ReporteService(ReporteRepository reporteRepository,
                          AlmacenamientoFotosService almacenamientoFotos,
                          ConsultaFotosService consultaFotos) {
        this.reporteRepository = reporteRepository;
        this.almacenamientoFotos = almacenamientoFotos;
        this.consultaFotos = consultaFotos;
    }

    public static class ReporteInvalidoException extends RuntimeException {
        public ReporteInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class ReporteNoEncontradoException extends RuntimeException {
        public ReporteNoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    public record FotoDescargable(Resource recurso, String tipoContenido) {
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
                .notificarPorCorreo(false) // las notificaciones por correo aun no estan implementadas
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

    @Transactional(readOnly = true)
    public List<ReporteResumenResponse> listarMios(Usuario usuario) {
        return reporteRepository.findConFotosByUsuarioIdOrderByFechaCreacionDesc(usuario.getId())
                .stream()
                .map(this::aResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReporteDetalleResponse detalle(Usuario usuario, Long id) {
        return aDetalle(buscarAccesible(usuario, id));
    }

    @Transactional(readOnly = true)
    public FotoDescargable foto(Usuario usuario, Long reporteId, Long fotoId) {
        Reporte reporte = buscarAccesible(usuario, reporteId);

        ReporteFoto foto = reporte.getFotos().stream()
                .filter(f -> f.getId().equals(fotoId))
                .findFirst()
                .orElseThrow(() -> new ReporteNoEncontradoException("No encontramos esa fotografía."));

        Resource recurso = consultaFotos.cargar(reporte.getId(), foto.getNombreArchivo())
                .orElseThrow(() -> new ReporteNoEncontradoException("La fotografía ya no está disponible."));

        return new FotoDescargable(recurso, foto.getTipoContenido());
    }

    private Reporte buscarAccesible(Usuario usuario, Long id) {
        Optional<Reporte> reporte = usuario.getRol() == Rol.ADMINISTRADOR
                ? reporteRepository.findConFotosById(id)
                : reporteRepository.findConFotosByIdAndUsuarioId(id, usuario.getId());

        return reporte.orElseThrow(() -> new ReporteNoEncontradoException("No encontramos ese reporte."));
    }

    private ReporteResumenResponse aResumen(Reporte r) {
        return new ReporteResumenResponse(
                r.getId(),
                codigo(r),
                r.getTipoDano(),
                r.getBloque(),
                r.getEspacioEspecifico(),
                r.getPrioridadEstimada(),
                r.getEstado(),
                r.getFechaCreacion(),
                r.getFotos().size()
        );
    }

    private ReporteDetalleResponse aDetalle(Reporte r) {
        List<ReporteDetalleResponse.FotoInfo> fotos = r.getFotos().stream()
                .map(f -> new ReporteDetalleResponse.FotoInfo(
                        f.getId(), f.getNombreOriginal(), f.getTipoContenido(), f.getTamanoBytes(), f.getOrden()))
                .toList();

        return new ReporteDetalleResponse(
                r.getId(),
                codigo(r),
                r.getTipoDano(),
                r.getBloque(),
                r.getEspacioEspecifico(),
                r.getPrioridadEstimada(),
                r.getEstado(),
                r.getDescripcion(),
                r.getLatitud(),
                r.getLongitud(),
                r.isNotificarPorCorreo(),
                r.getUsuario().getNombreCompleto(),
                r.getFechaCreacion(),
                r.getAsignadoA(),
                r.getFechaActualizacion() != null ? r.getFechaActualizacion() : r.getFechaCreacion(),
                fotos
        );
    }

    private String codigo(Reporte r) {
        return String.format("%04d", r.getId());
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