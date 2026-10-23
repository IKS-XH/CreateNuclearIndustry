import {createServer} from 'node:http';
import {readFileSync,writeFileSync,mkdirSync,existsSync,statSync,realpathSync} from 'node:fs';
import {resolve,dirname,extname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
const root=dirname(fileURLToPath(import.meta.url)),tree=resolve(root,'../../../..'),production=resolve(tree,'src/main/resources/assets/create_nuclear_industry'),library='E:/MyMC/NewMod/block-model-renderer',vendor='E:/MyMC/NewMod/Create_NuclearIndustry/tools/nuclear-plant-3d/vendor';
const report=resolve(tree,'build/reports/art/ART-ACCIDENT-CORIUM-22/preview'),port=4210,origin=`http://127.0.0.1:${port}`,used=new Map(),task='ART-ACCIDENT-CORIUM-22';
export const productionPaths=['blockstates/reactor_corium.json',...['reactor_corium','hot_reactor_corium','molten_corium'].map(n=>`models/block/${n}.json`),...['reactor_corium','accident_fuel_waste'].map(n=>`models/item/${n}.json`),...['block/reactor_corium','block/hot_reactor_corium','fluid/molten_corium_still','fluid/molten_corium_flow'].flatMap(n=>[`textures/${n}.png`,`textures/${n}.png.mcmeta`]),'textures/item/accident_fuel_waste.png'];
const native=['block/block','block/cube','block/cube_all','item/generated'].map(p=>`models/${p}.json`);
const mime={'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.mjs':'text/javascript; charset=utf-8','.js':'text/javascript; charset=utf-8','.json':'application/json; charset=utf-8','.mcmeta':'application/json; charset=utf-8','.png':'image/png','.wasm':'application/wasm','.txt':'text/plain; charset=utf-8'};
/** 固定来源白名单；不开放整个生产树、目录枚举、脚本源码或用户指定磁盘路径。 */
function locate(route){
  if(route.includes('..')||route.includes('\\')||route.includes('\0'))return null;
  if(route==='/')return resolve(root,'index.html');
  if(['/scene.mjs','/animation.mjs','/style.css'].includes(route))return resolve(root,route.slice(1));
  if(route.startsWith('/production/')&&productionPaths.includes(route.slice(12)))return resolve(production,route.slice(12));
  if(route.startsWith('/native/')&&(native.includes(route.slice(8))||route==='/native/source-index.json'))return resolve(root,'native',route.slice(8));
  if(/^\/vendor\/three\.(module|core)\.js$/.test(route))return resolve(vendor,route.slice(8));
  if(/^\/renderer\/(block-model-renderer\.min\.js|block_model_renderer_bg\.wasm)$/.test(route))return resolve(library,'dist',route.slice(10));
  if(route==='/licenses/renderer.txt')return resolve(library,'LICENSE');if(route==='/licenses/three.txt')return resolve(vendor,'LICENSE-three.txt');return null;
}
createServer(async(req,res)=>{
  try{
    const route=decodeURIComponent(new URL(req.url,origin).pathname);
    if(req.method==='POST'&&route==='/capture'){
      if(req.headers.origin!==origin||!String(req.headers['content-type']).startsWith('application/json')){res.writeHead(403);res.end('仅本页可保存');return;}
      let size=0;const chunks=[];for await(const c of req){size+=c.length;if(size>24*1024*1024)throw new Error('捕获过大');chunks.push(c);}const body=JSON.parse(Buffer.concat(chunks).toString('utf8'));
      if(body.evidence?.task!==task||!body.evidence.ready||body.evidence.errors?.length)throw new Error('缺少无错误ready快照');
      if(body.images?.map(x=>x.id).join(',')!=='cold,hot,still,flow,waste')throw new Error('捕获名称不符');
      const records={};mkdirSync(report,{recursive:true});
      for(const image of body.images){if(image.source!=='current-canvas.toDataURL'||!/^data:image\/png;base64,[A-Za-z0-9+/]+=*$/.test(image.png))throw new Error('非当前PNG画布');const bytes=Buffer.from(image.png.slice(22),'base64');if(bytes.length<24||!bytes.subarray(0,8).equals(Buffer.from([137,80,78,71,13,10,26,10])))throw new Error('PNG签名不符');const w=bytes.readUInt32BE(16),h=bytes.readUInt32BE(20);if(w<16||h<16||w>4096||h>4096)throw new Error('尺寸越界');writeFileSync(resolve(report,image.id+'.png'),bytes);records[image.id]={bytes:bytes.length,width:w,height:h,sha256:createHash('sha256').update(bytes).digest('hex')};}
      writeFileSync(resolve(report,'overview.json'),JSON.stringify({...body.evidence,captures:records,serverInputs:[...used.values()]},null,2));res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify({saved:true,files:Object.keys(records)}));return;
    }
    if(req.method!=='GET'){res.writeHead(405);res.end();return;}if(route==='/health'){res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify({task,pid:process.pid,port,root,tree,production,library}));return;}if(route==='/favicon.ico'){res.writeHead(204);res.end();return;}
    const file=locate(route);if(!file||!existsSync(file)||!statSync(file).isFile()){res.writeHead(404);res.end('路径未开放或素材未交付');return;}const bytes=readFileSync(file);used.set(file,{path:realpathSync(file),bytes:bytes.length,sha256:createHash('sha256').update(bytes).digest('hex')});res.writeHead(200,{'Content-Type':mime[extname(file)]??'application/octet-stream','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(bytes);
  }catch(e){res.writeHead(400,{'Content-Type':'text/plain; charset=utf-8'});res.end(e.message);}
}).listen(port,'127.0.0.1',()=>console.log(`${task} ${origin} PID=${process.pid}`));
