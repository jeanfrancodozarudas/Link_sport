const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/checkout.js'), 'utf8');
function setup(fetchImpl = async () => ({ ok: true, json: async () => ({ pedidoId: 8, total: 39.8, estado: 'PENDIENTE_PAGO' }) })) {
    const nodes = new Map();
    const get = id => {
        if (!nodes.has(id)) {
            const classes = new Set();
            nodes.set(id, { id, hidden: false, value: '', checked: false, disabled: false, children: [],
                classList: { add: c => classes.add(c), remove: c => classes.delete(c), contains: c => classes.has(c) },
                handlers: {}, addEventListener(event, handler) { this.handlers[event] = handler; },
                querySelector(selector) { return get(`${id}:${selector}`); }, querySelectorAll() { return []; },
                focus() {}, replaceChildren() { this.children = []; }, appendChild(child) { this.children.push(child); },
                reportValidity() { return true; }, reset() {}, getClientRects() { return [1]; }
            });
        }
        return nodes.get(id);
    };
    let init, cleared = 0, calls = [], uuid = 0, alerts = [], guardados = [];
    const document = { getElementById: get, createElement: () => get(`line-${nodes.size}`), addEventListener: (e, fn) => init = fn };
    const customer = { nombre: 'Ana Pérez', correo: 'ana@example.com', telefono: '987654321', ciudad: 'Lima', direccion: 'Avenida Primavera 123' };
    const window = { PedidosStore: { guardar: p => guardados.push(p) }, linkCheckout: { items: () => [{ id: 42, nombre: 'Zapatillas', precio: 19.90, cantidad: 2 }], clear: () => cleared++ }, cerrarPanelCarrito() {} };
    const context = { document, window, crypto: { randomUUID: () => `clave-${++uuid}` },
        FormData: class { [Symbol.iterator]() { return Object.entries(customer)[Symbol.iterator](); } },
        alert: s => alerts.push(s), fetch: async (...args) => { calls.push(args); return fetchImpl(...args); }
    };
    vm.runInNewContext(source, context); init();
    const click = id => get(id).handlers.click({ currentTarget: get(id) });
    const submit = () => get('checkout-form').handlers.submit({ preventDefault() {} });
    return { get, window, calls, alerts, click, submit, guardados, cleared: () => cleared };
}
test('abrir el checkout y revisar no registra una compra', async () => {
    const app = setup(); app.click('btn-proceder-pago');
    assert.equal(app.get('modal-checkout').classList.contains('is-active'), true);
    await app.submit();
    assert.equal(app.calls.length, 0);
    assert.equal(app.get('checkout-revision').hidden, false);
    assert.match(app.get('checkout-total').textContent, /39.80/);
    assert.equal(app.get('checkout-resumen').children.length, 1);
});
test('rechaza un carrito vacío', () => {
    const app = setup(); app.window.linkCheckout.items = () => [];
    app.click('btn-proceder-pago');
    assert.equal(app.alerts.length, 1);
    assert.equal(app.calls.length, 0);
});
test('requiere confirmar antes de enviar', async () => {
    const app = setup(); app.click('btn-proceder-pago'); await app.submit(); await app.submit();
    assert.equal(app.calls.length, 0);
    assert.equal(app.get('checkout-error').hidden, false);
});
test('envía IDs reales y datos, vacía el carrito solo tras recibir un pedido pendiente', async () => {
    const app = setup(); app.click('btn-proceder-pago'); await app.submit();
    app.get('checkout-confirmacion').checked = true; await app.submit();
    const body = JSON.parse(app.calls[0][1].body);
    assert.deepEqual(body.productos, [{ id: 42, cantidad: 2 }]);
    assert.equal(body.comprador.correo, 'ana@example.com');
    assert.equal(body.totalConfirmado, 39.80);
    assert.equal(body.metodoPago, 'CONTRA_ENTREGA');
    assert.equal(app.cleared(), 1);
    assert.equal(app.guardados[0].id, 8); assert.equal(app.guardados[0].clave, body.clavePedido);
    assert.match(app.get('modal-compra-exitosa:.subtitle').textContent, /No se ha realizado ningún cobro/);
});
test('conserva carrito y clave al reintentar un error de red, incluso tras cerrar', async () => {
    const app = setup(async () => { throw new TypeError('offline'); });
    app.click('btn-proceder-pago'); await app.submit(); app.get('checkout-confirmacion').checked = true; await app.submit();
    app.click('checkout-close'); app.click('btn-proceder-pago'); await app.submit(); app.get('checkout-confirmacion').checked = true; await app.submit();
    assert.equal(app.cleared(), 0);
    assert.equal(app.calls.length, 2);
    assert.equal(JSON.parse(app.calls[0][1].body).clavePedido, JSON.parse(app.calls[1][1].body).clavePedido);
});
test('no admite envíos simultáneos ni cierra mientras espera', async () => {
    let resolve;
    const app = setup(() => new Promise(r => resolve = r));
    app.click('btn-proceder-pago'); await app.submit(); app.get('checkout-confirmacion').checked = true;
    const pending = app.submit(); await app.submit(); app.click('checkout-close');
    assert.equal(app.calls.length, 1);
    assert.equal(app.get('modal-checkout').classList.contains('is-active'), true);
    resolve({ ok: false, json: async () => ({ message: 'Los precios cambiaron.' }) }); await pending;
    assert.equal(app.cleared(), 0);
    assert.equal(app.get('checkout-error').textContent, 'Los precios cambiaron.');
    assert.equal(app.get('checkout-continuar').disabled, false);
});
test('las compras de cuenta no guardan códigos privados en el historial compartido del navegador', async () => {
    const app = setup(async () => ({ ok: true, json: async () => ({ pedidoId: 8, total: 39.8, estado: 'PENDIENTE_PAGO', cuentaAsociada: true }) }));
    app.click('btn-proceder-pago'); await app.submit(); app.get('checkout-confirmacion').checked = true; await app.submit();
    assert.equal(app.cleared(), 1); assert.equal(app.guardados.length, 0);
});