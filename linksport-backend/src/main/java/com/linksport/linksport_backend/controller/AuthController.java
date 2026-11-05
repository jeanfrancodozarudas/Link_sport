package com.linksport.linksport_backend.controller;
import com.linksport.linksport_backend.security.*;
import com.linksport.linksport_backend.service.UsuarioService;
import com.linksport.linksport_backend.model.Usuario;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioService usuarios; private final TokenService tokens; private final boolean secure;
    public AuthController(UsuarioService usuarios, TokenService tokens, @Value("${linksport.auth.cookie-secure:false}") boolean secure) {
        this.usuarios=usuarios; this.tokens=tokens; this.secure=secure;
    }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("token", token.getToken(), "headerName", token.getHeaderName()); }
    @PostMapping("/registro") public Sesion registro(@Valid @RequestBody AuthDatos.Registro datos, HttpServletRequest req, HttpServletResponse res) {
        res.setStatus(201); return iniciar(usuarios.registrar(datos), req, res);
    }
    @PostMapping("/login") public Sesion login(@Valid @RequestBody AuthDatos.Login datos, HttpServletRequest req, HttpServletResponse res) {
        return iniciar(usuarios.autenticar(datos), req, res);
    }
    private Sesion iniciar(Usuario usuario, HttpServletRequest req, HttpServletResponse res) {
        var jwt=tokens.emitir(usuario);
        res.addHeader(HttpHeaders.SET_COOKIE, cookie(jwt.getTokenValue(), tokens.duracion(), req));
        res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return new Sesion(UsuarioActual.de(usuario), jwt.getExpiresAt());
    }
    @GetMapping("/me") public UsuarioActual actual(@AuthenticationPrincipal UsuarioActual usuario) { return usuario; }
    @PostMapping("/logout") public ResponseEntity<Void> salir(@AuthenticationPrincipal UsuarioActual usuario, HttpServletRequest req, HttpServletResponse res) {
        usuarios.cerrarSesion(usuario.id()); res.addHeader(HttpHeaders.SET_COOKIE, cookie("", 0, req));
        return ResponseEntity.noContent().build();
    }
    private String cookie(String value, long maxAge, HttpServletRequest req) {
        return ResponseCookie.from(SeguridadConfig.COOKIE,value).httpOnly(true).secure(secure || req.isSecure()).sameSite("Lax").path("/").maxAge(maxAge).build().toString();
    }
    public record Sesion(UsuarioActual usuario, Instant venceEn) {}
}