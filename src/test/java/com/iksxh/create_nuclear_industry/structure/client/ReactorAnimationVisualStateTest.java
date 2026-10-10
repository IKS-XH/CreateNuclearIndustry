package com.iksxh.create_nuclear_industry.structure.client;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.iksxh.create_nuclear_industry.structure.client.ReactorAnimationVisualState.*;
/** 用实际数值和体积核对客户端核心；不启动世界，不复制生产算法。 */
class ReactorAnimationVisualStateTest {
 /** 三列三层的真实descriptor：空/燃料/控制各一列，容量只对应六个合法空气格。 */
 static com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor liquidDescriptor(boolean onlyFuel){
  var p=new net.minecraft.core.BlockPos(0,1,1);var columns=new ArrayList<com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor.Column>();
  for(int x=1;x<=3;x++){
   var cap=new net.minecraft.core.BlockPos(x,4,1);var body=List.of(cap.below(),cap.below(2),cap.below(3));
   columns.add(onlyFuel || x==2?new com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor.FuelColumn(cap,body,true,6)
      :x==1?new com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor.EmptyColumn(cap,body)
      :new com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor.ControlRodColumn(cap,body,.5,.5,false));
  }
  return new com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor("minecraft:overworld",p,new UUID(0,2),1,1,1,1,true,
      net.minecraft.core.BlockPos.ZERO,new net.minecraft.core.BlockPos(4,4,2),3000,0,6000,0,columns);
 }
 @Test void fuelEnvelopeUsesCanonicalLayersAndRemovesPartitions(){
  clear();var owner=id(301);var descriptor=liquidDescriptor(false);
  assertEquals(6,descriptor.coolantSpace().size());
  var partial=mesh(owner,descriptor,.5,0);
  assertEquals(22,partial.size(),"连续三列1.5格高的外包络不能保留燃料隔墙");
  var fuelTop=partial.stream().filter(f->f.cell().equals(new Cell(2,2,1)) && f.side()==Side.UP).findFirst().orElseThrow();
  assertEquals(.5,fuelTop.high(),1e-12,"新增燃料面积不能把原半层液位压低");
  assertTrue(partial.stream().noneMatch(f->f.cell().y()==3));
  assertTrue(partial.stream().noneMatch(f->f.cell().x()==2 && (f.side()==Side.WEST || f.side()==Side.EAST)),"相邻空/控制与燃料的内部界面必须撤下");
  var full=mesh(owner,descriptor,1,1);assertEquals(30,full.size());
  assertTrue(full.stream().anyMatch(f->f.cell().equals(new Cell(2,3,1)) && f.side()==Side.UP && f.high()==1));
  assertTrue(mesh(owner,descriptor,0,2).isEmpty());assertEquals(6,descriptor.coolantSpace().size());clear();
 }
 @Test void fuelFaceSamplesNearestLegalCellWithFreshWorldLight(){
  clear();var faces=mesh(id(302),liquidDescriptor(false),1,0);
  var candidate=faces.stream().filter(f->f.cell().equals(new Cell(2,2,1)) && f.side()==Side.NORTH).findFirst();
  assertTrue(candidate.isPresent(),"燃料外包络必须存在，之后才能核合法采光");var face=candidate.orElseThrow();
  var queried=new ArrayList<net.minecraft.core.BlockPos>();var pose=new com.mojang.blaze3d.vertex.PoseStack();
  for(int sky:new int[]{12,7}){
   var vertex=new VertexRecord();int light=net.minecraft.client.renderer.LightTexture.pack(0,sky);
   ReactorInternalRenderer.submitFace(vertex,pose.last(),face,net.minecraft.core.BlockPos.ZERO,p->{queried.add(p);return light;},0,4);
   assertEquals(new net.minecraft.core.BlockPos(1,2,1),queried.getLast(),"等距时按X/Y/Z字典序固定选择同层合法空气格，不能采实体");
   assertTrue(vertex.entries.stream().allMatch(v->v.light==light),"缓存只保存采光坐标，当前世界光每次提交重读");
  }clear();
 }
 @Test void actualEnvelopeSubmissionInsetsOnlyNormalAndRetainsSeamsAlphaAndWinding(){
  var pose=new com.mojang.blaze3d.vertex.PoseStack();var base=net.minecraft.core.BlockPos.ZERO;double epsilon=1./1024;
  for(var side:Side.values()){
   var vertex=new VertexRecord();ReactorInternalRenderer.submitFace(vertex,pose.last(),new Face(new Cell(2,2,1),side,0,1),base,p->0,0,4);
   assertEquals(4,vertex.entries.size());
   for(var v:vertex.entries){
    assertTrue(v.a>=114 && v.a<=143,"液体保持可辨半透明，不能沿用过淡顶点或变成不透明");
    switch(side){
     case EAST -> {assertEquals(3-epsilon,v.x,1e-7);assertTrue(v.z==1 || v.z==2);}
     case WEST -> {assertEquals(2+epsilon,v.x,1e-7);assertTrue(v.z==1 || v.z==2);}
     case SOUTH -> {assertEquals(2-epsilon,v.z,1e-7);assertTrue(v.x==2 || v.x==3);}
     case NORTH -> {assertEquals(1+epsilon,v.z,1e-7);assertTrue(v.x==2 || v.x==3);}
     case UP -> {assertEquals(3-epsilon,v.y,1e-7);assertTrue(v.x==2 || v.x==3);assertTrue(v.z==1 || v.z==2);}
     case DOWN -> {assertEquals(2+epsilon,v.y,1e-7);assertTrue(v.x==2 || v.x==3);assertTrue(v.z==1 || v.z==2);}
    }
    if(side==Side.UP || side==Side.DOWN)assertEquals(114,v.a);
   }
   var a=vertex.entries.get(0);var b=vertex.entries.get(1);var c=vertex.entries.get(2);
   double wx=(b.y-a.y)*(c.z-a.z)-(b.z-a.z)*(c.y-a.y),wy=(b.z-a.z)*(c.x-a.x)-(b.x-a.x)*(c.z-a.z),wz=(b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x);
   assertTrue(wx*a.nx+wy*a.ny+wz*a.nz>0,"真实提交绕序应与外法线一致");
  }
  var left=new VertexRecord();var right=new VertexRecord();
  ReactorInternalRenderer.submitFace(left,pose.last(),new Face(new Cell(1,1,1),Side.UP,.5,.5),base,p->0,0,4);
  ReactorInternalRenderer.submitFace(right,pose.last(),new Face(new Cell(2,1,1),Side.UP,.5,.5),base,p->0,0,4);
  var l=left.entries.stream().filter(v->v.x==2).toList();var r=right.entries.stream().filter(v->v.x==2).toList();
  assertEquals(2,l.size());assertEquals(2,r.size());for(var a:l)assertTrue(r.stream().anyMatch(b->a.x==b.x && a.y==b.y && a.z==b.z && a.u==b.u && a.v==b.v));
 }
 @Test void envelopeCacheIncludesFuelAndInvalidationDoesNotKeepDryLayers(){
  clear();var owner=id(303);var descriptor=liquidDescriptor(false);var first=mesh(owner,descriptor,.5,0);
  assertSame(first,mesh(owner,descriptor,.5,1));invalidate(owner);assertNotSame(first,mesh(owner,descriptor,.5,2));
  assertTrue(mesh(id(304),liquidDescriptor(true),1,3).isEmpty(),"没有合法空气液位的全燃料结构不能凭fill虚构层");clear();
 }
 static Identity id(long session){return new Identity(session,"minecraft:overworld",0,new UUID(0,1),1);}
 @Test void heatUsesUsabilityAndBoundedMonotonicAlpha(){
  assertEquals(0,glow(true,0));assertEquals(0,glow(false,100));assertEquals(0,glow(true,Double.NaN));
  assertEquals(.385,glow(true,6),1e-12);assertTrue(glow(true,1)<glow(true,6));assertTrue(glow(true,1e9)<.65);
 }
 @Test void inventoryDoesNotOverflowAndZeroRetracts(){
  assertEquals(1,fill(Long.MAX_VALUE,Long.MAX_VALUE,1));assertEquals(.5,hotRatio(Long.MAX_VALUE,Long.MAX_VALUE));
  assertEquals(0,fill(4,2,0));assertEquals(0,fill(0,0,100));assertEquals(.25,fill(10,15,100));
 }
 @Test void volumeFillsActualUnequalLayerAreasBottomUp(){
  var a=new Cell(0,0,0);var b=new Cell(1,0,0);var c=new Cell(0,1,0);
  var m=allocate(Set.of(a,b,c),.8);assertEquals(1,m.get(a));assertEquals(1,m.get(b));assertEquals(.4,m.get(c),1e-12);
  assertEquals(2.4,m.values().stream().mapToDouble(Double::doubleValue).sum(),1e-12);
  assertTrue(allocate(Set.of(a),0).isEmpty());
 }
 @Test void unionRemovesSharedFacesButKeepsExposedHeightStrip(){
  var a=new Cell(0,0,0);var b=new Cell(1,0,0);
  var f=faces(Map.of(a,1.,b,1.));assertEquals(10,f.size());assertFalse(f.stream().anyMatch(x->x.cell().equals(a)&&x.side()==Side.EAST));
  var partial=faces(Map.of(a,1.,b,.4));var strip=partial.stream().filter(x->x.cell().equals(a)&&x.side()==Side.EAST).findFirst().orElseThrow();
  assertEquals(.4,strip.low());assertEquals(1,strip.high());
  assertEquals(10,faces(Map.of(a,1.,new Cell(0,1,0),1.)).size());
 }
 @Test void rodInterpolatesActualForFourTicksAndJammedSnaps(){
  var r=new Rods();assertEquals(.2,r.depth(id(1),0,.2,false,0));
  assertEquals(.2,r.depth(id(1),0,.8,false,1));assertEquals(.5,r.depth(id(1),0,.8,false,3),1e-12);
  assertEquals(.8,r.depth(id(1),0,.8,false,5));assertEquals(.4,r.depth(id(1),0,.4,true,6));
 }
 @Test void identityAndAbsenceDoNotContinueOldRodMotion(){
  var r=new Rods();r.depth(id(1),0,0,false,0);r.depth(id(1),0,1,false,1);
  assertEquals(.75,r.depth(id(2),0,.75,false,2));assertEquals(.1,r.depth(id(1),0,.1,false,10));
  r.clear();assertEquals(.9,r.depth(id(1),0,.9,false,11));
 }
 @Test void pauseFreezesPartialTime(){
  var c=new Clock();c.tick(false);assertEquals(1.4,c.time(.4f,false),1e-6);c.tick(true);
  assertEquals(1.4,c.time(.9f,true),1e-6);c.tick(true);assertEquals(1.4,c.time(0,true),1e-6);
 }
 @Test void realColumnTargetDoesNotMoveCompleteBodyAndUnknownRecoverySnaps(){
  var cap=new net.minecraft.core.BlockPos(1,4,1);
  var body=List.of(cap.below(),cap.below(2),cap.below(3));
  var column=new com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor.ControlRodColumn(cap,body,.25,.9,false);
  var owner=new com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor("minecraft:overworld",new net.minecraft.core.BlockPos(0,2,1),new UUID(0,9),1,1,1,1,true,
    net.minecraft.core.BlockPos.ZERO,new net.minecraft.core.BlockPos(2,4,2),0,0,100,0,List.of(column));
  var key=identity(owner);RODS.clear();var position=rodPose(key,column,0);
  assertEquals(3,position.travel());assertEquals(3.25,position.bottom());assertEquals(6.25,position.top());
  var next=new com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor.ControlRodColumn(cap,body,.75,.1,false);
  rodPose(key,next,1);boundary(ReactorRuntimeSnapshot.empty());
  assertEquals(1.75,rodPose(key,next,2).bottom());RODS.clear();
 }
 @Test void stableGeometryAndFillReuseTheActualBoundaryMesh(){
  var cells=Set.of(new Cell(0,0,0),new Cell(1,0,0));var first=mesh(id(7),cells,.5,0);
  assertSame(first,mesh(id(7),cells,.5,1));assertNotSame(first,mesh(id(7),cells,.6,2));clear();
 }

