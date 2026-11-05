package com.linksport.linksport_backend.security;
import com.linksport.linksport_backend.model.*;
import com.linksport.linksport_backend.repository.UsuarioRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Component
public class InicializarSeguridad implements ApplicationRunner {
    private final UsuarioRepository usuarios; private final PasswordEncoder encoder; private final String correo,password;
    public InicializarSeguridad(UsuarioRepository usuarios, PasswordEncoder encoder,
        @Value("${linksport.admin.correo:}") String correo, @Value("${linksport.admin.password:}") String password) {
        this.usuarios=usuarios;this.encoder=encoder;this.correo=correo;this.password=password;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        // Las credenciales antiguas de demostración se invalidan, conservando sus ventas.
        for (var usuario:usuarios.findAll()) {
            if (!usuario.getPassword().matches("\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}")) {
                usuario.setPassword(encoder.encode(UUID.randomUUID().toString())); usuario.setActivo(false);
                usuario.setTokenVersion(usuario.getTokenVersion()+1);
            }
        }
        if (correo.isBlank() && password.isBlank()) return;
        if (correo.isBlank() || !correo.contains("@") || password.length()<12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("Configura correo de administrador y contraseña de 12 a 72 bytes.");
        var existente=usuarios.findByCorreoIgnoreCase(correo.trim());
        if (existente.isPresent()) {
            if (existente.get().getRol()!=Rol.ADMIN) throw new IllegalStateException("El correo inicial ya pertenece a un cliente. Usa otro correo de administrador.");
            return;
        }
        var admin=new Usuario(null,"Administrador",correo.trim().toLowerCase(Locale.ROOT),encoder.encode(password));
        admin.setRol(Rol.ADMIN); usuarios.saveAndFlush(admin);
    }
}