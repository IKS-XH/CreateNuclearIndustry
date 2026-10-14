import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..'),report=resolve(root,'../../../build/reports/art/ART-CENTRIFUGE-14A/preview/r2');
const base=process.argv[2]??'http://127.0.0.1:4191',sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const manifest=JSON.parse(readFileSync(resolve(root,'assets/concepts.json'),'utf8')),checks=[];
/** R2只读当前三rotor、契约及必要文案补丁的字节身份；不重复R1十八SVG或旧矩阵。 */
if(manifest.schemaVersion!==1||manifest.revision!=='r2'||manifest.concepts.map(v=>v.id).join(',')!=='a,b,c')throw new Error('R2契约不符');
const routes=['index.html','preview/scene.mjs','assets/concepts.json',...manifest.concepts.map(concept=>`assets/${concept.models.rotor}`)];
for(const path of routes){const response=await fetch(`${base}/${path==='index.html'?'':path}`),bytes=Buffer.from(await response.arrayBuffer()),local=readFileSync(resolve(root,path));const pass=response.status===200&&sha(bytes)===sha(local);checks.push({path,http:response.status,bytes:bytes.length,sha256:sha(bytes),localSame:pass});if(!pass)throw new Error(`R2 HTTP与实际载荷不同源 ${path}`);}
mkdirSync(report,{recursive:true});writeFileSync(resolve(report,'input-checks.json'),JSON.stringify({task:'ART-CENTRIFUGE-14A-R2',at:new Date().toISOString(),base,checks,reused:'R1六槽/六壳/相机灯光与操作证据；原30/52矩阵不重跑',browserVisualVerified:false},null,2));
console.log(`${checks.length}/${checks.length} R2三转子、契约与必要网页补丁同源，实际窗内/剖视/旋转由负责人观察。`);
