package com.fet.reportuni.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultaFotosServiceTest {

    @TempDir
    Path carpeta;

    @Test
    void encuentraUnaFotoGuardada() throws Exception {
        Files.createDirectories(carpeta.resolve("5"));
        Files.write(carpeta.resolve("5").resolve("a.jpg"), new byte[]{1, 2});

        ConsultaFotosService servicio = new ConsultaFotosService(carpeta.toString());

        assertTrue(servicio.cargar(5L, "a.jpg").isPresent());
    }

    @Test
    void noDevuelveNadaSiElArchivoNoExiste() {
        ConsultaFotosService servicio = new ConsultaFotosService(carpeta.toString());

        assertFalse(servicio.cargar(5L, "no-existe.jpg").isPresent());
    }

    @Test
    void rechazaNombresQueIntentenSalirDeLaCarpeta() throws Exception {
        Files.write(carpeta.resolve("secreto.txt"), new byte[]{1});

        ConsultaFotosService servicio = new ConsultaFotosService(carpeta.toString());

        assertFalse(servicio.cargar(5L, "../../secreto.txt").isPresent());
    }
}