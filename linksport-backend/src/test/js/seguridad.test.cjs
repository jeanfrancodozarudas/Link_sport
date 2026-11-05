const {test}=require('node:test');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const fs=require('node:fs');
const path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/seguridad.js'),'utf8');
function setup(handler) {
    const calls=[]; const window={};
    const fetch=async (url,options={})=>{calls.push({url,options}); return handler ? handler(url,options) : {ok:true,status:200,json:async()=>url==='/api/auth/csrf'?{token:'token-csrf',headerName:'X-XSRF-TOKEN'}:{id:1}};};
    vm.runInNewContext(source,{window,fetch,URL,Headers,location:{origin:'http://localhost:8080'}});
    return {security:window.Seguridad,calls};
}
test('las lecturas no añaden JWT ni consultan CSRF innecesariamente',async()=>{
    const a=setup();await a.security.fetch('/api/productos');assert.equal(a.calls.length,1);assert.equal(a.calls[0].options.credentials,'same-origin');assert.equal(a.calls[0].options.headers.has('Authorization'),false);
});
test('POST PATCH y DELETE envían CSRF con cookies del mismo sitio',async()=>{
    const a=setup();for(const method of ['POST','PATCH','DELETE'])await a.security.fetch('/api/operacion',{method});
    assert.equal(a.calls.filter(c=>c.url==='/api/auth/csrf').length,1);
    for(const c of a.calls.filter(c=>c.url==='/api/operacion'))assert.equal(c.options.headers.get('X-XSRF-TOKEN'),'token-csrf');
});
test('varias peticiones concurrentes comparten la comprobación CSRF',async()=>{
    const a=setup();await Promise.all([a.security.fetch('/uno',{method:'POST'}),a.security.fetch('/dos',{method:'POST'})]);assert.equal(a.calls.filter(c=>c.url==='/api/auth/csrf').length,1);
});
test('un fallo al obtener CSRF impide enviar la operación',async()=>{
    const a=setup(async()=>({ok:false,status:503}));await assert.rejects(a.security.fetch('/api/auth/login',{method:'POST'}));assert.equal(a.calls.length,1);
});
test('no envía verificaciones ni cookies a otro origen',async()=>{
    const a=setup();await assert.rejects(a.security.fetch('https://otro.example/api',{method:'POST'}));assert.equal(a.calls.length,0);
});
test('un 403 descarta CSRF para poder recuperar la sesión en el siguiente intento',async()=>{
    const a=setup(async url=>url==='/api/auth/csrf'?{ok:true,status:200,json:async()=>({token:'csrf',headerName:'X-XSRF-TOKEN'})}:{ok:false,status:403});await a.security.fetch('/api/operacion',{method:'POST'});await a.security.fetch('/api/operacion',{method:'POST'});assert.equal(a.calls.filter(c=>c.url==='/api/auth/csrf').length,2);
});