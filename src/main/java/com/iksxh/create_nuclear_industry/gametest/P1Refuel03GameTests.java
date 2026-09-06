package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelRefuelingArmInteractionPoint;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

/** P1-REFUEL-03 的 Create 机械臂选点、原子取放、失效拒绝和预检回滚验收测试。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1Refuel03GameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos REFUELING_PORT = new BlockPos(1, 4, 1);
    private static final BlockPos ADJACENT_REFUELING_PORT = new BlockPos(1, 4, 2);
    private static final BlockPos ARM_POS = new BlockPos(1, 5, 1);
    private static final BlockPos FORMATION_GAP = new BlockPos(0, 0, 0);
    private static final CoreColumnPosition TEST_COLUMN = new CoreColumnPosition(0, 0);

    private P1Refuel03GameTests() {
    }

    /** Create 机械臂 API 的模拟预检不改状态，正式调用完成一次取放且保留完整物品栈。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void armPointCompletesAtomicLoadAndUnload(GameTestHelper helper) {
        buildStructure(helper, layoutWithAdjacentControlRod());
        placeMechanicalArm(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper, REFUELING_PORT);
            ArmBlockEntity arm = mechanicalArm(helper);
            ArmInteractionPoint point = createPoint(helper, REFUELING_PORT);
            if (!require(helper, point != null
                            && point.getType() == FuelRefuelingArmInteractionPoint.TYPE
                            && point.isValid(),
                    "有效换料端口没有创建正式机械臂交互点")) {
                return;
            }

            ItemStack input = freshFuel(12_345, 2);
            var beforeSimulation = instrument.snapshot();
            ItemStack simulatedRemainder = point.insert(arm, input, true);
            require(helper, ItemStack.matches(input.copyWithCount(1), simulatedRemainder),
                    "机械臂装料预检没有返回提交后的正确余量");
            require(helper, port.fuelAssembly().isEmpty()
                            && instrument.snapshot().equals(beforeSimulation),
                    "机械臂装料预检修改了端口或反应堆快照");

            ItemStack remainder = point.insert(arm, input, false);
            require(helper, remainder.getCount() == 1
                            && ItemStack.matches(port.fuelAssembly(), input.copyWithCount(1)),
                    "机械臂正式装料没有只提交一个完整燃料组件");
            require(helper, instrument.snapshot().fuelColumns().get(TEST_COLUMN) != null
                            && instrument.snapshot().fuelColumns().get(TEST_COLUMN)
                            .fuelAssembly().present(),
                    "机械臂装料没有同步唯一反应堆列投影");

            ItemStack simulatedOutput = point.extract(arm, 0, 1, true);
            require(helper, ItemStack.matches(port.fuelAssembly(), simulatedOutput)
                            && !port.fuelAssembly().isEmpty(),
                    "机械臂取料预检没有返回端口精确物品或错误修改端口");
            ItemStack output = point.extract(arm, 0, 1, false);
            require(helper, ItemStack.matches(input.copyWithCount(1), output)
                            && port.fuelAssembly().isEmpty(),
                    "机械臂正式取料没有原子清空端口或丢失物品组件");
            require(helper, !instrument.snapshot().fuelColumns().get(TEST_COLUMN)
                            .fuelAssembly().present(),
                    "机械臂取料没有清空唯一反应堆列投影");
            helper.succeed();
        });
    }

    /** 错误物品和放热中的列都必须原样回滚，不能被 Create 机械臂绕过。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void armPointRejectsWrongItemAndActiveColumn(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        placeMechanicalArm(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper, REFUELING_PORT);
            ReactorPortBlockEntity adjacent = refuelingPort(helper, ADJACENT_REFUELING_PORT);
            ArmBlockEntity arm = mechanicalArm(helper);
            ArmInteractionPoint point = createPoint(helper, REFUELING_PORT);
            if (!require(helper, point != null && point.isValid(), "错误物品测试无法创建机械臂交互点")) {
                return;
            }

            ItemStack wrong = new ItemStack(Items.IRON_INGOT, 3);
            var beforeWrong = instrument.snapshot();
            ItemStack wrongRemainder = point.insert(arm, wrong, false);
            require(helper, ItemStack.matches(wrong, wrongRemainder)
                            && port.fuelAssembly().isEmpty()
                            && instrument.snapshot().equals(beforeWrong),
                    "机械臂错误物品路径没有完整回滚");

            ItemStack targetFuel = freshFuel(2_000, 1);
            require(helper, point.insert(arm, targetFuel, false).isEmpty(),
                    "机械臂无法先装入有效目标燃料");
            require(helper, adjacent.tryInsertFuel(freshFuel(3_000, 1)).success(),
                    "无法建立相邻燃料列的放热前置状态");
            var beforeActive = instrument.snapshot();
            ItemStack activeOutput = point.extract(arm, 0, 1, false);
            require(helper, activeOutput.isEmpty()
                            && ItemStack.matches(targetFuel, port.fuelAssembly())
                            && instrument.snapshot().equals(beforeActive),
                    "机械臂在列放热时仍然取料或修改了快照");
            helper.succeed();
        });
    }

    /** 结构失效、端口消失和预检后状态变化都必须使机械臂事务返回原输入且不产生半提交。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void armPointRejectsInvalidTargetsAndRollsBack(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        placeMechanicalArm(helper);
        helper.runAfterDelay(5, () -> {
            ReactorPortBlockEntity port = refuelingPort(helper, REFUELING_PORT);
            ArmBlockEntity arm = mechanicalArm(helper);
            ArmInteractionPoint point = createPoint(helper, REFUELING_PORT);
            if (!require(helper, point != null && point.isValid(), "回滚测试无法创建机械臂交互点")) {
                return;
            }

            ItemStack input = freshFuel(7_777, 1);
            ItemStack simulated = point.insert(arm, input, true);
            require(helper, simulated.isEmpty(), "有效状态下的机械臂装料预检没有返回可提交结果");
            helper.setBlock(FORMATION_GAP, Blocks.AIR.defaultBlockState());
            ReactorStructureLifecycle.rescanAroundNow(
                    helper.getLevel(), helper.absolutePos(FORMATION_GAP));
            helper.runAfterDelay(8, () -> {
                require(helper, !point.isValid(), "结构失效后旧机械臂交互点仍被标记为有效");
                ItemStack afterInvalidation = point.insert(arm, input, false);
                require(helper, ItemStack.matches(input, afterInvalidation)
                                && port.fuelAssembly().isEmpty(),
                        "预检后结构失效没有回滚机械臂输入或产生半提交");

                helper.setBlock(REFUELING_PORT, Blocks.AIR.defaultBlockState());
                ItemStack afterPortRemoval = point.insert(arm, input, false);
                require(helper, ItemStack.matches(input, afterPortRemoval),
                        "换料端口消失后机械臂没有原样退回输入");
                helper.succeed();
            });
        });
    }

    /** 通过正式结构坐标契约搭建固定 5×5×5 反应堆。 */
    private static void buildCanonicalStructure(GameTestHelper helper) {
        buildStructure(helper, ReactorStructureDefinition.defaultColumnLayout());
    }

    /** 放置真实 Create 机械臂方块实体，让交互点调用经过正式机械臂参数边界。 */
    private static void placeMechanicalArm(GameTestHelper helper) {
        helper.setBlock(ARM_POS, AllBlocks.MECHANICAL_ARM.get().defaultBlockState());
    }

    /** 通过正式结构坐标契约搭建指定列布局，不能让测试自行伪造端口坐标。 */
    private static void buildStructure(
            GameTestHelper helper,
            Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> columns
    ) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.templateFor(columns).entrySet()) {
            helper.setBlock(
                    new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 为成功取料测试提供相邻完全插入控制棒，使局部燃料列当前裂变发热为零。 */
    private static Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType>
    layoutWithAdjacentControlRod() {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout =
                new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());
        layout.put(new CoreColumnPosition(1, 0), ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        return layout;
    }

    /** 使用 Create 公开工厂创建目标点，确保测试走正式机械臂注册路径。 */
    private static ArmInteractionPoint createPoint(GameTestHelper helper, BlockPos relativePos) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        return ArmInteractionPoint.create(
                helper.getLevel(), absolutePos, helper.getLevel().getBlockState(absolutePos));
    }

    /** 读取真实 Create 机械臂方块实体；测试不伪造 {@link ArmBlockEntity} 参数。 */
    private static ArmBlockEntity mechanicalArm(GameTestHelper helper) {
        var entity = helper.getBlockEntity(ARM_POS);
        require(helper, entity instanceof ArmBlockEntity, "Create 机械臂方块没有建立方块实体");
        return (ArmBlockEntity) entity;
    }

    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var entity = helper.getBlockEntity(INSTRUMENT);
        require(helper, entity instanceof ReactorInstrumentPortBlockEntity,
                "固定仪表坐标没有方块实体");
        return (ReactorInstrumentPortBlockEntity) entity;
    }

    private static ReactorPortBlockEntity refuelingPort(GameTestHelper helper, BlockPos position) {
        var entity = helper.getBlockEntity(position);
        require(helper, entity instanceof ReactorPortBlockEntity,
                "固定坐标没有换料端口方块实体");
        return (ReactorPortBlockEntity) entity;
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

    private static ItemStack freshFuel(int damage, int count) {
        ItemStack stack = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get(), count);
        stack.setDamageValue(damage);
        return stack;
    }

    /** 将夹具错误转为 GameTest 失败，并让异步回调安全退出。 */
    private static boolean require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
            return false;
        }
        return true;
    }
}
