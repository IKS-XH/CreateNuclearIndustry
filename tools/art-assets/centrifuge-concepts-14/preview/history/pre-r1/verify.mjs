import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..'),main=resolve(root,'../../../../Create_NuclearIndustry'),library=resolve(root,'../../../../block-model-renderer');
const report=resolve(root,'../../../build/reports/art/ART-CENTRIFUGE-14A/preview');
const base=process.argv[2]??'http://127.0.0.1:4191';
const checks=[],sha=bytes=>createHash('sha256').update(bytes).digest('hex');
/** 有限检查只比对本页HTTP依赖与实际源身份；此脚本不创建浏览器或把HTTP成功称作视觉通过。 */
function check(name,condition,detail){checks.push({name,pass:!!condition,detail});if(!condition)throw new Error(`有限检查失败：${name}`);}
const manifest=JSON.parse(readFileSync(resolve(root,'assets/concepts.json'),'utf8'));
check('三稿契约',manifest.schemaVersion===1&&manifest.concepts.map(v=>v.id).join(',')==='a,b,c');
check('渲染库2.20.0',JSON.parse(readFileSync(resolve(library,'package.json'),'utf8')).version==='2.20.0');
const locked=JSON.parse(readFileSync(resolve(main,'tools/nuclear-plant-3d/vendor/renderer/manifest.json'),'utf8'));
for(const entry of locked.files.filter(file=>/\.(js|wasm)$/.test(file.path))){const name=entry.path.split('/').at(-1),hash=sha(readFileSync(resolve(library,'dist',name)));check(`已核渲染库来源 ${name}`,hash===entry.sha256,{sha256:hash});}
const core=readFileSync(resolve(main,'tools/nuclear-plant-3d/vendor/three.core.js'),'utf8');check('现有Three0.180.0',core.includes("const REVISION = '180'"));
for(const entry of [...Object.values(manifest.baseline.models),...Object.values(manifest.baseline.textures)]){const hash=sha(readFileSync(resolve(main,entry.path)));check(`原机对照与作者基线一致 ${entry.path}`,hash===entry.sha256,{sha256:hash});}
const routes=['/','/preview/style.css','/preview/scene.mjs','/vendor/three.module.js','/vendor/three.core.js','/renderer/block-model-renderer.min.js','/renderer/block_model_renderer_bg.wasm','/assets/concepts.json'];
for(const concept of manifest.concepts)for(const path of [...Object.values(concept.models),...Object.values(concept.textures)])routes.push(`/assets/${path}`);
for(const name of ['enrichment_centrifuge','enrichment_centrifuge_upper','enrichment_centrifuge_rotor'])routes.push(`/formal/assets/create_nuclear_industry/models/block/${name}.json`);
for(const name of ['casing','brass','glass','dark'])routes.push(`/formal/assets/create_nuclear_industry/textures/block/enrichment_centrifuge_${name}.png`);
for(const route of routes){const response=await fetch(base+route);const bytes=Buffer.from(await response.arrayBuffer());check(`本页HTTP ${route}`,response.status===200&&bytes.length>0,{status:response.status,bytes:bytes.length,sha256:sha(bytes),mime:response.headers.get('content-type')});}
for(const route of ['/formal/pack.mcmeta','/vendor/OrbitControls.js','/README.md','/assets/../server.mjs']){const response=await fetch(base+route);check(`无关路由拒绝 ${route}`,response.status===404,{status:response.status});}
mkdirSync(report,{recursive:true});writeFileSync(resolve(report,'finite-checks.json'),JSON.stringify({task:'ART-CENTRIFUGE-14A',at:new Date().toISOString(),base,checks,httpAndSourceOnly:true,browserVisualVerified:false},null,2));
console.log(`${checks.length}/${checks.length} 本页依赖/来源检查通过；浏览器渲染由负责人独立实看。`);
