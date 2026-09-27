package com.fet.reportuni;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ReportUni - extension del modulo de Reportes sobre la plataforma
 * institucional Q10 (Fundacion Escuela Tecnologica de Neiva).
 *
 * Semana 3: autenticacion institucional + navegacion base,
 * trabajando con una base de datos MySQL local.
 */
@SpringBootApplication
public class ReportUniApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportUniApplication.class, args);
    }
}
