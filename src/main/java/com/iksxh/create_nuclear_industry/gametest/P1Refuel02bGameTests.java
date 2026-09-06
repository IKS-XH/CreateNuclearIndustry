package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyState;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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

/** P1-REFUEL-02B 的未成型人工取料、危险锁和服务端生命周期验收测试。 */
@GameTestHolder("create_nuclear_industry")
@PrefixGameTestTemplate(false)
public final class P1Refuel02bGameTests {
    private static final String TEMPLATE = "p0_probe_empty";
    private static final BlockPos INSTRUMENT = new BlockPos(2, 2, 0);
    private static final BlockPos REFUELING_PORT = new BlockPos(1, 4, 1);
    private static final BlockPos FORMATION_GAP = new BlockPos(0, 0, 0);
    private static final CoreColumnPosition TEST_COLUMN = new CoreColumnPosition(0, 0);
    private static final CoreColumnPosition CONTROL_COLUMN = new CoreColumnPosition(1, 0);

    private P1Refuel02bGameTests() {
    }

    /** 安全停机后结构失效，玩家可以取回完整耐久和自定义数据组件，并且重复取料不复制。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void safeUnformedPortReturnsExactStackOnlyOnce(GameTestHelper helper) {
        buildStructure(helper, ReactorStructureDefinition.defaultColumnLayout());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            ItemStack stored = freshFuel(54_321, 1);
            stored.set(DataComponents.CUSTOM_NAME, Component.literal("02B 标记燃料"));
            port.setFuelAssembly(stored);

            invalidateStructure(helper);
            require(helper, instrument.boundPortCount() == 0 && port.boundOwner() == null,
                    "结构失效后换料端口仍保留运行时绑定");
            require(helper, port.isUnformedExtractionAllowed(),
                    "安全停机的未成型端口没有获得取料许可");

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            ItemInteractionResult first = rightClick(helper, player);
            require(helper, first.consumesAction(), "未成型安全取料没有消费交互动作");
            require(helper, ItemStack.matches(stored, player.getItemInHand(InteractionHand.MAIN_HAND)),
                    "未成型取料没有保留完整 ItemStack 数据");
            require(helper, port.fuelAssembly().isEmpty(), "未成型取料后端口仍保存燃料");

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            rightClick(helper, player);
            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
                            && port.fuelAssembly().isEmpty(),
                    "连续第二次未成型取料产生了重复物品");
            helper.succeed();
        });
    }

    /** 未成型端口只开放取料，不允许手持新燃料绕过结构绑定装入。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void unformedPortRejectsPlayerInsertion(GameTestHelper helper) {
        buildStructure(helper, ReactorStructureDefinition.defaultColumnLayout());
        helper.runAfterDelay(5, () -> {
            ReactorPortBlockEntity port = refuelingPort(helper);
            invalidateStructure(helper);

            ItemStack input = freshFuel(12_345, 2);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, input);
            ItemInteractionResult result = rightClick(helper, player);

            require(helper, result.consumesAction(), "未成型装料没有截断交互动作");
            require(helper, ItemStack.matches(input, player.getItemInHand(InteractionHand.MAIN_HAND)),
                    "未成型装料错误消耗或替换了玩家物品");
            require(helper, port.fuelAssembly().isEmpty(), "未成型装料写入了端口库存");
            helper.succeed();
        });
    }

    /** 结构从运行中失效时必须锁定取料，且端口组件和玩家手均保持不变。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void runningReactorLocksUnformedExtraction(GameTestHelper helper) {
        buildStructure(helper, layoutWithAdjacentControlRod());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            ItemStack stored = freshFuel(23_456, 1);
            port.setFuelAssembly(stored);
            instrument.setSnapshot(runningSnapshot());

            invalidateStructure(helper);
            require(helper, !port.isUnformedExtractionAllowed(),
                    "运行中结构失效错误地开放了未成型取料");

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            rightClick(helper, player);
            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
                            && ItemStack.matches(stored, port.fuelAssembly()),
                    "运行中危险锁拒绝后没有保持端口原物品");
            helper.succeed();
        });
    }

    /** 已建立的融毁倒计时（包括暂停中的持久化标记）始终禁止未成型取料。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void meltdownCountdownAndPausedStateLockUnformedExtraction(GameTestHelper helper) {
        buildStructure(helper, layoutWithAdjacentControlRod());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            ItemStack stored = freshFuel(23_457, 1);
            port.setFuelAssembly(stored);
            instrument.setSnapshot(runningSnapshot().withMeltdown(17L, true));

            invalidateStructure(helper);
            require(helper, !port.isUnformedExtractionAllowed(),
                    "融毁倒计时或暂停标记存在时错误地开放了未成型取料");
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            rightClick(helper, player);
            require(helper, player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()
                            && ItemStack.matches(stored, port.fuelAssembly()),
                    "融毁危险锁拒绝后没有保持端口原物品");
            helper.succeed();
        });
    }

    /** 重新成型后只有服务端确认已安全停机才能解锁，危险重绑定不能借机放行。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void safeRebindingUnlocksAfterDangerIsCleared(GameTestHelper helper) {
        buildStructure(helper, layoutWithAdjacentControlRod());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            ItemStack stored = freshFuel(34_567, 1);
            port.setFuelAssembly(stored);
            instrument.setSnapshot(runningSnapshot());

            invalidateStructure(helper);
            require(helper, !port.isUnformedExtractionAllowed(),
                    "危险结构失效没有锁定端口");

            restoreStructure(helper);
            require(helper, instrument.structureValid() && !port.isUnformedExtractionAllowed(),
                    "危险状态重新成型时错误地解锁了端口");

            instrument.setSnapshot(safeControlledSnapshot());
            ReactorStructureLifecycle.rescanAroundNow(
                    helper.getLevel(), helper.absolutePos(INSTRUMENT));
            require(helper, port.isBound() && port.isUnformedExtractionAllowed(),
                    "安全状态重新绑定后没有更新取料许可");

            invalidateStructure(helper);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            rightClick(helper, player);
            require(helper, ItemStack.matches(stored, player.getItemInHand(InteractionHand.MAIN_HAND))
                            && port.fuelAssembly().isEmpty(),
                    "安全重新绑定后未能取回端口燃料");
            helper.succeed();
        });
    }

    /** 02A 旧端口缺少锁字段时默认拒绝，重载不会吞掉原组件或自行解锁。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void missingLockFieldDefaultsToDenyAndPreservesFuel(GameTestHelper helper) {
        buildStructure(helper, ReactorStructureDefinition.defaultColumnLayout());
        helper.runAfterDelay(5, () -> {
            ReactorPortBlockEntity port = refuelingPort(helper);
            ItemStack stored = freshFuel(45_678, 1);
            port.setFuelAssembly(stored);
            CompoundTag saved = port.saveForServerTest(helper.getLevel().registryAccess());
            saved.remove("UnformedExtractionAllowed");

            ReactorPortBlockEntity reloaded = new ReactorPortBlockEntity(
                    helper.absolutePos(REFUELING_PORT),
                    P1Blocks.REACTOR_REFUELING_PORT.get().defaultBlockState());
            reloaded.loadForServerTest(saved, helper.getLevel().registryAccess());
            require(helper, !reloaded.isUnformedExtractionAllowed(),
                    "缺少 02B 锁字段的旧端口没有默认拒绝");
            require(helper, ItemStack.matches(stored, reloaded.fuelAssembly()),
                    "旧端口缺少锁字段时丢失了原燃料组件");
            require(helper, reloaded.tryExtractUnformedFuel().isEmpty()
                            && ItemStack.matches(stored, reloaded.fuelAssembly()),
                    "未知旧 NBT 状态错误地允许了未成型取料");
            helper.succeed();
        });
    }

    /** 危险锁和精确物品栈经过服务端 NBT 重载后保持，客户端更新包不参与解锁。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 140)
    public static void deniedLockSurvivesNbtReload(GameTestHelper helper) {
        buildStructure(helper, layoutWithAdjacentControlRod());
        helper.runAfterDelay(5, () -> {
            ReactorInstrumentPortBlockEntity instrument = instrument(helper);
            ReactorPortBlockEntity port = refuelingPort(helper);
            ItemStack stored = freshFuel(45_679, 1);
            port.setFuelAssembly(stored);
            instrument.setSnapshot(runningSnapshot());
            invalidateStructure(helper);

            CompoundTag saved = port.saveForServerTest(helper.getLevel().registryAccess());
            ReactorPortBlockEntity reloaded = new ReactorPortBlockEntity(
                    helper.absolutePos(REFUELING_PORT),
                    P1Blocks.REACTOR_REFUELING_PORT.get().defaultBlockState());
            reloaded.loadForServerTest(saved, helper.getLevel().registryAccess());
            require(helper, !reloaded.isUnformedExtractionAllowed()
                            && ItemStack.matches(stored, reloaded.fuelAssembly()),
                    "危险锁或端口组件在服务端 NBT 重载后发生变化");
            helper.succeed();
        });
    }

    /** 返回一个含相邻控制棒列的合法局部结构，用于构造可安全停堆的测试场景。 */
    private static Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType>
    layoutWithAdjacentControlRod() {
        Map<CoreColumnPosition, ReactorStructureDefinition.ColumnType> layout =
                new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());
        layout.put(CONTROL_COLUMN, ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        return layout;
    }

