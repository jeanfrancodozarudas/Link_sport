document.addEventListener('DOMContentLoaded', () => {
    const ui = window.PedidosUI, get = id => document.getElementById(id);
    const lista = get('mis-pedidos-lista');
    let cargando = false;
    async function consultar(clave) {
        if (cargando) return;
        cargando = true; get('consulta-boton').disabled = true; get('consulta-error').hidden = true; get('consulta-detalle').hidden = true;
        try {
            const pedido = await ui.request('/api/pedidos/consulta', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ clave }) });
            get('consulta-detalle').hidden = false; get('consulta-titulo').textContent = `Pedido #${pedido.id}`;
            ui.render(get('consulta-contenido'), pedido); get('consulta-titulo').focus();
            if (!pedido.cuentaAsociada) window.PedidosStore.guardar({ id: pedido.id, clave });
            renderLista();
        } catch (err) { get('consulta-error').textContent = err.message; get('consulta-error').hidden = false; }
        finally { cargando = false; get('consulta-boton').disabled = false; }
    }
    function renderLista() {
        lista.replaceChildren();
        const pedidos = window.PedidosStore.listar();
        if (!pedidos.length) ui.texto(lista, 'No hay compras guardadas en este navegador.');
        pedidos.forEach(p => {
            const button = document.createElement('button'); button.className = 'button is-info is-light mr-2 mb-2'; button.textContent = `Ver pedido #${p.id}`;
            button.addEventListener('click', () => { if (!cargando) { get('consulta-clave').value = p.clave; consultar(p.clave); } }); lista.appendChild(button);
        });
    }
    get('consulta-pedido-form').addEventListener('submit', e => { e.preventDefault(); consultar(get('consulta-clave').value.trim()); });
    renderLista();
    let paginaCuenta = 0, cargandoCuenta = false;
    async function cuenta(reiniciar = false) {
        if (cargandoCuenta) return;
        cargandoCuenta = true;
        if (reiniciar) { paginaCuenta = 0; get('pedidos-cuenta-lista').replaceChildren(); }
        try {
            const response = await window.Seguridad.fetch(`/api/pedidos/mios?pagina=${paginaCuenta}`);
            if (response.status === 401) { get('pedidos-cuenta').hidden = true; return; }
            if (!response.ok) throw new Error('No se pudieron cargar las compras de tu cuenta.');
            const data = await response.json(); get('pedidos-cuenta').hidden = false;
            if (!data.totalPedidos && reiniciar) ui.texto(get('pedidos-cuenta-lista'), 'Todavía no tienes compras asociadas a esta cuenta.');
            data.pedidos.forEach(p => {
                const box = document.createElement('details'); box.className = 'box';
                ui.texto(box, `Pedido #${p.id} · ${ui.etiquetas[p.estadoEntrega]} · ${ui.dinero(p.total)}`, 'summary');
                const content = document.createElement('div'); content.className='mt-4'; ui.render(content,p); box.appendChild(content); get('pedidos-cuenta-lista').appendChild(box);
            });
            paginaCuenta++; get('pedidos-cuenta-mas').hidden = paginaCuenta >= data.totalPaginas;
        } catch (err) { get('consulta-error').textContent=err.message; get('consulta-error').hidden=false; }
        finally { cargandoCuenta = false; }
    }
    get('pedidos-cuenta-mas').addEventListener('click', () => cuenta());
    window.addEventListener('sesion-cambiada', e => { if (e.detail) cuenta(true); else get('pedidos-cuenta').hidden=true; });
});