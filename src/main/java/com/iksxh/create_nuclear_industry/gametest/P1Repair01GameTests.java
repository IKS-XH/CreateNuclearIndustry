package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnRepairTransaction;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

/** P1-REPAIR-01 真实换料端口钢板维修交互与服务端状态边界验收。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1Repair01GameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos REFUELING_PORT = new BlockPos(1, 4, 1);
    private static final CoreColumnPosition TEST_COLUMN = new CoreColumnPosition(0, 0);
    private static final CoreColumnPosition CONTROL_COLUMN = new CoreColumnPosition(1, 0);
    private static final int MAX_DAMAGE = ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY;
    private static final double REPAIR_AMOUNT = 0.25D / ReactorSnapshot.INTERNAL_HEIGHT;

    private P1Repair01GameTests() {
    }

    /** 真实右键连续消耗钢板，并只恢复目标燃料列的固定完整度。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void fuelPortConsumesSteelPlateAndRepairsOneColumn(GameTestHelper helper) {
        buildSafeRepairStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            port.setFuelAssembly(freshFuel(12_345));
            instrument.setSnapshot(snapshot(0.5D, 0.0D, 0L, false, true));

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(ModItems.STEEL_PLATE.get(), 3));
            ItemInteractionResult result = rightClick(helper, player);

            require(helper, result.consumesAction(), "钢板维修没有消费方块交互");
            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 2,
                    "成功维修没有只消耗一块合金钢板");
            require(helper, close(instrument.snapshot().fuelColumns().get(TEST_COLUMN).integrity(),
                            0.5D + REPAIR_AMOUNT),
                    "燃料列完整度没有按 0.25 / internalHeight 恢复");
            require(helper, instrument.snapshot().fuelColumns().get(TEST_COLUMN)
                            .fuelAssembly().damage() == 12_345,
                    "维修错误修改了燃料组件耐久");
            helper.succeed();
        });
    }

    /** 最后一次维修封顶到 1.0，满完整度再次使用钢板不产生消耗。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void fuelPortCapsRepairAndRejectsFullColumn(GameTestHelper helper) {
        buildSafeRepairStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            port.setFuelAssembly(freshFuel(22_222));
            instrument.setSnapshot(snapshot(0.95D, 0.0D, 0L, false, true));

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(ModItems.STEEL_PLATE.get(), 2));
            rightClick(helper, player);
            require(helper, close(instrument.snapshot().fuelColumns().get(TEST_COLUMN).integrity(), 1.0D),
                    "最后一次维修没有封顶到完整度 1.0");
            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
                    "封顶维修没有只消耗一块钢板");

            rightClick(helper, player);
            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
                    "满完整度列错误消耗了合金钢板");
            require(helper, close(instrument.snapshot().fuelColumns().get(TEST_COLUMN).integrity(), 1.0D),
                    "满完整度列被重复维修改变");
            helper.succeed();
        });
    }

    /** 目标列仍在产生裂变热时拒绝维修，并保持钢板、耐久和完整度不变。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void activeFuelColumnRejectsRepair(GameTestHelper helper) {
        buildActiveRepairStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            port.setFuelAssembly(freshFuel(33_333));
            instrument.setSnapshot(snapshot(0.5D, 0.0D, 0L, false, false));

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(ModItems.STEEL_PLATE.get(), 2));
            rightClick(helper, player);

            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 2,
                    "放热中的燃料列错误消耗了合金钢板");
            require(helper, close(instrument.snapshot().fuelColumns().get(TEST_COLUMN).integrity(), 0.5D),
                    "放热中的燃料列错误发生维修");
            helper.succeed();
        });
    }

    /** 有缓存余热但当前裂变发热为零时允许维修，并保留余热及小数账本。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void residualHeatDoesNotBlockRepairOrDisappear(GameTestHelper helper) {
        buildSafeRepairStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            port.setFuelAssembly(freshFuel(44_444));
            instrument.setSnapshot(snapshotWithColumn(
                    0.5D, 8.0D, 0.4D, 2.0D, 0L, false, true,
                    FuelAssemblyState.installed(MAX_DAMAGE, 0)));

            FuelColumnRepairTransaction.Result result = port.tryRepairFuelColumn(
                    new ItemStack(ModItems.STEEL_PLATE.get()));
            FuelColumnState repaired = instrument.snapshot().fuelColumns().get(TEST_COLUMN);
            require(helper, result.success(), "存在余热但当前停止放热时错误拒绝维修");
            require(helper, close(repaired.integrity(), 0.5D + REPAIR_AMOUNT),
                    "余热场景没有恢复燃料列完整度");
            require(helper, close(repaired.cachedHeatHu(), 8.0D)
                            && close(repaired.fuelBurnRemainder(), 0.4D)
                            && close(repaired.quantizedHeatRemainderHu(), 2.0D),
                    "维修清除了燃料列余热或小数账本");
            helper.succeed();
        });
    }

    /** 空燃料端口仍可维修结构完整度，但钢板维修不得凭空补入燃料组件。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void emptyFuelColumnRepairsWithoutAddingFuel(GameTestHelper helper) {
        buildSafeRepairStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            instrument.setSnapshot(snapshotWithColumn(
                    0.5D, 0.0D, 0.0D, 0.0D, 0L, false, true,
                    FuelAssemblyState.empty()));

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(ModItems.STEEL_PLATE.get()));
            ItemInteractionResult result = rightClick(helper, player);

            require(helper, result.consumesAction(), "空燃料列维修没有消费方块交互");
            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                    "空燃料列维修没有消耗一块钢板");
            FuelColumnState repaired = instrument.snapshot().fuelColumns().get(TEST_COLUMN);
            require(helper, close(repaired.integrity(), 0.5D + REPAIR_AMOUNT),
                    "空燃料列没有恢复固定完整度");
            require(helper, repaired.fuelAssembly().equals(FuelAssemblyState.empty()),
                    "空燃料列维修错误补入了燃料组件");
            require(helper, port.fuelAssembly().isEmpty(),
                    "空燃料端口维修后错误出现燃料组件");
            helper.succeed();
        });
    }

    /** 维修融毁倒计时中的受损列时只改变完整度，进度和启动标志不得回退。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void repairDoesNotRollbackMeltdownProgress(GameTestHelper helper) {
        buildSafeRepairStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            port.setFuelAssembly(freshFuel(55_555));
            instrument.setSnapshot(snapshot(0.5D, 0.0D, 37L, true, true));

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(ModItems.STEEL_PLATE.get()));
            rightClick(helper, player);

            require(helper, instrument.snapshot().meltdownProgressTicks() == 37L
                            && instrument.snapshot().meltdownCountdownStarted(),
                    "维修错误回退或清除了融毁倒计时");
            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                    "融毁倒计时中的安全停热维修没有消耗一块钢板");
            helper.succeed();
        });
    }

    /** 空输入由服务端事务拒绝且不改写端口和权威列状态。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void insufficientItemKeepsState(GameTestHelper helper) {
        buildSafeRepairStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            port.setFuelAssembly(freshFuel(66_666));
            instrument.setSnapshot(snapshot(0.5D, 0.0D, 0L, false, true));
            FuelColumnState before = instrument.snapshot().fuelColumns().get(TEST_COLUMN);

            FuelColumnRepairTransaction.Result empty = port.tryRepairFuelColumn(ItemStack.EMPTY);
            require(helper, empty.status() == FuelColumnRepairTransaction.Status.EMPTY_INPUT,
                    "没有钢板时服务端没有返回物品不足原因");
            require(helper, instrument.snapshot().fuelColumns().get(TEST_COLUMN).equals(before),
                    "物品不足时错误修改了完整度");

            helper.succeed();
        });
    }

    /** 搭建目标列相邻控制棒完全插入的安全测试结构。 */
    private static void buildSafeRepairStructure(GameTestHelper helper) {
        buildStructure(helper);
    }

    /** 搭建目标列相邻控制棒拔出、会产生裂变热的测试结构。 */
    private static void buildActiveRepairStructure(GameTestHelper helper) {
        buildStructure(helper);
    }

    /** 使用结构契约搭建目标列相邻控制棒角色的固定五乘五乘五反应堆。 */
    private static void buildStructure(GameTestHelper helper) {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> columns =
                new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());
        columns.put(CONTROL_COLUMN, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.templateFor(columns).entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 构造单个目标燃料列和相邻控制棒列的权威快照。 */
    private static ReactorSnapshot snapshot(
            double integrity,
            double cachedHeatHu,
            long meltdownProgress,
            boolean meltdownStarted,
            boolean controlRodInserted
    ) {
        return snapshotWithColumn(integrity, cachedHeatHu, 0.0D, 0.0D,
                meltdownProgress, meltdownStarted, controlRodInserted,
                FuelAssemblyState.installed(MAX_DAMAGE, 0));
    }

    /** 构造保留余热与小数余量的维修测试快照。 */
    private static ReactorSnapshot snapshotWithColumn(
            double integrity,
            double cachedHeatHu,
            double fuelBurnRemainder,
            double quantizedHeatRemainderHu,
            long meltdownProgress,
            boolean meltdownStarted,
            boolean controlRodInserted,
            FuelAssemblyState fuelAssembly
    ) {
        double depth = controlRodInserted ? 1.0D : 0.0D;
        return new ReactorSnapshot(
                Map.of(TEST_COLUMN, new FuelColumnState(
                        fuelAssembly,
                        integrity,
                        cachedHeatHu,
                        fuelBurnRemainder,
                        quantizedHeatRemainderHu)),
                Map.of(CONTROL_COLUMN, new ControlRodColumnState(
                        1.0D, depth, depth, false, 0.0D)),
                0L,
                0L,
                meltdownProgress,
                meltdownStarted);
    }

    /** 调用真实换料端口方块的服务端玩家交互入口。 */
    private static ItemInteractionResult rightClick(GameTestHelper helper, Player player) {
        BlockPos absolute = helper.absolutePos(REFUELING_PORT);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        return helper.getLevel().getBlockState(absolute).useItemOn(
                player.getItemInHand(InteractionHand.MAIN_HAND),
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                hit);
    }

    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "固定坐标没有仪表端口方块实体");
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    private static ReactorPortBlockEntity refuelingPort(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(REFUELING_PORT);
        require(helper, blockEntity instanceof ReactorPortBlockEntity,
                "固定坐标没有换料端口方块实体");
        return (ReactorPortBlockEntity) blockEntity;
    }

    private static ItemStack freshFuel(int damage) {
        ItemStack stack = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());
        stack.setDamageValue(damage);
        return stack;
    }

    private static net.minecraft.world.level.block.Block blockForId(String id) {
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

    private static boolean require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
            return false;
        }
        return true;
    }
}
