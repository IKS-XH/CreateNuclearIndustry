import { createServer } from 'node:http';
import { readFileSync, writeFileSync, mkdirSync, existsSync, statSync } from 'node:fs';
import { resolve, relative, extname, dirname, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';
const root = dirname(fileURLToPath(import.meta.url));
const main = resolve(root, '../../../../Create_NuclearIndustry');
const library = resolve(root, '../../../../block-model-renderer/dist');
const vendor = resolve(main, 'tools/nuclear-plant-3d/vendor');
const formal = resolve(main, 'src/main/resources');
const report = resolve(root, '../../../build/reports/art/ART-CENTRIFUGE-14A/preview');
const port = Number(process.argv[2] ?? 4191);
const used = new Map();
const mime = {'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.mjs':'text/javascript; charset=utf-8','.js':'text/javascript; charset=utf-8','.json':'application/json; charset=utf-8','.svg':'image/svg+xml','.png':'image/png','.wasm':'application/wasm'};
/** 只读路由拒绝路径穿越；第三方仅开放本页固定库和正式离心机输入。 */
function locate(route) {
  if (route.includes('\\') || route.includes('\0')) return null;
  let base = root, name = route === '/' ? 'index.html' : route.slice(1);
  if (route.startsWith('/vendor/')) { base=vendor; name=route.slice(8); if(!['three.module.js','three.core.js'].includes(name))return null; }
  if (route.startsWith('/renderer/')) { base=library; name=route.slice(10); if(!['block-model-renderer.min.js','block_model_renderer_bg.wasm'].includes(name))return null; }
  if (route.startsWith('/formal/')) { base=formal; name=route.slice(8); if(!/^assets\/create_nuclear_industry\/(models\/block\/enrichment_centrifuge(?:_upper|_rotor)?\.json|textures\/block\/enrichment_centrifuge_(?:casing|brass|glass|dark)\.png)$/.test(name))return null; }
  if(base===root&&!/^(index\.html|preview\/(?:scene\.mjs|style\.css)|assets\/(?:concepts\.json|[abc]\/[a-z0-9_.-]+\.(?:json|svg)))$/.test(name))return null;
  const file=resolve(base,name), rel=relative(base,file);
  return rel.startsWith(`..${sep}`)||rel==='..'||!existsSync(file)||!statSync(file).isFile()?null:file;
}
/** 写入口仅保存本页固定尺寸观察证据到授权报告目录，无任意输出路径。 */
createServer(async(req,res)=>{
  try {
    const route=decodeURIComponent(new URL(req.url,'http://127.0.0.1').pathname);
    if(req.method==='POST'&&route==='/capture'){
      const chunks=[];let size=0;for await(const chunk of req){size+=chunk.length;if(size>12*1024*1024)throw new Error('证据载荷过大');chunks.push(chunk);}
      const body=JSON.parse(Buffer.concat(chunks).toString('utf8'));
      if(!Array.isArray(body.images)||body.images.length>4)throw new Error('最多保存四稿观察图');
      mkdirSync(report,{recursive:true});
      for(const item of body.images){if(!/^(a|b|c|original)$/.test(item.id)||!/^data:image\/png;base64,/.test(item.png))throw new Error('证据标识错误');writeFileSync(resolve(report,`${item.id}-latest.png`),Buffer.from(item.png.split(',')[1],'base64'));}
      writeFileSync(resolve(report,'browser-evidence.json'),JSON.stringify({...body.evidence,sources:[...used.values()]},null,2));
      res.writeHead(200,{'Content-Type':'application/json'});res.end('{"saved":true}');return;
    }
    if(req.method!=='GET'){res.writeHead(405);res.end();return;}
    if(route==='/health'){res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify({task:'ART-CENTRIFUGE-14A',pid:process.pid,port,executable:process.execPath}));return;}
    const file=locate(route);if(!file){res.writeHead(404);res.end('本页资源不存在或路径未开放');return;}
    const bytes=readFileSync(file);used.set(file,{path:file,bytes:bytes.length,sha256:createHash('sha256').update(bytes).digest('hex')});
    res.writeHead(200,{'Content-Type':mime[extname(file)]??'application/octet-stream','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(bytes);
  }catch(error){res.writeHead(500,{'Content-Type':'text/plain; charset=utf-8'});res.end(error.message);}
}).listen(port,'127.0.0.1',()=>console.log(`离心机14A预览 http://127.0.0.1:${port} PID=${process.pid}`));
