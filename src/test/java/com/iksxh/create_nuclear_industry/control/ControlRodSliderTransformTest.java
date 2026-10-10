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
    /**
     * 锁定ValueBox.render在生产transform后的原生Pose指令；不是复制生产offset或命中算法。
     * 无客户端Font/纹理管理器时只执行最终矩阵，具体完整入口障碍记入输出，不声称游戏render通过。
     */
    private static PoseStack outlinePose(CornerTransform transform) {
        var pose=nativePose();transform.transform(null,POS,null,pose);
        pose.scale(-2.01f,-2.01f,2.01f);pose.translate(-.5,-.5,-1./32);
        return pose;
    }
    private static PoseStack textBaselinePose(CornerTransform transform) {
        var pose=nativePose();transform.transform(null,POS,null,pose);
        float font=-transform.getFontScale();pose.scale(font,font,font);
        // TextValueBox.renderContents的Font.width之前：后续字体适配平移z=0，不改变基线所在深度面。
        pose.scale(3,3,1);pose.translate(-4,-3.75,5);
        return pose;
    }
    private static double outward(Direction side,Vector3f point) {
        return switch(side){case UP->point.y-1;case DOWN->-point.y;case SOUTH->point.z-1;case NORTH->-point.z;case EAST->point.x-1;case WEST->-point.x;};
    }
    @Test void nativeFinalOutlineFourCornersClearAllSixBlockFaces() {
        // 完整原生构造器首先访问Minecraft.level；JUnit无客户端单例，保留实际入口异常边界。
        try {
            new com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.TextValueBox(
                    net.minecraft.network.chat.Component.literal("深度"),new net.minecraft.world.phys.AABB(0,0,0,1,1,1),POS,
                    (net.minecraft.world.level.block.state.BlockState)null,net.minecraft.network.chat.Component.literal("100%"));
            System.out.println("NATIVE_DRAW_ENTRY|构造器可用；未启动Font/纹理绘制");
        }catch(RuntimeException unavailable){System.out.println("NATIVE_DRAW_ENTRY|"+unavailable.getClass().getName()+"|"+unavailable.getMessage());}
        var transform=new CornerTransform();
        for(var side:new Direction[]{Direction.UP,Direction.DOWN,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}){
            transform.fromSide(side);var pose=outlinePose(transform);
            // 宽框实际6PX位于16PX图标中心，四个可见边角为5/16..11/16，透明图标边距不当实体边框。
            for(float x:new float[]{5f/16,11f/16})for(float y:new float[]{5f/16,11f/16}){
                var point=new Vector3f(x,y,0).mulPosition(pose.last().pose());
                System.out.println("FINAL_FRAME|"+side+"|"+point.x+"|"+point.y+"|"+point.z+"|"+outward(side,point));
                assertTrue(outward(side,point)>0,"最终宽框必须在实体面外："+side+" "+point);
                assertEquals(side.getAxis()==Direction.Axis.Y?.01130625:.025125,outward(side,point),1e-6);
                if(side.getAxis()==Direction.Axis.Y){assertTrue(point.x>0 && point.x<.175 && point.z>0 && point.z<.175,"可见框四角仍避开固定方箍");}
            }
        }
    }
    @Test void nativeFinalTextBaselineClearsAllSixBlockFaces() {
        var transform=new CornerTransform();
        for(var side:new Direction[]{Direction.UP,Direction.DOWN,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST}){
            transform.fromSide(side);var point=new Vector3f().mulPosition(textBaselinePose(transform).last().pose());
            System.out.println("FINAL_TEXT|"+side+"|"+point.x+"|"+point.y+"|"+point.z+"|"+outward(side,point));
            assertTrue(outward(side,point)>0,"正常深度测试的文字基线必须在实体面外："+side);
            assertEquals(side.getAxis()==Direction.Axis.Y?.0140625:.03125,outward(side,point),1e-6);
        }
    }
    private static final Map<Direction, Vec3> EXPECTED = Map.of(
            Direction.UP, new Vec3(.10, 1, .10), Direction.DOWN, new Vec3(.10, 0, .10),
            Direction.SOUTH, new Vec3(.25, .75, 1), Direction.NORTH, new Vec3(.75, .75, 0),
            Direction.EAST, new Vec3(1, .75, .75), Direction.WEST, new Vec3(0, .75, .25));

    /** 点击点在原方块真实面上，显示中心与实际鼠标落点共用该块面。 */
    private static Vec3 onSurface(Direction side, Vec3 point) {
        double normal = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
        return switch (side.getAxis()) {
            case X -> new Vec3(normal, point.y, point.z);
            case Y -> new Vec3(point.x, normal, point.z);
            case Z -> new Vec3(point.x, point.y, normal);
        };
    }

    private static Vec3 tangent(Direction side, Vec3 point, double distance) {
        return side.getAxis() == Direction.Axis.X ? point.add(0, distance, 0) : point.add(distance, 0, 0);
    }

    @Test void constructionStartsWithValidUpScaleAndNativeHit() {
        var transform = assertDoesNotThrow(CornerTransform::new);
        assertEquals(Direction.UP, transform.getSide());
        assertEquals(.18f, transform.getScale());
        assertTrue(transform.testHit(null, POS, null, new Vec3(.10, 1, .10)));
        assertFalse(transform.testHit(null, POS, null, new Vec3(.21, 1, .10)), "初始UP缓存不能保留旧.5尺度");
        var pose = nativePose(); transform.transform(null, POS, null, pose);
        assertEquals(.18, new Vector3f(1, 0, 0).mulDirection(pose.last().pose()).length(), 1e-6);
    }

    @Test void sixFacesUseFrozenOffsetsAndActualNativePoseOrientation() {
        var transform = new CornerTransform();
        for (var side : Direction.values()) {
            assertSame(transform, transform.fromSide(side));
            var offset = transform.getLocalOffset(null, POS, null); assertEquals(EXPECTED.get(side), offset);
            assertTrue(offset.x >= 0 && offset.x <= 1 && offset.y >= 0 && offset.y <= 1 && offset.z >= 0 && offset.z <= 1);
            var pose = nativePose(); transform.transform(null, POS, null, pose);
            var origin = new Vector3f().mulPosition(pose.last().pose());
            assertEquals(offset.x, origin.x, 1e-6); assertEquals(offset.y, origin.y, 1e-6); assertEquals(offset.z, origin.z, 1e-6);
            var nativePose = nativePose();
            new CenteredSideValueBoxTransform().fromSide(side).rotate(null, POS, null, nativePose);
            for (var axis : new Vector3f[]{new Vector3f(1,0,0), new Vector3f(0,1,0), new Vector3f(0,0,1)}) {
                var actual = new Vector3f(axis).mulDirection(pose.last().pose());
                var nativeAxis = new Vector3f(axis).mulDirection(nativePose.last().pose()).mul(transform.getScale());
                assertEquals(nativeAxis.x, actual.x, 1e-6); assertEquals(nativeAxis.y, actual.y, 1e-6); assertEquals(nativeAxis.z, actual.z, 1e-6);
            }
            var normal = new Vector3f(0,0,1).mul(pose.last().normal()).normalize();
            var face = side.getNormal(); assertEquals(1, Math.abs(normal.x*face.getX()+normal.y*face.getY()+normal.z*face.getZ()), 1e-6);
            System.out.println("LAYOUT|"+side+"|"+origin.x+"|"+origin.y+"|"+origin.z+"|"+new Vector3f(1,0,0).mulDirection(pose.last().pose()).length());
        }
    }

    @Test void sixRealBlockFacesAcceptInnerCircleAndRejectOutside() {
        var transform = new CornerTransform();
        for (var side : Direction.values()) {
            transform.fromSide(side); var center = onSurface(side, transform.getLocalOffset(null, POS, null));
            boolean vertical = side.getAxis() == Direction.Axis.Y;
            var inside = tangent(side, center, vertical ? .089 : .199);
            var outside = tangent(side, center, vertical ? .091 : .201);
            assertTrue(transform.testHit(null, POS, null, center)); assertTrue(transform.testHit(null, POS, null, inside));
            assertFalse(transform.testHit(null, POS, null, outside));
            System.out.println("HIT|"+side+"|"+inside.x+"|"+inside.y+"|"+inside.z+"|true");
            System.out.println("HIT|"+side+"|"+outside.x+"|"+outside.y+"|"+outside.z+"|false");
        }
    }

    @Test void inheritedSphereUsesStrictRadiusBoundary() {
        var transform = new CornerTransform();
        for (var side : Direction.values()) {
            transform.fromSide(side); var center = transform.getLocalOffset(null, POS, null); double radius = transform.getScale()/2.;
            var normal = side.getNormal();
            var boundary = center.add(normal.getX()*radius, normal.getY()*radius, normal.getZ()*radius);
            assertFalse(transform.testHit(null, POS, null, boundary), "原生严格<边界必须拒绝："+side);
            var justInside = center.add(normal.getX()*(radius-1e-6), normal.getY()*(radius-1e-6), normal.getZ()*(radius-1e-6));
            assertTrue(transform.testHit(null, POS, null, justInside));
        }
    }

    @Test void oldCentersAndVerticalRodOrHeadCornersAreNotTargets() {
        var transform = new CornerTransform();
        for (var side : Direction.values()) {
            transform.fromSide(side); assertFalse(transform.testHit(null, POS, null, onSurface(side, new Vec3(.5,.5,.5))));
            if (side.getAxis() == Direction.Axis.Y) {
                double y = side == Direction.UP ? 1 : 0;
                assertFalse(transform.testHit(null, POS, null, new Vec3(.2,y,.2)), "杆体最近角不属于热点");
                assertFalse(transform.testHit(null, POS, null, new Vec3(.175,y,.175)), "固定方箍最近角也不属于热点");
            }
        }
    }

    @Test void successiveUpSideDownSideChangesRefreshDrawAndHitCache() {
        var transform = new CornerTransform();
        for (var side : new Direction[]{Direction.UP, Direction.SOUTH, Direction.DOWN, Direction.EAST}) {
            assertSame(transform, transform.fromSide(side)); var center = onSurface(side, transform.getLocalOffset(null, POS, null));
            boolean vertical = side.getAxis() == Direction.Axis.Y;
            assertEquals(vertical ? .18f : .40f, transform.getScale());
            assertEquals(!vertical, transform.testHit(null, POS, null, tangent(side, center, .12)), "命中缓存必须跟当前面尺度一起切换");
            var pose = nativePose(); transform.transform(null, POS, null, pose);
            assertEquals(transform.getScale(), new Vector3f(1,0,0).mulDirection(pose.last().pose()).length(), 1e-6);
        }
    }
}
