package com.fet.reportuni.service;

import com.fet.reportuni.service.AlmacenamientoFotosService.FotoInvalidaException;
import com.fet.reportuni.service.AlmacenamientoFotosService.FotoValidada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlmacenamientoFotosServiceTest {

    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

    @TempDir
    Path carpetaTemporal;

    private AlmacenamientoFotosService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AlmacenamientoFotosService(carpetaTemporal.toString());
    }

    private MockMultipartFile foto(String nombre, byte[] contenido) {
        return new MockMultipartFile("fotos", nombre, "image/jpeg", contenido);
    }

    @Test
    void rechazaCuandoNoHayFotos() {
        assertThatThrownBy(() -> servicio.validar(List.of()))
                .isInstanceOf(FotoInvalidaException.class)
                .hasMessageContaining("al menos una");
        assertThatThrownBy(() -> servicio.validar(null))
                .isInstanceOf(FotoInvalidaException.class);
    }

    @Test
    void rechazaMasDeCincoFotos() {
        List<MockMultipartFile> seis = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            seis.add(foto("foto" + i + ".jpg", JPG));
        }
        assertThatThrownBy(() -> servicio.validar(new ArrayList<>(seis)))
                .isInstanceOf(FotoInvalidaException.class)
                .hasMessageContaining("máximo 5");
    }

    @Test
    void rechazaArchivoQueNoEsImagenAunqueDigaSerJpg() {
        assertThatThrownBy(() -> servicio.validar(List.of(foto("virus.jpg", "hola mundo".getBytes()))))
                .isInstanceOf(FotoInvalidaException.class)
                .hasMessageContaining("no es una imagen JPG o PNG");
    }

    @Test
    void rechazaFotoMayorA10MB() {
        byte[] grande = new byte[(int) AlmacenamientoFotosService.MAX_BYTES_POR_FOTO + 1];
        System.arraycopy(JPG, 0, grande, 0, JPG.length);

        assertThatThrownBy(() -> servicio.validar(List.of(foto("grande.jpg", grande))))
                .isInstanceOf(FotoInvalidaException.class)
                .hasMessageContaining("supera el tamaño máximo");
    }

    @Test
    void aceptaJpgYPngValidos() {
        List<FotoValidada> validas = servicio.validar(List.of(foto("a.jpg", JPG), foto("b.png", PNG)));

        assertThat(validas).extracting(FotoValidada::extension).containsExactly("jpg", "png");
        assertThat(validas).extracting(FotoValidada::tipoContenido).containsExactly("image/jpeg", "image/png");
    }

    @Test
    void guardaConNombreGeneradoDentroDeLaCarpetaDelReporte() throws IOException {
        FotoValidada validada = servicio.validar(List.of(foto("../../etc/passwd.jpg", JPG))).get(0);

        String nombreArchivo = servicio.guardar(7L, validada);

        assertThat(nombreArchivo).endsWith(".jpg").doesNotContain("..").doesNotContain("/");
        Path guardado = carpetaTemporal.resolve("7").resolve(nombreArchivo);
        assertThat(guardado).exists();
        assertThat(Files.readAllBytes(guardado)).isEqualTo(JPG);
    }

    @Test
    void eliminaLaCarpetaDelReporte() {
        FotoValidada validada = servicio.validar(List.of(foto("a.jpg", JPG))).get(0);
        servicio.guardar(9L, validada);

        servicio.eliminarCarpeta(9L);

        assertThat(carpetaTemporal.resolve("9")).doesNotExist();
        assertThatCode(() -> servicio.eliminarCarpeta(9L)).doesNotThrowAnyException();
    }
}