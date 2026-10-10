package com.iksxh.create_nuclear_industry.structure.client;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.iksxh.create_nuclear_industry.structure.client.ReactorAnimationMaterials.*;
/** 验证ABGR真实端点及槽后端调用；后端仅记录GPU边界，不假造槽行为。 */
class ReactorAnimationMaterialsTest {
 static ReactorAnimationVisualState.Identity id(long n){return new ReactorAnimationVisualState.Identity(1,"minecraft:overworld",n,new UUID(0,1),1);}
 static int[] mix(int[][] c,int[][] h,double phase,double ratio){int[] p=new int[256];mixInto(c,h,phase,ratio,p);return p;}
 static final class Device implements Backend {
  List<String> calls=new ArrayList<>();
  public void create(int s){calls.add("create:"+s);}public void update(int s,int[] p){calls.add("update:"+s+":"+p[0]);}public void release(int s){calls.add("release:"+s);}
 }
 @Test void abgrEndpointsAndContinuousChannels(){
  assertEquals(0xff0000ff,lerp(0xff0000ff,0xffff0000,0));assertEquals(0xffff0000,lerp(0xff0000ff,0xffff0000,1));
  assertEquals(0xff800080,lerp(0xff0000ff,0xffff0000,.5));
 }
 @Test void frameInterpolationPrecedesInventoryBlend(){
  int[][] c=new int[8][256],h=new int[8][256];for(int i=0;i<8;i++){Arrays.fill(c[i],0xff000000);Arrays.fill(h[i],0xffffffff);}Arrays.fill(c[1],0xff0000ff);
  assertEquals(0xff000080,mix(c,h,.5,0)[0]);assertEquals(0xffffffff,mix(c,h,0,1)[0]);assertEquals(0xff808080,mix(c,h,0,.5)[0]);
 }
 @Test void boundedStableSlotCannotBeStolenWithinFrame(){
  var d=new Device();var p=new Pool(1,d);int[] pixels={7};assertEquals(0,p.draw(id(1),0,out->out[0]=pixels[0]));assertEquals(0,p.draw(id(1),0,out->out[0]=pixels[0]));
  assertEquals(-1,p.draw(id(2),0,out->out[0]=pixels[0]));assertEquals(List.of("create:0","update:0:7","update:0:7"),d.calls);assertEquals(1,p.size());
 }
 @Test void expiryAndInvalidationOnlyAtSafeBoundary(){
  var d=new Device();var p=new Pool(1,d);p.draw(id(1),0,pixels->pixels[0]=7);p.boundary(20,Set.of(id(1)));assertEquals(1,p.size());
  p.boundary(21,Set.of(id(1)));assertEquals(0,p.size());assertEquals("release:0",d.calls.getLast());
  assertEquals(0,p.draw(id(2),21,pixels->pixels[0]=9));p.boundary(22,Set.of());assertEquals(0,p.size());
 }
 @Test void reloadClearReleasesEachRegisteredSlot(){
  var d=new Device();var p=new Pool(2,d);p.draw(id(1),0,pixels->pixels[0]=1);p.draw(id(2),0,pixels->pixels[0]=2);p.clear();assertEquals(0,p.size());
  assertEquals(2,d.calls.stream().filter(x->x.startsWith("release:")).count());
 }
 @Test void productionWriterReusesSlotPixelsAndPausedBoundaryRetractsInvalidOwner(){
  var d=new Device();var p=new Pool(1,d);var arrays=new ArrayList<int[]>();
  p.draw(id(1),4,pixels->{arrays.add(pixels);pixels[0]=11;});p.draw(id(1),4,pixels->{arrays.add(pixels);pixels[0]=12;});
  assertSame(arrays.get(0),arrays.get(1));assertEquals("update:0:12",d.calls.getLast());
  p.boundary(4,Set.of());assertEquals(0,p.size());assertEquals("release:0",d.calls.getLast());
 }
 @Test void hardLimit256AndSessionIdentityNeverShareSlots(){
  var d=new Device();var p=new Pool(256,d);for(int i=0;i<256;i++){int value=i;assertEquals(i,p.draw(id(i),0,pixels->pixels[0]=value));}
  assertEquals(-1,p.draw(id(256),0,pixels->pixels[0]=1));assertEquals(256,p.size());
  var otherSession=new ReactorAnimationVisualState.Identity(2,"minecraft:overworld",0,new UUID(0,1),1);
  assertEquals(-1,p.draw(otherSession,0,pixels->pixels[0]=3));p.clear();assertEquals(0,p.draw(otherSession,1,pixels->pixels[0]=3));
 }
}
