package com.linksport.linksport_backend.controller;
import com.linksport.linksport_backend.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class CuentaController {
    private final UsuarioService usuarios;
    public CuentaController(UsuarioService usuarios) { this.usuarios=usuarios; }
    @GetMapping("/login") public String login() { return "login"; }
    @GetMapping("/ver-usuarios") public String usuarios(Model model) { model.addAttribute("usuarios",usuarios.obtenerTodos()); return "admin-usuarios"; }
}