package com.iksxh.create_nuclear_industry.turbine.client;

import com.iksxh.create_nuclear_industry.content.TurbineContent;
import com.iksxh.create_nuclear_industry.turbine.TurbineControllerBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbineOutputShaftBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbinePartBlock;
import com.iksxh.create_nuclear_industry.turbine.TurbineRotorBlockEntity;
import com.iksxh.create_nuclear_industry.turbine.TurbineShaftBlock;
import com.iksxh.create_nuclear_industry.turbine.display.TurbineRuntimeLease;
import com.iksxh.create_nuclear_industry.turbine.display.TurbineRuntimeSample;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 实际生产提交入口负责全部变换；夹具只替代外部世界查询和最终SBB提交。 */
class TurbineAnimationRendererTest {
    @AfterEach void clearClientWorld() { TurbineClientEvents.clearWorld(); }

    @Test void actualSubmissionRotatesAroundLocalCenterBeforeMappingAllFourFacings() {
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        float[][] expected = {{.5f, .75f, .25f}, {.75f, .75f, .5f}, {.5f, .75f, .75f}, {.25f, .75f, .5f}};
        for (int i = 0; i < facings.length; i++) {
            var record = new BufferRecord();
            var outer = new PoseStack();
            outer.translate(10, 20, 30);
            TurbineOutputShaftRenderer.renderPiece(record.buffer(), facings[i], 90, outer, null, 15728880, 7);
            var point = record.local.last().pose().transformPosition(new Vector3f(.75f, .5f, .25f));
            assertEquals(expected[i][0], point.x, 1e-6);
            assertEquals(expected[i][1], point.y, 1e-6);
            assertEquals(expected[i][2], point.z, 1e-6);
            var center = record.local.last().pose().transformPosition(new Vector3f(.5f, .5f, .5f));
            assertEquals(.5, center.x, 1e-6);
            assertEquals(.5, center.y, 1e-6);
            assertEquals(.5, center.z, 1e-6);
            assertEquals(new Matrix4f().translation(10, 20, 30), record.submitted);
            assertEquals(record.submitted, outer.last().pose());
            assertEquals(15728880, record.light);
            assertEquals(7, record.overlay);
            assertEquals(1, record.submissions);
        }
    }

    @Test void actualShaftSpeedKeepsCreateWorldAxisSignInsteadOfUsingGeneratedRpm() {
        var world = world();
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        double[] expected = {48, 312, 312, 48};
        for (int i = 0; i < facings.length; i++) {
            TurbineClientEvents.clearWorld();
            var shaft = shaft(world, new BlockPos(i, 60, 0), facings[i]);
            shaft.setSpeed(16);
            assertEquals(0, shaft.getGeneratedSpeed(), "测试没有机组生成源，但原生真实网络速度仍可非零");
            assertEquals(0, TurbineClientEvents.outputAngle(shaft, world, 0, false), 1e-9);
            for (int tick = 0; tick < 10; tick++) TurbineClientEvents.tickWorld(world, false);
            assertEquals(expected[i], TurbineClientEvents.outputAngle(shaft, world, 0, false), 1e-8);
        }
    }

