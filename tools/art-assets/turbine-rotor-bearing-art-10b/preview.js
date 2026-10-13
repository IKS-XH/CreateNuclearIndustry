// 只加载实际候选网格/MTL/PNG；使用相机朝向剔除背面并按深度排序，不翻转内外法向。
(() => {
  const base="candidate/assets/create_nuclear_industry/", meshBase=base+"models/block/turbine/mesh/";
  const canvas=document.querySelector("#scene"),ctx=canvas.getContext("2d"),models={},materials={},images={};
  const state={size:5,view:"side",angle:0,playing:false,explode:false,last:0},q=id=>document.getElementById(id);
  const add=(a,b)=>a.map((v,i)=>v+b[i]),sub=(a,b)=>a.map((v,i)=>v-b[i]),mul=(a,k)=>a.map(v=>v*k),dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0);
  const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],unit=a=>mul(a,1/Math.hypot(...a));
  function basis(view){
    let right,up;
    if(view==="end"){right=[-1,0,0];up=[0,1,0];}
    else if(view==="rear"){right=[1,0,0];up=[0,1,0];}
    else if(view==="insideRear"){right=[-.72,0,.70];up=[.20,.88,-.22];}
    else {right=[.72,0,-.70];up=view==="inside"?[-.20,.88,-.22]:[-.20,-.88,-.22];}
    return {right,up,normal:unit(cross(right,up))};
  }
  function rotate(p,a){const x=p[0]-.5,y=p[1]-.5;return [.5+x*Math.cos(a)-y*Math.sin(a),.5+x*Math.sin(a)+y*Math.cos(a),p[2]];}
  async function loadObj(name,path=meshBase+name+".obj"){
    const response=await fetch(path);if(!response.ok)throw Error(path+" "+response.status);const text=await response.text();
    const points=[],uvs=[],normals=[],faces=[];let material=null,group=null,mtl=null;
    for(const line of text.split(/\r?\n/)){const f=line.trim().split(/\s+/),kind=f.shift();
      if(kind==="v")points.push(f.map(Number));else if(kind==="vt")uvs.push(f.map(Number));else if(kind==="vn")normals.push(f.map(Number));
      else if(kind==="usemtl")material=f[0];else if(kind==="g")group=f[0];else if(kind==="mtllib")mtl=f[0];
      else if(kind==="f"){const ids=f.map(s=>s.split("/").map(Number));faces.push({points:ids.map(i=>points[i[0]-1]),uv:ids.map(i=>uvs[i[1]-1]),normal:normals[ids[0][2]-1],material,group});}
    }
    if(mtl!=="turbine_rotor_bearing_10b.mtl")throw Error("意外的shared MTL引用");
    models[name]=faces;return faces;
  }
  async function loadMaterials(){
    const response=await fetch(meshBase+"turbine_rotor_bearing_10b.mtl");if(!response.ok)throw Error("候选MTL缺失");
    let name=null;for(const line of (await response.text()).split(/\r?\n/)){const f=line.split(/\s+/);if(f[0]==="newmtl")name=f[1];else if(f[0]==="map_Kd")materials[name]=f[1];}
    await Promise.all(Object.entries(materials).map(async([name,reference])=>{
      const path=base+"textures/"+reference.split(":")[1]+".png",image=new Image();image.src=path;await image.decode();images[name]=image;
      // 纯色面可能所有UV均取一个色块中心；实际PNG像素仍是预览的唯一色源。
      const pixels=document.createElement("canvas");pixels.width=image.width;pixels.height=image.height;const sampleContext=pixels.getContext("2d");sampleContext.drawImage(image,0,0);image.pixels=sampleContext.getImageData(0,0,image.width,image.height).data;
      const figure=document.createElement("figure"),label=document.createElement("figcaption"),sample=document.createElement("img");label.textContent=name+" · 16px SVG源";sample.src=path;sample.className="swatch";figure.append(sample,label);q("textures").append(figure);
    }));
  }
  function facePath(points){ctx.beginPath();points.forEach((p,i)=>i?ctx.lineTo(...p):ctx.moveTo(...p));ctx.closePath();}
  function pixelColor(image,uv,shade=1){const x=Math.min(image.width-1,Math.max(0,Math.floor(uv[0]*image.width))),y=Math.min(image.height-1,Math.max(0,Math.floor(uv[1]*image.height))),pixel=(y*image.width+x)*4;return `rgb(${[0,1,2].map(i=>Math.round(image.pixels[pixel+i]*shade)).join(",")})`;}
  function texturedFace(points,uv,image,shade){
    // 锁定NeoForge ObjModel的flip_v=false直接getV(v)，不能在网页额外1-v翻向。
    const s=uv.map(p=>[p[0]*image.width,p[1]*image.height]),a=s[1][0]-s[0][0],b=s[1][1]-s[0][1],c=s[2][0]-s[0][0],d=s[2][1]-s[0][1],det=a*d-b*c;
    // OBJ面的UV在R1均为同一平面的仿射投影；整面只裁剪/提交一次，避免人为三角内缝。
    ctx.save();facePath(points);ctx.clip();
    if(Math.abs(det)>1e-8){const dx1=points[1][0]-points[0][0],dy1=points[1][1]-points[0][1],dx2=points[2][0]-points[0][0],dy2=points[2][1]-points[0][1];
      const A=(dx1*d-dx2*b)/det,B=(dy1*d-dy2*b)/det,C=(-dx1*c+dx2*a)/det,D=(-dy1*c+dy2*a)/det;
      ctx.transform(A,B,C,D,points[0][0]-A*s[0][0]-C*s[0][1],points[0][1]-B*s[0][0]-D*s[0][1]);ctx.imageSmoothingEnabled=false;ctx.filter=`brightness(${shade})`;ctx.drawImage(image,0,0);
    }else{ctx.fillStyle=pixelColor(image,uv[0],shade);ctx.fill();}
    ctx.restore();
  }
  function renderParts(parts,cx,cy,scale,view,centerZ=0){
    const cam=basis(view),queue=[],light=unit([-.45,.72,.52]);
    for(const part of parts)for(const face of models[part.name]){
      let points=face.points.map(p=>part.rotating?rotate(p,state.angle):p.slice());
      points=points.map(p=>[p[0]-.5+(part.x||0),p[1]-.5+(part.y||0),p[2]+(part.z||0)-centerZ]);
      const normal=unit(cross(sub(points[1],points[0]),sub(points[2],points[0])));if(dot(normal,cam.normal)<=1e-8)continue;
      const center=mul(points.reduce(add,[0,0,0]),1/points.length);queue.push({face,points,normal,depth:dot(center,cam.normal)});
    }
    queue.sort((a,b)=>a.depth-b.depth);
    const sharedEdges=new Map(),vertexKey=p=>p.map(v=>v.toFixed(7)).join(","),edgeKey=(a,b)=>[vertexKey(a),vertexKey(b)].sort().join("|");
    for(const item of queue){item.projected=item.points.map(p=>[cx+dot(p,cam.right)*scale,cy-dot(p,cam.up)*scale]);
      // 原B仅圆柱侧壁施加这组离线采光；纹理端面、板面和纯色箱座不另乘统一灰蓝阴影。
      const cylindrical=/^(stepped_hub|rotor_shaft|output_shaft|fixed_outer_race|rotating_inner_race|.*lock.*)$/.test(item.face.group)&&Math.abs(item.normal[2])<1e-8;
      item.shade=cylindrical?.72+.28*Math.max(0,dot(item.normal,light)):1;item.image=images[item.face.material];
      item.solid=item.face.uv.every(uv=>uv.every((v,i)=>Math.abs(v-item.face.uv[0][i])<1e-8));
      if(item.solid){item.color=pixelColor(item.image,item.face.uv[0],item.shade);item.bridges=[];
        for(let i=0;i<item.points.length;i++){const key=edgeKey(item.points[i],item.points[(i+1)%item.points.length]);if(!sharedEdges.has(key))sharedEdges.set(key,[]);sharedEdges.get(key).push({item,index:i});}
      }
    }
    // 只给同色、同平面、成对共享的内部边补亚像素覆盖，不给孔边或外轮廓画线框。
    for(const pair of sharedEdges.values())if(pair.length===2){const [a,b]=pair;if(a.item.color===b.item.color&&dot(a.item.normal,b.item.normal)>.999999){a.item.bridges.push(a.index);b.item.bridges.push(b.index);}}
    for(const item of queue){
      if(item.solid){facePath(item.projected);ctx.fillStyle=item.color;ctx.fill();ctx.strokeStyle=item.color;ctx.lineWidth=.65;
        for(const i of item.bridges){ctx.beginPath();ctx.moveTo(...item.projected[i]);ctx.lineTo(...item.projected[(i+1)%item.projected.length]);ctx.stroke();}
      }else texturedFace(item.projected,item.face.uv,item.image,item.shade);
    }
  }
  function label(text,x,y,size=15,color="#cbd8d8"){ctx.font=`${size}px Segoe UI, Microsoft YaHei`;ctx.fillStyle=color;ctx.textAlign="center";ctx.fillText(text,x,y);}
  function mainParts(){
    const L=state.size,spread=state.explode?.45:0,parts=[];
    for(const end of ["front","rear"]){const z=end==="front"?-spread:L-1+spread;parts.push({name:"output_shaft_"+end,z},{name:"output_shaft_"+end+"_rotating",z,rotating:true});}
    for(let i=1;i<L-1;i++)parts.push({name:"rotor_blades_d"+L,z:i,rotating:true,x:state.explode?.7:0});return parts;
  }
  function draw(){
    ctx.clearRect(0,0,canvas.width,canvas.height);const view=state.view;
    if(view==="inside"||view==="insideRear"){
      const end=view==="inside"?"front":"rear",parts=[{name:"output_shaft_"+end},{name:"output_shaft_"+end+"_rotating",rotating:true,x:state.explode?.85:0}];
      renderParts(parts,610,350,480,view,.5);label((end==="front"?"前":"后")+"端内侧：箱座/外圈固定，轴/内圈旋转",620,54,20);
    }else if(view==="unformed"){
      renderParts([{name:"turbine_output_shaft_unformed"}],270,360,250,"inside",.5);
      renderParts([{name:"turbine_output_shaft_unformed_rear"}],650,360,250,"insideRear",.5);
      renderParts([{name:"turbine_rotor_unformed"}],1040,360,250,"side",.5);
      label("未成型前端完整箱座/轴",270,80,19);label("未成型后端完整箱座/轴",650,80,19);label("完整B转子缩放单件",1040,80,19);
    }else if(view==="blade"){
      renderParts([{name:"rotor_blades_d3",rotating:true}],320,345,220,"side",.5);renderParts([{name:"before",rotating:true}],930,345,220,"side",.5);
      label("正式0.125格 · 两列中心0.30 / 0.70",320,84,20);label("旧稿实际0.34格 · 同源算法对照",930,84,20);
    }else{
      renderParts(mainParts(),640,330,view==="side"?72:85,view,state.size/2);label(`${state.size}档完整动态单格组 · 两端固定箱座`,620,48,20);
      renderParts([{name:"rotor_blades_d3",rotating:true}],260,555,82,"side",.5);renderParts([{name:"before",rotating:true}],960,555,82,"side",.5);
      label("0.125格薄叶片",260,675,17);label("旧0.34格仅比较，不安装",960,675,17);
    }
    if(view!=="unformed"&&view!=="blade"){renderParts([{name:"turbine_rotor_unformed",rotating:false}],1090,167,90,"end",.5);label("完整B未成型单件",1090,225,13);}
    q("status").textContent=`实际加载 ${Object.keys(models).length} OBJ / 1 MTL / ${Object.keys(images).length} PNG · 角度 ${(state.angle*180/Math.PI).toFixed(1)}° · ${state.playing?"演示旋转":"静止"} · 背面剔除启用`;
    canvas.dataset.loaded="true";canvas.dataset.angle=String(state.angle);
  }
  function frame(now){const dt=state.last?Math.min(now-state.last,50):0;state.last=now;if(state.playing){state.angle=(state.angle+dt*.0006)%(Math.PI*2);draw();}requestAnimationFrame(frame);}
  q("size").onchange=e=>{state.size=Number(e.target.value);draw();};q("view").onchange=e=>{state.view=e.target.value;draw();};
  q("play").onclick=e=>{state.playing=!state.playing;e.target.textContent=state.playing?"暂停":"播放";draw();};q("explode").onclick=e=>{state.explode=!state.explode;e.target.textContent="拆件："+(state.explode?"开":"关");draw();};
  q("phase").onclick=()=>{state.angle=Math.PI/4;draw();};
  Promise.all([loadMaterials(),...([3,5,7].map(d=>loadObj("rotor_blades_d"+d))),loadObj("turbine_rotor_unformed"),loadObj("turbine_output_shaft_unformed"),loadObj("turbine_output_shaft_unformed_rear"),...(["front","rear"].flatMap(end=>[loadObj("output_shaft_"+end),loadObj("output_shaft_"+end+"_rotating")])),loadObj("before","comparison/rotor_blades_d3_before.obj")]).then(()=>{draw();requestAnimationFrame(frame);}).catch(e=>{q("error").textContent=e.stack||e.message;});
})();
