package com.linksport.linksport_backend.repository;

import com.linksport.linksport_backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    // JpaRepository hereda automáticamente todos los métodos del CRUD hacia
    // PostgreSQL
}
