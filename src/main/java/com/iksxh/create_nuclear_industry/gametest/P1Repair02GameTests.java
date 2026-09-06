package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderService;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnRepairTransaction;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

/** P1-REPAIR-02 真实控制棒驱动器钢板维修与卡死解除边界验收。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1Repair02GameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos TARGET_DRIVE = new BlockPos(2, 4, 2);
    private static final BlockPos OTHER_DRIVE = new BlockPos(1, 4, 1);
    private static final CoreColumnPosition TARGET_COLUMN = new CoreColumnPosition(1, 1);
    private static final CoreColumnPosition OTHER_COLUMN = new CoreColumnPosition(0, 0);
    private static final double REPAIR_AMOUNT = 0.25D / ReactorSnapshot.INTERNAL_HEIGHT;

    private P1Repair02GameTests() {
    }

    /** 卡死控制棒可逐次维修，跨过失效阈值后解除卡死且不重置插入深度。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void jammedControlRodRepairsUntilUnjammed(GameTestHelper helper) {
        buildRepairStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ControlRodColumnState jammed = new ControlRodColumnState(
                    0.0D, 0.8D, 0.35D, true, 2.0D);
            ControlRodColumnState other = new ControlRodColumnState(
                    0.7D, 0.4D, 0.4D, false, 1.0D);
            instrument.setSnapshot(snapshot(jammed, other, 19L, true));

            Player player = playerAtDrive(helper, TARGET_DRIVE);
            player.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(ModItems.STEEL_PLATE.get(), 16));
            int repairs = 0;
            while (instrument.snapshot().controlRodColumns().get(TARGET_COLUMN).jammed()
                    && repairs < 16) {
                ItemInteractionResult result = rightClick(helper, TARGET_DRIVE, player);
                require(helper, result.consumesAction(), "控制棒维修没有消费方块交互");
                repairs++;
                require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).getCount()
                                == 16 - repairs,
                        "控制棒维修没有逐次消耗一块合金钢板");
            }

            ControlRodColumnState repaired = instrument.snapshot().controlRodColumns()
                    .get(TARGET_COLUMN);
            require(helper, !repaired.jammed(), "控制棒完整度恢复后仍然保持卡死");
            require(helper, repaired.integrity() > 0.0D,
                    "卡死控制棒维修没有恢复完整度");
            require(helper, close(repaired.targetDepth(), 0.8D)
                            && close(repaired.actualDepth(), 0.35D),
                    "解除卡死错误重置了控制棒插入深度");
            require(helper, close(repaired.cachedHeatHu(), 2.0D),
                    "维修错误清除了控制棒缓存热量");
            require(helper, instrument.snapshot().controlRodColumns().get(OTHER_COLUMN)
                            .equals(other),
                    "维修错误修改了未绑定的其他控制棒列");
            require(helper, instrument.snapshot().meltdownProgressTicks() == 19L
                            && instrument.snapshot().meltdownCountdownStarted(),
                    "维修错误回退或清除了融毁进度");
            helper.succeed();
        });
    }

    /** 非卡死受损控制棒只恢复绑定列，并保留另一列、深度与融毁状态。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void nonJammedControlRodRepairsOnlyBoundColumn(GameTestHelper helper) {
        buildRepairStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ControlRodColumnState target = new ControlRodColumnState(
                    0.5D, 0.2D, 0.15D, false, 1.0D);
            ControlRodColumnState other = new ControlRodColumnState(
                    0.4D, 0.7D, 0.6D, true, 3.0D);
            instrument.setSnapshot(snapshot(target, other, 7L, true));

            Player player = playerAtDrive(helper, TARGET_DRIVE);
            player.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(ModItems.STEEL_PLATE.get()));
            rightClick(helper, TARGET_DRIVE, player);

            ControlRodColumnState repaired = instrument.snapshot().controlRodColumns()
                    .get(TARGET_COLUMN);
            require(helper, close(repaired.integrity(), 0.5D + REPAIR_AMOUNT),
                    "非卡死控制棒没有恢复固定完整度");
            require(helper, close(repaired.targetDepth(), 0.2D)
                            && close(repaired.actualDepth(), 0.15D)
                            && close(repaired.cachedHeatHu(), 1.0D),
                    "非卡死控制棒维修修改了非完整度字段");
            require(helper, instrument.snapshot().controlRodColumns().get(OTHER_COLUMN)
                            .equals(other),
                    "非卡死控制棒维修修改了其他绑定列");
            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                    "非卡死控制棒维修没有消耗一块钢板");
            require(helper, instrument.snapshot().meltdownProgressTicks() == 7L
                            && instrument.snapshot().meltdownCountdownStarted(),
                    "非卡死控制棒维修回退了融毁状态");
            helper.succeed();
        });
    }

    /** 没有钢板时服务端拒绝维修，并保持控制棒列和输入状态不变。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void missingPlateDoesNotChangeControlRodState(GameTestHelper helper) {
        buildRepairStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ControlRodColumnState target = new ControlRodColumnState(
                    0.5D, 0.2D, 0.15D, false, 1.0D);
            ControlRodColumnState other = new ControlRodColumnState(
                    0.4D, 0.7D, 0.6D, false, 3.0D);
            instrument.setSnapshot(snapshot(target, other, 0L, false));
            ReactorSnapshot before = instrument.snapshot();

            Player player = playerAtDrive(helper, TARGET_DRIVE);
            ControlRodColumnRepairTransaction.Result result =
                    ControlRodSliderService.repairFromPlayer(
                            player, helper.absolutePos(TARGET_DRIVE), ItemStack.EMPTY);

            require(helper, result.status() == ControlRodColumnRepairTransaction.Status.EMPTY_INPUT,
                    "没有钢板时没有返回物品不足原因");
            require(helper, instrument.snapshot().equals(before),
                    "没有钢板时错误修改了控制棒权威快照");
            helper.succeed();
        });
    }

    /** 错误驱动器位置不能借维修入口修改任一反应堆状态。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void unboundDriveRejectsRepairWithoutMutation(GameTestHelper helper) {
        buildRepairStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ControlRodColumnState target = new ControlRodColumnState(
                    0.5D, 0.2D, 0.15D, false, 1.0D);
            ControlRodColumnState other = new ControlRodColumnState(
                    0.4D, 0.7D, 0.6D, false, 3.0D);
            instrument.setSnapshot(snapshot(target, other, 0L, false));
            Player player = playerAtDrive(helper, TARGET_DRIVE);
            ItemStack input = new ItemStack(ModItems.STEEL_PLATE.get(), 2);

            ControlRodColumnRepairTransaction.Result result =
                    ControlRodSliderService.repairFromPlayer(
                            player, helper.absolutePos(new BlockPos(4, 4, 4)), input);

            require(helper, result.status() == ControlRodColumnRepairTransaction.Status.INVALID_DRIVE,
                    "未绑定驱动器位置没有被拒绝");
            require(helper, result.remainingInput().getCount() == 2,
                    "无效驱动器错误消耗了钢板");
            require(helper, instrument.snapshot().equals(
                            snapshot(target, other, 0L, false)),
                    "无效驱动器修改了反应堆权威快照");
            helper.succeed();
        });
    }

    /** 构造目标列和另一根控制棒列均有效的固定五乘五乘五结构。 */
    private static void buildRepairStructure(GameTestHelper helper) {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> columns =
                new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());
        columns.put(TARGET_COLUMN, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        columns.put(OTHER_COLUMN, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.templateFor(columns).entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 构造只替换控制棒列的权威快照，并保留事故状态字段。 */
    private static ReactorSnapshot snapshot(
            ControlRodColumnState target,
            ControlRodColumnState other,
            long meltdownProgress,
            boolean meltdownStarted
    ) {
        return new ReactorSnapshot(
                Map.of(),
                Map.of(TARGET_COLUMN, target, OTHER_COLUMN, other),
                17L,
                23L,
                meltdownProgress,
                meltdownStarted);
    }

    /** 调用真实控制棒驱动器方块的服务端玩家交互入口。 */
    private static ItemInteractionResult rightClick(
            GameTestHelper helper,
            BlockPos drive,
            Player player
    ) {
        BlockPos absolute = helper.absolutePos(drive);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        return helper.getLevel().getBlockState(absolute).useItemOn(
                player.getItemInHand(InteractionHand.MAIN_HAND),
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                hit);
    }

    private static Player playerAtDrive(GameTestHelper helper, BlockPos drive) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absolute = helper.absolutePos(drive);
        player.setPos(absolute.getX() + 0.5D, absolute.getY() + 0.5D,
                absolute.getZ() + 2.0D);
        return player;
    }

    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var entity = helper.getBlockEntity(INSTRUMENT);
        require(helper, entity instanceof ReactorInstrumentPortBlockEntity,
                "固定坐标没有仪表端口方块实体");
        return (ReactorInstrumentPortBlockEntity) entity;
    }

    private static Block blockForId(String id) {
        return switch (id) {
            case "minecraft:air" -> Blocks.AIR;
            case "create_nuclear_industry:reactor_casing" -> P1Blocks.REACTOR_CASING.get();
            case "create_nuclear_industry:reactor_window" -> P1Blocks.REACTOR_WINDOW.get();
            case "create_nuclear_industry:reactor_instrument_port" ->
                    P1Blocks.REACTOR_INSTRUMENT_PORT.get();
            case "create_nuclear_industry:reactor_cold_port" -> P1Blocks.REACTOR_COLD_PORT.get();
            case "create_nuclear_industry:reactor_hot_port" -> P1Blocks.REACTOR_HOT_PORT.get();
            case "create_nuclear_industry:reactor_refueling_port" ->
                    P1Blocks.REACTOR_REFUELING_PORT.get();
            case "create_nuclear_industry:reactor_fuel_rod" -> P1Blocks.REACTOR_FUEL_ROD.get();
            case "create_nuclear_industry:control_rod_drive" -> P1Blocks.CONTROL_ROD_DRIVE.get();
            default -> throw new IllegalArgumentException("未知反应堆方块 ID：" + id);
        };
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) <= 1.0E-12D;
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
