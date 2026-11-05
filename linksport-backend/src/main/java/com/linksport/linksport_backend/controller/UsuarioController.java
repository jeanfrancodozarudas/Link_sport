package com.linksport.linksport_backend.controller;
import com.linksport.linksport_backend.service.UsuarioService;
import com.linksport.linksport_backend.security.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarios;
    public UsuarioController(UsuarioService usuarios) { this.usuarios=usuarios; }
    @GetMapping public List<UsuarioActual> listar() { return usuarios.obtenerTodos(); }
    // Compatibilidad con la ruta anterior, sin aceptar rol ni devolver credenciales.
    @PostMapping("/registro") public UsuarioActual registrar(@Valid @RequestBody AuthDatos.Registro datos) { return UsuarioActual.de(usuarios.registrar(datos)); }
}