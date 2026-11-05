package com.linksport.linksport_backend;

import com.linksport.linksport_backend.model.*;
import com.linksport.linksport_backend.repository.*;
import com.linksport.linksport_backend.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SeguridadIntegrationTest {
    @Autowired org.springframework.core.env.Environment env;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired TokenService tokens;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtDecoder decoder;
    @Autowired com.linksport.linksport_backend.service.PedidoService pedidos;
    @Autowired ProductoRepository productos;
    @Autowired CategoriaRepository categorias;
    private URI uri(String path) { return URI.create("http://localhost:"+env.getProperty("local.server.port")+path); }
    private Usuario usuario(Rol rol) {
        var u=new Usuario(null,"Persona Prueba",UUID.randomUUID()+"@example.com",encoder.encode("Clave-segura-123")); u.setRol(rol); return usuarios.saveAndFlush(u);
    }
    private HttpResponse<String> get(HttpClient client,String ruta) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(ruta)).GET().build(),HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> post(HttpClient client,String ruta,String json) throws Exception {
        return client.send(SecurityHttp.request(client,uri(ruta)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private HttpClient cliente(Usuario usuario) { return SecurityHttp.autenticado(uri("/"),tokens.emitir(usuario).getTokenValue()); }
    @Test void visitanteNoAccedeAAdministracionNiDatosDeUsuarios() throws Exception {
        try(var client=SecurityHttp.cliente()) {
            assertEquals(401,get(client,"/api/admin/pedidos").statusCode());
            assertEquals(401,get(client,"/api/usuarios").statusCode());
            var pantalla=get(client,"/ver-productos"); assertEquals(302,pantalla.statusCode()); assertEquals("/login",URI.create(pantalla.headers().firstValue("location").orElseThrow()).getPath());
            assertEquals(200,get(client,"/").statusCode()); assertEquals(200,get(client,"/login").statusCode());
        }
    }
    @Test void registroPersisteHashEmiteJwtYNoPermiteElegirRol() throws Exception {
        String correo=UUID.randomUUID()+"@example.com";
        try(var client=SecurityHttp.cliente()) {
            var response=post(client,"/api/auth/registro","{\"nombre\":\"Cliente Real\",\"correo\":\""+correo+"\",\"password\":\"Clave-segura-123\",\"rol\":\"ADMIN\"}");
            assertEquals(201,response.statusCode(),response.body());
            var u=usuarios.findByCorreo(correo).orElseThrow(); assertEquals(Rol.CLIENTE,u.getRol());
            assertNotEquals("Clave-segura-123",u.getPassword()); assertTrue(encoder.matches("Clave-segura-123",u.getPassword()));
            assertFalse(response.body().contains("password")); assertFalse(response.body().contains("Clave-segura-123"));
            String cookie=response.headers().allValues("set-cookie").stream().filter(s->s.startsWith(SeguridadConfig.COOKIE+"=")).findFirst().orElseThrow();
            assertTrue(cookie.contains("HttpOnly")); assertTrue(cookie.contains("SameSite=Lax"));
            var jwt=decoder.decode(SecurityHttp.token(client)); assertEquals(u.getId().toString(),jwt.getSubject()); assertEquals("CLIENTE",jwt.getClaimAsString("rol"));
            assertEquals(200,get(client,"/api/auth/me").statusCode());
            assertEquals(403,get(client,"/api/admin/pedidos").statusCode());
            assertEquals(403,get(client,"/ver-productos").statusCode());
        }
    }
    @Test void credencialesIncorrectasSeRechazanYCorreoSeNormaliza() throws Exception {
        var u=usuario(Rol.CLIENTE);
        try(var client=SecurityHttp.cliente()) {
            assertEquals(401,post(client,"/api/auth/login","{\"correo\":\""+u.getCorreo()+"\",\"password\":\"incorrecta\"}").statusCode());
            var ok=post(client,"/api/auth/login","{\"correo\":\""+u.getCorreo().toUpperCase(Locale.ROOT)+"\",\"password\":\"Clave-segura-123\"}");
            assertEquals(200,ok.statusCode());
            assertEquals(409,post(client,"/api/auth/registro","{\"nombre\":\"Duplicado\",\"correo\":\""+u.getCorreo().toUpperCase(Locale.ROOT)+"\",\"password\":\"Clave-segura-123\"}").statusCode());
        }
    }
    @Test void administradorAccedeYListadosNoExponenPasswords() throws Exception {
        try(var client=cliente(usuario(Rol.ADMIN))) {
            assertEquals(200,get(client,"/ver-productos").statusCode()); assertEquals(200,get(client,"/admin/pedidos").statusCode());
            var listado=get(client,"/api/usuarios"); assertEquals(200,listado.statusCode()); assertFalse(listado.body().contains("password")); assertFalse(listado.body().contains("$2"));
            var html=get(client,"/ver-usuarios"); assertEquals(200,html.statusCode()); assertFalse(html.body().contains("$2"));
        }
    }
    @Test void csrfEsObligatorioInclusoConJwtEnCookie() throws Exception {
        try(var client=cliente(usuario(Rol.ADMIN))) {
            var request=HttpRequest.newBuilder(uri("/api/auth/logout")).POST(HttpRequest.BodyPublishers.noBody()).build();
            assertEquals(403,client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());
            assertEquals(200,get(client,"/api/auth/me").statusCode());
            assertEquals(204,post(client,"/api/auth/logout","{}").statusCode());
        }
    }
    @Test void logoutRevocaElJwtInclusoSiSeReutilizaComoBearer() throws Exception {
        var u=usuario(Rol.CLIENTE);
        try(var client=cliente(u)) {
            String jwt=SecurityHttp.token(client);
            assertEquals(204,post(client,"/api/auth/logout","{}").statusCode());
            assertEquals(401,get(client,"/api/auth/me").statusCode());
            var replay=HttpRequest.newBuilder(uri("/api/auth/me")).header("Authorization","Bearer "+jwt).GET().build();
            assertEquals(401,client.send(replay,HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }
    @Test void firmaManipuladaYTokenVencidoSeRechazan() throws Exception {
        var u=usuario(Rol.CLIENTE); String token=tokens.emitir(u).getTokenValue();
        String[] partes=token.split("\\."); String alterado=partes[0]+"."+Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"1\"}".getBytes())+"."+partes[2];
        var claims=JwtClaimsSet.builder().issuer("linksport").subject(u.getId().toString()).issuedAt(Instant.now().minusSeconds(7200)).expiresAt(Instant.now().minusSeconds(3600)).claim("version",u.getTokenVersion()).build();
        String vencido=jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        try(var client=SecurityHttp.cliente()) {
            for(String jwt:List.of(alterado,vencido)) {
                var request=HttpRequest.newBuilder(uri("/api/auth/me")).header("Authorization","Bearer "+jwt).GET().build();
                assertEquals(401,client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());
            }
        }
    }
    @Test void clienteNoPuedeCrearNiRetirarProductos() throws Exception {
        try(var client=cliente(usuario(Rol.CLIENTE))) {
            assertEquals(403,post(client,"/api/productos","{}").statusCode());
            var request=SecurityHttp.request(client,uri("/api/productos/1")).DELETE().build();
            assertEquals(403,client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }
    @Test void rolYCuentaActivaSeCompruebanEnCadaPeticion() throws Exception {
        var u=usuario(Rol.ADMIN);
        try(var client=cliente(u)) {
            u.setRol(Rol.CLIENTE); usuarios.saveAndFlush(u); assertEquals(403,get(client,"/api/admin/pedidos").statusCode());
            u.setActivo(false); usuarios.saveAndFlush(u); assertEquals(401,get(client,"/api/auth/me").statusCode());
        }
    }
    @Test void registroInvalidoYLoginSinCsrfNoCreanSesion() throws Exception {
        try(var client=SecurityHttp.cliente()) {
            assertEquals(400,post(client,"/api/auth/registro","{\"nombre\":\"A\",\"correo\":\"mal\",\"password\":\"123\"}").statusCode());
            var req=HttpRequest.newBuilder(uri("/api/auth/login")).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build();
            assertEquals(403,client.send(req,HttpResponse.BodyHandlers.ofString()).statusCode());
        }
    }
    @Test void comprasSeAsocianAlUsuarioYSeAislanEntreCuentas() throws Exception {
        var a=usuario(Rol.CLIENTE); var b=usuario(Rol.CLIENTE);
        var categoria=categorias.saveAndFlush(new Categoria("Categoria-"+UUID.randomUUID()));
        var p=new Producto(null,"Compra-"+UUID.randomUUID(),10.0,"foto.jpg");p.setCategoria(categoria);p.setCantidad(5);p=productos.saveAndFlush(p);
        String body="{\"comprador\":{\"nombre\":\"Ana Pérez\",\"correo\":\"ana@example.com\",\"telefono\":\"987654321\",\"ciudad\":\"Lima\",\"direccion\":\"Avenida Primavera 123\"},\"productos\":[{\"id\":"+p.getId()+",\"cantidad\":1}],\"metodoPago\":\"CONTRA_ENTREGA\",\"confirmado\":true,\"totalConfirmado\":10,\"clavePedido\":\""+UUID.randomUUID()+"\"}";
        try(var ca=cliente(a);var cb=cliente(b)) {
            assertEquals(200,post(ca,"/api/productos/procesar-pago",body).statusCode());
            var propios=get(ca,"/api/pedidos/mios");assertEquals(200,propios.statusCode());assertTrue(propios.body().contains(p.getNombre()));
            var ajenos=get(cb,"/api/pedidos/mios");assertEquals(200,ajenos.statusCode());assertFalse(ajenos.body().contains(p.getNombre()));
            assertTrue(propios.body().contains("CLIENTE #"+a.getId()));
        }
    }
}