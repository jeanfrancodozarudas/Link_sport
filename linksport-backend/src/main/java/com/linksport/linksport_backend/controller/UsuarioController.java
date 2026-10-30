package com.linksport.linksport_backend.controller;

import com.linksport.linksport_backend.model.Usuario;
import com.linksport.linksport_backend.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Listar todos los usuarios registrados 
    @GetMapping
    public List<Usuario> listarUsuarios() {
        return usuarioService.obtenerTodos();
    }

    // Ruta POST encargada de procesar el Registro de una nueva cuenta
    @PostMapping("/registro")
    public ResponseEntity<Usuario> registrarUsuario(@RequestBody Usuario usuario) {
        if (usuarioService.buscarPorCorreo(usuario.getCorreo()).isPresent()) {
            return ResponseEntity.badRequest().build(); // Evita correos duplicados
        }
        Usuario nuevoUsuario = usuarioService.registrar(usuario);
        return ResponseEntity.ok(nuevoUsuario);
    }
}

