package com.linksport.linksport_backend.service;

import com.linksport.linksport_backend.model.Usuario;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {
    // SEGUNDO ARRAYLIST REQUERIDO POR LA RÚBRICA
    private final List<Usuario> listaUsuarios = new ArrayList<>();
    private Long nextId = 1L;

    public UsuarioService() {
        // Usuario administrador quemado por defecto para pruebas
        listaUsuarios.add(new Usuario(nextId++, "Admin Linksport", "admin@linksport.com", "123456"));
    }

    public List<Usuario> obtenerTodos() {
        return listaUsuarios;
    }

    public Usuario registrar(Usuario usuario) {
        usuario.setId(nextId++);
        listaUsuarios.add(usuario);
        return usuario;
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        return listaUsuarios.stream()
                .filter(u -> u.getCorreo().equalsIgnoreCase(correo))
                .findFirst();
    }
}
