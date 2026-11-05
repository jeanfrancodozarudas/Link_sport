// Renderizado compartido: los datos del servidor se insertan como texto.
window.PedidosUI = {
    etiquetas: { REGISTRADO: 'Registrado', PREPARANDO: 'Preparando', EN_CAMINO: 'En camino', ENTREGADO: 'Entregado', CANCELADO: 'Cancelado', PENDIENTE_PAGO: 'Pendiente de pago', PAGADO: 'Pagado' },
    dinero(value) { return new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(Number(value)); },
    fecha(value) { return value ? new Date(value).toLocaleString('es-PE') : '—'; },
    texto(container, value, tag = 'p', className = 'mb-2') {
        const node = document.createElement(tag); node.className = className; node.textContent = value; container.appendChild(node); return node;
    },
    render(container, pedido) {
        container.replaceChildren();
        this.texto(container, `Fecha: ${this.fecha(pedido.fecha)} · Entrega: ${this.etiquetas[pedido.estadoEntrega]} · Pago: ${this.etiquetas[pedido.estadoPago]}`);
        const comprador = pedido.comprador;
        if (comprador) {
            this.texto(container, `${comprador.nombre} · ${comprador.correo} · ${comprador.telefono}`);
            this.texto(container, `Dirección: ${comprador.direccion}, ${comprador.ciudad}`);
        } else this.texto(container, 'Pedido antiguo sin datos de entrega registrados.');
        this.texto(container, 'Productos', 'h3', 'title is-6 mt-4');
        pedido.productos.forEach(p => this.texto(container, `${p.nombre} × ${p.cantidad} · ${this.dinero(p.precioUnitario)} c/u · Subtotal ${this.dinero(p.subtotal)}`));
        this.texto(container, `Total: ${this.dinero(pedido.total)} · Pago contra entrega`, 'p', 'has-text-weight-bold my-4');
        this.texto(container, 'Historial de cambios', 'h3', 'title is-6');
        if (!pedido.historial.length) this.texto(container, 'Sin cambios registrados (pedido anterior a este módulo).');
        pedido.historial.forEach(e => this.texto(container, `${this.fecha(e.fecha)} · ${this.etiquetas[e.estadoEntrega]} · ${this.etiquetas[e.estadoPago]} · ${e.observacion} · ${e.actor || 'Registro anterior'}`));
    },
    async request(url, options) {
        const response = await (window.Seguridad?.fetch || fetch)(url, options);
        const result = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(result.message || 'No se pudo completar la operación. Recarga y vuelve a intentarlo.');
        return result;
    }
};