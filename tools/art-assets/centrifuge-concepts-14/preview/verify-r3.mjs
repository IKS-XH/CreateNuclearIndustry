import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..'),report=resolve(root,'../../../build/reports/art/ART-CENTRIFUGE-14A/preview/r3');
const base=process.argv[2]??'http://127.0.0.1:4191',sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const manifest=JSON.parse(readFileSync(resolve(root,'assets/concepts.json'),'utf8')),checks=[];
/** R3只核三lower/shaft、契约与实际网页补丁的字节身份；旧转子/上壳/十八SVG和全矩阵复用。 */
if(manifest.schemaVersion!==1||manifest.revision!=='r3'||manifest.concepts.map(v=>v.id).join(',')!=='a,b,c')throw new Error('R3契约不符');
const routes=['index.html','preview/scene.mjs','assets/concepts.json'];
for(const concept of manifest.concepts){if(!concept.models.shaft)throw new Error(`R3缺少实际shaft ${concept.id}`);routes.push(`assets/${concept.models.lower}`,`assets/${concept.models.shaft}`);}
for(const path of routes){const response=await fetch(`${base}/${path==='index.html'?'':path}`),bytes=Buffer.from(await response.arrayBuffer()),local=readFileSync(resolve(root,path));const pass=response.status===200&&sha(bytes)===sha(local);checks.push({path,http:response.status,bytes:bytes.length,sha256:sha(bytes),localSame:pass});if(!pass)throw new Error(`R3 HTTP与实际载荷不同源 ${path}`);}
mkdirSync(report,{recursive:true});writeFileSync(resolve(report,'input-checks.json'),JSON.stringify({task:'ART-CENTRIFUGE-14A-R3',at:new Date().toISOString(),base,checks,reused:'R2转子/上壳/十八SVG与原相机灯光/控件/capture；原矩阵不重跑',browserVisualVerified:false},null,2));
console.log(`${checks.length}/${checks.length} R3三lower/shaft、契约与网页补丁同源，实际底部/剖视/旋转由负责人观察。`);
