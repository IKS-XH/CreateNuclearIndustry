package com.iksxh.create_nuclear_industry.industrial;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.iksxh.create_nuclear_industry.content.IndustrialFluidContent;
import com.iksxh.create_nuclear_industry.industrial.client.IndustrialVisualOrientation;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.*;
import javax.imageio.ImageIO;
import net.createmod.catnip.render.TemplateMesh;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

/** 用锁定FaceBakery和实际SuperByteBuffer检查表面/像素；无GPU、无世界和流体事务。 */
class IndustrialPipeSurfaceTest {
    private record Surface(BakedQuad quad, TextureAtlasSprite sprite, BufferedImage pixels) {}
    private static final String NS = "create_nuclear_industry";
    /** 只替代GPU atlas装载边界；纹理尺寸来自真实PNG，烘焙与UV映射仍调用原Minecraft代码。 */
    private static TextureAtlasSprite sprite(BufferedImage image) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        var contents = (SpriteContents)((Unsafe)field.get(null)).allocateInstance(SpriteContents.class);
        for (String name : List.of("width", "height")) { var size = SpriteContents.class.getDeclaredField(name); size.setAccessible(true); size.setInt(contents, name.equals("width") ? image.getWidth() : image.getHeight()); }
        var ctor = TextureAtlasSprite.class.getDeclaredConstructor(ResourceLocation.class, SpriteContents.class, int.class, int.class, int.class, int.class); ctor.setAccessible(true);
        return ctor.newInstance(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/atlas/blocks.png"), contents, 1024, 1024, 0, 0);
    }
    private static Vector3f vector(com.google.gson.JsonArray value) { return new Vector3f(value.get(0).getAsFloat(), value.get(1).getAsFloat(), value.get(2).getAsFloat()); }
    /** 元素坐标为1/16格，UV固定16单位；由原FaceBakery处理旋转/UV缩边和面法向。 */
    private static List<Surface> bake(JsonObject application) throws Exception {
        var model = IndustrialVisualContractTest.resolved(application.get("model").getAsString(), new HashSet<>());
        List<Surface> surfaces = new ArrayList<>();
        var rotation = BlockModelRotation.by(application.has("x") ? application.get("x").getAsInt() : 0, application.has("y") ? application.get("y").getAsInt() : 0);
        for (var element : model.getAsJsonArray("elements")) {
            var geometry = element.getAsJsonObject();
            for (var entry : geometry.getAsJsonObject("faces").entrySet()) {
                var face = entry.getValue().getAsJsonObject(); var ref = IndustrialVisualContractTest.texture(face.get("texture").getAsString(), model.getAsJsonObject("textures")); var key = IndustrialVisualContractTest.id(ref);
                var image = ImageIO.read(new ByteArrayInputStream(IndustrialVisualContractTest.resource(key[0], "textures/" + key[1] + ".png"))); var sprite = sprite(image);
                float[] uv = new float[4]; for (int i=0;i<4;i++) uv[i]=face.getAsJsonArray("uv").get(i).getAsFloat();
                var blockFace = new BlockElementFace(face.has("cullface") ? Direction.byName(face.get("cullface").getAsString()) : null, -1, ref, new BlockFaceUV(uv, face.has("rotation") ? face.get("rotation").getAsInt() : 0));
                var quad = new FaceBakery().bakeQuad(vector(geometry.getAsJsonArray("from")), vector(geometry.getAsJsonArray("to")), blockFace, sprite, Direction.byName(entry.getKey()), rotation, null, true);
                surfaces.add(new Surface(quad, sprite, image));
            }
        }
        return surfaces;
    }
    private static List<Surface> pipe(String name, int mask) throws Exception {
        var state = IndustrialVisualContractTest.json(NS, "blockstates/" + name + ".json"); Map<String,String> values = new HashMap<>();
        for (Direction direction : Direction.values()) values.put(direction.getName(), Boolean.toString((mask & (1 << direction.ordinal())) != 0));
        List<Surface> result = new ArrayList<>();
        for (var element : state.getAsJsonArray("multipart")) { var part = element.getAsJsonObject(); if (!part.has("when") || IndustrialVisualContractTest.matches(part.getAsJsonObject("when"), values)) result.addAll(bake(part.getAsJsonObject("apply"))); }
        return result;
    }
    private static Vector3f vertex(BakedQuad quad, int index) { var data=quad.getVertices(); return new Vector3f(Float.intBitsToFloat(data[index*8]),Float.intBitsToFloat(data[index*8+1]),Float.intBitsToFloat(data[index*8+2])); }
    private static float coordinate(Vector3f value, Direction.Axis axis) { return axis == Direction.Axis.X ? value.x : axis == Direction.Axis.Y ? value.y : value.z; }
    /** 求真实烘焙quad的平面UV逆映射；坐标范围断言能抓到被截掉的glyph而非只验名义UP。 */
    private static Vector3f pixelPosition(Surface surface, float pixelX, float pixelY) {
        var data=surface.quad.getVertices(); float u=surface.sprite.getU(pixelX/surface.pixels.getWidth()), v=surface.sprite.getV(pixelY/surface.pixels.getHeight());
        float u0=Float.intBitsToFloat(data[4]),v0=Float.intBitsToFloat(data[5]); float du1=Float.intBitsToFloat(data[12])-u0,dv1=Float.intBitsToFloat(data[13])-v0,du3=Float.intBitsToFloat(data[28])-u0,dv3=Float.intBitsToFloat(data[29])-v0;
        float determinant=du1*dv3-du3*dv1; float a=((u-u0)*dv3-du3*(v-v0))/determinant,b=(du1*(v-v0)-(u-u0)*dv1)/determinant;
        assertTrue(a>=0 && a<=1 && b>=0 && b<=1,"实际不透明尖端/杆部必须都在完整烘焙UV内");
        var origin=vertex(surface.quad,0);return new Vector3f(origin).add(new Vector3f(vertex(surface.quad,1)).sub(origin).mul(a)).add(new Vector3f(vertex(surface.quad,3)).sub(origin).mul(b));
    }
    private static int sample(Surface surface, Vector3f point) {
        var a=vertex(surface.quad,0);var ab=new Vector3f(vertex(surface.quad,1)).sub(a);var ad=new Vector3f(vertex(surface.quad,3)).sub(a);var delta=new Vector3f(point).sub(a);
        float b=delta.dot(ab)/ab.lengthSquared(),d=delta.dot(ad)/ad.lengthSquared();var ints=surface.quad.getVertices();
        float u=Float.intBitsToFloat(ints[4])+(Float.intBitsToFloat(ints[12])-Float.intBitsToFloat(ints[4]))*b+(Float.intBitsToFloat(ints[28])-Float.intBitsToFloat(ints[4]))*d;
        float v=Float.intBitsToFloat(ints[5])+(Float.intBitsToFloat(ints[13])-Float.intBitsToFloat(ints[5]))*b+(Float.intBitsToFloat(ints[29])-Float.intBitsToFloat(ints[5]))*d;
        int x=Math.min(surface.pixels.getWidth()-1,Math.max(0,(int)(surface.sprite.getUOffset(u)*surface.pixels.getWidth()))),y=Math.min(surface.pixels.getHeight()-1,Math.max(0,(int)(((v-surface.sprite.getV0())/(surface.sprite.getV1()-surface.sprite.getV0()))*surface.pixels.getHeight())));
        return surface.pixels.getRGB(x,y);
    }
    /** 从对应外侧半空间沿格中心取朝外面，禁止把远端反面误当自由端闭合。 */
    private static Surface outside(List<Surface> surfaces, Direction side) {
        return surfaces.stream().filter(s->s.quad.getDirection()==side).filter(s->(coordinate(vertex(s.quad,0),side.getAxis())-.5f)*side.getAxisDirection().getStep()>=-.00001f).filter(s->{var p=vertex(s.quad,0);var q=vertex(s.quad,2);for(Direction.Axis axis:Direction.Axis.values())if(axis!=side.getAxis() && (.5f<Math.min(coordinate(p,axis),coordinate(q,axis))-.00001f || .5f>Math.max(coordinate(p,axis),coordinate(q,axis))+.00001f))return false;return true;}).max(Comparator.comparingDouble(s->coordinate(vertex(s.quad,0),side.getAxis())*side.getAxisDirection().getStep())).orElse(null);
    }
    @Test void allConnectionsHaveVisibleCentersClosedFreeFacesAndNoCoplanarOverlaps() throws Exception {
        for(String name:List.of("reinforced_fluid_pipe","ceramic_fluid_pipe"))for(int mask=0;mask<64;mask++){
            var surfaces=pipe(name,mask);
            for(Direction side:Direction.values()){
                var hit=outside(surfaces,side);assertNotNull(hit,"中心或自由端空面 mask="+mask+" side="+side);
                var point=new Vector3f(.5f,.5f,.5f);point.setComponent(side.getAxis().ordinal(),coordinate(vertex(hit.quad,0),side.getAxis()));assertEquals(255,sample(hit,point)>>>24,"端面/中心不能落在透明或悬空纹理");
            }
            for(int i=0;i<surfaces.size();i++)for(int j=i+1;j<surfaces.size();j++){
                var a=surfaces.get(i).quad;var b=surfaces.get(j).quad;if(a.getDirection()!=b.getDirection())continue;var axis=a.getDirection().getAxis();if(Math.abs(coordinate(vertex(a,0),axis)-coordinate(vertex(b,0),axis))>.00001)continue;
                boolean overlap=true;for(Direction.Axis other:Direction.Axis.values())if(other!=axis){float amin=Float.MAX_VALUE,amax=-Float.MAX_VALUE,bmin=Float.MAX_VALUE,bmax=-Float.MAX_VALUE;for(int k=0;k<4;k++){amin=Math.min(amin,coordinate(vertex(a,k),other));amax=Math.max(amax,coordinate(vertex(a,k),other));bmin=Math.min(bmin,coordinate(vertex(b,k),other));bmax=Math.max(bmax,coordinate(vertex(b,k),other));}if(Math.min(amax,bmax)-Math.max(amin,bmin)<.00001)overlap=false;}
                assertFalse(overlap,"共面重叠会闪烁 mask="+mask);
            }
        }
    }
    @Test void everyMaskUsesCorrectExteriorFootprintAndMaterialAndMatchingShape() throws Exception {
        for(String name:List.of("reinforced_fluid_pipe","ceramic_fluid_pipe"))for(int mask=0;mask<64;mask++){
            var surfaces=pipe(name,mask);int count=Integer.bitCount(mask);
            boolean straight=(mask&3)==mask || (mask&12)==mask || (mask&48)==mask;
            // 独立组合预期：四向以上必需壳；三向只有共面T需壳，三轴各一向不补壳。
            boolean casing=count>=4 || count==3 && ((mask&3)==0 || (mask&12)==0 || (mask&48)==0);
            Direction.Axis segmentAxis=mask==0 ? Direction.Axis.Z : Direction.values()[Integer.numberOfTrailingZeros(mask)].getAxis();
            for(Direction side:Direction.values())if((mask&(1<<side.ordinal()))==0){
                float offset=straight && side.getAxis()==segmentAxis ? .5f : casing ? 5f/16 : 4f/16;
                var surface=outside(surfaces,side);assertNotNull(surface);
                assertEquals(.5f+side.getAxisDirection().getStep()*offset,coordinate(vertex(surface.quad,0),side.getAxis()),.00001,"自由面包络 mask="+mask+" side="+side);
            }
            var block=name.startsWith("ceramic") ? IndustrialFluidContent.CERAMIC_PIPE.get() : IndustrialFluidContent.REINFORCED_PIPE.get();var state=block.defaultBlockState();
            for(Direction direction:Direction.values())state=state.setValue(IndustrialPipeBlock.CONNECTIONS[direction.ordinal()],(mask&(1<<direction.ordinal()))!=0);
            var shape=block.getShape(state,null,BlockPos.ZERO,net.minecraft.world.phys.shapes.CollisionContext.empty()).bounds();
            for(Direction.Axis axis:Direction.Axis.values()){
                float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;for(Surface surface:surfaces)for(int i=0;i<4;i++){float value=coordinate(vertex(surface.quad,i),axis);min=Math.min(min,value);max=Math.max(max,value);}
                assertEquals(min,shape.min(axis),.00001,"碰撞最小包络");assertEquals(max,shape.max(axis),.00001,"碰撞最大包络");
            }
            String tone=name.startsWith("ceramic") ? "ceramic" : "steel";
            for(Surface surface:surfaces){
                float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;for(int i=0;i<4;i++){float value=coordinate(vertex(surface.quad,i),surface.quad.getDirection().getAxis());min=Math.min(min,value);max=Math.max(max,value);}
                boolean centerTile=Math.abs(min-.25f)<.00001 || Math.abs(min-.75f)<.00001;
                for(Direction.Axis axis:Direction.Axis.values())if(axis!=surface.quad.getDirection().getAxis()){
                    float a=Float.MAX_VALUE,b=-Float.MAX_VALUE;for(int i=0;i<4;i++){float value=coordinate(vertex(surface.quad,i),axis);a=Math.min(a,value);b=Math.max(b,value);}centerTile&=Math.abs(b-a-.5f)<.00001;
                }
                var expected=ImageIO.read(new ByteArrayInputStream(IndustrialVisualContractTest.resource(NS,"textures/block/industrial_fluid_visual_01/"+tone+"_"+(centerTile?"pipes_connected":"pipes")+".png")));
                assertArrayEquals(expected.getRGB(0,0,expected.getWidth(),expected.getHeight(),null,0,expected.getWidth()),surface.pixels.getRGB(0,0,surface.pixels.getWidth(),surface.pixels.getHeight(),null,0,surface.pixels.getWidth()),"真实面绑定错原atlas mask="+mask);
                for(int i=0;i<4;i++){var data=surface.quad.getVertices();float u=surface.sprite.getUOffset(Float.intBitsToFloat(data[i*8+4])),v=(Float.intBitsToFloat(data[i*8+5])-surface.sprite.getV0())/(surface.sprite.getV1()-surface.sprite.getV0());assertTrue(u>=0&&u<=1&&v>=0&&v<=1,"面UV越出原纹理");}
            }
        }
    }
    @Test void standalonePipeIsAFullCreateSegmentWithItsOriginalSidePixels() throws Exception {
        for(String name:List.of("reinforced_fluid_pipe","ceramic_fluid_pipe")){
            var surfaces=pipe(name,0);var side=outside(surfaces,Direction.UP);assertNotNull(side);
            int pixel=sample(side,new Vector3f(.5f,coordinate(vertex(side.quad,0),Direction.Axis.Y),.5f));assertTrue(((pixel>>16)&255)>30,"孤立段不能取黑色连接atlas占位区");
            assertEquals(0f,coordinate(vertex(outside(surfaces,Direction.NORTH).quad,0),Direction.Axis.Z),.0001,"完整段自由端应在格边");
            assertEquals(1f,coordinate(vertex(outside(surfaces,Direction.SOUTH).quad,0),Direction.Axis.Z),.0001);
        }
    }
    @Test void tJunctionExteriorUsesTheNativeCasingFootprint() throws Exception {
        int t=(1<<Direction.NORTH.ordinal())|(1<<Direction.SOUTH.ordinal())|(1<<Direction.EAST.ordinal());
        for(String name:List.of("reinforced_fluid_pipe","ceramic_fluid_pipe")){
            var hit=outside(pipe(name,t),Direction.UP);assertNotNull(hit,"T中心顶部消失");assertEquals(13f/16f,coordinate(vertex(hit.quad,0),Direction.Axis.Y),.00001,"T应沿Create动态壳的10像素包络");
        }
    }
    @Test void everyActualArrowFaceShowsOpaqueTipTowardRealPumpOutput() throws Exception {
        var application=new JsonObject();application.addProperty("model",NS+":block/industrial_fluid_visual_01/output_arrow");var faces=bake(application);
        var pump=new IndustrialPumpBlockEntity(BlockPos.ZERO,IndustrialFluidContent.PUMP.get().defaultBlockState());
        for(Direction facing:Direction.values())for(float speed:List.of(-256f,256f)){
            pump.setBlockState(pump.getBlockState().setValue(IndustrialPumpBlock.FACING,facing));pump.setSpeed(speed);var output=pump.outputDirection();
            for(Surface surface:faces){
                int first=-1,last=-1,widest=-1,maxWidth=0;float tipX=0,stemX=0;
                for(int y=0;y<surface.pixels.getHeight();y++){
                    int width=0;float center=0;for(int x=0;x<surface.pixels.getWidth();x++)if((surface.pixels.getRGB(x,y)>>>24)==255){width++;center+=x+.5f;}
                    if(width==0)continue;if(first<0){first=y;tipX=center/width;}last=y;stemX=center/width;if(width>maxWidth){maxWidth=width;widest=y;}
                }
                assertTrue(first>=0 && last>first && widest<(first+last)/2f,"原SVG箭头头部须在杆部上方");
                var tip=pixelPosition(surface,tipX,first+.5f);var stem=pixelPosition(surface,stemX,last+.5f);
                Object buffer=Class.forName("net.createmod.catnip.render.ShadeSeparatingSuperByteBuffer").getConstructor(TemplateMesh.class).newInstance(new TemplateMesh(0));
                try { IndustrialVisualOrientation.class.getMethod("orientArrow",Class.forName("net.createmod.catnip.render.SuperByteBuffer"),Direction.class).invoke(null,buffer,output); }
                catch(NoSuchMethodException absent){buffer.getClass().getMethod("center").invoke(buffer); buffer.getClass().getMethod("rotateYDegrees",float.class).invoke(buffer,(float)IndustrialVisualOrientation.yRotation(output)); buffer.getClass().getMethod("rotateXDegrees",float.class).invoke(buffer,(float)IndustrialVisualOrientation.xRotation(output)); buffer.getClass().getMethod("uncenter").invoke(buffer);}
                var matrix=((com.mojang.blaze3d.vertex.PoseStack)buffer.getClass().getMethod("getTransforms").invoke(buffer)).last().pose();var delta=matrix.transformPosition(new Vector3f(tip)).sub(matrix.transformPosition(new Vector3f(stem))).normalize();
                assertTrue(delta.dot(new Vector3f(output.getStepX(),output.getStepY(),output.getStepZ()))>.9999,"真实FaceBakery glyph及SuperByteBuffer变换必须朝真实出液口");
            }
        }
        pump.setSpeed(0);assertNull(IndustrialVisualOrientation.displayedOutput(pump));
        pump.setSpeed(256);var stress=com.simibubi.create.content.kinetics.base.KineticBlockEntity.class.getDeclaredField("overStressed");stress.setAccessible(true);stress.setBoolean(pump,true);assertNull(IndustrialVisualOrientation.displayedOutput(pump));
    }
    /** 实际齿轮大面不透明近黑孔会成为方向伪图案；只消除该孔黑色，不能透明化或替换几何。 */
    @Test void actualPumpCogFaceKeepsOpaqueAlphaAndRemovesDarkPseudoIndicator() throws Exception {
        var application = new JsonObject();
        application.addProperty("model", NS + ":block/industrial_fluid_visual_01/steel/mechanical_pump/cog");
        var faces = bake(application);
        // 第一根十字轴大面UV[0,9,9,14]中心取实际PNG第9列/23行，不凭资源名生成方向预期。
        var surface = faces.get(1);
        var center = new Vector3f(vertex(surface.quad, 0)).add(vertex(surface.quad, 2)).mul(.5f);
        int pixel = sample(surface, center);
        assertEquals(255, pixel >>> 24, "近黑孔原alpha255必须保持，不能用cutout假修");
        assertTrue((pixel >> 16 & 255) >= 80 && (pixel >> 8 & 255) >= 80 && (pixel & 255) >= 80,
                "真实COG大面仍采到近黑方向伪图案，需局部恢复同面钢灰");
    }

}
