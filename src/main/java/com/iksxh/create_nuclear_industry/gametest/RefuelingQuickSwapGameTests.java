package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelAssemblyItemCodec;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.FuelRefuelingArmInteractionPoint;
import com.iksxh.create_nuclear_industry.reactor.FuelRefuelingTransaction;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import com.mojang.logging.LogUtils;

/** 独立测试世界中的玩家快捷换料；真实入口和单个非五格结构避免伪造绑定。 */
@GameTestHolder("create_nuclear_industry_quick_refuel")
@PrefixGameTestTemplate(false)
public final class RefuelingQuickSwapGameTests {
    private static final BlockPos OWNER = new BlockPos(1, 1, 0);
    private static final BlockPos PORT = new BlockPos(4, 7, 5);
    private static final CoreColumnPosition TARGET = new CoreColumnPosition(3, 4);

    private RefuelingQuickSwapGameTests() {
    }

    /** 用原正式右击入口期望占用槽快捷替换；同一 tick 检查物品和权威快照守恒。 */
    @GameTest(template = "p0_probe_empty", timeoutTicks = 80)
    public static void playerQuickSwapPreservesItemsAndRectangularState(GameTestHelper helper) {
        build(helper);
        helper.runAfterDelay(3, () -> {
            var owner = (ReactorInstrumentPortBlockEntity) helper.getBlockEntity(OWNER);
            var port = (ReactorPortBlockEntity) helper.getBlockEntity(PORT);
            helper.assertTrue(owner.structureValid() && port.isBoundTo(owner,
                    ReactorPortBlockEntity.BindingType.REFUELING, TARGET), "非5尺寸目标端口未按实际坐标成型绑定");
            ItemStack old = FuelAssemblyItemCodec.createCooledSpentFuel();
            old.set(DataComponents.CUSTOM_NAME, Component.literal("旧组件标记"));
            port.setFuelAssembly(old);
            owner.setSnapshot(owner.snapshot().withFuelColumn(TARGET, new FuelColumnState(
                    FuelAssemblyItemCodec.simulationState(old), 0.7D, 9.0D, 0.0D, 0.4D))
                    .withFuelColumn(new CoreColumnPosition(0, 0), new FuelColumnState(
                            FuelAssemblyItemCodec.simulationState(ItemStack.EMPTY), 0.8D, 2.0D))
                    .withCoolantInventories(12L, 7L));
            ReactorSnapshot before = owner.snapshot();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack incoming = fresh(123, 1);
            incoming.set(DataComponents.CUSTOM_NAME, Component.literal("精确新组件"));
            player.setItemInHand(InteractionHand.MAIN_HAND, incoming);

            rightClick(helper, player, InteractionHand.MAIN_HAND);

            helper.assertTrue(ItemStack.matches(old, player.getItemInHand(InteractionHand.MAIN_HAND)),
                    "快捷替换未把精确枯竭组件交回消耗后空出的主手（原占用槽拒绝路径）");
            helper.assertTrue(ItemStack.matches(incoming, port.fuelAssembly()), "新组件未原子写入占用端口");
            assertSnapshot(helper, owner, before, incoming);
            helper.assertTrue(owner.currentFuelColumnFissionHeatHu(TARGET) > 0.0D,
                    "无相邻控制棒的快捷换料没有沿用既有裂变规则");
            // 初始无控制棒先检验原放热规则；之后新增棒列，不拆除带权威状态的已有棒列。
            helper.setBlock(new BlockPos(3, 7, 5), P1Blocks.CONTROL_ROD_DRIVE.get().defaultBlockState());
            ReactorStructureLifecycle.rescanAroundNow(helper.getLevel(), helper.absolutePos(OWNER));
            seed(owner, port, old);
            owner.updateRedstoneScram(true);
            helper.assertTrue(owner.snapshot().scramRequested(), "测试未建立 SCRAM 状态");
            LogUtils.getLogger().info("QUICK_REFUEL 主手单件及尺寸/余热/完整度/安全热余数/其他列/冷热库存/SCRAM保持通过");
            verifyReturn(helper, owner, port, InteractionHand.OFF_HAND, 1, false, GameType.SURVIVAL);
            verifyReturn(helper, owner, port, InteractionHand.MAIN_HAND, 3, false, GameType.SURVIVAL);
            verifyReturn(helper, owner, port, InteractionHand.OFF_HAND, 2, true, GameType.SURVIVAL);
            verifyReturn(helper, owner, port, InteractionHand.MAIN_HAND, 1, false, GameType.CREATIVE);
            verifyReturn(helper, owner, port, InteractionHand.OFF_HAND, 2, true, GameType.CREATIVE);
            verifyRejectionAndArm(helper, owner, port);
            verifyUnformed(helper, owner, port);
            LogUtils.getLogger().info("QUICK_REFUEL 全部有限场景通过");
            helper.succeed();
        });
    }

