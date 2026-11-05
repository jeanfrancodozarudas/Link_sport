package com.linksport.linksport_backend.controller;

import com.linksport.linksport_backend.model.EstadoEntrega;
import jakarta.validation.constraints.*;

public record CambioPedido(@NotNull EstadoEntrega estadoActual, @NotNull EstadoEntrega estadoEntrega,
        boolean cobroConfirmado, @NotBlank @Size(max = 300) String observacion) {}