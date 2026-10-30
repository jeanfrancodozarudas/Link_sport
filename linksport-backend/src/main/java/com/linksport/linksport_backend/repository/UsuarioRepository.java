package com.linksport.linksport_backend.repository;

import com.linksport.linksport_backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Permite buscar usuarios por correo directamente en las tablas de PostgreSQL
    Optional<Usuario> findByCorreo(String correo);
}
