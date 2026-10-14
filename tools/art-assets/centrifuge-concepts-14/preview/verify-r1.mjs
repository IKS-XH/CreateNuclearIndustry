import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..'),report=resolve(root,'../../../build/reports/art/ART-CENTRIFUGE-14A/preview/r1');
const base=process.argv[2]??'http://127.0.0.1:4191',sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const manifest=JSON.parse(readFileSync(resolve(root,'assets/concepts.json'),'utf8')),checks=[];
/** R1只核精修输入与网页补丁的HTTP字节身份；原52项及渲染库检查复用，不扩大设备检查。 */
if(manifest.schemaVersion!==1||manifest.concepts.map(v=>v.id).join(',')!=='a,b,c')throw new Error('R1三稿契约不符');
const routes=['index.html','preview/scene.mjs','assets/concepts.json'];
for(const concept of manifest.concepts){const slots=Object.keys(concept.textures);if(!['casing','brass','glass','dark','machined','fastener'].every(slot=>slots.includes(slot)))throw new Error(`R1精修材质槽未齐 ${concept.id}`);for(const path of [...Object.values(concept.models),...Object.values(concept.textures)])routes.push(`assets/${path}`);}
for(const path of routes){const response=await fetch(`${base}/${path==='index.html'?'':path}`),bytes=Buffer.from(await response.arrayBuffer()),local=readFileSync(resolve(root,path));const pass=response.status===200&&sha(bytes)===sha(local);checks.push({path,http:response.status,bytes:bytes.length,sha256:sha(bytes),localSame:pass});if(!pass)throw new Error(`R1 HTTP与实际输入不同源 ${path}`);}
mkdirSync(report,{recursive:true});writeFileSync(resolve(report,'input-checks.json'),JSON.stringify({task:'ART-CENTRIFUGE-14A-R1',at:new Date().toISOString(),base,checks,reused:'初版52/52；渲染库/Three与相机光照未变',browserVisualVerified:false},null,2));
console.log(`${checks.length}/${checks.length} R1当前输入与网页补丁同源，浏览器实际精修观察由负责人完成。`);
