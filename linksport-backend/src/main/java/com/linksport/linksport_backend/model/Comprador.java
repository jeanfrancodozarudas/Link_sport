package com.linksport.linksport_backend.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.*;

@Embeddable
public class Comprador {
    @NotBlank @Size(min = 3, max = 100)
    private String nombre;
    @NotBlank @Email @Size(max = 150)
    private String correo;
    @NotBlank @Pattern(regexp = "9[0-9]{8}", message = "Ingresa un celular peruano de 9 dígitos.")
    private String telefono;
    @NotBlank @Size(min = 10, max = 250)
    private String direccion;
    @NotBlank @Size(min = 2, max = 100)
    private String ciudad;
    public String getNombre() { return nombre; }
    public void setNombre(String value) { nombre = value == null ? null : value.trim(); }
    public String getCorreo() { return correo; }
    public void setCorreo(String value) { correo = value == null ? null : value.trim(); }
    public String getTelefono() { return telefono; }
    public void setTelefono(String value) { telefono = value == null ? null : value.trim(); }
    public String getDireccion() { return direccion; }
    public void setDireccion(String value) { direccion = value == null ? null : value.trim(); }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String value) { ciudad = value == null ? null : value.trim(); }
}
