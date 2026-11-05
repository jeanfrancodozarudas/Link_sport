package com.linksport.linksport_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;

    @Column(name = "total_pagar", nullable = false)
    private double totalPagar;

    // RELACIÓN: Muchas ventas pertenecen a UN mismo Usuario (Cliente)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Embedded
    private Comprador comprador;

    private String estado = "PENDIENTE_PAGO";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(30) default 'REGISTRADO'")
    private EstadoEntrega estadoEntrega = EstadoEntrega.REGISTRADO;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean stockDescontado;
    @ElementCollection
    @CollectionTable(name = "eventos_pedido", joinColumns = @JoinColumn(name = "venta_id"))
    @OrderColumn(name = "orden")
    private List<EventoPedido> historial = new ArrayList<>();

    public EstadoEntrega getEstadoEntrega() { return estadoEntrega; }
    public void setEstadoEntrega(EstadoEntrega value) { estadoEntrega = value; }
    public void setEstado(String value) { estado = value; }
    public boolean isStockDescontado() { return stockDescontado; }
    public void setStockDescontado(boolean value) { stockDescontado = value; }
    public List<EventoPedido> getHistorial() { return historial; }
    private String metodoPago = "CONTRA_ENTREGA";
    @Column(unique = true)
    private String clavePedido;

    public Comprador getComprador() { return comprador; }
    public void setComprador(Comprador value) { comprador = value; }
    public String getEstado() { return estado; }
    public String getMetodoPago() { return metodoPago; }
    public String getClavePedido() { return clavePedido; }
    public void setClavePedido(String value) { clavePedido = value; }

    // RELACIÓN: Una venta contiene muchos ítems específicos de detalle
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    public Venta() {
        this.fechaCompra = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(LocalDateTime fechaCompra) {
        this.fechaCompra = fechaCompra;
    }

    public double getTotalPagar() {
        return totalPagar;
    }

    public void setTotalPagar(double totalPagar) {
        this.totalPagar = totalPagar;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }
}
