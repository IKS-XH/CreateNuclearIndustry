import {createServer} from 'node:http';
import {readFileSync,writeFileSync,mkdirSync,existsSync,statSync} from 'node:fs';
import {resolve,dirname,extname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
const root=dirname(fileURLToPath(import.meta.url)),main=resolve(root,'../../../../Create_NuclearIndustry'),library=resolve(root,'../../../../block-model-renderer/dist');
const candidate=resolve(root,'assets/candidate/src/main/resources'),production=resolve(root,'../../../src/main/resources'),references=resolve(root,'../shielded-assembly-concepts-15/preview/refs');
const report=resolve(root,'../../../build/reports/art/ART-SHIELDED-ASSEMBLY-15B/preview'),port=Number(process.argv[2]??4194),used=new Map();
if(!Number.isInteger(port)||port<1024||port>65535)throw new Error('端口不合法');
const allowedResource=/^assets\/create_nuclear_industry\/(models\/block\/(shielded_assembly_station\/(parts\/part_[0-7]_(off|on)|partials\/(left_arm|right_arm|fixture))|shielded_assembly_station_item)\.json|textures\/block\/shielded_assembly_15c_(lead|steel|machined|fastener|brass|window|lamp_off|lamp_on|deck)\.png)$/;
const mime={'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.mjs':'text/javascript; charset=utf-8','.js':'text/javascript; charset=utf-8','.json':'application/json; charset=utf-8','.png':'image/png','.wasm':'application/wasm'};
/** 候选与生产只开放同一29项精确资源；第三方参考逐条复用15A，不开放任意目录。 */
function locate(route){if(route.includes('..')||route.includes('\\')||route.includes('\0'))return null;let file;
  if(route==='/')file=resolve(root,'index.html');
  else if(route==='/assets.json')file=resolve(root,'assets.json');
  else if(/^\/preview\/(scene\.mjs|style\.css)$/.test(route))file=resolve(root,route.slice(1));
  else if(/^\/(candidate|production)\//.test(route)){const mode=route.split('/')[1],name=route.slice(mode.length+2);if(allowedResource.test(name))file=resolve(mode==='candidate'?candidate:production,name);}
  else if(/^\/refs\/(reference\.json|assets\/create\/models\/block\/shaft_half\.json|assets\/create\/textures\/block\/axis(?:_top)?\.png|assets\/minecraft\/models\/block\/block\.json)$/.test(route))file=resolve(references,route.slice(6));
  else if(/^\/vendor\/(three\.module\.js|three\.core\.js)$/.test(route))file=resolve(main,'tools/nuclear-plant-3d/vendor',route.slice(8));
  else if(/^\/renderer\/(block-model-renderer\.min\.js|block_model_renderer_bg\.wasm)$/.test(route))file=resolve(library,route.slice(10));
  return file&&existsSync(file)&&statSync(file).isFile()?file:null;
}
/** 固定machine/item两图，先完整验证再写入本批报告；无任意输出路径。 */
function capture(body){if(body.evidence?.task!=='ART-SHIELDED-ASSEMBLY-15B'||body.evidence.ready!==true||!Array.isArray(body.images)||body.images.map(item=>item.id).join(',')!=='machine,item')throw new Error('需要本批整机/物品完整证据');return body.images.map(item=>{if(typeof item.png!=='string'||!/^data:image\/png;base64,[A-Za-z0-9+/]+=*$/.test(item.png))throw new Error('PNG载荷错误');const bytes=Buffer.from(item.png.slice(22),'base64');if(bytes.length<24||!bytes.subarray(0,8).equals(Buffer.from([137,80,78,71,13,10,26,10])))throw new Error('PNG签名错误');for(const offset of[16,20])if(bytes.readUInt32BE(offset)<32||bytes.readUInt32BE(offset)>4096)throw new Error('尺寸错误');return{id:item.id,bytes};});}
createServer(async(req,res)=>{try{const route=decodeURIComponent(new URL(req.url,'http://127.0.0.1').pathname);
  if(req.method==='POST'&&route==='/capture'){if(req.headers.origin!==`http://127.0.0.1:${port}`||!String(req.headers['content-type']).startsWith('application/json')){res.writeHead(403);res.end('仅本页保存');return;}const chunks=[];let count=0;for await(const chunk of req){count+=chunk.length;if(count>12*1024*1024)throw new Error('载荷过大');chunks.push(chunk);}const body=JSON.parse(Buffer.concat(chunks).toString('utf8')),images=capture(body);mkdirSync(report,{recursive:true});for(const image of images)writeFileSync(resolve(report,`${image.id}-latest.png`),image.bytes);writeFileSync(resolve(report,'browser-evidence.json'),JSON.stringify({...body.evidence,sources:[...used.values()]},null,2));res.writeHead(200,{'Content-Type':'application/json'});res.end('{"saved":true}');return;}
  if(req.method!=='GET'){res.writeHead(405);res.end();return;}if(route==='/health'){res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify({task:'ART-SHIELDED-ASSEMBLY-15B',pid:process.pid,executable:process.execPath,cwd:process.cwd(),root,port}));return;}
  const file=locate(route);if(!file){res.writeHead(404);res.end('未开放路径');return;}const bytes=readFileSync(file);used.set(file,{path:file,bytes:bytes.length,sha256:createHash('sha256').update(bytes).digest('hex')});res.writeHead(200,{'Content-Type':mime[extname(file)]??'application/octet-stream','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(bytes);
}catch(error){res.writeHead(400,{'Content-Type':'text/plain; charset=utf-8'});res.end(error.message);}}).listen(port,'127.0.0.1',()=>console.log(`屏蔽装配室15B http://127.0.0.1:${port} PID=${process.pid} cwd=${process.cwd()}`));
