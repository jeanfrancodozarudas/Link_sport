package com.linksport.linksport_backend.controller;
import jakarta.validation.constraints.*;
public final class AuthDatos {
    private AuthDatos() {}
    public record Registro(@NotBlank @Size(min=3,max=100) String nombre,
        @NotBlank @Email @Size(max=150) String correo, @NotBlank @Size(min=8,max=72) String password) { @Override public String toString() { return "Credenciales[protegidas]"; } }
    public record Login(@NotBlank @Email @Size(max=150) String correo, @NotBlank @Size(max=72) String password) { @Override public String toString() { return "Credenciales[protegidas]"; } }
}