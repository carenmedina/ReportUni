package com.fet.reportuni.controller;

import com.fet.reportuni.dto.ErrorResponse;
import com.fet.reportuni.service.PerfilService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice(assignableTypes = PerfilController.class)
public class PerfilExceptionHandler {

    @ExceptionHandler(PerfilService.PerfilInvalidoException.class)
    public ResponseEntity<ErrorResponse> invalido(PerfilService.PerfilInvalidoException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PerfilService.AlmacenamientoPerfilException.class)
    public ResponseEntity<ErrorResponse> almacenamiento(PerfilService.AlmacenamientoPerfilException ex) {
        log.error("Error almacenando la foto de perfil", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> datosInvalidos(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Los datos enviados no son válidos.");
        return ResponseEntity.badRequest().body(new ErrorResponse(mensaje));
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> sinArchivo(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Selecciona una imagen JPG o PNG."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoNoLegible(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Los datos enviados no son válidos."));
    }
}