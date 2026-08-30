package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyItemCodec;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.FuelRefuelingTransaction;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/** 在真实 NeoForge GameTest 服务端验证绑定换料端口的原子取放和耗尽产物。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1RefuelGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos REFUELING_PORT = new BlockPos(1, 4, 1);
    private static final CoreColumnPosition TEST_COLUMN = new CoreColumnPosition(0, 0);

    private P1RefuelGameTests() {
    }

    /** 空列装入新燃料时只提交一个列状态，且不会预先修改输入栈。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void emptyColumnLoadsOneFreshAssembly(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            if (!require(helper, instrument.structureValid() && port.isBoundTo(
                    instrument, ReactorPortBlockEntity.BindingType.REFUELING, TEST_COLUMN),
                    "标准结构未建立目标换料端口绑定")) {
                return;
            }

            ItemStack input = freshFuel(12_345, 2);
            FuelRefuelingTransaction.Result result = port.tryInsertFuel(input);
            if (!require(helper, result.status() == FuelRefuelingTransaction.Status.INSERTED,
                    "空燃料列未接受新燃料组件")) {
                return;
            }
            require(helper, input.getCount() == 2, "成功提交前不能修改调用方输入栈");
            require(helper, result.remainingInput().getCount() == 1,
                    "换料事务没有只消费一个组件");
            require(helper, instrument.snapshot().fuelColumns().get(TEST_COLUMN)
                            .fuelAssembly().damage() == 12_345,
                    "列状态没有保存 ItemStack 的耐久值");
            helper.succeed();
        });
    }

    /** 运行中拒绝取出，验证换料端口从仪表端口服务端快照计算放热状态。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void runningColumnRejectsUntilFissionStops(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            if (!require(helper, instrument.structureValid() && port.isBound(),
                    "标准结构未建立换料端口绑定")) {
                return;
            }

            int damage = 54_321;
            instrument.setSnapshot(instrument.snapshot().withFuelColumn(TEST_COLUMN,
                    new FuelColumnState(FuelAssemblyState.installed(
                            ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY, damage), 0.8D, 3.0D)));
            FuelRefuelingTransaction.Result rejected = port.tryExtractFuel();
            if (!require(helper, rejected.status() == FuelRefuelingTransaction.Status.COLUMN_ACTIVE
                            && rejected.output().isEmpty(),
                    "运行中的燃料列错误地允许取出组件")) {
                return;
            }

            require(helper, instrument.snapshot().fuelColumns().get(TEST_COLUMN)
                            .fuelAssembly().damage() == damage,
                    "运行中拒绝事务错误地改变了燃料耐久");
            helper.succeed();
        });
    }

    /** 耗尽组件取出时转为冷却乏燃料，且不会影响另一列的状态。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void exhaustedFuelProducesCooledSpentFuelAndKeepsOtherColumns(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            if (!require(helper, instrument.structureValid() && port.isBound(),
                    "标准结构未建立耗尽燃料测试绑定")) {
                return;
            }

            CoreColumnPosition otherColumn = new CoreColumnPosition(0, 1);
            ReactorSnapshot before = instrument.snapshot()
                    .withFuelColumn(TEST_COLUMN, new FuelColumnState(
                            FuelAssemblyState.installed(
                                    ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY,
                                    ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY),
                            0.5D, 2.0D))
                    .withFuelColumn(otherColumn, new FuelColumnState(
                            FuelAssemblyState.installed(
                                    ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY, 7),
                            1.0D, 0.0D));
            instrument.setSnapshot(before);

            FuelRefuelingTransaction.Result result = port.tryExtractFuel();
            if (!require(helper, result.status() == FuelRefuelingTransaction.Status.REMOVED,
                    "耗尽组件无法通过换料事务取出")) {
                return;
            }
            require(helper, FuelAssemblyItemCodec.isCooledSpentFuel(result.output()),
                    "耗尽组件没有直接产出冷却乏燃料");
            require(helper, instrument.snapshot().fuelColumns().get(otherColumn)
                            .fuelAssembly().damage() == 7,
                    "取出一列燃料时错误修改了另一列");
            helper.succeed();
        });
    }

    /** 使用结构定义的正式本地坐标搭建固定实验堆，确保端口绑定测试与生产逻辑同源。 */
    private static void buildCanonicalStructure(GameTestHelper helper) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 读取测试中的仪表端口。 */
    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "固定仪表坐标没有方块实体");
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    /** 读取测试中的顶部换料端口。 */
    private static ReactorPortBlockEntity refuelingPort(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(REFUELING_PORT);
        require(helper, blockEntity instanceof ReactorPortBlockEntity,
                "固定换料坐标没有方块实体");
        return (ReactorPortBlockEntity) blockEntity;
    }

    /** 将结构契约中的方块 ID 转换为真实注册对象。 */
    private static net.minecraft.world.level.block.Block blockForId(String id) {
        return switch (id) {
            case "minecraft:air" -> Blocks.AIR;
            case "create_nuclear_industry:reactor_casing" -> P1Blocks.REACTOR_CASING.get();
            case "create_nuclear_industry:reactor_window" -> P1Blocks.REACTOR_WINDOW.get();
            case "create_nuclear_industry:reactor_instrument_port" -> P1Blocks.REACTOR_INSTRUMENT_PORT.get();
            case "create_nuclear_industry:reactor_cold_port" -> P1Blocks.REACTOR_COLD_PORT.get();
            case "create_nuclear_industry:reactor_hot_port" -> P1Blocks.REACTOR_HOT_PORT.get();
            case "create_nuclear_industry:reactor_refueling_port" -> P1Blocks.REACTOR_REFUELING_PORT.get();
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
