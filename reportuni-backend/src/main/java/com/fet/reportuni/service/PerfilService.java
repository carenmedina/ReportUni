package com.fet.reportuni.service;

import com.fet.reportuni.dto.CambiarPasswordRequest;
import com.fet.reportuni.dto.PerfilResponse;
import com.fet.reportuni.dto.ResumenPerfil;
import com.fet.reportuni.model.EstadoReporte;
import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.repository.HistorialReporteRepository;
import com.fet.reportuni.repository.ReporteRepository;
import com.fet.reportuni.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PerfilService {

    public static final long MAX_BYTES_FOTO = 5L * 1024 * 1024;
    public static final int MIN_PASSWORD = 8;
    public static final int MAX_PASSWORD = 64;

    private final UsuarioRepository usuarioRepository;
    private final ReporteRepository reporteRepository;
    private final HistorialReporteRepository historialRepository;
    private final PasswordEncoder passwordEncoder;
    private final Path directorioBase;

    public PerfilService(
            UsuarioRepository usuarioRepository,
            ReporteRepository reporteRepository,
            HistorialReporteRepository historialRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.storage.perfiles-dir:${user.home}/reportuni-uploads/perfiles}") String directorio
    ) {
        this.usuarioRepository = usuarioRepository;
        this.reporteRepository = reporteRepository;
        this.historialRepository = historialRepository;
        this.passwordEncoder = passwordEncoder;
        this.directorioBase = Path.of(directorio).toAbsolutePath().normalize();
    }

    public static class PerfilInvalidoException extends RuntimeException {
        public PerfilInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class AlmacenamientoPerfilException extends RuntimeException {
        public AlmacenamientoPerfilException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }

    public record FotoPerfil(Resource recurso, String tipoContenido) {
    }

    public PerfilResponse obtener(Usuario usuario) {
        return construir(usuario);
    }

    public PerfilResponse subirFoto(Usuario usuario, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new PerfilInvalidoException("Selecciona una imagen JPG o PNG.");
        }
        if (archivo.getSize() > MAX_BYTES_FOTO) {
            throw new PerfilInvalidoException("La foto supera el tamaño máximo de 5 MB.");
        }

        String extension = detectarExtension(archivo);
        if (extension == null) {
            throw new PerfilInvalidoException("El archivo no es una imagen JPG o PNG válida.");
        }

        String nombre = usuario.getId() + "-" + UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(directorioBase);
            try (InputStream entrada = archivo.getInputStream()) {
                Files.copy(entrada, directorioBase.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new AlmacenamientoPerfilException("No fue posible guardar la foto. Intenta nuevamente.", ex);
        }

        String anterior = usuario.getFotoPerfil();
        usuario.setFotoPerfil(nombre);
        usuarioRepository.save(usuario);
        borrarArchivo(anterior);

        return construir(usuario);
    }

    public PerfilResponse eliminarFoto(Usuario usuario) {
        String anterior = usuario.getFotoPerfil();
        if (anterior != null) {
            usuario.setFotoPerfil(null);
            usuarioRepository.save(usuario);
            borrarArchivo(anterior);
        }
        return construir(usuario);
    }

    public Optional<FotoPerfil> foto(Usuario usuario) {
        String nombre = usuario.getFotoPerfil();
        if (nombre == null) {
            return Optional.empty();
        }
        Path ruta = directorioBase.resolve(nombre).normalize();
        if (!ruta.startsWith(directorioBase) || !Files.isRegularFile(ruta)) {
            return Optional.empty();
        }
        String tipo = nombre.endsWith(".png") ? "image/png" : "image/jpeg";
        return Optional.of(new FotoPerfil(new FileSystemResource(ruta), tipo));
    }

    public void cambiarPassword(Usuario usuario, CambiarPasswordRequest solicitud) {
        Usuario actual = usuarioRepository.findById(usuario.getId()).orElse(usuario);

        if (!passwordEncoder.matches(solicitud.actual(), actual.getPasswordHash())) {
            throw new PerfilInvalidoException("La contraseña actual no es correcta.");
        }
        String nueva = solicitud.nueva();
        if (nueva.length() < MIN_PASSWORD || nueva.length() > MAX_PASSWORD) {
            throw new PerfilInvalidoException("La nueva contraseña debe tener entre "
                    + MIN_PASSWORD + " y " + MAX_PASSWORD + " caracteres.");
        }
        if (!nueva.equals(solicitud.confirmacion())) {
            throw new PerfilInvalidoException("La confirmación no coincide con la nueva contraseña.");
        }
        if (nueva.equals(solicitud.actual())) {
            throw new PerfilInvalidoException("La nueva contraseña debe ser diferente a la actual.");
        }

        actual.setPasswordHash(passwordEncoder.encode(nueva));
        usuarioRepository.save(actual);
    }

    private PerfilResponse construir(Usuario usuario) {
        ResumenPerfil resumen;
        if (usuario.getRol() == Rol.ADMINISTRADOR) {
            resumen = new ResumenPerfil(null, null, null, null,
                    historialRepository.contarReportesGestionados(usuario.getId()));
        } else {
            Long id = usuario.getId();
            resumen = new ResumenPerfil(
                    reporteRepository.countByUsuarioId(id),
                    reporteRepository.countByUsuarioIdAndEstado(id, EstadoReporte.PENDIENTE),
                    reporteRepository.countByUsuarioIdAndEstado(id, EstadoReporte.EN_PROCESO),
                    reporteRepository.countByUsuarioIdAndEstado(id, EstadoReporte.RESUELTO),
                    null);
        }
        return new PerfilResponse(
                usuario.getNombreCompleto(),
                usuario.getCorreoInstitucional(),
                usuario.getRol().name(),
                usuario.getFotoPerfil() != null,
                resumen);
    }

    private void borrarArchivo(String nombre) {
        if (nombre == null) {
            return;
        }
        try {
            Path ruta = directorioBase.resolve(nombre).normalize();
            if (ruta.startsWith(directorioBase)) {
                Files.deleteIfExists(ruta);
            }
        } catch (IOException ex) {
            log.warn("No se pudo borrar la foto de perfil {}", nombre, ex);
        }
    }

    private String detectarExtension(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream()) {
            byte[] c = entrada.readNBytes(8);
            if (c.length >= 3 && (c[0] & 0xFF) == 0xFF && (c[1] & 0xFF) == 0xD8 && (c[2] & 0xFF) == 0xFF) {
                return "jpg";
            }
            if (c.length >= 8 && (c[0] & 0xFF) == 0x89 && c[1] == 'P' && c[2] == 'N' && c[3] == 'G'
                    && c[4] == 0x0D && c[5] == 0x0A && c[6] == 0x1A && c[7] == 0x0A) {
                return "png";
            }
            return null;
        } catch (IOException ex) {
            throw new AlmacenamientoPerfilException("No fue posible leer la imagen enviada.", ex);
        }
    }
}