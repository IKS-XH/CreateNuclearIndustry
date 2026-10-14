import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..'),workspace=resolve(root,'../../..'),main=resolve(root,'../../../../Create_NuclearIndustry'),library=resolve(root,'../../../../block-model-renderer');
const report=resolve(workspace,'build/reports/art/ART-SHIELDED-ASSEMBLY-15B-R1/preview'),base='http://127.0.0.1:4194',checks=[],identity=[];
const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
function check(name,passed,detail){checks.push({name,passed,detail});if(!passed)throw new Error(name);}
/** R1只核变化metadata模型及manifest身份；保原Three画面，网页alpha剔除不能证明Minecraft层已修复。 */
try{const manifest=JSON.parse(readFileSync(resolve(root,'assets.json'),'utf8')),revision=manifest.renderLayerR1;
  check('R1已批准C变更契约',manifest.schemaVersion===1&&manifest.selectedConcept==='c'&&revision?.task==='ART-SHIELDED-ASSEMBLY-15B-R1'&&Array.isArray(revision.changedTargets)&&revision.changedTargets.length>0);
  const syntax=spawnSync(process.execPath,['--check',fileURLToPath(import.meta.url)],{encoding:'utf8'});check('R1验证入口语法',syntax.status===0,syntax.stderr);
  check('原scene视觉实现保持',sha(readFileSync(resolve(root,'preview/scene.mjs')))==='989e60022e1006a7015494f2ea6a0fc789dc0596e911c6e8942a18dfc2c1eb32');
  const routes=new Map([['/assets.json',resolve(root,'assets.json')]]);
  for(const target of revision.changedTargets){check(`精确目标 ${target}`,/^src\/main\/resources\/assets\/create_nuclear_industry\/models\/block\/shielded_assembly_station\/parts\/part_[0-7]_(off|on)\.json$/.test(target));const entry=manifest.productionMappings.find(item=>item.target===target);check(`目标有生产映射 ${target}`,!!entry);const candidate=readFileSync(resolve(root,entry.source)),production=readFileSync(resolve(workspace,target));check(`R1候选/生产SHA ${target}`,sha(candidate)===entry.sha256&&sha(production)===entry.sha256);routes.set(`/production/${target.slice(19)}`,resolve(workspace,target));}
  for(const[route,file]of routes){const response=await fetch(base+route),source=readFileSync(file),bytes=new Uint8Array(await response.arrayBuffer());check(`HTTP身份 ${route}`,response.status===200&&sha(bytes)===sha(source),response.status);identity.push({route,path:file,bytes:source.length,sha256:sha(source)});}
  const health=await(await fetch(base+'/health')).json();check('原托管服务身份保持',health.task==='ART-SHIELDED-ASSEMBLY-15B'&&health.pid===16460&&health.root.replaceAll('\\','/')===root.replaceAll('\\','/'),health);
  mkdirSync(report,{recursive:true});writeFileSync(resolve(report,'http-input-identity.json'),JSON.stringify({task:'ART-SHIELDED-ASSEMBLY-15B-R1',passed:true,minecraftVisualVerified:false,webSceneUnchanged:true,checks,identity,service:health},null,2));console.log(`${checks.length}/${checks.length}R1有限检查通过；只核manifest及${revision.changedTargets.length}变化模型的${identity.length}HTTP输入。`);
}catch(error){mkdirSync(report,{recursive:true});writeFileSync(resolve(report,'http-input-failure.json'),JSON.stringify({passed:false,checks,identity,error:String(error.stack??error)},null,2));console.error(error);process.exitCode=1;}
