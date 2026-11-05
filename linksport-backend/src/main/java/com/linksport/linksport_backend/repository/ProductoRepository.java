package com.linksport.linksport_backend.repository;

import com.linksport.linksport_backend.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @Override @EntityGraph(attributePaths = "categoria")
    List<Producto> findAll();

    @Override @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}