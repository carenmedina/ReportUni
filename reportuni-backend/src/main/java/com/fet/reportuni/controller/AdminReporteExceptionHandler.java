package com.fet.reportuni.controller;

import com.fet.reportuni.dto.ErrorResponse;
import com.fet.reportuni.service.AdminReporteService;
import com.fet.reportuni.service.AlmacenamientoFotosService;
import com.fet.reportuni.service.ReporteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice(assignableTypes = AdminReporteController.class)
public class AdminReporteExceptionHandler {

    @ExceptionHandler(ReporteService.ReporteNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(ReporteService.ReporteNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(AdminReporteService.CambioInvalidoException.class)
    public ResponseEntity<ErrorResponse> cambioInvalido(AdminReporteService.CambioInvalidoException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(AlmacenamientoFotosService.FotoInvalidaException.class)
    public ResponseEntity<ErrorResponse> fotoInvalida(AlmacenamientoFotosService.FotoInvalidaException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(AlmacenamientoFotosService.AlmacenamientoException.class)
    public ResponseEntity<ErrorResponse> errorDeAlmacenamiento(AlmacenamientoFotosService.AlmacenamientoException ex) {
        log.error("Error almacenando fotografias de gestion", ex);
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

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> valorNoValido(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("El valor enviado no es válido."));
    }
}