package com.fet.reportuni.controller;

import com.fet.reportuni.dto.ErrorResponse;
import com.fet.reportuni.service.CargaEstudiantesService.CargaInvalidaException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice(assignableTypes = AdminEstudianteController.class)
public class AdminEstudianteExceptionHandler {

    @ExceptionHandler(CargaInvalidaException.class)
    public ResponseEntity<ErrorResponse> cargaInvalida(CargaInvalidaException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> sinArchivo(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Selecciona un archivo CSV con los estudiantes."));
    }
}