import {readFileSync,writeFileSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
import assert from 'node:assert/strict';
import * as THREE from '../../../../../Create_NuclearIndustry/tools/nuclear-plant-3d/vendor/three.module.js';
import {facingMatrix} from './motion.mjs';
const root=dirname(fileURLToPath(import.meta.url)),file=resolve(root,'scene.mjs'),bytes=readFileSync(file),source=bytes.toString('utf8'),sha=value=>createHash('sha256').update(value).digest('hex');
const state={facing:'north'},angles={north:0,east:-Math.PI/2,south:Math.PI,west:Math.PI/2};
// 仅取本仓当前两纯相机函数，避免在Node执行DOM/模型加载或另复制一套目标计算。
const functions=new Function('THREE','state','angles','facingMatrix',source.slice(source.indexOf('function cameraTarget(unit)'),source.indexOf('function camera(unit)'))+'; return {cameraTarget,clipPlane};')(THREE,state,angles,facingMatrix);
const cases=[];for(const[facing,target]of Object.entries({north:[0,1,0],east:[-1,1,0],south:[-1,1,-1],west:[0,1,-1]})){state.facing=facing;const actual=functions.cameraTarget({id:'machine'}),plane=functions.clipPlane();actual.toArray().forEach((value,index)=>assert.ok(Math.abs(value-target[index])<1e-10));assert.ok(Math.abs(plane.distanceToPoint(actual))<1e-10);assert.deepEqual(functions.cameraTarget({id:'item'}).toArray(),[0,0,0]);cases.push({facing,target:actual.toArray(),expected:target,clip:{normal:plane.normal.toArray(),constant:plane.constant},centerPlaneDistance:plane.distanceToPoint(actual)});}
const response=await fetch('http://127.0.0.1:4195/preview/scene.mjs'),httpBytes=Buffer.from(await response.arrayBuffer());assert.equal(response.status,200);assert.equal(sha(httpBytes),sha(bytes));const health=await(await fetch('http://127.0.0.1:4195/health')).json();assert.equal(health.pid,101936);
writeFileSync(resolve(root,'../../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15C/preview/scene-camera-identity.json'),JSON.stringify({task:'ART-SHIELDED-ASSEMBLY-15C',passed:true,firstSourceSha256:sha(readFileSync(resolve(root,'../../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15C/preview/camera-first/scene.mjs'))),source:{path:file,bytes:bytes.length,sha256:sha(bytes),httpSha256:sha(httpBytes)},health,cases,scope:'仅scene窄修；原52HTTP身份/Java/动作/rig/测试保持，GPU由负责人reload复看'},null,2));console.log(JSON.stringify({passed:true,sceneSha256:sha(bytes),facings:cases.length,httpFiles:1}));
