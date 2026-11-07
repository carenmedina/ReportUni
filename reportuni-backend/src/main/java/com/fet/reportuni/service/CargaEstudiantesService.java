package com.fet.reportuni.service;

import com.fet.reportuni.dto.CargaEstudiantesResponse;
import com.fet.reportuni.dto.CargaEstudiantesResponse.ErrorFila;
import com.fet.reportuni.model.Rol;
import com.fet.reportuni.model.Usuario;
import com.fet.reportuni.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CargaEstudiantesService {

    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final int MAX_FILAS = 1000;

    public static final String DOMINIO = "@fet.edu.co";

    private static final Pattern CORREO = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern DOCUMENTO = Pattern.compile("^[A-Za-z0-9]{5,20}$");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CargaEstudiantesService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CargaEstudiantesResponse procesar(InputStream contenido) {
        String texto = leer(contenido);
        List<String> lineas = lineasNoVacias(texto);

        if (lineas.isEmpty()) {
            throw new CargaInvalidaException("El archivo está vacío.");
        }

        char separador = lineas.get(0).contains(";") ? ';' : ',';
        int[] columnas = ubicarColumnas(dividir(lineas.get(0), separador));

        List<String> filas = lineas.subList(1, lineas.size());
        if (filas.isEmpty()) {
            throw new CargaInvalidaException("El archivo solo tiene los encabezados. Agrega al menos un estudiante.");
        }
        if (filas.size() > MAX_FILAS) {
            throw new CargaInvalidaException("El archivo tiene " + filas.size()
                    + " estudiantes. El máximo por carga es " + MAX_FILAS + ".");
        }

        List<ErrorFila> errores = new ArrayList<>();
        Set<String> correosVistos = new HashSet<>();
        int creados = 0;
        int actualizados = 0;

        for (int i = 0; i < filas.size(); i++) {
            int numeroFila = i + 2;
            List<String> campos = dividir(filas.get(i), separador);

            String documento = campo(campos, columnas[0]);
            String nombre = campo(campos, columnas[1]);
            String correo = campo(campos, columnas[2]).toLowerCase(Locale.ROOT);

            String motivo = validar(documento, nombre, correo);
            if (motivo == null && !correosVistos.add(correo)) {
                motivo = "El correo está repetido en el archivo.";
            }
            if (motivo != null) {
                errores.add(new ErrorFila(numeroFila, correo, motivo));
                continue;
            }

            Usuario existente = usuarioRepository.findByCorreoInstitucionalIgnoreCase(correo).orElse(null);
            if (existente == null) {
                usuarioRepository.save(Usuario.builder()
                        .nombreCompleto(nombre)
                        .correoInstitucional(correo)
                        .passwordHash(passwordEncoder.encode(documento))
                        .rol(Rol.ESTUDIANTE)
                        .activo(true)
                        .build());
                creados++;
            } else if (existente.getRol() != Rol.ESTUDIANTE) {
                errores.add(new ErrorFila(numeroFila, correo, "Ese correo pertenece a una cuenta de administración."));
            } else {
                existente.setNombreCompleto(nombre);
                usuarioRepository.save(existente);
                actualizados++;
            }
        }

        log.info("Carga masiva de estudiantes: {} filas, {} creados, {} actualizados, {} rechazados",
                filas.size(), creados, actualizados, errores.size());

        return new CargaEstudiantesResponse(filas.size(), creados, actualizados, errores.size(), errores);
    }

    private String validar(String documento, String nombre, String correo) {
        if (documento.isEmpty() || nombre.isEmpty() || correo.isEmpty()) {
            return "Faltan datos: documento, nombre y correo son obligatorios.";
        }
        if (!DOCUMENTO.matcher(documento).matches()) {
            return "El documento debe tener entre 5 y 20 letras o números, sin espacios ni símbolos.";
        }
        if (nombre.length() < 3 || nombre.length() > 150) {
            return "El nombre debe tener entre 3 y 150 caracteres.";
        }
        if (correo.length() > 150 || !CORREO.matcher(correo).matches()) {
            return "El correo no tiene un formato válido.";
        }
        if (!correo.endsWith(DOMINIO)) {
            return "El correo debe ser institucional (terminar en " + DOMINIO + ").";
        }
        return null;
    }

    private String leer(InputStream contenido) {
        byte[] bytes;
        try {
            bytes = contenido.readNBytes(MAX_BYTES + 1);
        } catch (IOException e) {
            throw new CargaInvalidaException("No se pudo leer el archivo.");
        }
        if (bytes.length > MAX_BYTES) {
            throw new CargaInvalidaException("El archivo supera el tamaño máximo de 2 MB.");
        }

        int inicio = 0;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            inicio = 3;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes, inicio, bytes.length - inicio);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(buffer).toString();
        } catch (CharacterCodingException e) {
            // Excel en Windows guarda como ANSI (Windows-1252) cuando no se elige UTF-8.
            return new String(bytes, inicio, bytes.length - inicio, Charset.forName("windows-1252"));
        }
    }

    private List<String> lineasNoVacias(String texto) {
        List<String> lineas = new ArrayList<>();
        for (String linea : texto.split("\\r?\\n|\\r")) {
            if (!linea.isBlank() && !linea.replace(";", "").replace(",", "").isBlank()) {
                lineas.add(linea);
            }
        }
        return lineas;
    }

    private int[] ubicarColumnas(List<String> encabezados) {
        int documento = -1;
        int nombre = -1;
        int correo = -1;
        for (int i = 0; i < encabezados.size(); i++) {
            String clave = normalizar(encabezados.get(i));
            if (clave.equals("documento") || clave.equals("numerodocumento") || clave.equals("cedula")) {
                documento = i;
            } else if (clave.equals("nombre") || clave.equals("nombrecompleto") || clave.equals("estudiante")) {
                nombre = i;
            } else if (clave.equals("correo") || clave.equals("correoinstitucional") || clave.equals("email")) {
                correo = i;
            }
        }
        if (documento < 0 || nombre < 0 || correo < 0) {
            throw new CargaInvalidaException(
                    "La primera fila debe tener los encabezados: documento, nombre y correo.");
        }
        return new int[]{documento, nombre, correo};
    }

    private String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String campo(List<String> campos, int indice) {
        return indice < campos.size() ? campos.get(indice).trim() : "";
    }

    private List<String> dividir(String linea, char separador) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (c == separador && !entreComillas) {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos;
    }

    public static class CargaInvalidaException extends RuntimeException {
        public CargaInvalidaException(String mensaje) {
            super(mensaje);
        }
    }
}