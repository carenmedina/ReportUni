package com.fet.reportuni.controller;

import com.fet.reportuni.dto.ErrorResponse;
import com.fet.reportuni.service.ReporteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = SeguimientoController.class)
public class SeguimientoExceptionHandler {

    @ExceptionHandler(ReporteService.ReporteNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(ReporteService.ReporteNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }
}