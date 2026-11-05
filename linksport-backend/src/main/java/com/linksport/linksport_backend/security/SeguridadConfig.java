package com.linksport.linksport_backend.security;

import com.linksport.linksport_backend.repository.UsuarioRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.*;

@Configuration
public class SeguridadConfig {
    public static final String COOKIE = "LINKSPORT_ACCESS";
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecretKey jwtKey(@Value("${linksport.auth.secreto:}") String configurado) {
        byte[] bytes;
        if (configurado.isBlank()) { bytes = new byte[32]; new java.security.SecureRandom().nextBytes(bytes); }
        else {
            try { bytes = Base64.getDecoder().decode(configurado); }
            catch (IllegalArgumentException error) { throw new IllegalArgumentException("El secreto JWT debe estar en Base64."); }
            if (bytes.length < 32) throw new IllegalArgumentException("El secreto JWT debe tener al menos 32 bytes.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
    @Bean JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }
    @Bean JwtDecoder jwtDecoder(SecretKey key) {
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("linksport"));
        return decoder;
    }
    @Bean SecurityFilterChain filtros(HttpSecurity http, UsuarioRepository usuarios, JwtDecoder decoder) throws Exception {
        var bearer = new DefaultBearerTokenResolver();
        var csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(cookie -> cookie.path("/").sameSite("Lax"));
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(c -> c.disable()).formLogin(c -> c.disable()).httpBasic(c -> c.disable()).logout(c -> c.disable())
            .csrf(c -> c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/", "/login", "/error", "/js/**", "/CSS/**", "/imagenes/**", "/favicon.ico", "/mis-pedidos").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/registro", "/api/usuarios/registro", "/api/productos/procesar-pago", "/api/pedidos/consulta").permitAll()
                .requestMatchers("/api/auth/**", "/api/pedidos/mios").authenticated()
                .requestMatchers("/admin/**", "/ver-productos", "/ver-usuarios", "/api/admin/**", "/api/usuarios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/*").permitAll()
                .requestMatchers("/api/productos/**").hasRole("ADMIN")
                .anyRequest().denyAll())
            .exceptionHandling(c -> c.authenticationEntryPoint(this::noAutenticado).accessDeniedHandler((req, res, ex) -> error(res, 403, "No tienes permiso para realizar esta operación o falta la verificación de seguridad.")));
        var provider = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(jwt -> {
            try {
                var usuario = usuarios.findById(Long.valueOf(jwt.getSubject())).orElseThrow();
                Number version = jwt.getClaim("version");
                if (!usuario.isActivo() || version == null || usuario.getTokenVersion() != version.longValue()) throw new IllegalArgumentException();
                return UsernamePasswordAuthenticationToken.authenticated(UsuarioActual.de(usuario), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
            } catch (RuntimeException ex) { throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"), "La sesión dejó de ser válida."); }
        });
        // Filtro explícito para conservar CSRF también cuando el JWT llega en cookie.
        var filter = new org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter(new org.springframework.security.authentication.ProviderManager(provider));
        filter.setBearerTokenResolver(req -> {
            String header = bearer.resolve(req);
            if (header != null) return header;
            if (Set.of("/login", "/api/auth/login", "/api/auth/registro", "/api/auth/csrf", "/api/usuarios/registro").contains(req.getRequestURI())) return null;
            if (req.getCookies() != null) for (Cookie cookie : req.getCookies()) if (COOKIE.equals(cookie.getName())) return cookie.getValue();
            return null;
        });
        filter.setAuthenticationEntryPoint(this::noAutenticado);
        http.addFilterBefore(filter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    private void noAutenticado(HttpServletRequest req, HttpServletResponse res, org.springframework.security.core.AuthenticationException ex) throws IOException {
        if (!req.getRequestURI().startsWith("/api/")) { res.sendRedirect("/login"); return; }
        error(res, 401, "Inicia sesión para continuar.");
    }
    private void error(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status); res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}