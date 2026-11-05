(function () {
    let csrf = null, pendiente = null;
    async function token() {
        if (csrf) return csrf;
        if (!pendiente) pendiente = fetch('/api/auth/csrf', { credentials: 'same-origin', cache: 'no-store' })
            .then(async res => { if (!res.ok) throw new Error('No se pudo verificar la seguridad de la conexión.'); return res.json(); })
            .then(data => { csrf = data; return data; }).finally(() => { pendiente = null; });
        return pendiente;
    }
    window.Seguridad = {
        async fetch(url, opciones = {}) {
            const destino = new URL(url, location.origin);
            if (destino.origin !== location.origin) throw new Error('La solicitud debe dirigirse a este sitio.');
            const metodo = (opciones.method || 'GET').toUpperCase();
            const headers = new Headers(opciones.headers || {});
            if (!['GET', 'HEAD', 'OPTIONS'].includes(metodo)) { const t = await token(); headers.set(t.headerName, t.token); }
            const res = await fetch(destino.pathname + destino.search, { ...opciones, headers, credentials: 'same-origin', cache: 'no-store' });
            if (res.status === 403) csrf = null;
            return res;
        },
        async usuario() {
            const res = await this.fetch('/api/auth/me');
            if (res.status === 401) return null;
            if (!res.ok) throw new Error('No se pudo comprobar tu sesión.');
            return res.json();
        }
    };
})();