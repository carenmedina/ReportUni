package com.fet.reportuni.controller;

import com.fet.reportuni.dto.ErrorResponse;
import com.fet.reportuni.service.ComentarioService;
import com.fet.reportuni.service.ReporteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ComentarioController.class)
public class ComentarioExceptionHandler {

    @ExceptionHandler(ReporteService.ReporteNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(ReporteService.ReporteNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ComentarioService.ComentarioInvalidoException.class)
    public ResponseEntity<ErrorResponse> invalido(ComentarioService.ComentarioInvalidoException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Escribe un comentario antes de enviarlo."));
    }
}