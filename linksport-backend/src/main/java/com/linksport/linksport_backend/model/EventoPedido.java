package com.linksport.linksport_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Embeddable
public class EventoPedido {
    private LocalDateTime fecha;
    private String estadoEntrega;
    private String estadoPago;
    @Column(length = 300)
    private String observacion;
    private String actor;
    public String getActor() { return actor; }
    public EventoPedido() {}
    public EventoPedido(String entrega, String pago, String observacion) {
        this(entrega,pago,observacion,"SISTEMA");
    }
    public EventoPedido(String entrega, String pago, String observacion, String actor) {
        this.actor=actor;
        fecha = LocalDateTime.now(); estadoEntrega = entrega; estadoPago = pago; this.observacion = observacion;
    }
    public LocalDateTime getFecha() { return fecha; }
    public String getEstadoEntrega() { return estadoEntrega; }
    public String getEstadoPago() { return estadoPago; }
    public String getObservacion() { return observacion; }
}