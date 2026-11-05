document.addEventListener('DOMContentLoaded', () => {
    const modal = document.getElementById('modal-checkout');
    const form = document.getElementById('checkout-form');
    const datos = document.getElementById('checkout-datos');
    const revision = document.getElementById('checkout-revision');
    const confirmar = document.getElementById('checkout-confirmacion');
    const continuar = document.getElementById('checkout-continuar');
    const volver = document.getElementById('checkout-volver');
    const cerrar = document.getElementById('checkout-close');
    const error = document.getElementById('checkout-error');
    const money = value => `S/. ${value.toFixed(2)}`;
    let items = [], comprador, total, clave, firma, revisando = false, enviando = false, origen;
    function mostrarDatos() {
        revisando = false;
        datos.hidden = false;
        revision.hidden = true;
        volver.hidden = true;
        confirmar.checked = false;
        continuar.textContent = 'Revisar pedido';
        document.getElementById('checkout-title').textContent = 'Datos de entrega';
    }
    function close() {
        if (enviando) return;
        modal.classList.remove('is-active');
        origen?.focus();
    }
    document.getElementById('btn-proceder-pago').addEventListener('click', e => {
        items = window.linkCheckout.items();
        if (!items.length) { alert('Agrega productos al carrito antes de continuar.'); return; }
        if (items.some(p => !Number.isSafeInteger(p.id) || p.id <= 0 || !Number.isFinite(p.precio))) {
            alert('No se pudo identificar un producto. Actualiza la página.'); return;
        }
        origen = e.currentTarget;

        total = items.reduce((sum, p) => sum + Math.round(p.precio * 100) * p.cantidad, 0) / 100;
        error.hidden = true;
        mostrarDatos();
        window.cerrarPanelCarrito();
        document.getElementById('modal-login-oferta')?.classList.remove('is-active');
        modal.classList.add('is-active');
        document.getElementById('checkout-nombre').focus();
    });
    cerrar.addEventListener('click', close);
    modal.querySelector('.modal-background').addEventListener('click', close);
    volver.addEventListener('click', () => { if (!enviando) mostrarDatos(); });
    modal.addEventListener('keydown', e => {
        if (e.key === 'Escape') close();
        if (e.key === 'Tab') {
            const visibles = [...modal.querySelectorAll('button, input, textarea')].filter(el => !el.disabled && el.getClientRects().length);
            const first = visibles[0], last = visibles[visibles.length - 1];
            if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
            if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
        }
    });
    form.addEventListener('submit', async e => {
        e.preventDefault();
        if (enviando) return;
        error.hidden = true;
        if (!revisando) {
            for (const input of datos.querySelectorAll('input, textarea')) input.value = input.value.trim();
            if (!form.reportValidity()) return;
            comprador = Object.fromEntries(new FormData(form));
            const resumen = document.getElementById('checkout-resumen');
            resumen.replaceChildren();
            items.forEach(p => {
                const line = document.createElement('p');
                line.className = 'mb-2';
                line.textContent = `${p.nombre} × ${p.cantidad} — ${money(Math.round(p.precio * 100) * p.cantidad / 100)}`;
                resumen.appendChild(line);
            });
            document.getElementById('checkout-entrega').textContent = `${comprador.nombre} · ${comprador.correo} · ${comprador.telefono}. Entrega: ${comprador.direccion}, ${comprador.ciudad}.`;
            document.getElementById('checkout-total').textContent = `Total a pagar al recibir: ${money(total)}`;
            datos.hidden = true;
            revision.hidden = false;
            volver.hidden = false;
            revisando = true;
            continuar.textContent = 'Confirmar pedido contra entrega';
            document.getElementById('checkout-title').textContent = 'Revisar y confirmar';
            confirmar.focus();
            return;
        }
        if (!confirmar.checked) {
            error.textContent = 'Confirma tus datos y el importe antes de registrar el pedido.';
            error.hidden = false;
            confirmar.focus();
            return;
        }
        const nuevaFirma = JSON.stringify({ comprador, items, total });
        if (firma !== nuevaFirma) { clave = crypto.randomUUID(); firma = nuevaFirma; }
        enviando = true;
        continuar.disabled = volver.disabled = cerrar.disabled = true;
        continuar.textContent = 'Registrando pedido…';
        try {
            const response = await (window.Seguridad?.fetch || fetch)('/api/productos/procesar-pago', {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ comprador, productos: items.map(p => ({ id: p.id, cantidad: p.cantidad })),
                    metodoPago: 'CONTRA_ENTREGA', confirmado: true, totalConfirmado: total, clavePedido: clave })
            });
            const result = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(result.message || result.detail || 'No se pudo registrar el pedido. Revisa tus datos e inténtalo de nuevo.');
            if (!result.pedidoId || !['PENDIENTE_PAGO', 'PAGADO', 'CANCELADO'].includes(result.estado)) throw new Error('Respuesta inesperada del servidor. Tu carrito se conserva.');
            modal.classList.remove('is-active');
            const success = document.getElementById('modal-compra-exitosa');
            const codigo = result.claveConsulta || clave;
            if (!result.cuentaAsociada) window.PedidosStore?.guardar({ id: result.pedidoId, clave: codigo });
            const pago = result.estado === 'PENDIENTE_PAGO' ? 'Pendiente de pago contra entrega. No se ha realizado ningún cobro.' : `Estado de pago: ${result.estado}.`;
            success.querySelector('.subtitle').textContent = `Pedido #${result.pedidoId}. Total: ${money(Number(result.total))}. ${pago} Código privado de consulta: ${codigo}. Consérvalo para consultar tu pedido.`;
            success.classList.add('is-active');
            window.linkCheckout.clear();
            form.reset(); firma = null; clave = null;
            document.getElementById('modal-compra-btn-aceptar').focus();
        } catch (err) {
            error.textContent = err instanceof TypeError ? 'No se pudo conectar. Tu carrito se conserva; puedes reintentar sin duplicar el pedido.' : err.message;
            error.hidden = false;
        } finally {
            enviando = false;
            continuar.disabled = volver.disabled = cerrar.disabled = false;
            continuar.textContent = 'Confirmar pedido contra entrega';
        }
    });
    ['modal-compra-btn-aceptar', 'modal-compra-x-close', 'modal-compra-bg-close'].forEach(id => {
        document.getElementById(id).addEventListener('click', () => {
            document.getElementById('modal-compra-exitosa').classList.remove('is-active');
            origen?.focus();
        });
    });
});