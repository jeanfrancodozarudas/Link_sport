package com.linksport.linksport_backend.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;

@RestControllerAdvice(assignableTypes={AuthController.class,UsuarioController.class})
public class AuthErrores {
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> negocio(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason())); }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class}) public ResponseEntity<?> formato() { return ResponseEntity.badRequest().body(Map.of("message","Revisa los datos. El nombre debe tener al menos 3 caracteres y la contraseña de registro entre 8 y 72 caracteres.")); }
    @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<?> duplicado() { return ResponseEntity.status(409).body(Map.of("message","No se pudo registrar ese correo. Prueba iniciar sesión o usa otro correo.")); }
}