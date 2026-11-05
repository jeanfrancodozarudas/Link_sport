package com.linksport.linksport_backend.controller;

import com.linksport.linksport_backend.model.Comprador;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record SolicitudPedido(
        @NotNull @Valid Comprador comprador,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid Item> productos,
        @NotBlank @Pattern(regexp = "CONTRA_ENTREGA") String metodoPago,
        @AssertTrue(message = "Debes confirmar los datos y el pago contra entrega.") boolean confirmado,
        @NotNull @DecimalMin("0.01") BigDecimal totalConfirmado,
        @NotBlank @Pattern(regexp = "[a-fA-F0-9-]{36}") String clavePedido) {
    public record Item(@NotNull @Positive Long id, @Min(1) @Max(99) int cantidad) {}
}
