package com.linksport.linksport_backend.repository;

import com.linksport.linksport_backend.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    // Hereda los métodos automáticos para gestionar las categorías en PostgreSQL
}
