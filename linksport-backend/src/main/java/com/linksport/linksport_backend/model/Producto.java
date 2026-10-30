package com.linksport.linksport_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del artículo deportivo es obligatorio.")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres.")
    @Column(nullable = false, unique = true)
    private String nombre;

    @Positive(message = "El precio debe ser un monto mayor a cero.")
    @DecimalMin(value = "1.00", message = "El precio mínimo permitido es de S/. 1.00")
    @Column(nullable = false)
    private double precio;

    @NotBlank(message = "La dirección URL de la imagen es obligatoria.")
    @Column(nullable = false)
    private String imagenSrc;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false)
    private boolean destacado = false; // Por defecto ningún producto es destacado

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = true)
    private Categoria categoria;

    // Constructor vacío obligatorio para JPA e Hibernate
    public Producto() {
    }

    public Producto(Long id, String nombre, double precio, String imagenSrc) {
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
        this.imagenSrc = imagenSrc;
        this.cantidad = 1;
    }

    // Getters y Setters Estables
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getImagenSrc() {
        return imagenSrc;
    }

    public void setImagenSrc(String imagenSrc) {
        this.imagenSrc = imagenSrc;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public boolean isDestacado() {
        return destacado;
    }

    public void setDestacado(boolean destacado) {
        this.destacado = destacado;
    }

}
