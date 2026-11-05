document.addEventListener('DOMContentLoaded', () => {
    const ui = window.PedidosUI, get = id => document.getElementById(id);
    let pagina = 0, totalPaginas = 0, seleccionado = null, cargaDetalle = 0, cargando = false, guardando = false;
    const error = err => { get('pedidos-error').textContent = err.message; get('pedidos-error').hidden = false; };
    function botones() {
        get('pagina-anterior').disabled = cargando || guardando || pagina === 0;
        get('pagina-siguiente').disabled = cargando || guardando || pagina + 1 >= totalPaginas;
        get('pedidos-refrescar').disabled = get('filtro-entrega').disabled = cargando || guardando;
        document.querySelectorAll('[data-pedido]').forEach(b => b.disabled = guardando);
    }
    async function cargar() {
        cargando = true; botones(); get('pedidos-error').hidden = true; get('pedidos-status').textContent = 'Cargando pedidos…';
        try {
            const filtro = get('filtro-entrega').value;
            const data = await ui.request(`/api/admin/pedidos?pagina=${pagina}${filtro ? `&estado=${encodeURIComponent(filtro)}` : ''}`);
            pagina = data.pagina; totalPaginas = data.totalPaginas;
            const lista = get('pedidos-lista'); lista.replaceChildren();
            data.pedidos.forEach(p => {
                const row = document.createElement('tr');
                [`#${p.id}`, ui.fecha(p.fecha), p.comprador?.nombre || 'Cliente anterior', ui.dinero(p.total), ui.etiquetas[p.estadoEntrega], ui.etiquetas[p.estadoPago]].forEach(value => ui.texto(row, value, 'td', ''));
                const cell = document.createElement('td'), button = document.createElement('button');
                button.className = 'button is-small is-info is-light'; button.textContent = 'Ver pedido'; button.dataset.pedido = p.id;
                button.addEventListener('click', () => { if (!guardando) detalle(p.id); }); cell.appendChild(button); row.appendChild(cell); lista.appendChild(row);
            });
            get('pedidos-status').textContent = data.totalPedidos ? `${data.totalPedidos} pedidos encontrados.` : 'No hay pedidos con este filtro.';
            get('pagina-texto').textContent = `Página ${totalPaginas ? pagina + 1 : 0} de ${totalPaginas}`;
        } catch (err) { get('pedidos-status').textContent = 'No se pudo cargar la lista.'; error(err); }
        finally { cargando = false; botones(); }
    }
    function mostrar(p) {
        seleccionado = p; get('pedido-detalle').hidden = false;
        get('pedido-titulo').textContent = `Pedido #${p.id}`; ui.render(get('pedido-contenido'), p);
        const select = get('pedido-nuevo-estado'); select.replaceChildren();
        p.siguientes.forEach(estado => { const option = document.createElement('option'); option.value = estado; option.textContent = ui.etiquetas[estado]; select.appendChild(option); });
        get('pedido-estado-form').hidden = !p.siguientes.length;
        get('pedido-observacion').value = ''; get('pedido-cobro').checked = false; cobro();
        get('pedido-titulo').focus();
    }
    async function detalle(id) {
        const solicitud = ++cargaDetalle; seleccionado = null;
        get('pedidos-error').hidden = true;
        // Evita editar un pedido mientras se carga el siguiente.
        get('pedido-estado-form').hidden = true;
        try { const p = await ui.request(`/api/admin/pedidos/${id}`); if (solicitud === cargaDetalle) mostrar(p); }
        catch (err) { if (solicitud === cargaDetalle) { seleccionado = null; get('pedido-detalle').hidden = true; error(err); } }
    }
    function cobro() {
        const entrega = get('pedido-nuevo-estado').value === 'ENTREGADO';
        get('pedido-cobro-field').hidden = !entrega; get('pedido-cobro').required = entrega;
        if (!entrega) get('pedido-cobro').checked = false;
    }
    get('pedido-nuevo-estado').addEventListener('change', cobro);
    get('pedido-estado-form').addEventListener('submit', async event => {
        event.preventDefault(); if (guardando || !seleccionado) return;
        const nuevo = get('pedido-nuevo-estado').value;
        if (nuevo === 'CANCELADO' && !confirm('¿Cancelar este pedido y devolver sus unidades al inventario?')) return;
        guardando = true; get('pedido-guardar').disabled = true; get('pedidos-error').hidden = true; botones();
        try {
            const p = await ui.request(`/api/admin/pedidos/${seleccionado.id}/estado`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ estadoActual: seleccionado.estadoEntrega, estadoEntrega: nuevo, cobroConfirmado: get('pedido-cobro').checked, observacion: get('pedido-observacion').value.trim() }) });
            mostrar(p); await cargar(); get('pedidos-status').textContent = `Pedido #${p.id} actualizado.`;
        } catch (err) { error(err); }
        finally { guardando = false; get('pedido-guardar').disabled = false; botones(); }
    });
    get('filtro-entrega').addEventListener('change', () => { pagina = 0; cargar(); });
    get('pedidos-refrescar').addEventListener('click', cargar);
    get('pagina-anterior').addEventListener('click', () => { if (!cargando && !guardando && pagina > 0) { pagina--; cargar(); } });
    get('pagina-siguiente').addEventListener('click', () => { if (!cargando && !guardando && pagina + 1 < totalPaginas) { pagina++; cargar(); } });
    cargar();
});