package com.fet.reportuni.controller;

import com.fet.reportuni.dto.CargaEstudiantesResponse;
import com.fet.reportuni.service.CargaEstudiantesService;
import com.fet.reportuni.service.CargaEstudiantesService.CargaInvalidaException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/estudiantes")
public class AdminEstudianteController {

    private final CargaEstudiantesService cargaService;

    public AdminEstudianteController(CargaEstudiantesService cargaService) {
        this.cargaService = cargaService;
    }

    @PostMapping(value = "/carga", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CargaEstudiantesResponse cargar(@RequestParam("archivo") MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new CargaInvalidaException("Selecciona un archivo CSV con los estudiantes.");
        }
        try {
            return cargaService.procesar(archivo.getInputStream());
        } catch (IOException e) {
            throw new CargaInvalidaException("No se pudo leer el archivo.");
        }
    }
}