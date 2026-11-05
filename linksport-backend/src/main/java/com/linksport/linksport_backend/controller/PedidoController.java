package com.linksport.linksport_backend.controller;

import com.linksport.linksport_backend.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class PedidoController {
    private final PedidoService pedidos;
    public PedidoController(PedidoService pedidos) { this.pedidos = pedidos; }

    @PostMapping("/api/productos/procesar-pago")
    public PedidoService.Resultado confirmar(@Valid @RequestBody SolicitudPedido solicitud) {
        return pedidos.crear(solicitud);
    }
}
