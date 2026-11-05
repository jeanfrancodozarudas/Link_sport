const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/script.js'), 'utf8');
function setup() {
    const nodes = new Map();
    const ids = ['carrito-sidebar', 'carrito-overlay', 'btn-abrir-carrito', 'btn-cerrar-carrito', 'carrito-productos-lista', 'carrito-subtotal-valor', 'carrito-burbuja-contador'];
    const node = () => ({ classList: { add() {}, remove() {}, toggle() {} }, addEventListener() {}, appendChild() {}, style: {}, innerHTML: '', textContent: '' });
    ids.forEach(id => nodes.set(id, node()));
    let init; const alerts = []; const window = {};
    const document = { addEventListener: (_, fn) => init = fn, getElementById: id => nodes.get(id) || null, querySelector: () => null, querySelectorAll: () => [], createElement: node };
    vm.runInNewContext(source, { window, document, setTimeout() {}, setInterval() {}, clearInterval() {}, alert: text => alerts.push(text) }); init();
    return { window, alerts, nodes };
}
test('el carrito no admite productos agotados', () => {
    const a = setup(); a.window.anadirProductoAMemoria(42, 'Producto', 10, 'foto.jpg', 0);
    assert.equal(a.window.linkCheckout.items().length, 0); assert.equal(a.alerts.length, 1);
});
test('añadir y aumentar cantidad respetan las existencias', () => {
    const a = setup();
    for (let i = 0; i < 3; i++) a.window.anadirProductoAMemoria(42, 'Producto', 10, 'foto.jpg', 2);
    a.window.modificarCantidadItem(0, 1);
    assert.equal(a.window.linkCheckout.items()[0].cantidad, 2);
    assert.equal(a.nodes.get('carrito-subtotal-valor').textContent, 'S/. 20.00');
});
test('identifica productos por ID real y no por posición o nombre', () => {
    const a = setup(); a.window.anadirProductoAMemoria(42, 'Igual', 10, 'foto.jpg', 2); a.window.anadirProductoAMemoria(99, 'Igual', 20, 'foto.jpg', 2);
    assert.equal(a.window.linkCheckout.items().length, 2);
    assert.equal(a.window.linkCheckout.items()[1].id, 99);
});
test('restar la última unidad elimina el producto del carrito', () => {
    const a = setup(); a.window.anadirProductoAMemoria(42, 'Producto', 10, 'foto.jpg', 2); a.window.modificarCantidadItem(0, -1);
    assert.equal(a.window.linkCheckout.items().length, 0);
});