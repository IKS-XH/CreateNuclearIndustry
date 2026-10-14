import { createServer } from 'node:http';
import { readFileSync, writeFileSync, mkdirSync, existsSync, statSync, realpathSync } from 'node:fs';
import { resolve, dirname, extname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';
const root=dirname(fileURLToPath(import.meta.url));
const main=resolve(root,'../../../../Create_NuclearIndustry');
const formal=resolve(root,'../../../src/main/resources');
// 15B安装会替换原机模型；15A对照只读取完整路径旧模型备份，原PNG保持原路径。
const originalModels=resolve(root,'../shielded-assembly-art-15b/assets/backup/src/main/resources');
const library=resolve(root,'../../../../block-model-renderer/dist');
const report=resolve(root,'../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15A/preview');
const port=Number(process.argv[2]??4193);
if(!Number.isInteger(port)||port<1024||port>65535)throw new Error('端口不合法');
const used=new Map();
const mime={'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.mjs':'text/javascript; charset=utf-8','.js':'text/javascript; charset=utf-8','.json':'application/json; charset=utf-8','.svg':'image/svg+xml','.png':'image/png','.wasm':'application/wasm'};
/** 仅精确路径进入只读白名单；不开放目录枚举、脚本源、任意refs或历史设备。 */
function locate(route){
  if(route.includes('\\')||route.includes('\0')||route.includes('..'))return null;
  let file;
  if(route==='/')file=resolve(root,'index.html');
  else if(/^\/preview\/(scene\.mjs|style\.css)$/.test(route))file=resolve(root,route.slice(1));
  else if(/^\/assets\/(concepts\.json|models\/[abc]\/[a-z0-9_-]+\.json|textures\/[abc]\/[a-z0-9_-]+\.svg)$/.test(route))file=resolve(root,route.slice(1));
  else if(/^\/vendor\/(three\.module\.js|three\.core\.js)$/.test(route))file=resolve(main,'tools/nuclear-plant-3d/vendor',route.slice(8));
  else if(/^\/renderer\/(block-model-renderer\.min\.js|block_model_renderer_bg\.wasm)$/.test(route))file=resolve(library,route.slice(10));
  else if(/^\/preview\/refs\/(reference\.json|assets\/create\/models\/block\/shaft_half\.json|assets\/create\/textures\/block\/axis(?:_top)?\.png|assets\/minecraft\/models\/block\/block\.json)$/.test(route))file=resolve(root,route.slice(1));
  else if(/^\/formal\/assets\/create_nuclear_industry\/(models\/block\/shielded_assembly_station\/(parts\/part_[0-7]_(off|on)|partials\/(left_arm|right_arm|fixture))\.json|textures\/block\/shielded_assembly_multiblock_(lead|steel|brass|window|top|bottom|deck|port|lamp_off|lamp_on)\.png)$/.test(route))file=resolve(route.includes('/models/')?originalModels:formal,route.slice(8));
  return file&&existsSync(file)&&statSync(file).isFile()?file:null;
}
/** 证据只允许完整A/B/C三图及固定JSON；先验证整份载荷，再写本批目录。 */
function validateCapture(body){
  if(!Array.isArray(body.images)||body.images.length!==3||body.images.map(item=>item.id).join(',')!=='a,b,c')throw new Error('仅保存A/B/C三稿');
  if(body.evidence?.task!=='ART-SHIELDED-ASSEMBLY-15A'||body.evidence.ready!==true)throw new Error('缺少本批已加载快照');
  return body.images.map(item=>{if(typeof item.png!=='string'||!/^data:image\/png;base64,[A-Za-z0-9+/]+=*$/.test(item.png))throw new Error('PNG载荷错误');const bytes=Buffer.from(item.png.slice(22),'base64');if(bytes.length<24||!bytes.subarray(0,8).equals(Buffer.from([137,80,78,71,13,10,26,10])))throw new Error('PNG签名错误');const width=bytes.readUInt32BE(16),height=bytes.readUInt32BE(20);if(width<32||height<32||width>4096||height>4096)throw new Error('观察图尺寸超出范围');return{id:item.id,bytes};});
}
createServer(async(req,res)=>{
  try{
    const route=decodeURIComponent(new URL(req.url,'http://127.0.0.1').pathname);
    if(req.method==='POST'&&route==='/capture'){
      if(req.headers.origin!==`http://127.0.0.1:${port}`||!String(req.headers['content-type']).startsWith('application/json')){res.writeHead(403);res.end('仅允许本页保存');return;}
      const chunks=[];let count=0;for await(const chunk of req){count+=chunk.length;if(count>12*1024*1024)throw new Error('证据载荷过大');chunks.push(chunk);}
      const body=JSON.parse(Buffer.concat(chunks).toString('utf8')),images=validateCapture(body);
      mkdirSync(report,{recursive:true});for(const item of images)writeFileSync(resolve(report,`${item.id}-latest.png`),item.bytes);
      writeFileSync(resolve(report,'browser-evidence.json'),JSON.stringify({...body.evidence,sources:[...used.values()]},null,2));
      res.writeHead(200,{'Content-Type':'application/json'});res.end('{"saved":true}');return;
    }
    if(req.method!=='GET'){res.writeHead(405);res.end();return;}
    if(route==='/health'){res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify({task:'ART-SHIELDED-ASSEMBLY-15A',pid:process.pid,port,executable:process.execPath,cwd:process.cwd(),root}));return;}
    const file=locate(route);if(!file){res.writeHead(404);res.end('本页路径未开放');return;}
    const bytes=readFileSync(file);used.set(file,{path:realpathSync(file),bytes:bytes.length,sha256:createHash('sha256').update(bytes).digest('hex')});
    res.writeHead(200,{'Content-Type':mime[extname(file)]??'application/octet-stream','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(bytes);
  }catch(error){res.writeHead(400,{'Content-Type':'text/plain; charset=utf-8'});res.end(error.message);}
}).listen(port,'127.0.0.1',()=>console.log(`屏蔽装配室15A http://127.0.0.1:${port} PID=${process.pid} cwd=${process.cwd()}`));
