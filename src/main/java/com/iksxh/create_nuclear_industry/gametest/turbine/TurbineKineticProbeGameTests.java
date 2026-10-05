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

/** 用真实 Create 方块、传播器及网络核对唯一整机总 SU 源、两端贯通共享与失效撤销。 */
@GameTestHolder(TurbineProbeContent.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class TurbineKineticProbeGameTests {
    private static final BlockPos FRONT = new BlockPos(4, 2, 5);

    private TurbineKineticProbeGameTests() {}

    /** 两端机内贯通后，分别从任一端读取同一份机组容量。 */
    @GameTest(template = "probe_empty", timeoutTicks = 150)
    public static void splitJoinAndSplitCapacity(GameTestHelper helper) {
        BlockPos front = helper.absolutePos(FRONT);
        build(helper.getLevel(), front, false);
        helper.runAfterDelay(20, () -> {
            require(helper, capacity(helper.getLevel(), front) == 32768, "前端未取得整机容量");
            require(helper, capacity(helper.getLevel(), front.south(5)) == 32768, "后端未取得整机容量");
            require(helper, shaft(helper.getLevel(), front).network.equals(shaft(helper.getLevel(), front.south(5)).network),
                    "完整机组的两端没有机内贯通");
            for (int z = 1; z <= 4; z++) set(helper.getLevel(), front.south(z),
                    AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
            helper.runAfterDelay(20, () -> {
                require(helper, shaft(helper.getLevel(), front).network.equals(shaft(helper.getLevel(), front.south(5)).network),
                        "联轴后两端没有成为同一个 Create 网络");
                require(helper, capacity(helper.getLevel(), front) == 32768, "外部回接使整机容量重复登记");
                set(helper.getLevel(), front.south(2), Blocks.AIR.defaultBlockState());
                helper.runAfterDelay(20, () -> {
                    require(helper, capacity(helper.getLevel(), front) == 32768, "拆除外部回接后前端容量错误");
                    require(helper, capacity(helper.getLevel(), front.south(5)) == 32768, "拆除外部回接后后端容量错误");
                    helper.succeed();
                });
            });
        });
    }

    /** 无外源断供时，验证父类先清零不会跳过原生拆源及下游速度更新包。 */
    @GameTest(template = "probe_empty", timeoutTicks = 180)
    public static void zeroSupplyDetachesAndNotifiesDownstream(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos front = helper.absolutePos(FRONT);
        BlockPos rear = front.south(5);
        BlockPos frontExternal = front.north();
        BlockPos rearExternal = rear.south();
        build(level, front, false);
        set(level, frontExternal, AllBlocks.SHAFT.getDefaultState()
                .setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        set(level, rearExternal, AllBlocks.SHAFT.getDefaultState()
                .setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        TurbineProbeShaftBlockEntity frontSource = shaft(level, front);
        TurbineProbeShaftBlockEntity rearSource = shaft(level, rear);
        TurbineProbeOwnerBlockEntity owner = (TurbineProbeOwnerBlockEntity) level.getBlockEntity(front.west());
        int[] rearPacketsBefore = {0};
        helper.runAfterDelay(25, () -> {
            require(helper, frontSource.hasNetwork() && frontSource.network.equals(rearSource.network)
                    && Math.abs(frontSource.getTheoreticalSpeed()) == 128
                    && Math.abs(rearSource.getTheoreticalSpeed()) == 128,
                    "断供探针未先建立真实双端Create网络");
            rearPacketsBefore[0] = rearSource.updatePacketCount();
            owner.setSupply(0, false);
        });
        helper.onEachTick(() -> {
            if (owner.shareFor(front) == 0)
                forceCreateKineticValidation(frontSource);
        });
        helper.runAfterDelay(105, () -> {
            TurbineProbeShaftBlockEntity frontNow = shaft(level, front);
            TurbineProbeShaftBlockEntity rearNow = shaft(level, rear);
            var frontExternalNow = (com.simibubi.create.content.kinetics.base.KineticBlockEntity)
                    level.getBlockEntity(frontExternal);
            var rearExternalNow = (com.simibubi.create.content.kinetics.base.KineticBlockEntity)
                    level.getBlockEntity(rearExternal);
            require(helper, !frontNow.hasNetwork() && frontNow.getTheoreticalSpeed() == 0
                    && rearNow.getTheoreticalSpeed() == 0
                    && frontExternalNow.getTheoreticalSpeed() == 0
                    && rearExternalNow.getTheoreticalSpeed() == 0
                    && rearNow.updatePacketCount() > rearPacketsBefore[0],
                    "Create父类先清零后未撤销源网络或通知下游：前网=" + frontNow.network
                            + " 后速=" + rearNow.getTheoreticalSpeed()
                            + " 后Source包=" + rearPacketsBefore[0] + "->" + rearNow.updatePacketCount());
            helper.succeed();
        });
    }

    /** 同速外源正常贡献自己的容量；红石停机仅撤销本机生成容量，恢复后重新登记。 */
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
                        TurbineProbeShaftBlockEntity frontShaft = shaft(level, front);
                        TurbineProbeShaftBlockEntity rearShaft = shaft(level, front.south(5));
                        require(helper, capacity(level, front) == motorSu
                                        && frontShaft.getGeneratedSpeed() == 0
                                        && Math.abs(frontShaft.getTheoreticalSpeed()) == 128
                                        && Math.abs(rearShaft.getTheoreticalSpeed()) == 128
                                        && Math.abs(motor.getTheoreticalSpeed()) == 128,
                                "同速外源下断汽未只撤本机容量并保留外源转速：网=" + capacity(level, front)
                                        + " 外源SU=" + motorSu + " 前源=" + frontShaft.getGeneratedSpeed()
                                        + " 前/后/电机RPM=" + frontShaft.getTheoreticalSpeed() + "/"
                                        + rearShaft.getTheoreticalSpeed() + "/" + motor.getTheoreticalSpeed());
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
                        && shaft(level, front.south(5)).getTheoreticalSpeed() == 128) {
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

    /** 前轴唯一源真实卸载、后轴与外源仍加载时，后网不能保留前轴的卸载容量。 */
    @GameTest(template = "probe_empty", timeoutTicks = 700)
    public static void sourceChunkUnloadWhileRearExternalNetworkRemains(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(768, 2, 256));
        BlockPos front = new BlockPos((anchor.getX() & ~15) + 15, anchor.getY(),
                (anchor.getZ() & ~15) + 14);
        BlockPos rear = front.south(5);
        BlockPos motorPos = rear.south(33);
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        chunks.add(new ChunkPos(front.west()));
        chunks.add(new ChunkPos(rear));
        chunks.add(new ChunkPos(front.east().south(1)));
        chunks.add(new ChunkPos(front.east().south(4)));
        ChunkPos rearChunk = new ChunkPos(rear);
        ChunkPos frontChunk = new ChunkPos(front);
        for (ChunkPos chunk : chunks) level.setChunkForced(chunk.x, chunk.z, true);
        ChunkPos motorChunk = new ChunkPos(motorPos);
        level.setChunkForced(motorChunk.x, motorChunk.z, true);
        build(level, front, false);
        for (int z = 1; z <= 32; z++) set(level, rear.south(z),
                AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.Z));
        set(level, motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState()
                .setValue(CreativeMotorBlock.FACING, Direction.NORTH));
        CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) level.getBlockEntity(motorPos);
        motor.generatedSpeed.setValue(-128);
        float motorSu = motor.calculateAddedStressCapacity() * Math.abs(motor.getGeneratedSpeed());
        long[] since = {level.getGameTime()};
        int[] phase = {0};
        helper.onEachTick(() -> {
            long elapsed = level.getGameTime() - since[0];
            if (phase[0] == 0 && elapsed >= 25) {
                require(helper, capacity(level, rear) == motorSu + 32768,
                        "反向卸载前后轴未与外源共享容量：后端=" + capacity(level, rear)
                                + " 电机=" + motorSu + " 前轴=" + level.getBlockState(front)
                                + " 后轴=" + level.getBlockState(rear)
                                + " 电机速度=" + motor.getTheoreticalSpeed());
                for (ChunkPos chunk : chunks) level.setChunkForced(chunk.x, chunk.z, false);
                phase[0] = 1;
                since[0] = level.getGameTime();
            } else if (phase[0] == 1) {
                boolean partial = level.getChunkSource().getChunkNow(frontChunk.x, frontChunk.z) == null
                        && level.getChunkSource().getChunkNow(rearChunk.x, rearChunk.z) != null;
                if (!partial && elapsed <= 400) return;
                if (!partial) {
                    level.setChunkForced(motorChunk.x, motorChunk.z, false);
                    helper.fail("前轴源未真实卸载且后轴保留加载：前端="
                            + (level.getChunkSource().getChunkNow(frontChunk.x, frontChunk.z) != null)
                            + " 后端=" + (level.getChunkSource().getChunkNow(rearChunk.x, rearChunk.z) != null));
                    return;
                }
                require(helper, motor.hasNetwork() && motor.getOrCreateNetwork().calculateCapacity() == motorSu,
                        "前轴源真实卸载后后轴外源网络留有幽灵 SU");
                for (ChunkPos chunk : chunks) level.setChunkForced(chunk.x, chunk.z, true);
                phase[0] = 2;
                since[0] = level.getGameTime();
            } else if (phase[0] == 2) {
                boolean restored = chunks.stream().allMatch(chunk ->
                        level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null)
                        && motor.hasNetwork() && motor.getOrCreateNetwork().calculateCapacity() == motorSu + 32768;
                if (restored) {
                    for (ChunkPos chunk : chunks) level.setChunkForced(chunk.x, chunk.z, false);
                    level.setChunkForced(motorChunk.x, motorChunk.z, false);
                    helper.succeed();
                } else if (elapsed > 150) {
                    for (ChunkPos chunk : chunks) level.setChunkForced(chunk.x, chunk.z, false);
                    level.setChunkForced(motorChunk.x, motorChunk.z, false);
                    helper.fail("前轴源重载后容量未恰好恢复");
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

    /** 仅在 GameTest 中将真实 Create 校验安排到供能归零后的下一 tick。 */
    private static void forceCreateKineticValidation(TurbineProbeShaftBlockEntity shaft) {
        try {
            var field = com.simibubi.create.content.kinetics.base.KineticBlockEntity.class
                    .getDeclaredField("validationCountdown");
            field.setAccessible(true);
            field.setInt(shaft, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法固定Create动力校验时序", exception);
        }
    }

    static float capacity(ServerLevel level, BlockPos pos) {
        TurbineProbeShaftBlockEntity source = shaft(level, pos);
        return source.hasNetwork() ? source.getOrCreateNetwork().calculateCapacity() : 0;
    }

    static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
