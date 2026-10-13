package com.iksxh.create_nuclear_industry.control;

import com.iksxh.create_nuclear_industry.control.ControlRodSliderBehaviour.CornerTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** 直接调用生产几何与原生PoseStack/testHit，核显示和鼠标同源；不启动世界或复制命中实现。 */
class ControlRodSliderTransformTest {
    /**
     * JUnit不应用客户端mixin，且Flywheel只在测试运行时可见；只生成原生扩展挂点，禁止代写旋转。
     * 夹具字节码仅构造PoseTransformStack(this)、保存字段并返回它；实际Create.rotate/MC PoseStack照常运行。
     */
    private static PoseStack nativePose() {
        try { return (PoseStack) NativePoseFixture.TYPE.getConstructor().newInstance(); }
        catch (ReflectiveOperationException exception) { throw new AssertionError("无法构造原生PoseStack夹具", exception); }
    }

    private static final class NativePoseFixture {
        static final Class<?> TYPE = define();
        private static Object call(Object target, String name, Class<?>[] types, Object... arguments) throws ReflectiveOperationException {
            // ASM的FieldWriter/MethodWriter是包内实现；通过公共Visitor声明调用，不绕过模块访问。
            Class<?> owner = target.getClass();
            while (!java.lang.reflect.Modifier.isPublic(owner.getModifiers())) owner = owner.getSuperclass();
            return owner.getMethod(name, types).invoke(target, arguments);
        }
        private static Class<?> define() {
            try {
                String name = "com/iksxh/create_nuclear_industry/control/NativePoseStackFixture";
                String pose = "com/mojang/blaze3d/vertex/PoseStack";
                String stack = "dev/engine_room/flywheel/lib/transform/PoseTransformStack";
                String descriptor = "L"+stack+";";
                var writer = Class.forName("org.objectweb.asm.ClassWriter").getConstructor(int.class).newInstance(0);
                call(writer, "visit", new Class[]{int.class,int.class,String.class,String.class,String.class,String[].class},
                        65, 17, name, null, pose, new String[]{"dev/engine_room/flywheel/impl/extension/PoseStackExtension"});
                var field = call(writer, "visitField", new Class[]{int.class,String.class,String.class,String.class,Object.class},
                        18, "transforms", descriptor, null, null);
                call(field, "visitEnd", new Class[]{});
                var constructor = call(writer, "visitMethod", new Class[]{int.class,String.class,String.class,String.class,String[].class},
                        1, "<init>", "()V", null, null);
                call(constructor, "visitCode", new Class[]{});
                call(constructor, "visitVarInsn", new Class[]{int.class,int.class}, 25, 0);
                call(constructor, "visitMethodInsn", new Class[]{int.class,String.class,String.class,String.class,boolean.class}, 183, pose, "<init>", "()V", false);
                call(constructor, "visitVarInsn", new Class[]{int.class,int.class}, 25, 0);
                call(constructor, "visitTypeInsn", new Class[]{int.class,String.class}, 187, stack);
                call(constructor, "visitInsn", new Class[]{int.class}, 89);
                call(constructor, "visitVarInsn", new Class[]{int.class,int.class}, 25, 0);
                call(constructor, "visitMethodInsn", new Class[]{int.class,String.class,String.class,String.class,boolean.class}, 183, stack, "<init>", "(L"+pose+";)V", false);
                call(constructor, "visitFieldInsn", new Class[]{int.class,String.class,String.class,String.class}, 181, name, "transforms", descriptor);
                call(constructor, "visitInsn", new Class[]{int.class}, 177);
                call(constructor, "visitMaxs", new Class[]{int.class,int.class}, 4, 1);
                call(constructor, "visitEnd", new Class[]{});
                var getter = call(writer, "visitMethod", new Class[]{int.class,String.class,String.class,String.class,String[].class},
                        1, "flywheel$transformStack", "()"+descriptor, null, null);
                call(getter, "visitCode", new Class[]{});
                call(getter, "visitVarInsn", new Class[]{int.class,int.class}, 25, 0);
                call(getter, "visitFieldInsn", new Class[]{int.class,String.class,String.class,String.class}, 180, name, "transforms", descriptor);
                call(getter, "visitInsn", new Class[]{int.class}, 176);
                call(getter, "visitMaxs", new Class[]{int.class,int.class}, 1, 1);
                call(getter, "visitEnd", new Class[]{});
                call(writer, "visitEnd", new Class[]{});
                byte[] bytes = (byte[]) call(writer, "toByteArray", new Class[]{});
                return java.lang.invoke.MethodHandles.lookup().defineClass(bytes);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError("运行时原生PoseStack扩展挂点失败", exception);
            }
        }
    }

