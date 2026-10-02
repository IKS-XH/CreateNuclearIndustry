package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.iksxh.create_nuclear_industry.production.FuelSinteringBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 独立命名空间的真实热源与顶面漏斗输入；侧面抽取逐面核对能力边界。 */
@GameTestHolder("create_nuclear_industry_02a")
@PrefixGameTestTemplate(false)
public final class ExtensionFuelSinteringGameTests {
    private static final BlockPos FURNACE = new BlockPos(2, 2, 2);
    private static final BlockPos BURNER = FURNACE.below();
    private static final BlockPos HOPPER = FURNACE.above();
    private ExtensionFuelSinteringGameTests() {}

    @GameTest(template = "p0_probe_empty", timeoutTicks = 600)
    public static void realHeatAndHopperInput(GameTestHelper helper) {
        helper.setBlock(FURNACE, FuelProcessingContent.FUEL_SINTERING_FURNACE.get());
        helper.setBlock(HOPPER, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        ((HopperBlockEntity) helper.getBlockEntity(HOPPER)).setItem(0,
                new ItemStack(FuelProcessingContent.GREEN_FUEL_PELLET.get()));
        FuelSinteringBlockEntity machine = (FuelSinteringBlockEntity) helper.getBlockEntity(FURNACE);
        require(helper, machine.itemPort(Direction.DOWN) == null, "底面错误开放物品端口");
        helper.runAfterDelay(35, () -> {
            require(helper, machine.state().input() == 1 && machine.state().progress() == 0,
                    "原生漏斗未从顶面投料，或冷炉错误加工");
            var side = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(FURNACE), Direction.EAST);
            require(helper, side != null && side.insertItem(0, new ItemStack(Items.IRON_INGOT), false).getCount() == 1
                    && side.extractItem(0, 1, false).isEmpty(), "侧面拒绝错误物料或空产物失败");
            helper.setBlock(BURNER, AllBlocks.BLAZE_BURNER.getDefaultState()
                    .setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
        });
        helper.runAfterDelay(70, () -> {
            require(helper, !machine.heated() && machine.state().progress() == 0, "阴燃错误推进工时");
            ItemStack coal = new ItemStack(Items.COAL);
            require(helper, BlazeBurnerBlock.tryInsert(helper.getBlockState(BURNER), helper.getLevel(),
                    helper.absolutePos(BURNER), coal, false, false, false).getResult().consumesAction()
                    && coal.isEmpty(), "真实燃烧室未消耗煤");
        });
        helper.runAfterDelay(90, () -> require(helper, machine.heated(), "真实燃烧室未达到普通热级"));
        helper.runAfterDelay(495, () -> {
            require(helper, machine.state().input() == 0 && machine.state().output() == 1
                    && machine.state().progress() == 0, "真实热源未按400有效tick产一件");
            var side = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                    helper.absolutePos(FURNACE), Direction.EAST);
            require(helper, side != null && side.extractItem(0, 1, false)
                    .is(FuelProcessingContent.SINTERED_FUEL_PELLET.get())
                    && machine.state().output() == 0, "侧面未抽出唯一成品");
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
            for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            player.getInventory().setItem(0, new ItemStack(FuelProcessingContent.GREEN_FUEL_PELLET.get(), 63));
            machine.state().insert(2, false);
            machine.changed();
            require(helper, machine.takeToPlayer(player, true) == 1 && machine.state().input() == 1
                    && player.getInventory().getItem(0).getCount() == 64,
                    "背包仅剩一格时手工取料未按实际容纳量结算");
            require(helper, machine.takeToPlayer(player, true) == 0 && machine.state().input() == 1,
                    "满背包错误扣除机器生料");
            helper.succeed();
        });
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
