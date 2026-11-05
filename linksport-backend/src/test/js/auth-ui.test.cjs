const {test}=require('node:test');const assert=require('node:assert/strict');const vm=require('node:vm');const fs=require('node:fs');const path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../../main/resources/static/js/auth-ui.js'),'utf8');
const tick=()=>new Promise(resolve=>setImmediate(resolve));
function setup(response) {
    const nodes=new Map(),requests=[],redirects=[];let init;
    const get=id=>{
        if(!nodes.has(id)){const classes=new Set();nodes.set(id,{value:'',hidden:false,disabled:false,textContent:'',handlers:{},resetCount:0,classList:{add:c=>classes.add(c),remove:c=>classes.delete(c),contains:c=>classes.has(c)},addEventListener(e,fn){this.handlers[e]=fn;},focus(){},reportValidity(){return true;},reset(){this.resetCount++;},querySelector(selector){return get(id+selector);}});}return nodes.get(id);
    };
    const document={getElementById:get,querySelectorAll:s=>[get(s)],body:{dataset:{pagina:'login'}},addEventListener:(e,fn)=>init=fn};
    const window={Seguridad:{usuario:async()=>null,fetch:async(url,options)=>{requests.push({url,options});return response;}},dispatchEvent(){}};
    vm.runInNewContext(source,{document,window,location:{assign:url=>redirects.push(url)},CustomEvent:class {constructor(type,data){this.type=type;this.detail=data.detail;}}});init();
    return {get,requests,redirects};
}
test('login incorrecto muestra el error real sin simular éxito',async()=>{
    const a=setup({ok:false,status:401,json:async()=>({message:'Correo o contraseña incorrectos.'})});await tick();
    a.get('login-correo').value='ana@example.com';a.get('login-pass').value='incorrecta';a.get('form-login-modal').handlers.submit({preventDefault(){},currentTarget:a.get('form-login-modal')});await tick();
    assert.equal(a.get('login-error').hidden,false);assert.match(a.get('login-error').textContent,/incorrectos/);assert.equal(a.get('form-login-modal').resetCount,0);assert.equal(a.redirects.length,0);
});
test('login correcto envía credenciales al backend y habilita al administrador',async()=>{
    const a=setup({ok:true,status:200,json:async()=>({usuario:{id:1,nombre:'Admin',rol:'ADMIN'}})});await tick();
    a.get('login-correo').value=' admin@example.com ';a.get('login-pass').value='Clave-segura-123';a.get('form-login-modal').handlers.submit({preventDefault(){},currentTarget:a.get('form-login-modal')});await tick();
    assert.equal(a.requests[0].url,'/api/auth/login');assert.deepEqual(JSON.parse(a.requests[0].options.body),{correo:'admin@example.com',password:'Clave-segura-123'});assert.equal(a.get('[data-admin]').hidden,false);assert.equal(a.redirects[0],'/ver-productos');
});
test('el registro crea una cuenta de cliente sin enviar un rol elegido por el navegador',async()=>{
    const a=setup({ok:true,status:201,json:async()=>({usuario:{id:2,nombre:'Ana',rol:'CLIENTE'}})});await tick();
    a.get('reg-nombre').value='Ana Pérez';a.get('reg-correo').value='ana@example.com';a.get('reg-pass').value='Clave-segura-123';a.get('form-registro-modal').handlers.submit({preventDefault(){},currentTarget:a.get('form-registro-modal')});await tick();
    assert.equal(a.requests[0].url,'/api/auth/registro');assert.equal(JSON.parse(a.requests[0].options.body).rol,undefined);assert.equal(a.get('[data-admin]').hidden,true);assert.equal(a.redirects[0],'/mis-pedidos');
});