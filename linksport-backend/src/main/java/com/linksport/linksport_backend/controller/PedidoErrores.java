package com.linksport.linksport_backend.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice(assignableTypes = {PedidoController.class, GestionPedidoController.class, ProductoController.class})
public class PedidoErrores {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> datosInvalidos(MethodArgumentNotValidException error) {
        return ResponseEntity.badRequest().body(Map.of("message", "Revisa los campos obligatorios, cantidades y confirmaciones antes de continuar."));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> formatoInvalido() {
        return ResponseEntity.badRequest().body(Map.of("message", "El formato del pedido no es válido."));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> pedidoInvalido(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason()));
    }
    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public ResponseEntity<?> ocupado() {
        return ResponseEntity.status(409).body(Map.of("message", "El inventario está siendo actualizado. Reintenta en unos segundos."));
    }
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<?> version() {
        return ResponseEntity.status(409).body(Map.of("message", "Los datos cambiaron. Recarga antes de guardar."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflicto() {
        return ResponseEntity.status(409).body(Map.of("message", "Los datos entran en conflicto con otro registro. Recarga los datos y vuelve a intentarlo."));
    }
}