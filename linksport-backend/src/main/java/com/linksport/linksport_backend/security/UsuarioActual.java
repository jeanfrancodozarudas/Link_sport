package com.linksport.linksport_backend.security;
import com.linksport.linksport_backend.model.*;
public record UsuarioActual(Long id, String nombre, String correo, Rol rol) {
    public static UsuarioActual de(Usuario u) { return new UsuarioActual(u.getId(), u.getNombre(), u.getCorreo(), u.getRol()); }
}