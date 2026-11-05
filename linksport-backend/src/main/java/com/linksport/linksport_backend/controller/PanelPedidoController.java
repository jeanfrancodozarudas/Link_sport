package com.linksport.linksport_backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PanelPedidoController {
    @GetMapping("/admin/pedidos")
    public String panel() { return "admin-pedidos"; }
    @GetMapping("/mis-pedidos")
    public String historial() { return "mis-pedidos"; }
}