document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-retirar]').forEach(button => button.addEventListener('click', async () => {
        if (!confirm('¿Retirar este producto de la tienda? Se conservarán sus pedidos y podrás reactivarlo desde Editar.')) return;
        const error = document.getElementById('productos-error');
        error.hidden = true; button.disabled = true;
        try {
            const response = await (window.Seguridad?.fetch || fetch)(`/api/productos/${button.dataset.retirar}`, { method: 'DELETE' });
            if (!response.ok) {
                const result = await response.json().catch(() => ({}));
                throw new Error(result.message || 'No se pudo retirar el producto.');
            }
            location.assign('/ver-productos');
        } catch (err) { error.textContent = err.message; error.hidden = false; }
        finally { button.disabled = false; }
    }));
});