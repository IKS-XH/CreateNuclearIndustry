package com.iksxh.create_nuclear_industry.gametest.turbine;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.LinkedHashSet;
import java.util.Set;

/** 用真实 Create 方块、传播器及网络核对双轴份额与撤销。 */
@GameTestHolder(TurbineProbeContent.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class TurbineKineticProbeGameTests {
    private static final BlockPos FRONT = new BlockPos(4, 2, 5);

    private TurbineKineticProbeGameTests() {}

    /** 单端、同网与拆网反复核对网络容量，未连通端不得把半额转给另一端。 */
    @GameTest(template = "probe_empty", timeoutTicks = 150)
    public static void splitJoinAndSplitCapacity(GameTestHelper helper) {
        BlockPos front = helper.absolutePos(FRONT);
        build(helper.getLevel(), front, false);
        helper.runAfterDelay(20, () -> {
            require(helper, capacity(helper.getLevel(), front) == 16384, "单端网络容量不等于半额");
            require(helper, capacity(helper.getLevel(), front.south(5)) == 16384, "后轴网络容量不等于半额");
            require(helper, !shaft(helper.getLevel(), front).network.equals(shaft(helper.getLevel(), front.south(5)).network),
                    "没有联轴时两端意外同网");
            for (int z = 1; z <= 4; z++) set(helper.getLevel(), front.south(z),
                    AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
            helper.runAfterDelay(20, () -> {
                require(helper, shaft(helper.getLevel(), front).network.equals(shaft(helper.getLevel(), front.south(5)).network),
                        "联轴后两端没有成为同一个 Create 网络");
                require(helper, capacity(helper.getLevel(), front) == 32768, "同网容量没有恰好合计一次总 SU");
                set(helper.getLevel(), front.south(2), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(20, () -> {
                    require(helper, capacity(helper.getLevel(), front) == 16384, "拆网后前端没有恢复半额");
                    require(helper, capacity(helper.getLevel(), front.south(5)) == 16384, "拆网后后端没有恢复半额");
                    helper.succeed();
                });
            });
        });
    }

    /** 同速外源正常贡献自己的容量；红石停机仅撤销本机份额，恢复后重新登记。 */
    @GameTest(template = "probe_empty", timeoutTicks = 150)
    public static void redstoneStopWithSameSpeedExternalSource(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos front = helper.absolutePos(FRONT);
        build(level, front, true);
        BlockPos motorPos = front.north(2);
        set(level, front.north(), AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        set(level, motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) level.getBlockEntity(motorPos);
        motor.generatedSpeed.setValue(128);
        helper.runAfterDelay(25, () -> {
            float motorSu = motor.calculateAddedStressCapacity() * Math.abs(motor.getGeneratedSpeed());
            require(helper, motorSu > 0 && capacity(level, front) == 32768 + motorSu,
                    "同速外源与双轴容量没有分别且仅一次登记");
            set(level, front.west().north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            helper.runAfterDelay(15, () -> {
                require(helper, capacity(level, front) == motorSu, "红石停机后仍保留汽轮机容量");
                set(level, front.west().north(), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(15, () -> {
                    require(helper, capacity(level, front) == motorSu + 32768, "解除红石后容量未恢复或重复登记");
                    ((TurbineProbeOwnerBlockEntity) level.getBlockEntity(front.west())).setSupply(0, false);
                    helper.runAfterDelay(10, () -> {
                        require(helper, capacity(level, front) == motorSu, "账本断供后仍保留本机容量");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /**
     * 远离 GameTest 票据，先证明前端仍加载而后轴与局部机身真实卸载，
 * 再撤销远侧 Creative Motor 票据直到机身与电机区块真实卸载；重载后检查 Create 容量缓存。
     */
    @GameTest(template = "probe_empty", timeoutTicks = 1100)
    public static void crossChunkRealUnloadReloadClearsCapacity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(256, 2, 256));
        BlockPos front = new BlockPos((anchor.getX() & ~15) + 15, anchor.getY(),
                (anchor.getZ() & ~15) + 14);
        BlockPos motorPos = front.north(33);
        Set<ChunkPos> machineChunks = new LinkedHashSet<>();
        machineChunks.add(new ChunkPos(front.west()));
        machineChunks.add(new ChunkPos(front.south(5)));
        machineChunks.add(new ChunkPos(front.east().south(1)));
        machineChunks.add(new ChunkPos(front.east().south(4)));
        require(helper, machineChunks.size() == 4, "布局没有覆盖控制器/前轴、后轴及两段机身的四个 chunk");
        ChunkPos motorChunk = new ChunkPos(motorPos);
        for (ChunkPos chunk : machineChunks) level.setChunkForced(chunk.x, chunk.z, true);
        level.setChunkForced(motorChunk.x, motorChunk.z, true);
        build(level, front, true);
        for (int z = 1; z <= 32; z++) set(level, front.north(z),
                AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        set(level, motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.SOUTH));
        CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) level.getBlockEntity(motorPos);
        motor.generatedSpeed.setValue(128);
        float motorSu = motor.calculateAddedStressCapacity() * Math.abs(motor.getGeneratedSpeed());
        ChunkPos frontChunk = new ChunkPos(front);
        ChunkPos rearChunk = new ChunkPos(front.south(5));
        ChunkPos farBodyChunk = new ChunkPos(front.east().south(4));
        long[] phaseStart = {level.getGameTime()};
        int[] phase = {0};
        helper.onEachTick(() -> {
            long elapsed = level.getGameTime() - phaseStart[0];
            if (phase[0] == 0 && elapsed >= 25) {
                require(helper, capacity(level, front) == motorSu + 32768,
                        "跨四个区块的双轴初始容量或同速外源容量错误");
                for (ChunkPos chunk : machineChunks) level.setChunkForced(chunk.x, chunk.z, false);
                phase[0] = 1;
                phaseStart[0] = level.getGameTime();
            } else if (phase[0] == 1) {
                boolean partial = level.getChunkSource().getChunkNow(frontChunk.x, frontChunk.z) != null
                        && level.getChunkSource().getChunkNow(rearChunk.x, rearChunk.z) == null
                        && level.getChunkSource().getChunkNow(farBodyChunk.x, farBodyChunk.z) == null;
                if (!partial && elapsed <= 400) return;
                if (!partial) {
                    level.setChunkForced(motorChunk.x, motorChunk.z, false);
                    String state = machineChunks.stream().map(chunk -> chunk + ":"
                            + (level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null ? "loaded" : "absent")
                            + "/forced=" + level.getForcedChunks().contains(chunk.toLong()))
                            .toList().toString();
                    helper.fail("撤票后未形成前端仍加载、后轴与局部机身真实卸载的边界：" + state);
                    return;
                }
                require(helper, motor.hasNetwork() && motor.getOrCreateNetwork().calculateCapacity() == motorSu,
                        "后轴/局部机身真实卸载后远侧Create网络仍有幽灵SU/unloadedCapacity");
                level.setChunkForced(motorChunk.x, motorChunk.z, false);
                phase[0] = 2;
                phaseStart[0] = level.getGameTime();
            } else if (phase[0] == 2) {
                boolean allGone = machineChunks.stream().allMatch(chunk ->
                        level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null)
                        && level.getChunkSource().getChunkNow(motorChunk.x, motorChunk.z) == null;
                if (allGone) {
                    for (ChunkPos chunk : machineChunks) level.setChunkForced(chunk.x, chunk.z, true);
                    level.setChunkForced(motorChunk.x, motorChunk.z, true);
                    phase[0] = 3;
                    phaseStart[0] = level.getGameTime();
                } else if (elapsed > 400) {
                    helper.fail("远端观察源撤票后仍有区块保留，未证实整网真实卸载");
                }
            } else if (phase[0] == 3) {
                boolean allLoaded = machineChunks.stream().allMatch(chunk ->
                        level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null)
                        && level.getChunkSource().getChunkNow(motorChunk.x, motorChunk.z) != null;
                if (allLoaded && level.getBlockEntity(motorPos) instanceof CreativeMotorBlockEntity reloadedMotor
                        && reloadedMotor.hasNetwork()
                        && reloadedMotor.getOrCreateNetwork().calculateCapacity() == motorSu + 32768
                        && shaft(level, front).getGeneratedSpeed() == 128
                        && shaft(level, front.south(5)).getGeneratedSpeed() == 128) {
                    for (ChunkPos chunk : machineChunks) level.setChunkForced(chunk.x, chunk.z, false);
                    level.setChunkForced(motorChunk.x, motorChunk.z, false);
                    helper.succeed();
                } else if (elapsed > 150) {
                    for (ChunkPos chunk : machineChunks) level.setChunkForced(chunk.x, chunk.z, false);
                    level.setChunkForced(motorChunk.x, motorChunk.z, false);
                    helper.fail("重载后双轴容量未恰好恢复，或Create保留了重复容量");
                }
            }
        });
    }

    /** 搭建机主、两端轴与独立机身；中间联轴可独立拆除而不使机身失效。 */
    static void build(ServerLevel level, BlockPos front, boolean connected) {
        set(level, front.west(), TurbineProbeContent.owner().defaultBlockState());
        set(level, front, TurbineProbeContent.shaft().defaultBlockState()
                .setValue(TurbineProbeShaftBlock.AXIS, Direction.Axis.Z));
        set(level, front.south(5), TurbineProbeContent.shaft().defaultBlockState()
                .setValue(TurbineProbeShaftBlock.AXIS, Direction.Axis.Z)
                .setValue(TurbineProbeShaftBlock.REAR, true));
        for (int z = 1; z <= 4; z++) {
            set(level, front.east().south(z), Blocks.IRON_BLOCK.defaultBlockState());
            if (connected) set(level, front.south(z), AllBlocks.SHAFT.getDefaultState()
                    .setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        }
    }

    static void set(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    static TurbineProbeShaftBlockEntity shaft(ServerLevel level, BlockPos pos) {
        return (TurbineProbeShaftBlockEntity) level.getBlockEntity(pos);
    }

    static float capacity(ServerLevel level, BlockPos pos) {
        TurbineProbeShaftBlockEntity source = shaft(level, pos);
        return source.hasNetwork() ? source.getOrCreateNetwork().calculateCapacity() : 0;
    }

    static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
