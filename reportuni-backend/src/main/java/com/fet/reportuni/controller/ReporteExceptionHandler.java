package com.fet.reportuni.controller;

import com.fet.reportuni.dto.ErrorResponse;
import com.fet.reportuni.dto.ErrorValidacionResponse;
import com.fet.reportuni.service.AlmacenamientoFotosService;
import com.fet.reportuni.service.ReporteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(assignableTypes = ReporteController.class)
public class ReporteExceptionHandler {

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorValidacionResponse> datosInvalidos(BindException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String mensaje = error.isBindingFailure()
                    ? "El valor enviado no es válido"
                    : error.getDefaultMessage();
            errores.putIfAbsent(error.getField(), mensaje);
        }

        String mensaje = errores.isEmpty()
                ? "Los datos del reporte no son válidos."
                : errores.values().iterator().next();

        return ResponseEntity.badRequest().body(new ErrorValidacionResponse(mensaje, errores));
    }

    @ExceptionHandler({
            ReporteService.ReporteInvalidoException.class,
            AlmacenamientoFotosService.FotoInvalidaException.class
    })
    public ResponseEntity<ErrorResponse> solicitudInvalida(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(AlmacenamientoFotosService.AlmacenamientoException.class)
    public ResponseEntity<ErrorResponse> errorDeAlmacenamiento(AlmacenamientoFotosService.AlmacenamientoException ex) {
        log.error("Error almacenando fotografias de un reporte", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(ex.getMessage()));
    }
}