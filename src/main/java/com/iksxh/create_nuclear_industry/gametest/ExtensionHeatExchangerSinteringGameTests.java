package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.content.FuelProcessingContent;
import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity;
import com.iksxh.create_nuclear_industry.production.FuelSinteringBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 使用真实换热器与烧结炉方块实体验证持续付款、加工工时和恢复语义。 */
@GameTestHolder("create_nuclear_industry_ext_b_sintering")
@PrefixGameTestTemplate(false)
public final class ExtensionHeatExchangerSinteringGameTests {
    private static final String TEMPLATE = "basin_empty";
    private static final BlockPos FURNACE = new BlockPos(3, 2, 3);
    private static final BlockPos EXCHANGER = FURNACE.below();

    private ExtensionHeatExchangerSinteringGameTests() {}

    /** 先登记炉、后登记热源；验证空炉耗热及反向ticker顺序下完成400有效tick加工。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 460)
    public static void emptyFurnaceConsumesHeatAndReverseTickerProcessesFuel(GameTestHelper helper) {
        FuelSinteringBlockEntity furnace = placeFurnace(helper);
        NuclearHeatExchangerBlockEntity exchanger = placeExchanger(helper, 2000);
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(exchanger.ledger().cold() >= 20 && exchanger.ledger().cold() <= 24,
                    "空烧结炉未按默认4mB/t持续耗热");
            helper.assertTrue(furnace.state().progress() == 0, "空炉不应累计加工工时");
            helper.assertTrue(furnace.insert(new ItemStack(FuelProcessingContent.GREEN_FUEL_PELLET.get())) == 1,
                    "烧结炉未接收生芯块");
        });
        helper.runAfterDelay(420, () -> {
            helper.assertTrue(furnace.state().input() == 0 && furnace.state().output() == 1,
                    "反向ticker顺序下烧结炉未完成一件400有效tick加工");
            helper.assertTrue(exchanger.ledger().cold() >= 1600,
                    "烧结加工期间未按持续负载转出等量冷液");
            helper.succeed();
        });
    }

    /** 冷液罐恰好留4mB时完成一次付款，回液堵满后暂停并保留工时，排液补热后恢复。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 1500)
    public static void blockedColdReturnPreservesProgressAndResumes(GameTestHelper helper) {
        helper.setBlock(FURNACE, AllBlocks.BASIN.get());
        NuclearHeatExchangerBlockEntity exchanger = placeExchanger(helper, 4000);
        helper.assertTrue(helper.getBlockEntity(FURNACE) instanceof BasinBlockEntity,
                "复用的盆空模板未提供预置工作盆");
        helper.runAfterDelay(1000, () -> {
            helper.assertTrue(exchanger.ledger().cold() == 4000,
                    "空盆持续运行未填满换热器冷罐");
            FuelSinteringBlockEntity furnace = placeFurnace(helper);
            helper.assertTrue(exchanger.ledger().drainCold(4, false) == 4,
                    "冷液堵塞用例未能腾出最后一tick回液空间");
            helper.assertTrue(exchanger.ledger().fillHot(20, false) == 20,
                    "冷液堵塞用例未能加入最后一tick热液");
            helper.assertTrue(furnace.insert(new ItemStack(FuelProcessingContent.GREEN_FUEL_PELLET.get())) == 1,
                    "冷液堵塞用例未接收生芯块");
        });
        helper.runAfterDelay(1010, () -> {
            FuelSinteringBlockEntity furnace = furnace(helper);
            helper.assertTrue(exchanger.ledger().cold() == 4000 && exchanger.ledger().hot() > 0
                            && furnace.state().progress() >= 1 && furnace.state().progress() <= 2
                            && !furnace.heated(),
                    "冷液满后未立即停止供热，或未保留最后一次有效工时");
            helper.assertTrue(exchanger.ledger().drainCold(4000, false) == 4000,
                    "无法排出堵塞冷液");
            helper.assertTrue(exchanger.ledger().fillHot(1600, false) == 1600,
                    "冷液恢复后无法补入热液");
        });
        helper.runAfterDelay(1430, () -> {
            FuelSinteringBlockEntity furnace = furnace(helper);
            helper.assertTrue(furnace.state().input() == 0 && furnace.state().output() == 1,
                    "冷液回流恢复后未从已保留工时继续完成加工");
            helper.succeed();
        });
    }

    /** 热液耗尽时暂停且进度不回退，重新供液后从原进度继续加工。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 500)
    public static void emptyHotTankPausesAndResumesSintering(GameTestHelper helper) {
        NuclearHeatExchangerBlockEntity exchanger = placeExchanger(helper, 20);
        FuelSinteringBlockEntity furnace = placeFurnace(helper);
        helper.assertTrue(furnace.insert(new ItemStack(FuelProcessingContent.GREEN_FUEL_PELLET.get())) == 1,
                "断流用例未接收生芯块");
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(exchanger.ledger().hot() == 0 && furnace.state().progress() >= 4
                            && furnace.state().progress() <= 6,
                    "热液耗尽后未停热，或有效工时与实际付款不符");
        });
        helper.runAfterDelay(16, () -> {
            helper.assertTrue(furnace.state().progress() >= 4 && furnace.state().progress() <= 6,
                    "断流期间烧结工时没有保持");
            helper.assertTrue(exchanger.ledger().fillHot(1600, false) == 1600,
                    "恢复供液未进入热罐");
        });
        helper.runAfterDelay(430, () -> {
            helper.assertTrue(furnace.state().input() == 0 && furnace.state().output() == 1,
                    "恢复热液后未从断流前的进度继续完成加工");
            helper.succeed();
        });
    }

    private static FuelSinteringBlockEntity placeFurnace(GameTestHelper helper) {
        helper.setBlock(FURNACE, FuelProcessingContent.FUEL_SINTERING_FURNACE.get());
        return furnace(helper);
    }

    private static NuclearHeatExchangerBlockEntity placeExchanger(GameTestHelper helper, int hotMb) {
        helper.setBlock(EXCHANGER, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get());
        NuclearHeatExchangerBlockEntity exchanger = exchanger(helper);
        helper.assertTrue(exchanger.ledger().fillHot(hotMb, false) == hotMb,
                "换热器未接收测试热液库存");
        return exchanger;
    }

    private static FuelSinteringBlockEntity furnace(GameTestHelper helper) {
        return (FuelSinteringBlockEntity) helper.getBlockEntity(FURNACE);
    }

    private static NuclearHeatExchangerBlockEntity exchanger(GameTestHelper helper) {
        return (NuclearHeatExchangerBlockEntity) helper.getBlockEntity(EXCHANGER);
    }
}
