// Solo guarda identificadores y códigos de consulta, sin datos de contacto.
(function (root) {
    const key = 'linksport-pedidos-v1';
    const valido = p => p && Number.isSafeInteger(p.id) && p.id > 0 && typeof p.clave === 'string' && /^[a-f0-9-]{36}$/i.test(p.clave);
    const store = {
        listar() {
            try {
                const datos = JSON.parse(root.localStorage.getItem(key) || '[]');
                return Array.isArray(datos) ? datos.filter(valido).slice(0, 50) : [];
            } catch { return []; }
        },
        guardar(pedido) {
            if (!valido(pedido)) return false;
            try {
                const pedidos = this.listar().filter(p => p.clave !== pedido.clave);
                root.localStorage.setItem(key, JSON.stringify([{ id: pedido.id, clave: pedido.clave }, ...pedidos].slice(0, 50)));
                return true;
            } catch { return false; }
        }
    };
    root.PedidosStore = store;
    if (typeof module !== 'undefined') module.exports = store;
})(typeof window !== 'undefined' ? window : globalThis);