 /** 记录原生VertexConsumer默认方法最终写入的顶点属性，不复制生产六面算法。 */
 static final class VertexRecord implements com.mojang.blaze3d.vertex.VertexConsumer {
  static final class Entry {float x,y,z,u,v,nx,ny,nz;int r,g,b,a,light,overlay;}
  final List<Entry> entries=new ArrayList<>();Entry current;
  public VertexRecord addVertex(float x,float y,float z){current=new Entry();current.x=x;current.y=y;current.z=z;entries.add(current);return this;}
  public VertexRecord setColor(int r,int g,int b,int a){current.r=r;current.g=g;current.b=b;current.a=a;return this;}
  public VertexRecord setUv(float u,float v){current.u=u;current.v=v;return this;}
  public VertexRecord setUv1(int u,int v){current.overlay=u|(v<<16);return this;}
  public VertexRecord setUv2(int u,int v){current.light=u|(v<<16);return this;}
  public VertexRecord setNormal(float x,float y,float z){current.nx=x;current.ny=y;current.nz=z;return this;}
 }
 @Test void actualLiquidSubmissionSamplesLegalAirAndKeepsVertexAttributes(){
  var base=new net.minecraft.core.BlockPos(10,20,30);var cell=new Cell(11,21,31);
  var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.translate(100,-20,50);
  int air=net.minecraft.client.renderer.LightTexture.pack(0,12);var queried=new ArrayList<net.minecraft.core.BlockPos>();
  var vertex=new VertexRecord();
  for(var side:List.of(Side.EAST,Side.UP))ReactorInternalRenderer.submitFace(vertex,pose.last(),new Face(cell,side,0,.75),base,p->{queried.add(p);return p.equals(base)?0:air;},20,23);
  assertEquals(List.of(new net.minecraft.core.BlockPos(11,21,31),new net.minecraft.core.BlockPos(11,21,31)),queried,"每面必须取自己的合法空气格，不能取宿主或边界外格");
  assertEquals(8,vertex.entries.size());for(var v:vertex.entries){assertEquals(air,v.light);assertEquals(255,v.r);assertEquals(255,v.g);assertEquals(255,v.b);assertEquals(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,v.overlay);}
  var east=vertex.entries.getFirst();assertEquals(102-1./1024,east.x);assertEquals(-19,east.y);assertEquals(52,east.z);assertEquals(2,east.u);assertEquals(1,east.v);assertEquals(1,east.nx);assertEquals(0,east.ny);assertEquals(125,east.a);
  var top=vertex.entries.get(4);assertEquals(101,top.x);assertEquals(-18.25-1./1024,top.y);assertEquals(51,top.z);assertEquals(1,top.u);assertEquals(1,top.v);assertEquals(1,top.ny);assertEquals(114,top.a);
 }
 /** 只替换SBB提交边界；平移/缩放使用原生PoseStack，采光与提交矩阵由真实生产入口决定。 */
 static final class RodRecord implements java.lang.reflect.InvocationHandler {
  final com.mojang.blaze3d.vertex.PoseStack own=new com.mojang.blaze3d.vertex.PoseStack();
  org.joml.Matrix4f world,external;int worldCalls,fixedLightCalls;
  net.createmod.catnip.render.SuperByteBuffer buffer(){return (net.createmod.catnip.render.SuperByteBuffer)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{net.createmod.catnip.render.SuperByteBuffer.class},this);}
  public Object invoke(Object proxy,java.lang.reflect.Method method,Object[] args){
   switch(method.getName()){
    case "translate" -> own.translate(((Number)args[0]).doubleValue(),((Number)args[1]).doubleValue(),((Number)args[2]).doubleValue());
    case "scale" -> own.scale(((Number)args[0]).floatValue(),((Number)args[1]).floatValue(),((Number)args[2]).floatValue());
    case "useLevelLight" -> {worldCalls++;world=new org.joml.Matrix4f((org.joml.Matrix4f)args[1]);}
    case "light" -> fixedLightCalls++;
    case "overlay" -> {}
    case "renderInto" -> {external=new org.joml.Matrix4f(((com.mojang.blaze3d.vertex.PoseStack)args[0]).last().pose());return null;}
    default -> throw new AssertionError("未预期SBB入口: "+method.getName());
   }return proxy;
  }
 }
 @Test void actualRodSubmissionUsesActualWorldPositionAndNoCameraOrDoubleTransform(){
  var cap=new net.minecraft.core.BlockPos(10,4,30);var body=List.of(cap.below(),cap.below(2),cap.below(3));
  for(double depth:new double[]{0,.5,1}){
   var column=new com.iksxh.create_nuclear_industry.structure.ReactorRuntimeDescriptor.ControlRodColumn(cap,body,depth,.9,true);
   RODS.clear();var actual=rodPose(id(99),column,0);
   for(boolean head:new boolean[]{false,true}){
    var record=new RodRecord();var camera=new com.mojang.blaze3d.vertex.PoseStack();camera.translate(100,-20,50);
    ReactorControlRodRenderer.renderPiece(record.buffer(),actual,cap,head,null,camera,new VertexRecord());
    assertEquals(1,record.worldCalls,"实际SBB必须启用按位姿采光，不能复用宿主0");assertEquals(0,record.fixedLightCalls);
    var point=new org.joml.Vector3f(.5f,head?.125f:.75f,.5f);
    var local=new org.joml.Vector3f(point);record.own.last().pose().transformPosition(local);
    var sampled=new org.joml.Vector3f(local);record.world.transformPosition(sampled);
    assertEquals(10.5,sampled.x,1e-6);assertEquals(30.5,sampled.z,1e-6);assertEquals((head?7.125:6.25)-3*depth,sampled.y,1e-6);
    var emitted=new org.joml.Vector3f(local);record.external.transformPosition(emitted);
    assertEquals(100.5,emitted.x,1e-6);assertEquals(50.5,emitted.z,1e-6);assertEquals((head?-16.875:-17.75)-3*depth,emitted.y,1e-6);
    assertEquals(new org.joml.Matrix4f().translation(100,-20,50),record.external,"BER外部Pose保持基准，实际位移只进入SBB一次");
    assertEquals(record.external,camera.last().pose());
   }
  }RODS.clear();
 }
}
