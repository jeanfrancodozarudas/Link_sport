document.addEventListener('DOMContentLoaded', () => {
    const get = id => document.getElementById(id);
    const modalLogin = get('modal-login-oferta'), modalRegistro = get('modal-registro-usuario');
    let actual = null, enviando = false;
    function abrir(modal) { modal?.classList.add('is-active'); modal?.querySelector('input')?.focus(); }
    function cerrar(modal) { modal?.classList.remove('is-active'); }
    function pintar(usuario) {
        actual = usuario;
        document.querySelectorAll('[data-cuenta]').forEach(n => n.textContent = usuario ? `${usuario.nombre} · ${usuario.rol === 'ADMIN' ? 'Administrador' : 'Cliente'}` : '');
        document.querySelectorAll('[data-login]').forEach(n => n.hidden = !!usuario);
        document.querySelectorAll('[data-logout]').forEach(n => n.hidden = !usuario);
        document.querySelectorAll('[data-admin]').forEach(n => n.hidden = usuario?.rol !== 'ADMIN');
        window.dispatchEvent(new CustomEvent('sesion-cambiada', { detail: usuario }));
    }
    get('btn-usuario-login')?.addEventListener('click', e => { e.preventDefault(); if (actual) location.assign('/mis-pedidos'); else abrir(modalLogin); });
    [['modal-login-x-close', modalLogin], ['modal-login-bg-close', modalLogin], ['modal-reg-x-close', modalRegistro], ['modal-reg-bg-close', modalRegistro]].forEach(([id, modal]) => get(id)?.addEventListener('click', () => cerrar(modal)));
    get('link-ir-a-registro')?.addEventListener('click', e => { e.preventDefault(); cerrar(modalLogin); abrir(modalRegistro); });
    get('link-ir-a-login')?.addEventListener('click', e => { e.preventDefault(); cerrar(modalRegistro); abrir(modalLogin); });
    async function enviar(form, registro) {
        if (enviando || !form.reportValidity()) return;
        enviando = true;
        const error = get(registro ? 'registro-error' : 'login-error'); error.hidden = true;
        const button = form.querySelector('button[type="submit"]'); button.disabled = true;
        const datos = registro ? { nombre: get('reg-nombre').value.trim(), correo: get('reg-correo').value.trim(), password: get('reg-pass').value } : { correo: get('login-correo').value.trim(), password: get('login-pass').value };
        try {
            const response = await window.Seguridad.fetch(registro ? '/api/auth/registro' : '/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(datos) });
            const result = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(result.message || 'No se pudo iniciar la sesión. Revisa los datos.');
            pintar(result.usuario); form.reset(); cerrar(modalLogin); cerrar(modalRegistro);
            if (document.body.dataset.pagina === 'login') {
                location.assign(result.usuario.rol === 'ADMIN' ? '/ver-productos' : '/mis-pedidos');
            } else {
                const status = get('sesion-status'); if (status) status.textContent = 'Sesión iniciada correctamente.';
            }
        } catch (err) { error.textContent = err.message; error.hidden = false; }
        finally { enviando = false; button.disabled = false; }
    }
    get('form-login-modal')?.addEventListener('submit', e => { e.preventDefault(); enviar(e.currentTarget, false); });
    get('form-registro-modal')?.addEventListener('submit', e => { e.preventDefault(); enviar(e.currentTarget, true); });
    document.querySelectorAll('[data-logout]').forEach(button => button.addEventListener('click', async () => {
        button.disabled = true;
        try {
            const response = await window.Seguridad.fetch('/api/auth/logout', { method: 'POST' });
            if (!response.ok) throw new Error('No se pudo cerrar la sesión. Vuelve a intentarlo.');
            location.assign('/');
        } catch (err) { button.disabled = false; const status = get('sesion-status'); if (status) status.textContent = err.message; }
    }));
    window.Seguridad.usuario().then(pintar).catch(err => { const status = get('sesion-status'); if (status) status.textContent = err.message; });
});