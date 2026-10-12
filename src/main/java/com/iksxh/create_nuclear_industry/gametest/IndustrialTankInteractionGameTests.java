package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.simibubi.create.AllBlocks;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 独立服务端手持交互探针；调用真实BlockState入口与能力，夹具不替代Create桶操作或库存。 */
@GameTestHolder("industrial_interaction_probe")
@PrefixGameTestTemplate(false)
public final class IndustrialTankInteractionGameTests {
    private static final BlockPos TANK = new BlockPos(1, 1, 1);
    private IndustrialTankInteractionGameTests() { }

    /** 生存玩家标记创造罐应保持桶数量；空桶清除后只按原Create路径返还一桶冷液。 */
    @GameTest(template = "empty", templateNamespace = "industrial_interaction_probe", timeoutTicks = 80)
    public static void creativeColdBucketMarksAndClearsWithOriginalCounts(GameTestHelper helper) {
        helper.setBlock(TANK, AllBlocks.CREATIVE_FLUID_TANK.getDefaultState());
        helper.runAfterDelay(5, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
            BlockPos absolute = helper.absolutePos(TANK);
            player.setPos(absolute.getX() + .5, absolute.getY() + 1, absolute.getZ() + .5);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.COMPOUND_COOLANT_BUCKET.get(), 2));
            helper.assertTrue(rightClick(helper, player) == ItemInteractionResult.SUCCESS, "冷液桶没有走成功手持交互");
            var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, absolute, null);
            helper.assertTrue(handler != null, "真实创造罐能力缺失");
            helper.assertTrue(handler.getFluidInTank(0).is(ModFluids.COMPOUND_COOLANT_SOURCE.get()), "冷液桶成功交互后创造罐仍未标记冷液身份");
            helper.assertTrue(player.getMainHandItem().is(ModItems.COMPOUND_COOLANT_BUCKET.get()) && player.getMainHandItem().getCount() == 2, "标记创造罐不得消耗原两桶冷液");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET, 3));
            helper.assertTrue(rightClick(helper, player) == ItemInteractionResult.SUCCESS, "空桶没有走成功清除交互");
            helper.assertTrue(handler.getFluidInTank(0).isEmpty(), "空桶清除后创造罐仍保留身份");
            helper.assertTrue(player.getMainHandItem().is(Items.BUCKET) && player.getMainHandItem().getCount() == 3, "创造罐清除应保留原三只空桶");
            helper.assertTrue(player.getInventory().countItem(ModItems.COMPOUND_COOLANT_BUCKET.get()) == 1, "原Create清除路径应仅返还一桶冷液");
            helper.succeed();
        });
    }

    /** 副手水桶同样标记/清除，非容器失败不得清身份或修改另一只手。 */
    @GameTest(template = "empty", templateNamespace = "industrial_interaction_probe", timeoutTicks = 80)
    public static void creativeWaterOffHandKeepsCountsAndRejectsNonContainer(GameTestHelper helper) {
        helper.setBlock(TANK, AllBlocks.CREATIVE_FLUID_TANK.getDefaultState());
        helper.runAfterDelay(5, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 7));
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET, 2));
            helper.assertTrue(rightClick(helper, player, InteractionHand.OFF_HAND) == ItemInteractionResult.SUCCESS, "副手水桶未成功标记");
            var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(TANK), null);
            helper.assertTrue(handler.getFluidInTank(0).is(net.minecraft.world.level.material.Fluids.WATER), "副手水桶身份缺失");
            helper.assertTrue(player.getOffhandItem().is(Items.WATER_BUCKET) && player.getOffhandItem().getCount() == 2, "创造罐标记消耗了水桶");
            rightClick(helper, player, InteractionHand.MAIN_HAND);
            helper.assertTrue(handler.getFluidInTank(0).is(net.minecraft.world.level.material.Fluids.WATER), "非容器失败路径清除了身份");
            helper.assertTrue(player.getMainHandItem().is(Items.STICK) && player.getMainHandItem().getCount() == 7, "副手操作修改另一只手");
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET, 3));
            helper.assertTrue(rightClick(helper, player, InteractionHand.OFF_HAND) == ItemInteractionResult.SUCCESS && handler.getFluidInTank(0).isEmpty(), "副手空桶未清水身份");
            helper.assertTrue(player.getOffhandItem().is(Items.BUCKET) && player.getOffhandItem().getCount() == 3, "副手清除消耗空桶");
            helper.assertTrue(player.getInventory().countItem(Items.WATER_BUCKET) == 1, "副手清除未按原路径返还一桶水");
            helper.succeed();
        });
    }

    /** 普通罐仍按真实创造玩家原helper逐桶转移有限1000mB，不获得创造无限身份。 */
    @GameTest(template = "empty", templateNamespace = "industrial_interaction_probe", timeoutTicks = 80)
    public static void ordinaryColdAndWaterBucketsKeepFiniteTransfers(GameTestHelper helper) {
        helper.setBlock(TANK, AllBlocks.FLUID_TANK.getDefaultState());
        helper.runAfterDelay(5, () -> {
            Player player = helper.makeMockPlayer(GameType.CREATIVE);
            GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
            var handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(TANK), null);
            for (var item : java.util.List.of(ModItems.COMPOUND_COOLANT_BUCKET.get(), Items.WATER_BUCKET)) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 2));
                helper.assertTrue(rightClick(helper, player) == ItemInteractionResult.SUCCESS, "普通罐未成功加注");
                helper.assertTrue(handler.getFluidInTank(0).getAmount() == 1000, "普通罐加注变成无限库存或丢失数量");
                helper.assertTrue(player.getMainHandItem().is(item) && player.getMainHandItem().getCount() == 2, "原创造玩家桶数量被改动");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET, 3));
                helper.assertTrue(rightClick(helper, player) == ItemInteractionResult.SUCCESS && handler.getFluidInTank(0).isEmpty(), "普通罐未按1000mB出库");
                helper.assertTrue(player.getMainHandItem().is(Items.BUCKET) && player.getMainHandItem().getCount() == 3, "原创造玩家空桶数量被改动");
            }
            helper.succeed();
        });
    }

    /** 只以实际手持栈点击目标，禁止夹具直接setContainedFluid掩盖生产类型分支丢失。 */
    private static ItemInteractionResult rightClick(GameTestHelper helper, Player player) {
        return rightClick(helper, player, InteractionHand.MAIN_HAND);
    }

    private static ItemInteractionResult rightClick(GameTestHelper helper, Player player, InteractionHand hand) {
        BlockPos absolute = helper.absolutePos(TANK);
        return helper.getLevel().getBlockState(absolute).useItemOn(player.getItemInHand(hand), helper.getLevel(), player,
                hand, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
    }
}
