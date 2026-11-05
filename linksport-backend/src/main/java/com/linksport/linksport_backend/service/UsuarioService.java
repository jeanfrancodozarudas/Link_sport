package com.linksport.linksport_backend.service;
import com.linksport.linksport_backend.controller.AuthDatos;
import com.linksport.linksport_backend.model.*;
import com.linksport.linksport_backend.repository.UsuarioRepository;
import com.linksport.linksport_backend.security.UsuarioActual;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;
    private final String passwordFicticio;
    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder passwords) {
        this.usuarios=usuarios; this.passwords=passwords; passwordFicticio=passwords.encode(UUID.randomUUID().toString());
    }
    @Transactional
    public Usuario registrar(AuthDatos.Registro datos) {
        String correo = normalizar(datos.correo());
        if (usuarios.existsByCorreoIgnoreCase(correo)) throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo registrar ese correo. Prueba iniciar sesión o usa otro correo.");
        if (datos.nombre().trim().length() < 3 || datos.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisa el nombre y la contraseña (máximo 72 bytes).");
        var u = new Usuario(null, datos.nombre().trim(), correo, passwords.encode(datos.password()));
        u.setRol(Rol.CLIENTE); return usuarios.saveAndFlush(u);
    }
    @Transactional(readOnly=true)
    public Usuario autenticar(AuthDatos.Login datos) {
        if (datos.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Correo o contraseña incorrectos.");
        var encontrado=usuarios.findByCorreoIgnoreCase(normalizar(datos.correo()));
        boolean coincide=passwords.matches(datos.password(), encontrado.map(Usuario::getPassword).orElse(passwordFicticio));
        if (encontrado.isEmpty() || !coincide || !encontrado.get().isActivo())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        return encontrado.get();
    }
    @Transactional public void cerrarSesion(Long id) { usuarios.revocarTokens(id); }
    @Transactional(readOnly=true) public List<UsuarioActual> obtenerTodos() { return usuarios.findAll().stream().map(UsuarioActual::de).toList(); }
    private String normalizar(String correo) { return correo.trim().toLowerCase(Locale.ROOT); }
}