    /** 每次使用新的模拟玩家及独立标记旧栈；逐件核对手、背包和实体的返还守恒。 */
    private static void verifyReturn(GameTestHelper helper, ReactorInstrumentPortBlockEntity owner,
                                     ReactorPortBlockEntity port, InteractionHand hand, int count,
                                     boolean full, GameType mode) {
        ItemStack old = FuelAssemblyItemCodec.createCooledSpentFuel();
        old.set(DataComponents.CUSTOM_NAME, Component.literal(hand + "-" + count + "-" + full + "-" + mode));
        seed(owner, port, old);
        ReactorSnapshot before = owner.snapshot();
        Player player = helper.makeMockPlayer(mode);
        // GameTest 的模拟玩家只重写 isCreative，原版库存实际读取 instabuild，必须显式同步能力。
        mode.updatePlayerAbilities(player.getAbilities());
        helper.assertTrue(player.hasInfiniteMaterials() == (mode == GameType.CREATIVE), "模拟玩家没有使用真实创造库存能力");
        BlockPos absolute = helper.absolutePos(PORT);
        player.setPos(absolute.getX() + 0.5D, absolute.getY() + 1.0D, absolute.getZ() + 0.5D);
        if (full) for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack sentinel = new ItemStack(Items.STICK, 64);
        player.setItemInHand(otherHand, sentinel.copy());
        ItemStack input = fresh(77, count);
        input.set(DataComponents.CUSTOM_NAME, Component.literal("输入数据"));
        player.setItemInHand(hand, input.copy());
        rightClick(helper, player, hand);
        helper.assertTrue(ItemStack.matches(sentinel, player.getItemInHand(otherHand)), "替换错误地修改了另一只手");
        helper.assertTrue(ItemStack.matches(input.copyWithCount(1), port.fuelAssembly()), "多件/副手输入数据没有保留");
        ItemStack expectedHand = count == 1 ? old : input.copyWithCount(count - 1);
        helper.assertTrue(ItemStack.matches(expectedHand, player.getItemInHand(hand)), "交互手余量或返还不符合单件消耗");
        int inventoryOutput = player.getInventory().items.stream()
                .filter(item -> ItemStack.isSameItemSameComponents(item, old)).mapToInt(ItemStack::getCount).sum();
        // 主手槽本身位于 items 列表中，手部返还不能再计作背包返还。
        if (ItemStack.isSameItemSameComponents(player.getItemInHand(InteractionHand.MAIN_HAND), old)) {
            inventoryOutput -= player.getItemInHand(InteractionHand.MAIN_HAND).getCount();
        }
        int drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                        new AABB(absolute).inflate(3.0D)).stream()
                .filter(entity -> ItemStack.isSameItemSameComponents(entity.getItem(), old))
                .mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(inventoryOutput == (count > 1 && !full ? 1 : 0), "背包返还数量错误或旧数据丢失");
        helper.assertTrue(drops == (count > 1 && full ? 1 : 0), "满背包没有只掉落一件精确旧组件");
        assertSnapshot(helper, owner, before, input);
        LogUtils.getLogger().info("QUICK_REFUEL 返还通过 hand={} input={} full={} mode={} inventory={} drops={}",
                hand, count, full, mode, inventoryOutput, drops);
    }

    /** 非枯竭、错误和耗尽输入保留物品；机械臂仍拒绝占用槽且模拟取料只读。 */
    private static void verifyRejectionAndArm(GameTestHelper helper, ReactorInstrumentPortBlockEntity owner,
                                             ReactorPortBlockEntity port) {
        seed(owner, port, fresh(ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY - 1, 1));
        helper.assertTrue(owner.currentFuelColumnFissionHeatHu(TARGET) == 0.0D, "拒绝夹具没有停止裂变");
        assertRejectedClick(helper, owner, port, fresh(0, 2));
        seed(owner, port, FuelAssemblyItemCodec.createCooledSpentFuel());
        assertRejectedClick(helper, owner, port, new ItemStack(Items.IRON_INGOT, 2));
        assertRejectedClick(helper, owner, port, fresh(ModItems.FRESH_FUEL_ASSEMBLY_MAX_DURABILITY, 2));
        ItemStack stored = port.fuelAssembly();
        ReactorSnapshot before = owner.snapshot();
        BlockPos absolute = helper.absolutePos(PORT);
        var point = FuelRefuelingArmInteractionPoint.TYPE.createPoint(helper.getLevel(), absolute,
                helper.getLevel().getBlockState(absolute));
        ItemStack input = fresh(0, 2);
        helper.assertTrue(ItemStack.matches(input, point.insert(null, input, true)), "机械臂模拟装入错误快捷替换了占用槽");
        helper.assertTrue(ItemStack.matches(input, point.insert(null, input, false)), "机械臂正式装入错误快捷替换了占用槽");
        helper.assertTrue(ItemStack.matches(stored, point.extract(null, 0, 1, true)), "机械臂模拟取料未返回原精确栈");
        helper.assertTrue(ItemStack.matches(stored, port.fuelAssembly()) && before.equals(owner.snapshot()), "机械臂失败/模拟修改了状态");
        helper.assertTrue(ItemStack.matches(stored, point.extract(null, 0, 1, false)) && port.fuelAssembly().isEmpty(), "机械臂原取出路径失效");
        helper.assertTrue(ItemStack.matches(input.copyWithCount(1), point.insert(null, input, true)) && port.fuelAssembly().isEmpty(), "机械臂空槽预检改变状态");
        helper.assertTrue(ItemStack.matches(input.copyWithCount(1), point.insert(null, input, false)), "机械臂原空槽装入路径失效");
        port.tryExtractFuel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.OFF_HAND, fresh(11, 1));
        rightClick(helper, player, InteractionHand.OFF_HAND);
        helper.assertTrue(player.getItemInHand(InteractionHand.OFF_HAND).isEmpty() && port.fuelAssembly().getDamageValue() == 11,
                "玩家原空槽装入路径失效");
        rightClick(helper, player, InteractionHand.OFF_HAND);
        helper.assertTrue(player.getItemInHand(InteractionHand.OFF_HAND).getDamageValue() == 11 && port.fuelAssembly().isEmpty(),
                "玩家原空手取出路径失效");
        LogUtils.getLogger().info("QUICK_REFUEL 剩一耐久/错物品/耗尽输入拒绝及原机械臂/玩家空槽/空手路径通过");
    }

    /** 失效结构不能再替换，失败保留端口物品和未成型前保存的结构状态。 */
    private static void verifyUnformed(GameTestHelper helper, ReactorInstrumentPortBlockEntity owner,
                                        ReactorPortBlockEntity port) {
        seed(owner, port, FuelAssemblyItemCodec.createCooledSpentFuel());
        helper.setBlock(BlockPos.ZERO, Blocks.AIR.defaultBlockState());
        ReactorStructureLifecycle.rescanAroundNow(helper.getLevel(), helper.absolutePos(OWNER));
        helper.assertTrue(!port.isBound(), "未成型拒绝夹具仍保留绑定");
        assertRejectedClick(helper, owner, port, fresh(0, 2));
        helper.assertTrue(port.tryReplaceExhaustedFuel(fresh(0, 2)).status() == FuelRefuelingTransaction.Status.INVALID_PORT,
                "失效绑定仍允许直接替换事务");
        LogUtils.getLogger().info("QUICK_REFUEL 未成型/失效绑定拒绝通过");
    }

    /** 对同一服务端 tick 的拒绝路径检查零消费、零返还、零快照提交。 */
    private static void assertRejectedClick(GameTestHelper helper, ReactorInstrumentPortBlockEntity owner,
                                            ReactorPortBlockEntity port, ItemStack input) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stored = port.fuelAssembly();
        ReactorSnapshot before = owner.snapshot();
        player.setItemInHand(InteractionHand.MAIN_HAND, input.copy());
        rightClick(helper, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(ItemStack.matches(input, player.getItemInHand(InteractionHand.MAIN_HAND))
                && player.getInventory().items.stream().skip(1).allMatch(ItemStack::isEmpty)
                && ItemStack.matches(stored, port.fuelAssembly()) && before.equals(owner.snapshot()), "拒绝交互消费/返还或修改权威状态");
    }

    /** 仅夹具播种物品与投影；余热、完整度、冷液及其他列沿用上一快照。 */
    private static void seed(ReactorInstrumentPortBlockEntity owner, ReactorPortBlockEntity port, ItemStack stored) {
        port.setFuelAssembly(stored);
        owner.setSnapshot(owner.snapshot().withFuelColumn(TARGET,
                owner.snapshot().fuelColumns().get(TARGET).withFuelAssemblyProjection(FuelAssemblyItemCodec.simulationState(stored))));
    }

    /** 比较完整快照而非孤立字段，只允许目标组件与其燃耗余数发生合法变化。 */
    private static void assertSnapshot(GameTestHelper helper, ReactorInstrumentPortBlockEntity owner,
                                        ReactorSnapshot before, ItemStack incoming) {
        ReactorSnapshot expected = before.withFuelColumn(TARGET,
                before.fuelColumns().get(TARGET).withFuelAssembly(FuelAssemblyItemCodec.readFreshFuel(incoming)));
        helper.assertTrue(expected.equals(owner.snapshot()), "快捷替换修改了尺寸/余热/完整度/其他列/冷热库存/SCRAM");
    }

    /** 单夹具为6×8×7，两根燃料列；目标位于默认3×3坐标范围之外。 */
    private static void build(GameTestHelper helper) {
        for (int x = 0; x < 6; x++) for (int y = 0; y < 8; y++) for (int z = 0; z < 7; z++) {
            boolean shell = x == 0 || x == 5 || y == 0 || y == 7 || z == 0 || z == 6;
            helper.setBlock(new BlockPos(x, y, z), shell
                    ? P1Blocks.REACTOR_CASING.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        for (int y = 1; y < 7; y++) {
            helper.setBlock(new BlockPos(4, y, 5), P1Blocks.REACTOR_FUEL_ROD.get().defaultBlockState());
            helper.setBlock(new BlockPos(1, y, 1), P1Blocks.REACTOR_FUEL_ROD.get().defaultBlockState());
        }
        helper.setBlock(PORT, P1Blocks.REACTOR_REFUELING_PORT.get().defaultBlockState());
        helper.setBlock(new BlockPos(1, 7, 1), P1Blocks.REACTOR_REFUELING_PORT.get().defaultBlockState());
        helper.setBlock(new BlockPos(5, 1, 1), P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState());
        helper.setBlock(new BlockPos(5, 1, 5), P1Blocks.REACTOR_HOT_PORT.get().defaultBlockState());
        helper.setBlock(OWNER, P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
        ReactorStructureLifecycle.rescanAroundNow(helper.getLevel(), helper.absolutePos(OWNER));
    }

    /** 主副手均通过真实 BlockState 入口，将交互手的栈交给服务端方块。 */
    private static void rightClick(GameTestHelper helper, Player player, InteractionHand hand) {
        BlockPos absolute = helper.absolutePos(PORT);
        helper.getLevel().getBlockState(absolute).useItemOn(player.getItemInHand(hand), helper.getLevel(),
                player, hand, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
    }

    private static ItemStack fresh(int damage, int count) {
        ItemStack item = new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get(), count);
        item.setDamageValue(damage);
        return item;
    }
}