    /** 使用生产结构契约搭建指定堆芯列布局。 */
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

    /** 让服务端结构生命周期立即观察一个失效结构，不通过玩家扫描或客户端状态。 */
    private static void invalidateStructure(GameTestHelper helper) {
        helper.setBlock(FORMATION_GAP, Blocks.AIR.defaultBlockState());
        ReactorStructureLifecycle.rescanAroundNow(
                helper.getLevel(), helper.absolutePos(FORMATION_GAP));
    }

    /** 恢复被测试删除的外壳方块并立即重新成型。 */
    private static void restoreStructure(GameTestHelper helper) {
        helper.setBlock(FORMATION_GAP, P1Blocks.REACTOR_CASING.get().defaultBlockState());
        ReactorStructureLifecycle.rescanAroundNow(
                helper.getLevel(), helper.absolutePos(FORMATION_GAP));
    }

    /** 构造具有正裂变发热的旧快照，作为结构失效前的危险状态。 */
    private static ReactorSnapshot runningSnapshot() {
        return new ReactorSnapshot(
                Map.of(TEST_COLUMN, new FuelColumnState(
                        FuelAssemblyState.installed(ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY, 0),
                        1.0D,
                        0.0D)),
                Map.of(CONTROL_COLUMN, new ControlRodColumnState(1.0D, 0.0D, 0.0D, false, 0.0D)),
                0L,
                0L,
                0L,
                false);
    }

    /** 构造控制棒完全插入、裂变热为零但仍保留组件投影的安全快照。 */
    private static ReactorSnapshot safeControlledSnapshot() {
        return new ReactorSnapshot(
                Map.of(TEST_COLUMN, new FuelColumnState(
                        FuelAssemblyState.installed(ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY, 0),
                        1.0D,
                        0.0D)),
                Map.of(CONTROL_COLUMN, ControlRodColumnState.fullyInserted()),
                0L,
                0L,
                0L,
                false);
    }

    /** 调用真实方块状态的服务端玩家交互入口。 */
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

    private static ItemStack freshFuel(int damage, int count) {
        ItemStack stack = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get(), count);
        stack.setDamageValue(damage);
        return stack;
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

    private static boolean require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
            return false;
        }
        return true;
    }
}
