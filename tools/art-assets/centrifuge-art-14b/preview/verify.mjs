import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..'),project=resolve(root,'../../..'),report=resolve(project,'build/reports/art/ART-CENTRIFUGE-14B/preview');
const base=process.argv[2]??'http://127.0.0.1:4192',sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const manifest=JSON.parse(readFileSync(resolve(root,'assets.json'),'utf8')),checks=[];
/** 本批只核实际安装十一载荷和本页依赖；SVG路由禁止，HTTP检查不宣称GPU/游戏视觉通过。 */
if(manifest.schemaVersion!==1||manifest.status!=='installed'||manifest.sourceConcept.id!=='a')throw new Error('14B必须是已安装A契约');
for(const entry of[...Object.values(manifest.models),...Object.values(manifest.textures)]){const path=entry.installed.replace(/^src\/main\/resources\//,''),response=await fetch(`${base}/installed/${path}`),bytes=Buffer.from(await response.arrayBuffer());const pass=response.status===200&&sha(bytes)===entry.sha256&&sha(bytes)===sha(readFileSync(resolve(project,entry.installed)));checks.push({path:entry.installed,http:response.status,bytes:bytes.length,sha256:sha(bytes),pass});if(!pass)throw new Error(`实际PNG/模型载荷不同源 ${path}`);}
for(const[path,file]of[['/','index.html'],['/assets.json','assets.json'],['/preview/scene.mjs','preview/scene.mjs'],['/preview/style.css','preview/style.css']]){const response=await fetch(base+path),bytes=Buffer.from(await response.arrayBuffer()),pass=response.status===200&&sha(bytes)===sha(readFileSync(resolve(root,file)));checks.push({path,http:response.status,bytes:bytes.length,sha256:sha(bytes),pass});if(!pass)throw new Error(`本页输入不同源 ${path}`);}
for(const path of['/vendor/three.module.js','/vendor/three.core.js','/renderer/block-model-renderer.min.js','/renderer/block_model_renderer_bg.wasm']){const response=await fetch(base+path),bytes=Buffer.from(await response.arrayBuffer()),pass=response.status===200&&bytes.length>0;checks.push({path,http:response.status,bytes:bytes.length,sha256:sha(bytes),pass});if(!pass)throw new Error(`固定库缺失 ${path}`);}
for(const path of['/assets/source/casing.svg','/installed/pack.mcmeta','/README.md']){const response=await fetch(base+path),pass=response.status===404;checks.push({path,http:response.status,pass});if(!pass)throw new Error(`非本批载荷未被拒绝 ${path}`);}
mkdirSync(report,{recursive:true});writeFileSync(resolve(report,'http-checks.json'),JSON.stringify({task:'ART-CENTRIFUGE-14B',at:new Date().toISOString(),base,checks,sourceConcept:manifest.sourceConcept,installedPngOnly:true,browserVisualVerified:false},null,2));
console.log(`${checks.length}/${checks.length} 14B真实安装PNG/模型与本页依赖检查通过；GPU由负责人实看。`);
