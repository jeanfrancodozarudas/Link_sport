package com.linksport.linksport_backend.repository;
import com.linksport.linksport_backend.model.Usuario;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCase(String correo);
    @Modifying(clearAutomatically = true)
    @Query("update Usuario u set u.tokenVersion = u.tokenVersion + 1 where u.id = :id")
    void revocarTokens(@Param("id") Long id);
}