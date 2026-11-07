package com.fet.reportuni.controller;

import com.fet.reportuni.dto.EstadisticasResponse;
import com.fet.reportuni.service.EstadisticasService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/estadisticas")
public class AdminEstadisticasController {

    private final EstadisticasService estadisticasService;

    public AdminEstadisticasController(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }

    @GetMapping
    public EstadisticasResponse obtener(
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return estadisticasService.calcular(desde, hasta);
    }
}