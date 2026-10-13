import { THREE, renderer, loadBlockAsset, loadPartialAsset } from '/plant/src/assets/model-loader.js';
import { createPackSource } from '/plant/src/assets/pack-source.js';
import { buildSurfaceFaces, createConnectedTransform } from '/plant/src/assets/connected-textures.js';
const status=document.querySelector('#status');
const colorDiagnostics=[];
const diagnosticMode=new URLSearchParams(location.search).get('mode')==='color-diagnostic';
/** OBJ适配器没有写color属性；仅在真实OBJ缺少该属性时补中性白乘数，不替换贴图或灯光。 */
function adaptNeutralObjColor(group,modelId){
  group.traverse(node=>{
    if(!node.isMesh)return;
    const geometry=node.geometry,materials=[node.material].flat();
    if(geometry.hasAttribute('color'))return;
    for(const material of materials)colorDiagnostics.push({modelId,mesh:node.name,hasColorBefore:false,hasColorAfter:true,vertexColors:material.vertexColors,color:material.color?.toArray(),materialType:material.type,mapPresent:!!material.map,mapWidth:material.map?.image?.width,mapHeight:material.map?.image?.height,neutralAdaptation:true});
    const colors=new Float32Array(geometry.getAttribute('position').count*3);colors.fill(1);
    geometry.setAttribute('color',new THREE.BufferAttribute(colors,3));
  });
}
/** 静态满液体包络覆盖整个3³内部，实际原生冷却液第0tick帧；偏移避免与内壳共面。 */
async function createFullCoolant(assets){
  const textureId='create_nuclear_industry:fluid/compound_coolant_still';
  const source=await renderer.readTexture('assets/create_nuclear_industry/textures/fluid/compound_coolant_still.png',assets);
  if(!source)throw new Error('缺少正式compound_coolant_still贴图');
  const frame=source.frameAt(0),canvas=new OffscreenCanvas(frame.width,frame.height);canvas.getContext('2d').drawImage(frame,0,0);
  const texture=new THREE.CanvasTexture(canvas);texture.colorSpace=THREE.SRGBColorSpace;texture.magFilter=THREE.NearestFilter;texture.minFilter=THREE.NearestFilter;
  texture.wrapS=texture.wrapT=THREE.RepeatWrapping;texture.repeat.set(3,3);
  const material=new THREE.MeshBasicMaterial({map:texture,transparent:true,opacity:.36,depthWrite:false,depthTest:true,side:THREE.FrontSide});
  const mesh=new THREE.Mesh(new THREE.BoxGeometry(2.98,2.98,2.98),material);mesh.position.set(2.5,2.5,2.5);mesh.renderOrder=2;mesh.name='FullCoolantPresentation';
  return {mesh,evidence:{textureId,frameTick:0,frameSize:[frame.width,frame.height],bounds:{min:[1.01,1.01,1.01],max:[3.99,3.99,3.99]},alpha:.36,renderOrder:2,depthWrite:false,depthTest:true,side:'FrontSide',textureRepeat:[3,3]}};
}
/** 固定宣传布局的完整125格；X/Z为水平格坐标，Y向上，不读取Minecraft世界或热工状态。 */
export function buildCoverLayout() {
  const blocks=[],partials=[];const prefix='create_nuclear_industry:';
  for(let y=0;y<5;y++)for(let z=0;z<5;z++)for(let x=0;x<5;x++){
    const interior=x>0&&x<4&&y>0&&y<4&&z>0&&z<4;
    let name=interior?(x===2?'air':'reactor_fuel_rod'):'reactor_casing';
    if(z===0&&x>0&&x<4&&y>0&&y<4)name='reactor_window';
    if(y===4&&x>0&&x<4&&z>0&&z<4)name=x===2?'control_rod_drive':'reactor_refueling_port';
    if(x===0&&y===2&&z===2)name='reactor_instrument_port';
    if(z===4&&y===2&&x===1)name='reactor_cold_port';
    if(z===4&&y===2&&x===3)name='reactor_hot_port';
    blocks.push({id:name==='air'?'minecraft:air':prefix+name,pos:[x,y,z],properties:{}});
    if(interior&&x===2)partials.push({modelId:prefix+'block/reactor_animation/control_rod_shaft',pos:[x,y,z]});
    if(interior&&x!==2)partials.push({modelId:prefix+'block/reactor_animation/fuel_rod_glow',pos:[x,y,z],glow:true});
  }
  for(let z=1;z<4;z++)partials.push({modelId:prefix+'block/reactor_animation/control_rod_head',pos:[2,3,z]});
  return {bounds:{min:[0,0,0],size:[5,5,5]},blocks,partials};
}
try {
  const manifest=await (await fetch('/plant/asset-manifest.json')).json();
  const mapping=await (await fetch('/plant/.cache/ct-mapping.json')).json();
  const packs=manifest.layers.map(layer=>createPackSource({...layer,baseUrl:'/plant'+layer.baseUrl}));
  const assets=await renderer.prepareAssets(packs,{version:'1.21.1',cache:true});
  const layout=buildCoverLayout(),scene=new THREE.Scene(),reactor=new THREE.Group();scene.add(reactor);
  const shell=layout.blocks.filter(cell=>cell.id!=='minecraft:air');
  const faces=buildSurfaceFaces({equipmentKind:'reactor',bounds:layout.bounds,cells:shell,owner:'cover13'});
  for(const [i,cell] of shell.entries()) {
    status.textContent=`装配真实模型 ${i+1}/${shell.length}`;
    const handle=await loadBlockAsset({assets,id:cell.id,properties:cell.properties,position:cell.pos,transform:createConnectedTransform({cell,faces,mapping,equipmentKind:'reactor'})});
    if(cell.id.endsWith(':reactor_fuel_rod'))adaptNeutralObjColor(handle.group,cell.id);
    reactor.add(handle.group);
  }
  for(const partial of layout.partials){
    const handle=await loadPartialAsset({assets,modelId:partial.modelId,position:partial.pos});
    adaptNeutralObjColor(handle.group,partial.modelId);
    if(partial.glow)handle.group.traverse(node=>{if(node.isMesh){node.renderOrder=diagnosticMode?0:3;for(const material of [node.material].flat()){material.emissive.set(0xffffff);material.emissiveMap=material.map;material.emissiveIntensity=.85;material.opacity=.65;material.transparent=true;material.depthWrite=false;}}});
    reactor.add(handle.group);
  }
  let coolant=null;
  if(!diagnosticMode){coolant=await createFullCoolant(assets);reactor.add(coolant.mesh);}
  scene.add(new THREE.AmbientLight(0xc7d9e4,1.5));
  const key=new THREE.DirectionalLight(0xffeed7,3.0);key.position.set(-3,12,-8);scene.add(key);
  const fill=new THREE.DirectionalLight(0xb5d9ff,1.2);fill.position.set(12,7,0);scene.add(fill);
  const camera=new THREE.OrthographicCamera(-4.35,4.35,4.35,-4.35,.1,100);
  camera.position.set(12,8.5,-13);camera.lookAt(2.5,2.5,2.5);
  const webgl=new THREE.WebGLRenderer({alpha:true,antialias:true,preserveDrawingBuffer:true});webgl.setSize(2048,2048);webgl.setPixelRatio(1);webgl.setClearColor(0x000000,0);webgl.outputColorSpace=THREE.SRGBColorSpace;
  webgl.toneMapping=THREE.ACESFilmicToneMapping;webgl.toneMappingExposure=1.0;
  document.querySelector('#view').append(webgl.domElement);webgl.render(scene,camera);
  const evidence={width:2048,height:2048,renderer:'block-model-renderer 2.20.0 / Three 0.180.0',minecraft:'1.21.1',layout,camera:{projection:'orthographic',position:camera.position.toArray(),target:[2.5,2.5,2.5],extent:4.35},lighting:{ambient:1.5,key:3,fill:1.2},presentation:{mode:diagnosticMode?'color-diagnostic':'r1-full-coolant',fixedGlowAlpha:.65,glowTexture:'actual fuel_rod_glow',coolant:coolant?.evidence??false,gameScreenshot:false},colorDiagnostics,drawCalls:webgl.info.render.calls,triangles:webgl.info.render.triangles};
  /** 保存仅提交本批canvas和固定参数；服务器没有任意写路径接口。 */
  async function save(){const response=await fetch('/capture',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({png:webgl.domElement.toDataURL('image/png'),evidence})});if(!response.ok)throw new Error(await response.text());status.textContent='透明2048×2048背景已保存';}
  document.querySelector('#save').disabled=false;document.querySelector('#save').onclick=save;await save();
} catch(error){status.textContent='渲染失败';document.querySelector('#error').textContent=error.stack;console.error(error);}
