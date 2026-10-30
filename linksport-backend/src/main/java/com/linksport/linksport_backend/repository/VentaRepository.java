package com.linksport.linksport_backend.repository;

import com.linksport.linksport_backend.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    // JpaRepository hereda automáticamente los métodos lógicos para insertar
    // facturas en PostgreSQL
}
