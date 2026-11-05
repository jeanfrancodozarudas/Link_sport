package com.linksport.linksport_backend.repository;

import com.linksport.linksport_backend.model.Venta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    org.springframework.data.domain.Page<Venta> findByEstadoEntrega(com.linksport.linksport_backend.model.EstadoEntrega estadoEntrega, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Venta> findByUsuarioId(Long usuarioId, org.springframework.data.domain.Pageable pageable);
    Optional<Venta> findByClavePedido(String clavePedido);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venta v where v.id = :id")
    Optional<Venta> findByIdForUpdate(@Param("id") Long id);
}