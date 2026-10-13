// 将闭合部件面按相机深度排序后投影，并让叶轮材质随转子示意角度移动。
(() => {
  const NS="http://www.w3.org/2000/svg";
  const designs={
    A:{title:"A · 宽弦斜叶",count:8,chord:.34,sweep:-.19,hub:.43,style:"split",rows:1,depth:.17,texture:"A"},
    B:{title:"B · 分级短叶盘",count:12,chord:.21,sweep:.09,hub:.52,style:"box",rows:2,depth:.12,texture:"B"},
    C:{title:"C · 稀疏双列叶轮",count:6,chord:.39,sweep:.26,hub:.34,style:"rollers",rows:2,depth:.15,texture:"C"}
  };
  const sizes={3:{r:1.1875,inner:1.3125,len:3},5:{r:2.1875,inner:2.3125,len:5},7:{r:3.1875,inner:3.3125,len:7}};
  const state={design:"A",size:5,view:"side",explode:false,playing:true,slow:false,speed:.7,angle:0,last:0};
  const q=id=>document.getElementById(id), node=(tag,a={})=>{const e=document.createElementNS(NS,tag);Object.entries(a).forEach(([k,v])=>e.setAttribute(k,String(v)));return e;};
  let svg=null, project=null, scale=56, cam="side", faces=[], outlines=[], defsNode=null, textureSerial=0, targetPrefix="preview";
  function setup(target){
    target.replaceChildren();const defs=node("defs");
    target.append(defs);defsNode=defs;textureSerial=0;targetPrefix=target.id||"preview";
  }
  function point(x,y,z){return {x,y,z};}
  function add(a,b){return point(a.x+b.x,a.y+b.y,a.z+b.z);}
  function sub(a,b){return point(a.x-b.x,a.y-b.y,a.z-b.z);}
  function mul(a,k){return point(a.x*k,a.y*k,a.z*k);}
  function dot(a,b){return a.x*b.x+a.y*b.y+a.z*b.z;}
  function cross(a,b){return point(a.y*b.z-a.z*b.y,a.z*b.x-a.x*b.z,a.x*b.y-a.y*b.x);}
  function unit(a){const length=Math.hypot(a.x,a.y,a.z);return mul(a,1/length);}
  function cameraBasis(view){
    const right=view==="end"?point(-1,0,0):point(.72,0,-.70),up=view==="end"?point(0,1,0):view==="inside"?point(-.20,.88,-.22):point(-.20,-.88,-.22);
    return {right,up,normal:unit(cross(right,up))};
  }
  function rotate(p,a){return point(p.x*Math.cos(a)-p.y*Math.sin(a),p.x*Math.sin(a)+p.y*Math.cos(a),p.z);}
  function projection(cx,cy,len,view){
    const basis=cameraBasis(view);
    return p=>{const relative=point(p.x,p.y,view==="end"||view==="inside"?p.z:p.z-len/2);return [cx+dot(relative,basis.right)*scale,cy-dot(relative,basis.up)*scale];};
  }
  function depth(p){return dot(p,cameraBasis(cam).normal);}
  function texturePattern(asset,origin,u,v){
    const id=`${targetPrefix}-uv${textureSerial++}`,o=project(origin),pu=project(point(origin.x+u.x,origin.y+u.y,origin.z+u.z)),pv=project(point(origin.x+v.x,origin.y+v.y,origin.z+v.z));
    const a=(pu[0]-o[0])/16,b=(pu[1]-o[1])/16,c=(pv[0]-o[0])/16,d=(pv[1]-o[1])/16;
    const p=node("pattern",{id,width:16,height:16,patternUnits:"userSpaceOnUse",patternTransform:`matrix(${a.toFixed(5)} ${b.toFixed(5)} ${c.toFixed(5)} ${d.toFixed(5)} ${o[0].toFixed(3)} ${o[1].toFixed(3)})`});
    const href=asset==="shell"?"../../../src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_casing_07_panel.png":`sources/textures/${asset}.svg`;
    p.append(node("image",{href,width:16,height:16,preserveAspectRatio:"none",style:"image-rendering:pixelated"}));defsNode.append(p);return `url(#${id})`;
  }
  function polygonNormal(points){return cross(sub(points[1],points[0]),sub(points[2],points[0]));}
  function face(points,fill,stroke="#252b2d",width=.8,opacity=1,twoSided=false){
    const center=points.reduce((s,p)=>add(s,p),point(0,0,0));
    const d=depth(mul(center,1/points.length)),normal=polygonNormal(points);
    faces.push({d,points,fill,stroke,width,opacity,normal,twoSided});
  }
  function edgeLoop(points,stroke,width,normalSign){const center=points.reduce((s,p)=>add(s,p),point(0,0,0));outlines.push({d:depth(mul(center,1/points.length)),points:[...points,points[0]],stroke,width,normal:point(0,0,normalSign)});}
  function shadeHex(color,factor){if(!/^#[0-9a-f]{6}$/i.test(color))return color;const n=parseInt(color.slice(1),16),r=Math.min(255,Math.round((n>>16&255)*factor)),g=Math.min(255,Math.round((n>>8&255)*factor)),b=Math.min(255,Math.round((n&255)*factor));return `#${[r,g,b].map(v=>v.toString(16).padStart(2,"0")).join("")}`;}
  function quad(a,b,c,d,fill,stroke,width,opacity=1,twoSided=false){face([a,b,c,d],fill,stroke,width,opacity,twoSided);}
  function emit(){
    const drawQueue=[...faces.map(f=>({...f,kind:"face"})),...outlines.map(f=>({...f,kind:"line"}))].sort((a,b)=>a.d-b.d);
    const view=cameraBasis(cam);
    for(const f of drawQueue){if(!f.twoSided&&dot(f.normal,view.normal)<=1e-8)continue;const pts=f.points.map(project).map(p=>p.map(v=>v.toFixed(2)).join(",")).join(" ");svg.append(f.kind==="line"?node("polyline",{points:pts,fill:"none",stroke:f.stroke,"stroke-width":f.width,"stroke-linejoin":"round"}):node("polygon",{points:pts,fill:f.fill,stroke:f.stroke,"stroke-width":f.width,opacity:f.opacity,"stroke-linejoin":"round"}));}
    faces=[];outlines=[];
  }
  function label(x,y,str,size=12,color="#dce2df",anchor="middle"){const t=node("text",{x,y,fill:color,"font-size":size,"text-anchor":anchor,"font-family":"Segoe UI, Microsoft YaHei, sans-serif"});t.textContent=str;svg.append(t);}
  function cylinder(radius,z0,z1,fill,angle=0,segments=16,inner=0,asset=null,cx=0,cy=0){
    const lo=Math.min(z0,z1),hi=Math.max(z0,z1);
    const outerRim=[],innerRim=[];
    for(let i=0;i<segments;i++){
      const a0=2*Math.PI*i/segments+angle,a1=2*Math.PI*(i+1)/segments+angle;
      const p0=point(cx+radius*Math.cos(a0),cy+radius*Math.sin(a0),lo),p1=point(cx+radius*Math.cos(a1),cy+radius*Math.sin(a1),lo),p2=point(cx+radius*Math.cos(a1),cy+radius*Math.sin(a1),hi),p3=point(cx+radius*Math.cos(a0),cy+radius*Math.sin(a0),hi);
      const mid=(a0+a1)/2,n=point(Math.cos(mid),Math.sin(mid),0),lighting=.72+.28*Math.max(0,dot(n,unit(point(-.45,.72,.52)))),local=asset?texturePattern(asset,p0,point(0,0,1),point(-Math.sin(mid),Math.cos(mid),0)):shadeHex(fill,lighting);
      outerRim.push(p0);quad(p0,p1,p2,p3,local,"none",0);
      if(inner>0){const i0=point(cx+inner*Math.cos(a0),cy+inner*Math.sin(a0),lo),i1=point(cx+inner*Math.cos(a1),cy+inner*Math.sin(a1),lo),i2=point(cx+inner*Math.cos(a1),cy+inner*Math.sin(a1),hi),i3=point(cx+inner*Math.cos(a0),cy+inner*Math.sin(a0),hi);innerRim.push(i0);quad(i0,i3,i2,i1,shadeHex("#41494b",lighting),"none",0);}
    }
    edgeLoop(outerRim,"#282e30",.7,-1);const outerHi=outerRim.map(p=>point(p.x,p.y,hi));edgeLoop(outerHi,"#282e30",.7,1);
    if(inner>0){edgeLoop(innerRim,"#242a2c",.65,-1);edgeLoop(innerRim.map(p=>point(p.x,p.y,hi)),"#242a2c",.65,1);}
    if(inner>0){annulus(radius,inner,lo,fill,angle,segments,asset,-1,cx,cy);annulus(radius,inner,hi,fill,angle,segments,asset,1,cx,cy);}
    else{diskFaces(radius,lo,cx,cy,fill,angle,segments,asset,-1);diskFaces(radius,hi,cx,cy,fill,angle,segments,asset,1);}
  }
  function diskFaces(radius,z,cx,cy,fill,angle=0,segments=16,asset=null,normalSign=1){
    const local=asset?texturePattern(asset,point(cx,cy,z),point(Math.cos(angle),Math.sin(angle),0),point(-Math.sin(angle),Math.cos(angle),0)):fill;
    const rim=[];for(let i=0;i<segments;i++){const a0=2*Math.PI*i/segments+angle,a1=2*Math.PI*(i+1)/segments+angle,p0=point(cx+radius*Math.cos(a0),cy+radius*Math.sin(a0),z),p1=point(cx+radius*Math.cos(a1),cy+radius*Math.sin(a1),z),center=point(cx,cy,z);rim.push(p0);face(normalSign>0?[center,p0,p1]:[center,p1,p0],local,"none",0);}edgeLoop(rim,"#303739",.7,normalSign);
  }
  function annulus(radius,inner,z,fill,angle=0,segments=20,asset=null,normalSign=1,cx=0,cy=0){
    const local=asset?texturePattern(asset,point(cx,cy,z),point(Math.cos(angle),Math.sin(angle),0),point(-Math.sin(angle),Math.cos(angle),0)):fill;
    const outer=[],innerLine=[];for(let i=0;i<segments;i++){const a0=2*Math.PI*i/segments+angle,a1=2*Math.PI*(i+1)/segments+angle,p0=point(cx+radius*Math.cos(a0),cy+radius*Math.sin(a0),z),p1=point(cx+radius*Math.cos(a1),cy+radius*Math.sin(a1),z),p2=point(cx+inner*Math.cos(a1),cy+inner*Math.sin(a1),z),p3=point(cx+inner*Math.cos(a0),cy+inner*Math.sin(a0),z);outer.push(p0);innerLine.push(p3);quad(...(normalSign>0?[p0,p1,p2,p3]:[p0,p3,p2,p1]),local,"none",0);}edgeLoop(outer,"#303638",.75,normalSign);edgeLoop(innerLine,"#242a2c",.7,normalSign);
  }
  function orientedQuad(points,target,fill,stroke,width){
    const normal=polygonNormal(points);quad(...(dot(normal,target)>=0?points:[...points].reverse()),fill,stroke,width);
  }
  function plateMesh(z0,z1,front,fill){
    // 端板两面围绕真实方孔闭合，外面不透明，内侧承载轴承套筒。
    const h=.19,lo=Math.min(z0,z1),hi=Math.max(z0,z1),outerZ=front?lo:hi,innerZ=front?hi:lo;
    const ring=[
      [[-.5,-h],[-h,-h],[-h,h],[-.5,h]],[[h,-h],[.5,-h],[.5,h],[h,h]],
      [[-.5,-.5],[-h,-.5],[-h,-h],[-.5,-h]],[[h,-.5],[.5,-.5],[.5,-h],[h,-h]],
      [[-.5,h],[-h,h],[-h,.5],[-.5,.5]],[[h,h],[.5,h],[.5,.5],[h,.5]],
      [[-h,-.5],[h,-.5],[h,-h],[-h,-h]],[[-h,h],[h,h],[h,.5],[-h,.5]]
    ];
    const outerSign=front?-1:1;
    for(const strip of ring){const p=strip.map(([x,y])=>point(x,y,outerZ));orientedQuad(p,point(0,0,outerSign),fill,"#aab4b2",1.1);orientedQuad(p.map(v=>point(v.x,v.y,innerZ)),point(0,0,-outerSign),"#4c5658","#252b2d",.8);}
    const outerSides=[
      [[-.5,-.5],[-h,-.5]],[[-h,-.5],[h,-.5]],[[h,-.5],[.5,-.5]],
      [[.5,-.5],[.5,-h]],[[.5,-h],[.5,h]],[[.5,h],[.5,.5]],
      [[.5,.5],[h,.5]],[[h,.5],[-h,.5]],[[-h,.5],[-.5,.5]],
      [[-.5,.5],[-.5,h]],[[-.5,h],[-.5,-h]],[[-.5,-h],[-.5,-.5]]
    ],hole=[[-h,-h],[h,-h],[h,h],[-h,h]];
    for(const [a,b] of outerSides){const mid=point((a[0]+b[0])/2,(a[1]+b[1])/2,(lo+hi)/2);orientedQuad([point(a[0],a[1],lo),point(b[0],b[1],lo),point(b[0],b[1],hi),point(a[0],a[1],hi)],point(mid.x,mid.y,0),"#626d6f","#303638",.75);}
    for(let i=0;i<4;i++){
      const j=(i+1)%4,c=hole[i],d=hole[j],hm=point((c[0]+d[0])/2,(c[1]+d[1])/2,(lo+hi)/2);
      orientedQuad([point(c[0],c[1],lo),point(d[0],d[1],lo),point(d[0],d[1],hi),point(c[0],c[1],hi)],point(-hm.x,-hm.y,0),"#424b4d","#252b2d",.65);
    }
    return {outerZ,innerZ};
  }
  function shellMesh(R,L,view,explode){
    const oct=[];for(let i=0;i<8;i++){const a=2*Math.PI*i/8+Math.PI/8;oct.push(point(R*Math.cos(a),R*Math.sin(a),0));}
    for(let i=0;i<8;i++){
      const j=(i+1)%8,a=oct[i],b=oct[j],normal=polygonNormal([a,b,point(b.x,b.y,1)]),visible=dot(normal,cameraBasis(cam).normal)>1e-8;
      if(explode&&visible)continue;
      const shellFill=texturePattern("shell",point(a.x,a.y,0),point(b.x-a.x,b.y-a.y,0),point(0,0,1));
      if(view==="window"&&visible){
        const p0=point(a.x,a.y,0),p1=point(b.x,b.y,0),p2=point(b.x,b.y,L),p3=point(a.x,a.y,L);
        const w0=point(a.x*.18+b.x*.82,a.y*.18+b.y*.82,.6),w1=point(a.x*.82+b.x*.18,a.y*.82+b.y*.18,.6),w2=point(a.x*.82+b.x*.18,a.y*.82+b.y*.18,L-.6),w3=point(a.x*.18+b.x*.82,a.y*.18+b.y*.82,L-.6);
        // 观察窗是真正留空的面片，四周仍保留实体机壳与法兰。
        quad(p0,p1,w1,w0,shellFill,"#c2cac7",1.25);quad(w3,w2,p2,p3,shellFill,"#c2cac7",1.25);
        quad(p0,w0,w3,p3,shellFill,"#c2cac7",1.25);quad(w1,p1,p2,w2,shellFill,"#c2cac7",1.25);
        quad(w0,w1,w2,w3,"rgba(108,158,164,.24)","#d0d7d4",1.8,.96,true);
      } else quad(point(a.x,a.y,0),point(b.x,b.y,0),point(b.x,b.y,L),point(a.x,a.y,L),shellFill,"#707b7d",1.05,.98);
    }
    const innerR=R-.1875;
    for(const z of [0,L])for(let i=0;i<8;i++){
      const a=oct[i],b=oct[(i+1)%8],ai=point(a.x*innerR/R,a.y*innerR/R,z),bi=point(b.x*innerR/R,b.y*innerR/R,z),outer=[a,b,bi,ai].map(p=>point(p.x,p.y,z));
      orientedQuad(outer,point(0,0,z===0?-1:1),"#566164","#879193",.8);
    }
    // 端部八棱束带保持当前疏线机壳的整体比例。
    for(const z of [0,L-.13])for(let i=0;i<8;i++){const a=oct[i],b=oct[(i+1)%8];quad(point(a.x,a.y,z),point(b.x,b.y,z),point(b.x,b.y,z+.13),point(a.x,a.y,z+.13),"#596365","#aab3b1",1.1);}
  }
  function supportPlate(z,front,key,style,exploded,angle=0){
    const shift=exploded?(front?-.45:.45):0,Z=z+shift,t=.1875,innerZ=Z+(front?t:-t),outerZ=Z;
    const plateFill=texturePattern(`${key}_bearing`,point(-.5,-.5,outerZ),point(1,0,0),point(0,1,0));
    plateMesh(outerZ,innerZ,front,plateFill);
    // 外板保持完整；轴承座与转动内圈只位于端板内侧。
    const bearingZ=innerZ+(front?.006:-.006),bearingEnd=innerZ+(front?.075:-.075);
    cylinder(.27,Math.min(innerZ,bearingEnd),Math.max(innerZ,bearingEnd),"#626d6f",0,24,.155);
    annulus(.27,.155,bearingZ,"#657173",0,24,null,front?1:-1);
    annulus(.155,.14,bearingZ+(front?.008:-.008),"#9ba5a4",angle,24,null,front?1:-1);
    cylinder(.14,Math.min(Z,innerZ),Math.max(Z,innerZ),"#858f91",angle,20);
    if(style==="split"){
      const detailZ=bearingZ+(front?.012:-.012);orientedQuad([point(-.26,-.018,detailZ),point(.26,-.018,detailZ),point(.26,.018,detailZ),point(-.26,.018,detailZ)],point(0,0,front?1:-1),"#bd8659","#6c4c34",.6);
    } else if(style==="box"){
      for(const x of [-.40,.40])orientedQuad([point(x-.045,-.34,bearingZ),point(x+.045,-.34,bearingZ),point(x+.045,.34,bearingZ),point(x-.045,.34,bearingZ)],point(0,0,front?1:-1),"#899395","#343b3d",1);
      annulus(.265,.24,bearingZ+(front?.015:-.015),"#b27a4e",0,24,null,front?1:-1);
    } else {
      for(let i=0;i<8;i++){const a0=2*Math.PI*i/8,cx=.207*Math.cos(a0),cy=.207*Math.sin(a0);cylinder(.034,bearingZ-.055,bearingZ+.055,"#b18b67",a0,10,0,null,cx,cy);}
    }
    for(let i=0;i<4;i++){const a0=Math.PI/4+i*Math.PI/2;const p=point(.39*Math.cos(a0),.39*Math.sin(a0),outerZ+(front?-.003:.003));diskMark(p,.025,"#c8cfcd",front?-1:1);}
  }
  function diskMark(p,r,color,normalSign){
    const points=[];for(let i=0;i<10;i++){const a=2*Math.PI*i/10;points.push(point(p.x+r*Math.cos(a),p.y+r*Math.sin(a),p.z));}
    face(normalSign>0?points:[...points].reverse(),color,"#252b2d",.65);
  }
  function rotorMesh(key,R,L,angle,explode,onlyOne=false,xyScale=1){
    const d=designs[key],ratio=(R/1.1875)*xyScale,actualR=R*xyScale,hub=d.hub*ratio,inner=(key==="C"?.20:.19)*ratio,stations=[];
    for(let z=1;z<=L-2;z++)stations.push(z+.5);if(!stations.length)stations.push(L/2);
    if(cam==="end"||onlyOne)stations.splice(0,stations.length,stations[Math.floor(stations.length/2)]);
    for(const center of stations){
      const a=angle,base=center-.5,hub0=base+.22,hub1=base+.78;
      if(key==="B"){
        cylinder(hub,hub0+.05,hub1-.05,"#657173",a,16,hub*.34);
        cylinder(hub*.82,base+.13,hub0+.05,"#7e898b",a,16,hub*.34);cylinder(hub*.82,hub1-.05,base+.87,"#596467",a,16,hub*.34);
        annulus(hub,hub*.34,hub0+.05,"#667274",a,20);annulus(hub,hub*.34,hub1-.05,"#9ba5a3",a,20);
      } else if(key==="C"){
        cylinder(hub,hub0,hub1,"#657173",a,16,inner);
        annulus(hub,inner,hub0,"#626d6f",a,20);annulus(hub,inner,hub1,"#919b9c",a,20);
        for(let i=0;i<6;i++){
          const theta=2*Math.PI*i/6,ri=inner*1.03,ro=hub*.96;
          const pts=[point(ri*Math.cos(theta-.08),ri*Math.sin(theta-.08),hub0),point(ro*Math.cos(theta-.12),ro*Math.sin(theta-.12),hub0),point(ro*Math.cos(theta-.12),ro*Math.sin(theta-.12),hub1),point(ri*Math.cos(theta-.08),ri*Math.sin(theta-.08),hub1)].map(p=>rotate(p,a));
          quad(...pts,"#899395","#303638",1.1);
        }
      } else {
        cylinder(hub,hub0,hub1,"#657173",a,16,inner);
        annulus(hub,inner,hub0,"#687477",a,20);annulus(hub,inner,hub1,"#a8b1af",a,20);
      }
      // 三方案的轴向外包均落在单格Z=.13..87内。
      if(key!=="B"){
        cylinder(hub*.86,base+.13,hub0,"#576265",a,16,inner);
        cylinder(hub*.86,hub1,base+.87,"#576265",a,16,inner);
      }
      const rows=d.rows===1?[[base+.13,base+.87]]:[[base+.13,base+.47],[base+.53,base+.87]];
      for(let row=0;row<rows.length;row++){
        const separation=explode?(row===0?-.36:.36):0,z0=rows[row][0]+separation,z1=rows[row][1]+separation;
        const chord=d.chord*ratio,half=chord/2,rStart=hub*.94,rEnd=Math.sqrt(Math.max(hub*hub,actualR*actualR-half*half))*.985;
        for(let i=0;i<d.count;i++){
          const theta=2*Math.PI*i/d.count+(row?Math.PI/d.count:0),sweep=d.sweep;
          const mk=(r,side,z)=>{const aa=theta+sweep*(r/actualR);return rotate(point(r*Math.cos(aa)-side*Math.sin(aa),r*Math.sin(aa)+side*Math.cos(aa),z),a);};
          const p=[mk(rStart,-half*.55,z0),mk(rEnd,-half,z0),mk(rEnd,half,z0),mk(rStart,half*.55,z0)],back=p.map(v=>point(v.x,v.y,z1));
          const radial=theta+sweep*.7+a,u=point(Math.cos(radial),Math.sin(radial),0),v=point(-Math.sin(radial),Math.cos(radial),0),uv=texturePattern(`${key}_rotor`,p[0],u,v);
          face(back,uv,"#252b2d",.85);
          for(let k=0;k<4;k++)quad(p[k],p[(k+1)%4],back[(k+1)%4],back[k],k===1?"#a8b1af":k===2?"#454e50":"#333b3d","#242a2c",.8);
          face([...p].reverse(),uv,"#303739",.85);
        }
      }
      // 不对称小锁圈真实放在轮毂偏置位置，并随同一旋转矩阵运行。
      const mark=rotate(point(hub*.72,hub*.18,center),a);cylinder(Math.max(.018,.038*ratio),mark.z-.025,mark.z+.025,"#b27a4e",a,8,0,null,mark.x,mark.y);
    }
  }
  function shaftMesh(L,angle){cylinder(.14,0,L,"#697476",angle,20);}
  function renderScene(target,key,sizeKey,view,angle,explode){
    const s=sizes[sizeKey],d=designs[key],vb=target.viewBox.baseVal,W=vb.width||1000,H=vb.height||620;scale=view==="inside"?245:56;svg=target;cam=view;faces=[];
    const centerX=view==="end"||view==="inside"?W*.37:W*.47,centerY=H*.52;project=projection(centerX,centerY,s.len,view);
    setup(target);
    if(view==="inside"){
      supportPlate(.0,true,key,d.style,false,angle);cylinder(.14,.20,.92,"#697476",angle,20);emit();
      label(W*.37,H*.14,"端板内侧 · 固定外座与旋转内圈",14);
    } else {
      shellMesh(s.inner/Math.cos(Math.PI/8),s.len,view,explode);supportPlate(.0,true,key,d.style,explode,angle);supportPlate(s.len,false,key,d.style,explode,angle);
      shaftMesh(s.len,angle);rotorMesh(key,s.r,s.len,angle,explode,false);emit();
    }
    target.setAttribute("data-design",key);target.setAttribute("data-size",String(sizeKey));target.setAttribute("data-angle",angle.toFixed(4));target.setAttribute("data-playing",String(state.playing));
    if(view==="end")label(W*.78,H*.13,"转子端面",14);
    else label(W*.79,H*.12,`${sizeKey===3?"小":sizeKey===5?"中":"大"} · ${sizeKey}×${sizeKey}×${sizeKey}`,14);
    // 未成型单件复用同一叶片网格并按R0.42缩小。
    const oldScale=scale,oldProject=project,oldCam=cam,ox=W*.84,oy=H*.76;scale=56;cam="end";project=projection(ox,oy,1,"end");faces=[];rotorMesh(key,1.1875,1,angle,false,true,.42/1.1875);emit();scale=oldScale;project=oldProject;cam=oldCam;label(ox,oy+34,"未成型单件",11,"#b7c0be");
    if(explode){label(W*.11,H*.9,"固定端板与外环",12,"#d0d7d4","start");label(W*.42,H*.9,"贯穿轴 / 旋转内圈",12,"#d0d7d4","start");label(W*.75,H*.9,"轮毂与叶片",12,"#d0d7d4","start");}
    label(W*.06,H*.95,"离线设计稿 · 转速为演示",11,"#9eaaa8","start");
  }
  function drawCards(){
    for(const key of Object.keys(designs)){
      const target=q(`card${key}`);svg=target;setup(target);faces=[];scale=49;cam="end";project=projection(115,77,1,"end");rotorMesh(key,1.1875,1,0,false,true);emit();label(115,150,"静止叶轮端面",11,"#aeb8b6");
      faces=[];scale=96;cam="side";project=projection(316,77,1,"side");supportPlate(0,true,key,designs[key].style,false,0);shaftMesh(.58,0);emit();label(316,150,"端部轴承",11,"#aeb8b6");
    }
  }
  function updateNotes(){const d=designs[state.design];q("designTitle").textContent=d.title;q("designText").textContent=d.title+"："+(state.design==="A"?"宽叶后掠，开放轮毂与分体重型端座。":state.design==="B"?"同一转子内双层短叶盘，阶梯盘体与窄锁圈。":"双列错相宽叶、开敞轮毂与外露滚子端座。");}
  function frame(now){if(!state.last)state.last=now;const dt=Math.min(50,now-state.last);state.last=now;if(state.playing)state.angle+=dt*.001*state.speed*(state.slow?.25:1);renderScene(q("viewport"),state.design,state.size,state.view,state.angle,state.explode);requestAnimationFrame(frame);}
  q("design").value=state.design;q("size").value=String(state.size);q("design").addEventListener("change",e=>{state.design=e.target.value;updateNotes();});q("size").addEventListener("change",e=>state.size=Number(e.target.value));
  document.querySelectorAll("[data-view]").forEach(b=>b.addEventListener("click",()=>{state.view=b.dataset.view;document.querySelectorAll("[data-view]").forEach(x=>x.setAttribute("aria-pressed",String(x===b)));}));
  q("explode").addEventListener("click",e=>{state.explode=!state.explode;e.currentTarget.setAttribute("aria-pressed",String(state.explode));e.currentTarget.textContent=state.explode?"合装":"拆件";});
  q("play").addEventListener("click",e=>{state.playing=!state.playing;e.currentTarget.textContent=state.playing?"暂停":"播放";});q("slow").addEventListener("click",e=>{state.slow=!state.slow;e.currentTarget.textContent=`慢放：${state.slow?"开":"关"}`;e.currentTarget.setAttribute("aria-pressed",String(state.slow));});q("speed").addEventListener("change",e=>state.speed=Number(e.target.value));
  function edgeAudit(items){
    const counts=new Map(),coord=v=>Math.abs(v)<1e-8?0:Number(v.toFixed(6)),key=p=>`${coord(p.x)},${coord(p.y)},${coord(p.z)}`;
    for(const f of items)for(let i=0;i<f.points.length;i++){const a=key(f.points[i]),b=key(f.points[(i+1)%f.points.length]),edge=a<b?`${a}|${b}`:`${b}|${a}`;counts.set(edge,(counts.get(edge)||0)+1);}
    const bad=[...counts.entries()].filter(([,n])=>n!==2);return {edgeCount:counts.size,unpairedOrOverused:bad.length,badSample:bad.slice(0,4).map(([edge,n])=>({edge,count:n}))};
  }
  window.runTurbineR1Diagnostic=()=>{
    const saved={svg,project,cam,faces,outlines,scale,defsNode,targetPrefix,textureSerial},testSvg=node("svg");svg=testSvg;scale=1;cam="inside";project=projection(0,0,1,"inside");setup(testSvg);faces=[];outlines=[];
    const plane=(z,fill,reverse=false)=>{const p=[point(-1,-1,z),point(1,-1,z),point(1,1,z),point(-1,1,z)];face(reverse?p.reverse():p,fill,"none",0);};
    plane(0,"far");plane(1,"near");plane(2,"backface",true);emit();const order=[...testSvg.querySelectorAll("polygon")].map(p=>p.getAttribute("fill"));
    const nearFarPass=order.join(",")==="far,near";faces=[];plateMesh(0,.1875,true,"front-plate");const frontFaces=faces.slice(),frontClosed=edgeAudit(frontFaces);
    const frontOutside=frontFaces.some(f=>f.points.every(p=>Math.abs(p.z)<1e-8)&&f.normal.z<0),frontInside=frontFaces.some(f=>f.points.every(p=>Math.abs(p.z-.1875)<1e-8)&&f.normal.z>0);
    faces=[];plateMesh(4.8125,5,false,"rear-plate");const rearFaces=faces.slice(),rearClosed=edgeAudit(rearFaces);
    const rearOutside=rearFaces.some(f=>f.points.every(p=>Math.abs(p.z-5)<1e-8)&&f.normal.z>0),rearInside=rearFaces.some(f=>f.points.every(p=>Math.abs(p.z-4.8125)<1e-8)&&f.normal.z<0);
    faces=[];cylinder(.27,.2,.4,"race",0,24,.155);const sleeveClosed=edgeAudit(faces);
    const result={nearFarBackfaceCull:{order,pass:nearFarPass},frontPlate:{...frontClosed,outerFaceNormal:frontOutside,innerFaceNormal:frontInside},rearPlate:{...rearClosed,outerFaceNormal:rearOutside,innerFaceNormal:rearInside},hollowRaceSleeve:sleeveClosed,pass:nearFarPass&&frontClosed.unpairedOrOverused===0&&rearClosed.unpairedOrOverused===0&&sleeveClosed.unpairedOrOverused===0&&frontOutside&&frontInside&&rearOutside&&rearInside};
    svg=saved.svg;project=saved.project;cam=saved.cam;faces=saved.faces;outlines=saved.outlines;scale=saved.scale;defsNode=saved.defsNode;targetPrefix=saved.targetPrefix;textureSerial=saved.textureSerial;return result;
  };
  drawCards();updateNotes();
  if(location.hash==="#r1-check"){
    const result=document.createElement("pre");result.id="r1-check-result";result.textContent=JSON.stringify(window.runTurbineR1Diagnostic(),null,2);document.body.append(result);
  } else requestAnimationFrame(frame);
})();
