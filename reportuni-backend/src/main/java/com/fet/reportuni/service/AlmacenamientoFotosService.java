package com.fet.reportuni.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AlmacenamientoFotosService {

    public static final int MAX_FOTOS = 5;
    public static final long MAX_BYTES_POR_FOTO = 10L * 1024 * 1024;

    private final Path directorioBase;

    public AlmacenamientoFotosService(@Value("${app.storage.fotos-dir}") String directorio) {
        this.directorioBase = Path.of(directorio).toAbsolutePath().normalize();
    }

    public static class FotoInvalidaException extends RuntimeException {
        public FotoInvalidaException(String mensaje) {
            super(mensaje);
        }
    }

    public static class AlmacenamientoException extends RuntimeException {
        public AlmacenamientoException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }

    public record FotoValidada(MultipartFile archivo, String tipoContenido, String extension) {
    }

    public List<FotoValidada> validar(List<MultipartFile> archivos) {
        List<MultipartFile> recibidos = archivos == null
                ? List.of()
                : archivos.stream().filter(a -> a != null && !a.isEmpty()).toList();

        if (recibidos.isEmpty()) {
            throw new FotoInvalidaException("Adjunta al menos una fotografía del daño.");
        }
        if (recibidos.size() > MAX_FOTOS) {
            throw new FotoInvalidaException("Puedes adjuntar máximo " + MAX_FOTOS + " fotografías.");
        }

        return recibidos.stream().map(this::validarUna).toList();
    }

    private FotoValidada validarUna(MultipartFile archivo) {
        String nombre = nombreSeguro(archivo);

        if (archivo.getSize() > MAX_BYTES_POR_FOTO) {
            throw new FotoInvalidaException("La fotografía \"" + nombre + "\" supera el tamaño máximo de 10 MB.");
        }

        String tipo = detectarTipo(archivo);
        if (tipo == null) {
            throw new FotoInvalidaException("El archivo \"" + nombre + "\" no es una imagen JPG o PNG válida.");
        }

        return new FotoValidada(archivo, tipo, "image/png".equals(tipo) ? "png" : "jpg");
    }

    public String guardar(Long reporteId, FotoValidada foto) {
        Path carpeta = directorioBase.resolve(String.valueOf(reporteId));
        String nombreArchivo = UUID.randomUUID() + "." + foto.extension();

        try {
            Files.createDirectories(carpeta);
            try (InputStream entrada = foto.archivo().getInputStream()) {
                Files.copy(entrada, carpeta.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new AlmacenamientoException("No fue posible guardar las fotografías. Intenta nuevamente.", ex);
        }

        return nombreArchivo;
    }

    public void eliminarCarpeta(Long reporteId) {
        try {
            FileSystemUtils.deleteRecursively(directorioBase.resolve(String.valueOf(reporteId)));
        } catch (IOException ex) {
            log.warn("No se pudo limpiar la carpeta de fotos del reporte {}", reporteId, ex);
        }
    }

    // El tipo se determina por los primeros bytes del archivo (firma), no por el
    // Content-Type ni la extension que declara el cliente, que se pueden falsear.
    private String detectarTipo(MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream()) {
            byte[] c = entrada.readNBytes(8);

            if (c.length >= 3 && (c[0] & 0xFF) == 0xFF && (c[1] & 0xFF) == 0xD8 && (c[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
            if (c.length >= 8 && (c[0] & 0xFF) == 0x89 && c[1] == 'P' && c[2] == 'N' && c[3] == 'G'
                    && c[4] == 0x0D && c[5] == 0x0A && c[6] == 0x1A && c[7] == 0x0A) {
                return "image/png";
            }
            return null;
        } catch (IOException ex) {
            throw new AlmacenamientoException("No fue posible leer la fotografía enviada.", ex);
        }
    }

    private String nombreSeguro(MultipartFile archivo) {
        String original = archivo.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "sin nombre";
        }
        String soloNombre = original.replace('\\', '/');
        soloNombre = soloNombre.substring(soloNombre.lastIndexOf('/') + 1);
        return soloNombre.length() > 80 ? soloNombre.substring(0, 80) : soloNombre;
    }
}