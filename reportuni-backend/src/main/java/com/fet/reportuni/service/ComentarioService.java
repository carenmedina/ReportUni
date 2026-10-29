package com.fet.reportuni.service;

import com.fet.reportuni.dto.ComentarioResponse;
import com.fet.reportuni.dto.NotificacionResponse;
import com.fet.reportuni.dto.TotalNoLeidasResponse;
import com.fet.reportuni.model.Comentario;
import com.fet.reportuni.model.Notificacion;
import com.fet.reportuni.model.Reporte;
import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.repository.ComentarioRepository;
import com.fet.reportuni.repository.NotificacionRepository;
import com.fet.reportuni.repository.ReporteRepository;
import com.fet.reportuni.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ComentarioService {

    public static final int MAX_CARACTERES = 1000;
    private static final int MAX_FRAGMENTO = 100;
    private static final int MAX_MENSAJE = 255;

    private final ComentarioRepository comentarioRepository;
    private final ReporteRepository reporteRepository;
    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public ComentarioService(ComentarioRepository comentarioRepository,
                             ReporteRepository reporteRepository,
                             NotificacionRepository notificacionRepository,
                             UsuarioRepository usuarioRepository) {
        this.comentarioRepository = comentarioRepository;
        this.reporteRepository = reporteRepository;
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public static class ComentarioInvalidoException extends RuntimeException {
        public ComentarioInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    @Transactional
    public List<ComentarioResponse> listar(Usuario usuario, Long reporteId) {
        obtenerReporte(usuario, reporteId);

        List<Notificacion> pendientes = notificacionRepository
                .findByUsuarioIdAndReporteIdAndComentarioTrueAndLeidaFalse(usuario.getId(), reporteId);
        if (!pendientes.isEmpty()) {
            pendientes.forEach(n -> n.setLeida(true));
            notificacionRepository.saveAll(pendientes);
        }

        return comentarioRepository.findByReporteIdOrderByFechaCreacionAscIdAsc(reporteId).stream()
                .map(c -> aRespuesta(c, usuario))
                .toList();
    }

    @Transactional
    public ComentarioResponse comentar(Usuario usuario, Long reporteId, String texto) {
        Reporte reporte = obtenerReporte(usuario, reporteId);

        String contenido = texto == null ? "" : texto.strip();
        if (contenido.isEmpty()) {
            throw new ComentarioInvalidoException("Escribe un comentario antes de enviarlo.");
        }
        if (contenido.length() > MAX_CARACTERES) {
            throw new ComentarioInvalidoException(
                    "El comentario no puede superar los " + MAX_CARACTERES + " caracteres.");
        }

        Comentario guardado = comentarioRepository.save(Comentario.builder()
                .reporte(reporte)
                .autor(usuario)
                .texto(contenido)
                .build());

        notificar(reporte, usuario, contenido);
        return aRespuesta(guardado, usuario);
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> misAvisos(Usuario usuario) {
        return notificacionRepository.findTop30ByUsuarioIdAndComentarioTrueOrderByFechaCreacionDescIdDesc(usuario.getId())
                .stream()
                .map(n -> new NotificacionResponse(
                        n.getId(),
                        n.getReporteId(),
                        codigo(n.getReporteId()),
                        n.getMensaje(),
                        n.isLeida(),
                        n.getFechaCreacion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TotalNoLeidasResponse totalNoLeidos(Usuario usuario) {
        return new TotalNoLeidasResponse(
                notificacionRepository.countByUsuarioIdAndComentarioTrueAndLeidaFalse(usuario.getId()));
    }

    @Transactional
    public void marcarTodosLeidos(Usuario usuario) {
        List<Notificacion> pendientes =
                notificacionRepository.findByUsuarioIdAndComentarioTrueAndLeidaFalse(usuario.getId());
        pendientes.forEach(n -> n.setLeida(true));
        notificacionRepository.saveAll(pendientes);
    }

    private Reporte obtenerReporte(Usuario usuario, Long reporteId) {
        Reporte reporte = reporteRepository.findById(reporteId)
                .orElseThrow(() -> new ReporteService.ReporteNoEncontradoException("No encontramos ese reporte."));

        boolean esAdmin = usuario.getRol() == Rol.ADMINISTRADOR;
        if (!esAdmin && !reporte.getUsuario().getId().equals(usuario.getId())) {
            // igual que en el detalle: un estudiante no se entera de que el reporte existe
            throw new ReporteService.ReporteNoEncontradoException("No encontramos ese reporte.");
        }
        return reporte;
    }

    private void notificar(Reporte reporte, Usuario autor, String contenido) {
        String fragmento = recortar(contenido.replaceAll("\\s+", " "), MAX_FRAGMENTO);
        String codigo = codigo(reporte.getId());

        if (autor.getRol() == Rol.ADMINISTRADOR) {
            Usuario dueno = reporte.getUsuario();
            if (!dueno.getId().equals(autor.getId())) {
                guardarAviso(dueno, reporte.getId(), autor.getNombreCompleto()
                        + " comentó en tu reporte #" + codigo + ": «" + fragmento + "»");
            }
            return;
        }

        for (Usuario admin : usuarioRepository.findByRolAndActivoTrue(Rol.ADMINISTRADOR)) {
            if (!admin.getId().equals(autor.getId())) {
                guardarAviso(admin, reporte.getId(), autor.getNombreCompleto()
                        + " comentó en el reporte #" + codigo + ": «" + fragmento + "»");
            }
        }
    }

    private void guardarAviso(Usuario destinatario, Long reporteId, String mensaje) {
        notificacionRepository.save(Notificacion.builder()
                .usuario(destinatario)
                .reporteId(reporteId)
                .mensaje(recortar(mensaje, MAX_MENSAJE))
                .comentario(true)
                .build());
    }

    private ComentarioResponse aRespuesta(Comentario c, Usuario quienPide) {
        return new ComentarioResponse(
                c.getId(),
                c.getReporte().getId(),
                c.getTexto(),
                c.getAutor().getNombreCompleto(),
                c.getAutor().getRol(),
                c.getAutor().getId().equals(quienPide.getId()),
                c.getFechaCreacion());
    }

    private String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }

    private String codigo(Long reporteId) {
        return String.format("%04d", reporteId);
    }
}