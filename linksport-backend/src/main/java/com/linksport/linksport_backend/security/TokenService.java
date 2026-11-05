package com.linksport.linksport_backend.security;

import com.linksport.linksport_backend.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final long segundos;
    public TokenService(JwtEncoder encoder, @Value("${linksport.auth.duracion-segundos:3600}") long segundos) {
        if (segundos < 60 || segundos > 86400) throw new IllegalArgumentException("La duración del JWT debe estar entre 60 y 86400 segundos.");
        this.encoder = encoder; this.segundos = segundos;
    }
    public Jwt emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("linksport").subject(usuario.getId().toString())
            .issuedAt(ahora).expiresAt(ahora.plusSeconds(segundos)).id(java.util.UUID.randomUUID().toString())
            .claim("rol", usuario.getRol().name()).claim("version", usuario.getTokenVersion()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
    }
    public long duracion() { return segundos; }
}