import {readFileSync,writeFileSync,mkdirSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..'),report=resolve(root,'../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15B/preview'),checks=[];
const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
function check(name,passed){checks.push({name,passed});if(!passed)throw new Error(name);}
/** 本批尚未启动服务时，仅核手写语法、候选/备份身份；不启动HTTP或游戏，不重复几何/PNG导出。 */
try{const manifest=JSON.parse(readFileSync(resolve(root,'assets.json'),'utf8'));for(const file of['server.mjs','preview/scene.mjs','preview/verify.mjs','preview/static-check.mjs']){const result=spawnSync(process.execPath,['--check',resolve(root,file)],{encoding:'utf8'});check(`语法 ${file}`,result.status===0);}
  check('C契约SHA绑定',manifest.selectedConcept==='c'&&manifest.approvedContractSha256==='b666789912cce985a7363e71d7dbb8c81cd294b490f86dc41ac3ab8bb79e41a2');
  for(const entry of manifest.productionMappings)check(`候选SHA ${entry.target}`,sha(readFileSync(resolve(root,entry.source)))===entry.sha256);
  for(const entry of manifest.backupModels)check(`原机备份SHA ${entry.target}`,sha(readFileSync(resolve(root,entry.backup)))===entry.sha256);
  mkdirSync(report,{recursive:true});writeFileSync(resolve(report,'static-checks.json'),JSON.stringify({passed:true,runtimeStarted:false,checks},null,2));console.log(`${checks.length}/${checks.length}非服务检查通过；服务/安装/HTTP/GPU/构建均未计通过。`);
}catch(error){mkdirSync(report,{recursive:true});writeFileSync(resolve(report,'static-failure.json'),JSON.stringify({passed:false,checks,error:String(error.stack??error)},null,2));console.error(error);process.exitCode=1;}