    private static final BlockPos POS = BlockPos.ZERO;
    /** 锁定ValueBox后置矩阵；生产定位/旋转仍实际执行，未运行客户端Font/GPU。 */
    private static PoseStack outlinePose(CornerTransform transform) {
        var pose=nativePose();transform.transform(null,POS,null,pose);
        pose.scale(-2.01f,-2.01f,2.01f);pose.translate(-.5,-.5,-1./32);
        return pose;
    }
    private static PoseStack textBaselinePose(CornerTransform transform) {
        var pose=nativePose();transform.transform(null,POS,null,pose);
        float font=-transform.getFontScale();pose.scale(font,font,font);
        pose.scale(3,3,1);pose.translate(-4,-3.75,5);
        return pose;
    }
    /**
     * 仅重放锁定TextValueBox的Font.width之后指令；12/18/24来自1.21.1默认ASCII的实际六像素advance。
     * 宽度来源保存于09的text-inputs.json；保守用整段advance×九像素行高，不声称字体烘焙已运行。
     */
    private static PoseStack formattedTextPose(CornerTransform transform,int width) {
        var pose=textBaselinePose(transform);
        float fit=9f/width;boolean shortText=width<10;
        if(shortText)fit/=2;
        float vertical=(width-9)/2f;
        pose.scale(fit,fit,fit);pose.translate(shortText?width/2:0,shortText?-vertical:vertical,0);
        return pose;
    }
    private static double outward(Direction side,Vector3f point) {
        return switch(side){case UP->point.y-1;case DOWN->-point.y;case SOUTH->point.z-1;case NORTH->-point.z;case EAST->point.x-1;case WEST->-point.x;};
    }
    @Test void nativeFinalOutlineFourCornersClearAllSixBlockFaces() {
        try {
            new com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.TextValueBox(
                    net.minecraft.network.chat.Component.literal("深度"),new net.minecraft.world.phys.AABB(0,0,0,1,1,1),POS,
                    (net.minecraft.world.level.block.state.BlockState)null,net.minecraft.network.chat.Component.literal("100%"));
            System.out.println("NATIVE_DRAW_ENTRY|构造器可用；未启动Font/纹理绘制");
        }catch(RuntimeException unavailable){System.out.println("NATIVE_DRAW_ENTRY|"+unavailable.getClass().getName()+"|"+unavailable.getMessage());}
        var transform=new CornerTransform();
        for(var side:Direction.values()){
            transform.fromSide(side);var pose=outlinePose(transform);
            // 原生6PX宽框四个可见角；不把透明图标边距当实体框。
            for(float x:new float[]{5f/16,11f/16})for(float y:new float[]{5f/16,11f/16}){
                var point=new Vector3f(x,y,0).mulPosition(pose.last().pose());
                System.out.println("FINAL_FRAME|"+side+"|"+point.x+"|"+point.y+"|"+point.z+"|"+outward(side,point));
                assertTrue(outward(side,point)>0,"最终框必须在真实面外");
                assertEquals(side.getAxis()==Direction.Axis.Y?.0125625:.025125,outward(side,point),1e-6);
                if(side.getAxis()==Direction.Axis.Y){
                    assertTrue(point.x>.42 && point.x<.58 && point.z>0 && point.z<.175,"宽框需沿边居中并避固定方箍");
                }
            }
        }
    }
    @Test void nativeFinalTextBaselineClearsAllSixBlockFaces() {
        var transform=new CornerTransform();
        for(var side:Direction.values()){
            transform.fromSide(side);var point=new Vector3f().mulPosition(textBaselinePose(transform).last().pose());
            System.out.println("FINAL_TEXT|"+side+"|"+point.x+"|"+point.y+"|"+point.z+"|"+outward(side,point));
            assertTrue(outward(side,point)>0,"文字基线必须在真实面外");
            assertEquals(side.getAxis()==Direction.Axis.Y?.028125:.03125,outward(side,point),1e-6);
        }
    }
    @Test void zeroHalfFullTextBoundsStayIsotropicWithinTheEdgeOutsideHead() {
        var transform=new CornerTransform();String[] labels={"0%","50%","100%"};int[] widths={12,18,24};
        for(var side:Direction.values()){
            transform.fromSide(side);
            assertEquals(side.getAxis()==Direction.Axis.Y?1.8f/64:1f/64,transform.getFontScale());
            for(int i=0;i<labels.length;i++){
                var pose=formattedTextPose(transform,widths[i]);
                float horizontal=new Vector3f(1,0,0).mulDirection(pose.last().pose()).length();
                float vertical=new Vector3f(0,1,0).mulDirection(pose.last().pose()).length();
                assertEquals(horizontal,vertical,1e-7,"字形不能被非等比拉扁");
                assertEquals(side.getAxis()==Direction.Axis.Y?.151875:.16875,horizontal*widths[i],1e-6,"顶底字号应为旧版两倍，四侧原值保持");
                for(float x:new float[]{0,widths[i]})for(float y:new float[]{0,9}){
                    var point=new Vector3f(x,y,0).mulPosition(pose.last().pose());
                    System.out.println("TEXT_BOUNDS|"+side+"|"+labels[i]+"|"+point.x+"|"+point.y+"|"+point.z+"|"+outward(side,point));
                    assertTrue(outward(side,point)>0,"完整字面需出面");
                    if(side.getAxis()==Direction.Axis.Y){
                        assertTrue(point.x>0 && point.x<1 && point.z>0 && point.z<.175,"整段字面需在本格边缘且避方箍");
                    }
                }
            }
        }
    }
    private static final Map<Direction,Vec3> EXPECTED=Map.of(
            Direction.UP,new Vec3(.5,1,.0875),Direction.DOWN,new Vec3(.5,0,.0875),
            Direction.SOUTH,new Vec3(.25,.75,1),Direction.NORTH,new Vec3(.75,.75,0),
            Direction.EAST,new Vec3(1,.75,.75),Direction.WEST,new Vec3(0,.75,.25));
    /** 原生输入已减blockPos，鼠标在真实0/1块面。 */
    private static Vec3 onSurface(Direction side,Vec3 point) {
        double normal=side.getAxisDirection()==Direction.AxisDirection.POSITIVE?1:0;
        return switch(side.getAxis()){
            case X->new Vec3(normal,point.y,point.z);case Y->new Vec3(point.x,normal,point.z);case Z->new Vec3(point.x,point.y,normal);
        };
    }
    private static Vec3 tangent(Direction side,Vec3 point,double distance) {
        return side.getAxis()==Direction.Axis.X?point.add(0,distance,0):point.add(distance,0,0);
    }
    @Test void constructionStartsWithValidUpScaleFontAndEdgeHit() {
        var transform=assertDoesNotThrow(CornerTransform::new);
        assertEquals(Direction.UP,transform.getSide());assertEquals(.20f,transform.getScale());assertEquals(1.8f/64,transform.getFontScale());
        assertTrue(transform.testHit(null,POS,null,new Vec3(.5,1,.0875)));
        assertTrue(transform.testHit(null,POS,null,new Vec3(.8,1,.0875)),"长边必须有效，不能残留旧小圆缓存");
        assertFalse(transform.testHit(null,POS,null,new Vec3(.86,1,.0875)));
        var pose=nativePose();transform.transform(null,POS,null,pose);
        assertEquals(.20,new Vector3f(1,0,0).mulDirection(pose.last().pose()).length(),1e-6);
    }
    @Test void sixFacesUseFrozenOffsetsAndActualNativePoseOrientation() {
        var transform=new CornerTransform();
        for(var side:Direction.values()){
            assertSame(transform,transform.fromSide(side));var offset=transform.getLocalOffset(null,POS,null);assertEquals(EXPECTED.get(side),offset);
            assertTrue(offset.x>=0 && offset.x<=1 && offset.y>=0 && offset.y<=1 && offset.z>=0 && offset.z<=1);
            var pose=nativePose();transform.transform(null,POS,null,pose);
            var origin=new Vector3f().mulPosition(pose.last().pose());
            assertEquals(offset.x,origin.x,1e-6);assertEquals(offset.y,origin.y,1e-6);assertEquals(offset.z,origin.z,1e-6);
            var nativePose=nativePose();new CenteredSideValueBoxTransform().fromSide(side).rotate(null,POS,null,nativePose);
            for(var axis:new Vector3f[]{new Vector3f(1,0,0),new Vector3f(0,1,0),new Vector3f(0,0,1)}){
                var actual=new Vector3f(axis).mulDirection(pose.last().pose());var nativeAxis=new Vector3f(axis).mulDirection(nativePose.last().pose()).mul(transform.getScale());
                assertEquals(nativeAxis.x,actual.x,1e-6);assertEquals(nativeAxis.y,actual.y,1e-6);assertEquals(nativeAxis.z,actual.z,1e-6);
            }
            var normal=new Vector3f(0,0,1).mul(pose.last().normal()).normalize();var face=side.getNormal();
            assertEquals(1,Math.abs(normal.x*face.getX()+normal.y*face.getY()+normal.z*face.getZ()),1e-6);
            System.out.println("LAYOUT|"+side+"|"+origin.x+"|"+origin.y+"|"+origin.z+"|"+transform.getScale()+"|"+transform.getFontScale());
        }
    }
    @Test void sixRealBlockFacesAcceptInsideAndRejectOutside() {
        var transform=new CornerTransform();
        for(var side:Direction.values()){
            transform.fromSide(side);var center=onSurface(side,EXPECTED.get(side));boolean top=side.getAxis()==Direction.Axis.Y;
            assertTrue(transform.testHit(null,POS,null,center));
            assertTrue(transform.testHit(null,POS,null,tangent(side,center,top?.349:.199)));
            assertFalse(transform.testHit(null,POS,null,tangent(side,center,top?.351:.201)));
            if(top){
                assertTrue(transform.testHit(null,POS,null,center.add(0,0,.074)));
                assertFalse(transform.testHit(null,POS,null,center.add(0,0,.076)));
                assertTrue(transform.testHit(null,POS,null,center.add(.21,0,.045)),"椭圆内部斜点命中");
                assertFalse(transform.testHit(null,POS,null,center.add(.28,0,.06)),"不能用长方形替代椭球，斜角必须拒绝");
                assertFalse(transform.testHit(null,POS,null,center.add(.21,.06,.045)),"法向也参与同一椭球");
            }
        }
    }
    @Test void strictBoundaryRejectsTangentialAndNormalEndpoints() {
        var transform=new CornerTransform();
        for(var side:Direction.values()){
            transform.fromSide(side);var center=EXPECTED.get(side);double radius=transform.getScale()/2.;var normal=side.getNormal();
            assertFalse(transform.testHit(null,POS,null,center.add(normal.getX()*radius,normal.getY()*radius,normal.getZ()*radius)));
            assertTrue(transform.testHit(null,POS,null,center.add(normal.getX()*(radius-1e-6),normal.getY()*(radius-1e-6),normal.getZ()*(radius-1e-6))));
            if(side.getAxis()==Direction.Axis.Y){
                assertFalse(transform.testHit(null,POS,null,new Vec3(.85,center.y,.0875)),"X严格边界");
                assertFalse(transform.testHit(null,POS,null,new Vec3(.5,center.y,.1625)),"Z严格边界");
                assertTrue(transform.testHit(null,POS,null,new Vec3(.849999,center.y,.0875)));
                assertTrue(transform.testHit(null,POS,null,new Vec3(.5,center.y,.162499)));
            }
        }
    }
    @Test void oldCentersRodHeadAndAdjacentCellsAreNotTargets() {
        var transform=new CornerTransform();
        for(var side:Direction.values()){
            transform.fromSide(side);assertFalse(transform.testHit(null,POS,null,onSurface(side,new Vec3(.5,.5,.5))));
            var center=EXPECTED.get(side);
            for(var delta:new Vec3[]{new Vec3(1,0,0),new Vec3(-1,0,0),new Vec3(0,0,1),new Vec3(0,0,-1)}){
                assertFalse(transform.testHit(null,POS,null,center.add(delta)),"相邻格坐标不是本格热点");
            }
            if(side.getAxis()==Direction.Axis.Y){
                double y=side==Direction.UP?1:0;
                assertFalse(transform.testHit(null,POS,null,new Vec3(.10,y,.10)),"旧角热点撤销");
                for(var point:new Vec3[]{new Vec3(.2,y,.2),new Vec3(.175,y,.175),new Vec3(.5,y,.175),new Vec3(.825,y,.825)}){
                    assertFalse(transform.testHit(null,POS,null,point),"0/50/100行程下相同XZ杆体/方箍投影不得命中");
                }
            }
        }
    }
    @Test void successiveUpSideDownSideChangesRefreshDrawFontAndHitCache() {
        var transform=new CornerTransform();
        for(var side:new Direction[]{Direction.UP,Direction.SOUTH,Direction.DOWN,Direction.EAST}){
            assertSame(transform,transform.fromSide(side));var center=EXPECTED.get(side);boolean top=side.getAxis()==Direction.Axis.Y;
            assertEquals(top?.20f:.40f,transform.getScale());assertEquals(top?1.8f/64:1f/64,transform.getFontScale());
            assertEquals(top,transform.testHit(null,POS,null,tangent(side,center,.30)),"顶底长边与侧面原圆按当前面切换");
            var pose=nativePose();transform.transform(null,POS,null,pose);
            assertEquals(transform.getScale(),new Vector3f(1,0,0).mulDirection(pose.last().pose()).length(),1e-6);
        }
    }
}
