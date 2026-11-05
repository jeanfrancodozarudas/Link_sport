package com.linksport.linksport_backend;
import com.linksport.linksport_backend.security.SeguridadConfig;
import java.net.*;
import java.net.http.*;
import java.util.regex.Pattern;

public final class SecurityHttp {
    private SecurityHttp() {}
    public static HttpClient cliente() { return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build(); }
    public static HttpClient autenticado(URI base, String token) {
        var client=cliente(); var cookie=new HttpCookie(SeguridadConfig.COOKIE,token); cookie.setPath("/"); cookie.setVersion(0);
        ((CookieManager)client.cookieHandler().orElseThrow()).getCookieStore().add(base,cookie); return client;
    }
    public static HttpRequest.Builder request(HttpClient client, URI uri) throws Exception {
        var csrf=client.send(HttpRequest.newBuilder(uri.resolve("/api/auth/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString());
        var match=Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(csrf.body());
        if (!match.find()) throw new IllegalStateException("No se pudo obtener CSRF: "+csrf.statusCode());
        return HttpRequest.newBuilder(uri).header("X-XSRF-TOKEN",match.group(1));
    }
    public static String token(HttpClient client) {
        return ((CookieManager)client.cookieHandler().orElseThrow()).getCookieStore().getCookies().stream()
            .filter(c->c.getName().equals(SeguridadConfig.COOKIE)).findFirst().orElseThrow().getValue();
    }
}