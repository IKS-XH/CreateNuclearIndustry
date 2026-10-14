import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..'),main=resolve(root,'../../../../Create_NuclearIndustry'),library=resolve(root,'../../../../block-model-renderer');
const report=resolve(root,'../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15A/preview'),base=`http://127.0.0.1:${Number(process.argv[2]??4193)}`;
const checks=[],identity=[];
function check(name,passed,detail){checks.push({name,passed,detail});if(!passed)throw new Error(name);}
function sha(bytes){return createHash('sha256').update(bytes).digest('hex');}
/** 只检查本批工具语法、必要输入和HTTP同源身份，GPU交负责人实际浏览。 */
try{
  for(const name of['server.mjs','preview/scene.mjs','preview/verify.mjs']){const result=spawnSync(process.execPath,['--check',resolve(root,name)],{encoding:'utf8'});check(`语法 ${name}`,result.status===0,result.stderr);}
  const packageInfo=JSON.parse(readFileSync(resolve(library,'package.json'),'utf8'));check('本地block-model-renderer 2.20.0',packageInfo.version==='2.20.0');check('现成Three 180',readFileSync(resolve(main,'tools/nuclear-plant-3d/vendor/three.core.js'),'utf8').includes("const REVISION = '180'"));
  const manifest=JSON.parse(readFileSync(resolve(root,'assets/concepts.json'),'utf8'));check('schema1三稿契约',manifest.schemaVersion===1&&Object.keys(manifest.concepts).join(',')==='a,b,c');
  const routes=new Map([['/',resolve(root,'index.html')],['/preview/style.css',resolve(root,'preview/style.css')],['/preview/scene.mjs',resolve(root,'preview/scene.mjs')],['/assets/concepts.json',resolve(root,'assets/concepts.json')],['/vendor/three.module.js',resolve(main,'tools/nuclear-plant-3d/vendor/three.module.js')],['/vendor/three.core.js',resolve(main,'tools/nuclear-plant-3d/vendor/three.core.js')],['/renderer/block-model-renderer.min.js',resolve(library,'dist/block-model-renderer.min.js')],['/renderer/block_model_renderer_bg.wasm',resolve(library,'dist/block_model_renderer_bg.wasm')],['/preview/refs/reference.json',resolve(root,'preview/refs/reference.json')]]);
  for(const concept of Object.values(manifest.concepts))for(const path of[...Object.values(concept.models),...Object.values(concept.textures)])routes.set(`/assets/${path}`,resolve(root,'assets',path));
  for(const path of['assets/create/models/block/shaft_half.json','assets/create/textures/block/axis.png','assets/create/textures/block/axis_top.png','assets/minecraft/models/block/block.json'])routes.set(`/preview/refs/${path}`,resolve(root,'preview/refs',path));
  for(const[route,file]of routes){const response=await fetch(base+route),actual=new Uint8Array(await response.arrayBuffer()),source=readFileSync(file);const passed=response.status===200&&sha(actual)===sha(source);check(`HTTP同源 ${route}`,passed,{status:response.status});identity.push({route,path:file,bytes:source.length,sha256:sha(source)});}
  for(const path of['/server.mjs','/preview/prepare-refs.ps1','/assets/../../../AGENTS.md']){const response=await fetch(base+path);check(`白名单拒绝 ${path}`,response.status===404);}
  const health=await(await fetch(base+'/health')).json();check('自有服务身份',health.task==='ART-SHIELDED-ASSEMBLY-15A'&&health.root.replaceAll('\\','/')===root.replaceAll('\\','/'),health);
  const reference=JSON.parse(readFileSync(resolve(root,'preview/refs/reference.json'),'utf8'));for(const entry of reference.entries){const path=resolve(root,'preview/refs',entry.entry);check(`实际参考SHA ${entry.entry}`,sha(readFileSync(path))===entry.sha256);}
  // 把实际半轴八角点按已读DOWN旋转映射，独立核对底部位置，避免引用记录与实际模型脱节。
  const shaft=JSON.parse(readFileSync(resolve(root,'preview/refs/assets/create/models/block/shaft_half.json'),'utf8')),points=[];
  for(const element of shaft.elements)for(const x of[element.from[0],element.to[0]])for(const y of[element.from[1],element.to[1]])for(const z of[element.from[2],element.to[2]])points.push([x,16-z,y]);
  const shaftBounds=[0,1].map(side=>[0,1,2].map(axis=>(side?Math.max:Math.min)(...points.map(point=>point[axis]))));
  check('实际Create DOWN半轴包络',JSON.stringify(shaftBounds)===JSON.stringify([[6,0,6],[10,8,10]])&&JSON.stringify(shaftBounds)===JSON.stringify(reference.shaft.boundsModel),shaftBounds);
  mkdirSync(report,{recursive:true});writeFileSync(resolve(report,'http-input-identity.json'),JSON.stringify({task:'ART-SHIELDED-ASSEMBLY-15A',passed:true,gpuVerified:false,checks,identity,service:health},null,2));console.log(`${checks.length}/${checks.length}本批有限检查通过；${identity.length}HTTP输入同源，GPU由负责人实看。`);
}catch(error){mkdirSync(report,{recursive:true});writeFileSync(resolve(report,'http-input-failure.json'),JSON.stringify({passed:false,checks,identity,error:String(error.stack??error)},null,2));console.error(error);process.exitCode=1;}
