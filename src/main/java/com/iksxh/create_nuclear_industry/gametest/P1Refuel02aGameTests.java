package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyItemCodec;
import com.iksxh.create_nuclear_industry.reactor.FuelRefuelingTransaction;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** P1-REFUEL-02A 的真实端口所有权、燃烧同步、组件持久化和能力隔离验收测试。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1Refuel02aGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos REFUELING_PORT = new BlockPos(1, 4, 1);
    private static final CoreColumnPosition TEST_COLUMN = new CoreColumnPosition(0, 0);

    private P1Refuel02aGameTests() {
    }

    /** 端口服务端保存和重新加载完整 ItemStack，不让仪表快照成为燃料库存。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void refuelingPortRoundTripsFullFuelItemStack(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorPortBlockEntity port = refuelingPort(helper);
            ItemStack stored = freshFuel(54_321, 1);
            stored.set(DataComponents.CUSTOM_NAME, Component.literal("02A 标记燃料"));
            port.setFuelAssembly(stored);

            var saved = port.saveForServerTest(helper.getLevel().registryAccess());
            ReactorPortBlockEntity reloaded = new ReactorPortBlockEntity(
                    helper.absolutePos(REFUELING_PORT),
                    P1Blocks.REACTOR_REFUELING_PORT.get().defaultBlockState());
            reloaded.loadForServerTest(saved, helper.getLevel().registryAccess());

            require(helper, ItemStack.matches(stored, reloaded.fuelAssembly()),
                    "换料端口没有完整保存耐久和数据组件");
            ListTag fuelColumns = instrument(helper).saveForServerTest(
                    helper.getLevel().registryAccess()).getCompound("ReactorSnapshot")
                    .getList("FuelColumns", Tag.TAG_COMPOUND);
            for (int index = 0; index < fuelColumns.size(); index++) {
                require(helper, !fuelColumns.getCompound(index).contains("Assembly"),
                        "v4 仪表快照仍然保存燃料组件镜像");
            }
            helper.succeed();
        });
    }

    /** 两个绑定端口各自持有自己的组件，事务不会串写其它燃料列。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void multipleFuelPortsKeepIndependentItemStacks(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            List<ReactorPortBlockEntity> ports = instrument.boundPorts(
                    ReactorPortBlockEntity.BindingType.REFUELING);
            if (!require(helper, ports.size() >= 2, "标准结构没有提供两个换料端口")) {
                return;
            }
            ItemStack first = freshFuel(7, 1);
            ItemStack second = freshFuel(8, 1);
            FuelRefuelingTransaction.Result firstResult = ports.get(0).tryInsertFuel(first);
            FuelRefuelingTransaction.Result secondResult = ports.get(1).tryInsertFuel(second);
            require(helper, firstResult.status() == FuelRefuelingTransaction.Status.INSERTED
                            && secondResult.status() == FuelRefuelingTransaction.Status.INSERTED,
                    "多个燃料端口未能分别装料");
            require(helper, ports.get(0).fuelAssembly().getDamageValue() == 7
                            && ports.get(1).fuelAssembly().getDamageValue() == 8,
                    "多个燃料端口发生耐久串写");
            helper.succeed();
        });
    }

    /** 正式 tick 只从端口组件读取燃料，并把整数耐久写回同一端口。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void formalTickWritesBurnDamageToRefuelingPort(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            port.setFuelAssembly(freshFuel(0, 1));
            require(helper, instrument.tickReactor(), "含端口燃料的正式 tick 未推进");
            ItemStack after = port.fuelAssembly();
            require(helper, FuelAssemblyItemCodec.isFreshFuel(after) && after.getDamageValue() > 0,
                    "正式 tick 没有把燃耗写回换料端口");
            require(helper, instrument.snapshot().fuelColumns().get(TEST_COLUMN)
                            .fuelAssembly().damage() == after.getDamageValue(),
                    "端口耐久与瞬态模拟投影不一致");
            helper.succeed();
        });
    }

    /** 换料端口不暴露 ItemHandler，普通漏斗必须被拒绝；玩家事务仍从专用入口执行。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void refuelingPortRejectsOrdinaryItemCapability(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            var handler = helper.getLevel().getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(REFUELING_PORT),
                    Direction.UP);
            require(helper, handler == null, "换料端口错误地暴露了普通 ItemHandler");

            ReactorPortBlockEntity port = refuelingPort(helper);
            port.setFuelAssembly(freshFuel(1_234, 1));
            List<Component> tooltip = new ArrayList<>();
            require(helper, port.addToGoggleTooltip(tooltip, false),
                    "换料端口没有提供本地燃料护目镜信息");
            require(helper, tooltip.stream().anyMatch(component ->
                            component.getContents() instanceof TranslatableContents contents
                                    && contents.getKey().endsWith("fuel_assembly_durability")),
                    "换料端口护目镜没有显示燃料耐久");
            helper.succeed();
        });
    }

    /** 仪表被替换时不清理端口物品，新仪表重建绑定后可重新得到同一燃料投影。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void instrumentReplacementRetainsPortFuel(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ItemStack stored = freshFuel(22_222, 1);
            refuelingPort(helper).setFuelAssembly(stored);
            helper.setBlock(INSTRUMENT, Blocks.AIR.defaultBlockState());
            helper.setBlock(INSTRUMENT, P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
            helper.runAfterDelay(8, () -> {
                ReactorInstrumentPortBlockEntity replacement = instrument(helper);
                ReactorPortBlockEntity port = refuelingPort(helper);
                require(helper, replacement.structureValid() && port.isBoundTo(
                                replacement, ReactorPortBlockEntity.BindingType.REFUELING, TEST_COLUMN),
                        "替换仪表后没有重建燃料端口绑定");
                ItemStack afterReplacement = port.fuelAssembly();
                ItemStack afterWithoutDurability = afterReplacement.copy();
                afterWithoutDurability.setDamageValue(stored.getDamageValue());
                require(helper, !afterReplacement.isEmpty()
                                && afterReplacement.getItem() == stored.getItem()
                                && afterReplacement.getCount() == 1
                                && afterReplacement.getDamageValue() >= stored.getDamageValue()
                                && ItemStack.matches(stored, afterWithoutDurability),
                        "替换仪表错误地清除了端口燃料组件或丢失数据组件");
                require(helper, replacement.snapshot().fuelColumns().get(TEST_COLUMN)
                                .fuelAssembly().damage() == afterReplacement.getDamageValue(),
                        "新仪表没有从端口恢复燃料投影");
                helper.succeed();
            });
        });
    }

    /** 按生产结构契约搭建标准反应堆，绑定位置与正式结构扫描同源。 */
    private static void buildCanonicalStructure(GameTestHelper helper) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.canonicalTemplate().entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var entity = helper.getBlockEntity(INSTRUMENT);
        require(helper, entity instanceof ReactorInstrumentPortBlockEntity,
                "固定仪表坐标没有方块实体");
        return (ReactorInstrumentPortBlockEntity) entity;
    }

    private static ReactorPortBlockEntity refuelingPort(GameTestHelper helper) {
        var entity = helper.getBlockEntity(REFUELING_PORT);
        require(helper, entity instanceof ReactorPortBlockEntity,
                "固定换料坐标没有方块实体");
        return (ReactorPortBlockEntity) entity;
    }

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

    private static boolean require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
            return false;
        }
        return true;
    }
}
