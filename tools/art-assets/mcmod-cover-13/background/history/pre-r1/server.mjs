import { createServer } from 'node:http';
import { readFileSync, writeFileSync, existsSync, statSync } from 'node:fs';
import { resolve, relative, extname, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';
import { resolveRequestPath, resolveMimeType } from 'file:///E:/MyMC/NewMod/Create_NuclearIndustry/tools/nuclear-plant-3d/scripts/serve.mjs';
const root=dirname(fileURLToPath(import.meta.url));
const plant='E:/MyMC/NewMod/Create_NuclearIndustry/tools/nuclear-plant-3d';
const used=new Map();
/** 本批仅在127.0.0.1公开只读模型路由；唯一写入口只接收本批PNG及渲染记录。 */
createServer(async(req,res)=>{
  try {
    const route=new URL(req.url,'http://127.0.0.1').pathname;
    if(req.method==='POST'&&route==='/capture') {
      const chunks=[];for await(const chunk of req)chunks.push(chunk);
      const body=JSON.parse(Buffer.concat(chunks).toString('utf8'));
      if(!/^data:image\/png;base64,/.test(body.png))throw new Error('只接受PNG');
      writeFileSync(resolve(root,'reactor-5x5x5.png'),Buffer.from(body.png.split(',')[1],'base64'));
      writeFileSync(resolve(root,'render-evidence.json'),JSON.stringify({...body.evidence,sources:[...used.values()]},null,2));
      res.writeHead(200,{'Content-Type':'application/json'});res.end('{"saved":true}');return;
    }
    if(req.method!=='GET'){res.writeHead(405);res.end();return;}
    let file=route.startsWith('/plant/')?resolveRequestPath(route.slice(6),plant):resolve(root,route==='/'?'index.html':route.slice(1));
    if(!file||(!route.startsWith('/plant/')&&relative(root,file).startsWith('..'))||!existsSync(file)||!statSync(file).isFile()){res.writeHead(404);res.end();return;}
    const data=readFileSync(file);
    if(route.startsWith('/plant/'))used.set(file,{path:file,bytes:data.length,sha256:createHash('sha256').update(data).digest('hex')});
    res.writeHead(200,{'Content-Type':extname(file)==='.mjs'?'text/javascript; charset=utf-8':resolveMimeType(file),'Cache-Control':'no-store'});res.end(data);
  } catch(error){res.writeHead(500);res.end(error.message);}
}).listen(4189,'127.0.0.1',()=>console.log('背景预览 http://127.0.0.1:4189'));
