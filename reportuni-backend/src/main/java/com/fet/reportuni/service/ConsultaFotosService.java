package com.fet.reportuni.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class ConsultaFotosService {

    private final Path directorioBase;

    public ConsultaFotosService(
            @Value("${app.storage.fotos-dir:${user.home}/reportuni-uploads/reportes}") String directorio) {
        this.directorioBase = Path.of(directorio).toAbsolutePath().normalize();
    }

    public Optional<Resource> cargar(Long reporteId, String nombreArchivo) {
        Path ruta = directorioBase
                .resolve(String.valueOf(reporteId))
                .resolve(nombreArchivo)
                .normalize();

        if (!ruta.startsWith(directorioBase) || !Files.isRegularFile(ruta)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(ruta));
    }
}