    @Test void offscreenTickSamplingAnchorsSpeedChangesAndPauseFreezesTheSubmittedAngle() {
        var world = world();
        var shaft = shaft(world, new BlockPos(0, 60, 0), Direction.NORTH);
        shaft.setSpeed(16);
        TurbineClientEvents.outputAngle(shaft, world, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        shaft.setSpeed(32);
        TurbineClientEvents.tickWorld(world, false);
        for (int i = 0; i < 4; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(67.2, TurbineClientEvents.outputAngle(shaft, world, 0, false), 1e-8);
        double paused = TurbineClientEvents.outputAngle(shaft, world, .4f, false);
        for (int i = 0; i < 100; i++) TurbineClientEvents.tickWorld(world, true);
        assertEquals(paused, TurbineClientEvents.outputAngle(shaft, world, .9f, true), 1e-9);
        assertEquals(paused, TurbineClientEvents.outputAngle(shaft, world, 0, true), 1e-9);
    }

    @Test void nativeOverstressAndFrozenSpeedStopTheClientPhase() throws ReflectiveOperationException {
        var world = world();
        var shaft = shaft(world, new BlockPos(0, 60, 0), Direction.NORTH);
        shaft.setSpeed(16);
        TurbineClientEvents.outputAngle(shaft, world, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        Field overstressed = KineticBlockEntity.class.getDeclaredField("overStressed");
        overstressed.setAccessible(true);
        overstressed.setBoolean(shaft, true);
        TurbineClientEvents.outputAngle(shaft, world, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(24, TurbineClientEvents.outputAngle(shaft, world, 0, false), 1e-8);
        overstressed.setBoolean(shaft, false);
        world.tickRateManager().setFrozen(true);
        TurbineClientEvents.outputAngle(shaft, world, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(24, TurbineClientEvents.outputAngle(shaft, world, 0, false), 1e-8);
    }

    @Test void trustedLoadedShaftKeepsStoppedAngleAndSamplesSpeedThroughoutLongOffscreenInterval() {
        var world = world();
        var shaft = shaft(world, new BlockPos(0, 60, 0), Direction.NORTH);
        shaft.setSpeed(16);
        TurbineClientEvents.outputAngle(shaft, world, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        shaft.setSpeed(0);
        assertEquals(24, TurbineClientEvents.outputAngle(shaft, world, 0, false), 1e-8);
        // 不调用BER角度入口；已加载同一实体离屏100tick不是身份失效。
        for (int i = 0; i < 100; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(24, TurbineClientEvents.outputAngle(shaft, world, 0, false), 1e-8);
        shaft.setSpeed(32);
        TurbineClientEvents.tickWorld(world, false);
        for (int i = 0; i < 40; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(48, TurbineClientEvents.outputAngle(shaft, world, 0, false), 1e-8);
    }

    @Test void actualWorldUnloadEventRetractsAnOtherwiseTrustedStoppedPhase() {
        var world = world();
        var shaft = shaft(world, new BlockPos(0, 60, 0), Direction.NORTH);
        shaft.setSpeed(16);
        TurbineClientEvents.outputAngle(shaft, world, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        shaft.setSpeed(0);
        assertEquals(24, TurbineClientEvents.outputAngle(shaft, world, 0, false), 1e-8);
        TurbineClientEvents.GameEvents.unloaded(new LevelEvent.Unload(world));
        assertEquals(0, TurbineClientEvents.outputAngle(shaft, world, 0, false), 1e-9);
    }

    @Test void replacedEntityChunkLossAndWorldChangeRetractOldPhase() {
        var firstWorld = world();
        var pos = new BlockPos(0, 60, 0);
        var first = shaft(firstWorld, pos, Direction.NORTH);
        first.setSpeed(16);
        TurbineClientEvents.outputAngle(first, firstWorld, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(firstWorld, false);
        assertEquals(24, TurbineClientEvents.outputAngle(first, firstWorld, 0, false), 1e-8);
        var replacement = shaft(firstWorld, pos, Direction.NORTH);
        replacement.setSpeed(16);
        assertEquals(0, TurbineClientEvents.outputAngle(replacement, firstWorld, 0, false), 1e-9);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(firstWorld, false);
        firstWorld.loaded = false;
        TurbineClientEvents.tickWorld(firstWorld, false);
        assertEquals(0, TurbineClientEvents.outputAngle(replacement, firstWorld, 0, false), 1e-9);
        firstWorld.loaded = true;
        assertEquals(0, TurbineClientEvents.outputAngle(replacement, firstWorld, 0, false), 1e-9);
        var secondWorld = world();
        var second = shaft(secondWorld, pos, Direction.NORTH);
        second.setSpeed(16);
        assertEquals(0, TurbineClientEvents.outputAngle(second, secondWorld, 0, false), 1e-9);
    }

    @Test void frontAndRearSelectDistinctRotatingPartials() throws ReflectiveOperationException {
        // Flywheel实现只在锁定运行classpath中；反射跨越第三方类型边界，仍调用真实生产选择入口。
        var select = TurbineOutputShaftRenderer.class.getDeclaredMethod("partialFor", TurbineShaftBlock.End.class);
        Object frontPartial = select.invoke(null, TurbineShaftBlock.End.FRONT);
        Object rearPartial = select.invoke(null, TurbineShaftBlock.End.REAR);
        var location = frontPartial.getClass().getMethod("modelLocation");
        var front = (ResourceLocation) location.invoke(frontPartial);
        var rear = (ResourceLocation) location.invoke(rearPartial);
        assertNotEquals(front, rear);
        assertEquals(ResourceLocation.fromNamespaceAndPath("create_nuclear_industry",
                "block/turbine/output_shaft_front_rotating"), front);
        assertEquals(ResourceLocation.fromNamespaceAndPath("create_nuclear_industry",
                "block/turbine/output_shaft_rear_rotating"), rear);
    }

    @Test void unknownContextCannotResetTheCurrentWorldTrackAtTheSamePosition() {
        var current = world();
        var pos = new BlockPos(0, 60, 0);
        var shaft = shaft(current, pos, Direction.NORTH);
        shaft.setSpeed(16);
        TurbineClientEvents.outputAngle(shaft, current, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(current, false);
        var foreign = shaft(world(), pos, Direction.NORTH);
        assertEquals(0, TurbineClientEvents.outputAngle(foreign, current, 0, false), 1e-9);
        assertEquals(24, TurbineClientEvents.outputAngle(shaft, current, 0, false), 1e-8);
    }

    @Test void actualRotorDisplaySharesAllColumnsAndDoesNotFlipLocalRpmAgain() {
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        float[][] expected = {{.5f, .75f, .25f}, {.75f, .75f, .5f}, {.5f, .75f, .75f}, {.25f, .75f, .5f}};
        for (int i = 0; i < facings.length; i++) {
            TurbineClientEvents.clearWorld();
            var world = world();
            var firstValue = value(facings[i], 0, 100, 60, 1);
            var secondValue = value(facings[i], 1, 100, 60, 1);
            var first = rotor(world, firstValue);
            var second = rotor(world, secondValue);
            assertTrue(first.runtimeDisplay().isPresent());
            TurbineClientEvents.rotorPose(first, world, 0, false);
            TurbineClientEvents.rotorPose(second, world, 0, false);
            for (int tick = 0; tick < 5; tick++) TurbineClientEvents.tickWorld(world, false);
            var pose = TurbineClientEvents.rotorPose(first, world, 0, false);
            assertEquals(90, pose.degrees(), 1e-8);
            assertEquals(90, TurbineClientEvents.rotorPose(second, world, 0, false).degrees(), 1e-8);
            var record = new BufferRecord();
            TurbineRotorRenderer.renderPiece(record.buffer(), pose, first.getBlockState(), new PoseStack(), null, 11, 7);
            var point = record.local.last().pose().transformPosition(new Vector3f(.75f, .5f, .25f));
            assertEquals(expected[i][0], point.x, 1e-6);
            assertEquals(expected[i][1], point.y, 1e-6);
            assertEquals(expected[i][2], point.z, 1e-6);
            assertEquals(1, record.submissions);
        }
    }

    @Test void rotorUsesNewestWholeServerTickRegardlessOfMemberArrivalAndRenderOrder() {
        var world = world();
        var first = rotor(world, value(Direction.EAST, 0, 100, 16, 1));
        var second = rotor(world, value(Direction.EAST, 1, 105, 32, 1));
        TurbineClientEvents.rotorPose(second, world, 0, false);
        TurbineClientEvents.rotorPose(first, world, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(48, TurbineClientEvents.rotorPose(first, world, 0, false).degrees(), 1e-8);
        assertEquals(48, TurbineClientEvents.rotorPose(second, world, 0, false).degrees(), 1e-8);
        bind(world, first, value(Direction.EAST, 0, 1_000_000, 0, 1));
        assertEquals(48, TurbineClientEvents.rotorPose(first, world, 0, false).degrees(), 1e-8);
        for (int i = 0; i < 30; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(48, TurbineClientEvents.rotorPose(second, world, 0, false).degrees(), 1e-8);
        bind(world, first, value(Direction.EAST, 0, 1_000_001, -16, 1));
        TurbineClientEvents.rotorPose(first, world, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(24, TurbineClientEvents.rotorPose(second, world, 0, false).degrees(), 1e-8);
    }

    @Test void emptyLastMemberRetiresPhaseAndAnOlderDifferentColumnCannotReviveIt() {
        var world = world();
        var first = rotor(world, value(Direction.NORTH, 0, 105, 16, 1));
        TurbineClientEvents.rotorPose(first, world, 0, false);
        for (int i = 0; i < 10; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(48, TurbineClientEvents.rotorPose(first, world, 0, false).degrees(), 1e-8);
        field(first, TurbineRotorBlockEntity.class, "sample", null);
        assertFalse(TurbineClientEvents.rotorPose(first, world, 0, false).sample().isPresent());
        var late = rotor(world, value(Direction.NORTH, 1, 100, 16, 1));
        assertTrue(late.runtimeDisplay().isPresent(), "不同列的旧包仍可信，但不能恢复组内更高排序水位之前的旋转");
        assertEquals(0, TurbineClientEvents.rotorPose(late, world, 0, false).degrees(), 1e-9);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(0, TurbineClientEvents.rotorPose(late, world, 0, false).degrees(), 1e-9);
        bind(world, late, value(Direction.NORTH, 1, 106, 16, 1));
        assertEquals(0, TurbineClientEvents.rotorPose(late, world, 0, false).degrees(), 1e-9);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(24, TurbineClientEvents.rotorPose(late, world, 0, false).degrees(), 1e-8);
    }

    @Test void losingOneMemberKeepsTheRemainingReliableGroupPhase() {
        var world = world();
        var first = rotor(world, value(Direction.NORTH, 0, 100, 16, 1));
        var second = rotor(world, value(Direction.NORTH, 1, 100, 16, 1));
        TurbineClientEvents.rotorPose(first, world, 0, false);
        TurbineClientEvents.rotorPose(second, world, 0, false);
        for (int i = 0; i < 10; i++) TurbineClientEvents.tickWorld(world, false);
        field(first, TurbineRotorBlockEntity.class, "sample", null);
        assertFalse(TurbineClientEvents.rotorPose(first, world, 0, false).sample().isPresent());
        assertEquals(48, TurbineClientEvents.rotorPose(second, world, 0, false).degrees(), 1e-8);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(72, TurbineClientEvents.rotorPose(second, world, 0, false).degrees(), 1e-8);
    }

    @Test void pauseFreezesRotorPartialButReliableLossStillWithdrawsItImmediately() {
        var world = world();
        var rotor = rotor(world, value(Direction.NORTH, 0, 100, 16, 1));
        TurbineClientEvents.rotorPose(rotor, world, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        double stoppedTime = TurbineClientEvents.rotorPose(rotor, world, .4f, false).degrees();
        for (int i = 0; i < 100; i++) TurbineClientEvents.tickWorld(world, true);
        assertEquals(stoppedTime, TurbineClientEvents.rotorPose(rotor, world, .9f, true).degrees(), 1e-9);
        field(rotor, TurbineRotorBlockEntity.class, "sample", null);
        var empty = TurbineClientEvents.rotorPose(rotor, world, .9f, true);
        assertFalse(empty.sample().isPresent());
        assertEquals(0, empty.degrees(), 1e-9);
    }

    @Test void realWorldUnloadCannotReopenRotorPhaseFromTheStillAttachedOldLevel() {
        var world = world();
        var rotor = rotor(world, value(Direction.NORTH, 0, 100, 16, 1));
        TurbineClientEvents.rotorPose(rotor, world, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(24, TurbineClientEvents.rotorPose(rotor, world, 0, false).degrees(), 1e-8);
        TurbineClientEvents.GameEvents.unloaded(new LevelEvent.Unload(world));
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        assertFalse(TurbineClientEvents.rotorPose(rotor, world, 0, false).sample().isPresent());
        var nextWorld = world();
        var next = rotor(nextWorld, value(Direction.NORTH, 0, 110, 16, 1));
        assertEquals(0, TurbineClientEvents.rotorPose(next, nextWorld, 0, false).degrees(), 1e-9);
    }

    @Test void newEpochAndResourceReloadNeverCarryThePreviousGroupAngle() {
        var world = world();
        var before = rotor(world, value(Direction.NORTH, 0, 100, 16, 1));
        TurbineClientEvents.rotorPose(before, world, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(24, TurbineClientEvents.rotorPose(before, world, 0, false).degrees(), 1e-8);
        var after = rotor(world, value(Direction.NORTH, 0, 110, 16, 2));
        assertEquals(0, TurbineClientEvents.rotorPose(after, world, 0, false).degrees(), 1e-9);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(24, TurbineClientEvents.rotorPose(after, world, 0, false).degrees(), 1e-8);
        TurbineClientEvents.reloaded(null);
        assertEquals(0, TurbineClientEvents.rotorPose(after, world, 0, false).degrees(), 1e-9);
    }

    @Test void sameTickConflictingGroupSamplesStayStaticUntilANewerWholeSample() {
        var world = world();
        var first = rotor(world, value(Direction.NORTH, 0, 100, 16, 1));
        var second = rotor(world, value(Direction.NORTH, 1, 100, 32, 1));
        TurbineClientEvents.rotorPose(first, world, 0, false);
        TurbineClientEvents.rotorPose(second, world, 0, false);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(0, TurbineClientEvents.rotorPose(first, world, 0, false).degrees(), 1e-9);
        assertEquals(0, TurbineClientEvents.rotorPose(second, world, 0, false).degrees(), 1e-9);
        bind(world, first, value(Direction.NORTH, 0, 101, 16, 1));
        assertEquals(0, TurbineClientEvents.rotorPose(first, world, 0, false).degrees(), 1e-9);
        for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
        assertEquals(24, TurbineClientEvents.rotorPose(second, world, 0, false).degrees(), 1e-8);
    }

    @Test void sameClientMemberReplacementResetsSoleGroupForPostFirstAndBerFirst() {
        for (boolean postFirst : new boolean[]{true, false}) {
            TurbineClientEvents.clearWorld();
            var world = world();
            var rotor = rotor(world, value(Direction.NORTH, 0, 100, 16, 1));
            TurbineClientEvents.rotorPose(rotor, world, 0, false);
            for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
            bind(world, rotor, value(Direction.NORTH, 0, 105, 0, 1));
            assertEquals(24, TurbineClientEvents.rotorPose(rotor, world, 0, false).degrees(), 1e-8);
            UUID nextMember = new UUID(0, 90);
            bind(world, rotor, withMember(value(Direction.NORTH, 0, 110, 0, 1), nextMember));
            assertEquals(nextMember, rotor.runtimeDisplay().orElseThrow().member());
            // 同一个客户端BE、同owner generation/epoch，仅可信整包member更换。
            if (postFirst) TurbineClientEvents.tickWorld(world, false);
            else {
                assertEquals(0, TurbineClientEvents.rotorPose(rotor, world, 0, false).degrees(), 1e-9);
                TurbineClientEvents.tickWorld(world, false);
            }
            assertEquals(0, TurbineClientEvents.rotorPose(rotor, world, 0, false).degrees(), 1e-9,
                    "唯一登记成员替换必须撤销旧停角，不能由Post/BER先后决定");
            for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
            assertEquals(0, TurbineClientEvents.rotorPose(rotor, world, 0, false).degrees(), 1e-9);
        }
    }

    @Test void sameClientMemberReplacementKeepsOtherReliableMemberForBothEventOrders() {
        for (boolean postFirst : new boolean[]{true, false}) {
            TurbineClientEvents.clearWorld();
            var world = world();
            var first = rotor(world, value(Direction.NORTH, 0, 100, 16, 1));
            var second = rotor(world, value(Direction.NORTH, 1, 100, 16, 1));
            TurbineClientEvents.rotorPose(first, world, 0, false);
            TurbineClientEvents.rotorPose(second, world, 0, false);
            for (int i = 0; i < 5; i++) TurbineClientEvents.tickWorld(world, false);
            bind(world, first, value(Direction.NORTH, 0, 105, 0, 1));
            bind(world, second, value(Direction.NORTH, 1, 105, 0, 1));
            assertEquals(24, TurbineClientEvents.rotorPose(first, world, 0, false).degrees(), 1e-8);
            bind(world, first, withMember(value(Direction.NORTH, 0, 110, 0, 1), new UUID(0, 90)));
            if (postFirst) TurbineClientEvents.tickWorld(world, false);
            else {
                assertEquals(24, TurbineClientEvents.rotorPose(first, world, 0, false).degrees(), 1e-8);
                TurbineClientEvents.tickWorld(world, false);
            }
            assertEquals(24, TurbineClientEvents.rotorPose(first, world, 0, false).degrees(), 1e-8);
            assertEquals(24, TurbineClientEvents.rotorPose(second, world, 0, false).degrees(), 1e-8,
                    "另一个可靠成员仍支持共享相位，替换单列不能让它跳到0");
        }
    }

    static TurbineRuntimeSample withMember(TurbineRuntimeSample value, UUID member) {
        return new TurbineRuntimeSample(value.dimension(), value.owner(), value.generation(), value.epoch(),
                value.front(), value.facing(), value.diameter(), value.rotors(), value.index(), member,
                value.serverTick(), value.rpm());
    }

    static TurbineRuntimeSample value(Direction facing, int index, long serverTick, float rpm, int epoch) {
        var front = new BlockPos(0, 60, 0);
        var owner = front.relative(facing.getClockWise()).relative(facing.getOpposite(), 2);
        return new TurbineRuntimeSample("minecraft:overworld", owner, new UUID(0, 10), epoch,
                front, facing, 3, 3, index, new UUID(0, index + 20), serverTick, rpm);
    }

    static TurbineRotorBlockEntity rotor(FixtureWorld world, TurbineRuntimeSample value) {
        var state = TurbineContent.ROTOR.get().defaultBlockState().setValue(TurbinePartBlock.LOCATED, true)
                .setValue(TurbinePartBlock.FORMED, true).setValue(TurbinePartBlock.MACHINE_FACING, value.facing())
                .setValue(TurbinePartBlock.DIAMETER, TurbinePartBlock.Diameter.of(value.diameter()));
        var rotor = new TurbineRotorBlockEntity(value.memberPos(), state);
        rotor.setLevel(world);
        world.entities.put(value.memberPos().asLong(), rotor);
        bind(world, rotor, value);
        return rotor;
    }

    /** 仅配置外部同步/租约存储及已加载世界边界；runtimeDisplay、共享phase和提交仍调用真实生产代码。 */
    static void bind(FixtureWorld world, TurbineRotorBlockEntity rotor, TurbineRuntimeSample value) {
        TurbineControllerBlockEntity owner = world.entities.get(value.owner().asLong()) instanceof TurbineControllerBlockEntity known
                ? known : new TurbineControllerBlockEntity(value.owner(), TurbineContent.CONTROLLER.get().defaultBlockState());
        owner.setLevel(world);
        world.entities.put(value.owner().asLong(), owner);
        field(owner, TurbineControllerBlockEntity.class, "clientRuntimeGeneration", value.generation());
        field(owner, TurbineControllerBlockEntity.class, "clientRuntimeEpoch", value.epoch());
        for (int end = 0; end < 2; end++) {
            BlockPos pos = value.front().relative(value.facing().getOpposite(), end == 0 ? 0 : value.rotors() + 1);
            world.states.put(pos.asLong(), TurbineContent.OUTPUT_SHAFT.get().defaultBlockState()
                    .setValue(TurbinePartBlock.FORMED, true).setValue(TurbinePartBlock.MACHINE_FACING, value.facing())
                    .setValue(TurbineShaftBlock.END, end == 0 ? TurbineShaftBlock.End.FRONT : TurbineShaftBlock.End.REAR));
        }
        field(rotor, TurbineRotorBlockEntity.class, "sample", value);
        field(rotor, TurbineRotorBlockEntity.class, "clientMember", value.member());
        field(rotor, TurbineRotorBlockEntity.class, "clientTick", 0L);
        field(rotor, TurbineRotorBlockEntity.class, "clientOwner", new WeakReference<>(owner));
        try {
            Field field = TurbineRotorBlockEntity.class.getDeclaredField("lease");
            field.setAccessible(true);
            Object lease = field.get(rotor);
            field(lease, TurbineRuntimeLease.class, "latest", value);
            field(lease, TurbineRuntimeLease.class, "receivedTick", 0L);
            field(lease, TurbineRuntimeLease.class, "active", true);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    static void field(Object instance, Class<?> owner, String name, Object value) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    static TurbineOutputShaftBlockEntity shaft(FixtureWorld world, BlockPos pos, Direction facing) {
        var state = TurbineContent.OUTPUT_SHAFT.get().defaultBlockState()
                .setValue(TurbinePartBlock.LOCATED, true).setValue(TurbinePartBlock.MACHINE_FACING, facing);
        var shaft = new TurbineOutputShaftBlockEntity(pos, state);
        shaft.setLevel(world);
        world.entities.put(pos.asLong(), shaft);
        return shaft;
    }

    /** 夹具只提供已加载世界/实体/原生TickRateManager，不替代getSpeed、相位或矩阵算法。 */
    static final class FixtureWorld extends ClientLevel {
        Map<Long, BlockEntity> entities;
        Map<Long, BlockState> states;
        TickRateManager rates;
        boolean loaded;
        private FixtureWorld() { super(null, null, null, null, 0, 0, null, null, false, 0); throw new AssertionError(); }
        @Override public boolean hasChunkAt(BlockPos pos) { return loaded; }
        @Override public boolean hasChunk(int x, int z) { return loaded; }
        @Override public BlockEntity getBlockEntity(BlockPos pos) { return entities.get(pos.asLong()); }
        @Override public BlockState getBlockState(BlockPos pos) {
            BlockEntity entity = entities.get(pos.asLong());
            return entity == null ? states.getOrDefault(pos.asLong(), Blocks.AIR.defaultBlockState()) : entity.getBlockState();
        }
        @Override public TickRateManager tickRateManager() { return rates; }
    }

    static FixtureWorld world() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            var world = (FixtureWorld) ((Unsafe) field.get(null)).allocateInstance(FixtureWorld.class);
            world.entities = new HashMap<>();
            world.states = new HashMap<>();
            world.rates = new TickRateManager();
            world.loaded = true;
            field(world, Level.class, "isClientSide", true);
            field(world, Level.class, "dimension", Level.OVERWORLD);
            return world;
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    /** 默认Affine方法照常执行，记录真实PoseStack及提交边界；不在夹具重写四向变换。 */
    static final class BufferRecord implements InvocationHandler {
        final PoseStack local = new PoseStack();
        Matrix4f submitted;
        int light, overlay, submissions;
        SuperByteBuffer buffer() {
            return (SuperByteBuffer) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{SuperByteBuffer.class}, this);
        }
        @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
            switch (method.getName()) {
                case "translate" -> local.translate(((Number) args[0]).doubleValue(),
                        ((Number) args[1]).doubleValue(), ((Number) args[2]).doubleValue());
                case "rotate" -> local.mulPose((Quaternionf) args[0]);
                case "light" -> light = ((Number) args[0]).intValue();
                case "overlay" -> overlay = ((Number) args[0]).intValue();
                case "renderInto" -> {
                    submitted = new Matrix4f(((PoseStack) args[0]).last().pose());
                    submissions++;
                    return null;
                }
                default -> throw new AssertionError("未预期的真实SBB入口：" + method.getName());
            }
            return proxy;
        }
    }
}
