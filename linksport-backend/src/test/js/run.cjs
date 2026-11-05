const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const folder = path.join(__dirname, '../../main/resources/static/js');
for (const name of fs.readdirSync(folder).filter(name => name.endsWith('.js'))) {
    new vm.Script(fs.readFileSync(path.join(folder, name), 'utf8'), { filename: name });
}
console.log('Sintaxis JavaScript: todos los archivos correctos.');
for (const name of fs.readdirSync(__dirname).filter(name => name.endsWith('.test.cjs'))) require(path.join(__dirname, name));