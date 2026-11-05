const { test } = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/pedidos-store.js'), 'utf8');
const clave = n => `${String(n).padStart(8, '0')}-1234-1234-1234-123456789abc`;
function setup(initial = '[]', blocked = false) {
    let value = initial;
    const window = { localStorage: { getItem() { if (blocked) throw new Error('blocked'); return value; }, setItem(k, v) { if (blocked) throw new Error('blocked'); value = v; } } };
    vm.runInNewContext(source, { window });
    return { store: window.PedidosStore, data: () => JSON.parse(value) };
}
test('conserva el código de seguimiento sin datos personales', () => {
    const a = setup(); a.store.guardar({ id: 1, clave: clave(1), nombre: 'No guardar', correo: 'no@example.com' });
    assert.deepEqual(a.data(), [{ id: 1, clave: clave(1) }]);
});
test('reintentos del mismo pedido no duplican el historial', () => {
    const a = setup(); a.store.guardar({ id: 1, clave: clave(1) }); a.store.guardar({ id: 1, clave: clave(1) });
    assert.equal(a.store.listar().length, 1);
});
test('historial limita sus registros y conserva primero los recientes', () => {
    const a = setup(); for (let i = 1; i <= 60; i++) a.store.guardar({ id: i, clave: clave(i) });
    assert.equal(a.store.listar().length, 50); assert.equal(a.store.listar()[0].id, 60);
});
test('almacenamiento bloqueado o corrupto no rompe la confirmación', () => {
    assert.equal(setup('{broken').store.listar().length, 0);
    assert.equal(setup('{}').store.listar().length, 0);
    const a = setup('[]', true); assert.equal(a.store.guardar({ id: 1, clave: clave(1) }), false);
});
test('no guarda IDs o códigos inválidos', () => {
    const a = setup(); assert.equal(a.store.guardar({ id: -1, clave: clave(1) }), false);
    assert.equal(a.store.guardar({ id: 1, clave: '123' }), false); assert.equal(a.store.listar().length, 0);
});