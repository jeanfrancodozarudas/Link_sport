package com.linksport.linksport_backend.model;

public enum EstadoEntrega {
    REGISTRADO, PREPARANDO, EN_CAMINO, ENTREGADO, CANCELADO;

    public boolean permite(EstadoEntrega siguiente) {
        return switch (this) {
            case REGISTRADO -> siguiente == PREPARANDO || siguiente == CANCELADO;
            case PREPARANDO -> siguiente == EN_CAMINO || siguiente == CANCELADO;
            case EN_CAMINO -> siguiente == ENTREGADO || siguiente == CANCELADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }
}