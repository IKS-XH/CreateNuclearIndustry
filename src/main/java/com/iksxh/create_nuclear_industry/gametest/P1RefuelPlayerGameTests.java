package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyItemCodec;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

/** 在真实服务端 GameTest 中验证玩家通过顶部换料端口执行单列交互。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1RefuelPlayerGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos REFUELING_PORT = new BlockPos(1, 4, 1);
    private static final CoreColumnPosition TEST_COLUMN = new CoreColumnPosition(0, 0);
    private static final CoreColumnPosition ADJACENT_CONTROL_COLUMN = new CoreColumnPosition(1, 0);

    private P1RefuelPlayerGameTests() {
    }

    /** 玩家持有新燃料时装入一个组件，空手再次右键时取回同一耐久度组件。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void playerRightClickLoadsAndExtractsFuel(GameTestHelper helper) {
        buildStructure(helper, layoutWithAdjacentControlRod());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            if (!require(helper, instrument.structureValid(), "标准结构未成型，无法测试玩家换料")) {
                return;
            }

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, freshFuel(12_345, 2));
            ItemInteractionResult inserted = rightClick(helper, player, InteractionHand.MAIN_HAND);
            if (!require(helper, inserted.consumesAction(), "玩家装料右键没有消费交互动作")) {
                return;
            }
            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
                    "玩家装料没有只消耗一个新燃料组件");
            require(helper, instrument.snapshot().fuelColumns().get(TEST_COLUMN)
                            .fuelAssembly().damage() == 12_345,
                    "玩家装入的燃料耐久度没有写入目标列");

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            ItemInteractionResult removed = rightClick(helper, player, InteractionHand.MAIN_HAND);
            if (!require(helper, removed.consumesAction(), "玩家取料右键没有消费交互动作")) {
                return;
            }
            ItemStack output = player.getItemInHand(InteractionHand.MAIN_HAND);
            require(helper, FuelAssemblyItemCodec.isFreshFuel(output)
                            && output.getDamageValue() == 12_345,
                    "玩家取出的燃料组件没有保留原耐久度");
            require(helper, !instrument.snapshot().fuelColumns().get(TEST_COLUMN)
                            .fuelAssembly().present(),
                    "玩家取料后目标列仍然占用燃料组件");
            helper.succeed();
        });
    }

    /** 目标列正在裂变放热时拒绝玩家空手取料，且玩家手和权威快照均不改变。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void playerRightClickRejectsRunningColumnWithoutMutation(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorSnapshot before = instrument.snapshot().withFuelColumn(TEST_COLUMN,
                    new FuelColumnState(FuelAssemblyState.installed(
                            ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY, 54_321), 0.8D, 0.0D));
            instrument.setSnapshot(before);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            ItemInteractionResult result = rightClick(helper, player, InteractionHand.MAIN_HAND);
            require(helper, result.consumesAction(), "运行中拒绝没有截断玩家右键动作");
            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                    "运行中拒绝错误地产生了玩家输出物品");
            require(helper, instrument.snapshot().equals(before),
                    "运行中拒绝改变了燃料列或其他反应堆 NBT");
            helper.succeed();
        });
    }

    /** 目标列裂变已停止但仍有缓存余热时允许取料，余热和其他列状态保持不变。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void playerRightClickAllowsResidualHeatAfterFissionStops(GameTestHelper helper) {
        buildStructure(helper, layoutWithAdjacentControlRod());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorSnapshot before = instrument.snapshot().withFuelColumn(TEST_COLUMN,
                    new FuelColumnState(FuelAssemblyState.installed(
                            ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY, 22_222), 0.7D, 9.0D));
            instrument.setSnapshot(before);
            require(helper, instrument.currentFuelColumnFissionHeatHu(TEST_COLUMN) == 0.0D,
                    "完全插入的相邻控制棒没有将目标列裂变发热压到零");

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            ItemInteractionResult result = rightClick(helper, player, InteractionHand.MAIN_HAND);
            if (!require(helper, result.consumesAction(), "余热状态下玩家取料没有消费交互动作")) {
                return;
            }
            require(helper, FuelAssemblyItemCodec.isFreshFuel(player.getItemInHand(InteractionHand.MAIN_HAND))
                            && player.getItemInHand(InteractionHand.MAIN_HAND).getDamageValue() == 22_222,
                    "余热状态下没有取出保留耐久度的燃料组件");
            require(helper, instrument.snapshot().fuelColumns().get(TEST_COLUMN).cachedHeatHu() == 9.0D,
                    "玩家取料错误地清除了目标列余热");
            helper.succeed();
        });
    }

    /** SCRAM 只要已将目标列裂变发热压为零，玩家仍可执行该列的局部换料。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void playerRightClickWorksDuringScram(GameTestHelper helper) {
        buildStructure(helper, layoutWithAdjacentControlRod());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            instrument.setSnapshot(instrument.snapshot().withFuelColumn(TEST_COLUMN,
                    new FuelColumnState(FuelAssemblyState.installed(
                            ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY, 33_333), 1.0D, 0.0D)));
            require(helper, instrument.updateRedstoneScram(true).status().name().startsWith("SCRAM"),
                    "SCRAM 请求没有建立服务端状态");
            require(helper, instrument.snapshot().scramRequested(), "SCRAM 请求标志没有写入权威快照");
            require(helper, instrument.currentFuelColumnFissionHeatHu(TEST_COLUMN) == 0.0D,
                    "SCRAM 状态下目标列仍有裂变发热");

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            ItemInteractionResult result = rightClick(helper, player, InteractionHand.MAIN_HAND);
            require(helper, result.consumesAction(), "SCRAM 状态下玩家取料没有消费交互动作");
            require(helper, FuelAssemblyItemCodec.isFreshFuel(player.getItemInHand(InteractionHand.MAIN_HAND)),
                    "SCRAM 状态下没有取出目标列燃料组件");
            require(helper, instrument.snapshot().scramRequested(),
                    "局部换料错误地清除了整堆 SCRAM 请求");
            helper.succeed();
        });
    }

    /** 空列收到错误物品时拒绝交互，不消耗物品，也不改变快照。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void playerRightClickRejectsWrongItemWithoutMutation(GameTestHelper helper) {
        buildCanonicalStructure(helper);
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorSnapshot before = instrument.snapshot();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack wrongItem = new ItemStack(Items.IRON_INGOT, 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, wrongItem);

            ItemInteractionResult result = rightClick(helper, player, InteractionHand.MAIN_HAND);
            require(helper, result.consumesAction(), "错误物品右键没有被换料端口截断");
            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 3
                            && player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.IRON_INGOT),
                    "错误物品被玩家换料交互消耗或替换");
            require(helper, instrument.snapshot().equals(before),
                    "错误物品交互改变了反应堆 NBT");
            helper.succeed();
        });
    }

    /** 返回一个含相邻控制棒列的合法局部结构，用于验证停热和 SCRAM 边界。 */
    private static Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layoutWithAdjacentControlRod() {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout =
                new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());
        layout.put(ADJACENT_CONTROL_COLUMN, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        return layout;
    }

    /** 使用生产结构契约搭建标准反应堆。 */
    private static void buildCanonicalStructure(GameTestHelper helper) {
        buildStructure(helper, ReactorStructureDefinition.defaultColumnLayout());
    }

    /** 使用生产结构契约搭建指定列布局，避免测试自行伪造绑定坐标。 */
    private static void buildStructure(
            GameTestHelper helper,
            Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> columns
    ) {
        for (Map.Entry<ReactorStructureDefinition.LocalPosition, String> entry
                : ReactorStructureDefinition.templateFor(columns).entrySet()) {
            helper.setBlock(new BlockPos(entry.getKey().x(), entry.getKey().y(), entry.getKey().z()),
                    blockForId(entry.getValue()).defaultBlockState());
        }
    }

    /** 调用真实 BlockState 玩家物品交互入口，输入手由服务端交互栈决定。 */
    private static ItemInteractionResult rightClick(
            GameTestHelper helper,
            Player player,
            InteractionHand hand
    ) {
        BlockPos absolute = helper.absolutePos(REFUELING_PORT);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        return helper.getLevel().getBlockState(absolute).useItemOn(
                player.getItemInHand(hand), helper.getLevel(), player, hand, hit);
    }

    /** 读取测试中的仪表端口。 */
    private static ReactorInstrumentPortBlockEntity instrument(GameTestHelper helper) {
        var blockEntity = helper.getBlockEntity(INSTRUMENT);
        require(helper, blockEntity instanceof ReactorInstrumentPortBlockEntity,
                "固定仪表坐标没有方块实体");
        return (ReactorInstrumentPortBlockEntity) blockEntity;
    }

    /** 按正式物品耐久度构造指定损伤和数量的新燃料组件。 */
    private static ItemStack freshFuel(int damage, int count) {
        ItemStack stack = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get(), count);
        stack.setDamageValue(damage);
        return stack;
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

    /** 将夹具错误转为 GameTest 失败，并让异步回调安全退出。 */
    private static boolean require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
            return false;
        }
        return true;
    }
}
