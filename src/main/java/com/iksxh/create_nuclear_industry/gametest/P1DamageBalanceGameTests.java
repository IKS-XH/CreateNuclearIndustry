package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.config.P1ServerConfig;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/**
 * 验证服务端配置中的非默认损伤终点会进入真实仪表端口 tick，并在保存重载后保持燃料状态。
 * 测试只在单个真实服务端 tick 内临时覆盖配置值，结束后恢复原值，避免污染其他用例。
 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1DamageBalanceGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos FUEL_PORT = new BlockPos(1, 4, 1);
    private static final CoreColumnPosition FUEL_COLUMN = new CoreColumnPosition(0, 0);

    private P1DamageBalanceGameTests() {
    }

    /** 非默认 3.0/5.0 配置必须同时改变正式 tick 的产热和燃耗，并可经 NBT 重载连续保存。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void customDamageEndpointsReachFormalTickAndReload(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            require(helper, instrument.structureValid(), "canonical reactor did not form");

            double previousHeatMultiplier = P1ServerConfig.VALUES.fuelColumnDamageHeatMultiplier.get();
            double previousBurnMultiplier = P1ServerConfig.VALUES.fuelColumnDamageBurnMultiplier.get();
            try {
                P1ServerConfig.VALUES.fuelColumnDamageHeatMultiplier.set(3.0D);
                P1ServerConfig.VALUES.fuelColumnDamageBurnMultiplier.set(5.0D);
                ReactorSnapshot before = ReactorSnapshot.singleFuelColumn(
                        FUEL_COLUMN,
                        new FuelColumnState(
                                FuelAssemblyState.installed(216_000, 0), 0.5D, 0.0D));
                instrument.setSnapshot(before);

                require(helper, instrument.tickReactor(), "formal tick did not consume custom damage configuration");
                require(helper,
                        instrument.telemetry().totalGeneratedFissionHeatHuPerTick() == 6.0D,
                        "formal tick did not apply custom half-damage heat multiplier 2.0");
                FuelColumnState afterFuel = instrument.snapshot().fuelColumns().get(FUEL_COLUMN);
                require(helper, afterFuel.fuelAssembly().damage() == 9,
                        "formal tick did not apply custom half-damage burn multiplier 3.0; expected 9 durability units, actual damage="
                                + afterFuel.fuelAssembly().damage() + ", remainder=" + afterFuel.fuelBurnRemainder());

                ReactorPortBlockEntity fuelPort = refuelingPort(helper);
                CompoundTag savedFuelPort = fuelPort.saveForServerTest(helper.getLevel().registryAccess());
                ReactorPortBlockEntity reloadedFuelPort = new ReactorPortBlockEntity(
                        helper.absolutePos(FUEL_PORT),
                        P1Blocks.REACTOR_REFUELING_PORT.get().defaultBlockState());
                reloadedFuelPort.loadForServerTest(savedFuelPort, helper.getLevel().registryAccess());
                require(helper, ItemStack.matches(fuelPort.fuelAssembly(), reloadedFuelPort.fuelAssembly())
                                && reloadedFuelPort.fuelAssembly().getDamageValue() == 9,
                        "fuel port NBT reload did not preserve custom damage durability");

                CompoundTag saved = instrument.saveForServerTest(helper.getLevel().registryAccess());
                ReactorInstrumentPortBlockEntity reloaded = new ReactorInstrumentPortBlockEntity(
                        helper.absolutePos(INSTRUMENT),
                        P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
                reloaded.loadForServerTest(saved, helper.getLevel().registryAccess());
                FuelColumnState reloadedFuel = reloaded.snapshot().fuelColumns().get(FUEL_COLUMN);
                require(helper, reloadedFuel != null
                                && reloadedFuel.integrity() == afterFuel.integrity()
                                && reloadedFuel.fuelBurnRemainder() == afterFuel.fuelBurnRemainder()
                                && !reloadedFuel.fuelAssembly().present(),
                        "instrument NBT reload did not preserve runtime state or ownership boundary");
                helper.succeed();
            } finally {
                P1ServerConfig.VALUES.fuelColumnDamageHeatMultiplier.set(previousHeatMultiplier);
                P1ServerConfig.VALUES.fuelColumnDamageBurnMultiplier.set(previousBurnMultiplier);
            }
        });
    }

    /** 使用统一结构定义放置测试结构，确保验证覆盖正式结构扫描与仪表端口路径。 */
    private static void buildCanonicalStructure(GameTestHelper helper) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            helper.setBlock(
                    new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 从固定本地坐标取得正式仪表端口。 */
    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "expected reactor instrument port block entity");
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    /** 从正式结构中取得燃料组件唯一持久化所有者。 */
    private static ReactorPortBlockEntity refuelingPort(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(FUEL_PORT);
        require(helper, blockEntity instanceof ReactorPortBlockEntity,
                "expected fuel column refueling port block entity");
        return (ReactorPortBlockEntity) blockEntity;
    }

    /** 将结构定义中的注册 ID 映射为当前 1.21.1 测试世界中的方块。 */
    private static Block blockForId(String id) {
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
            default -> throw new IllegalArgumentException("unknown reactor block " + id);
        };
    }

    /** 统一将异步夹具断言转换为 GameTest 失败。 */
    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
