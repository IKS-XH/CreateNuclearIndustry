package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerPartBlock;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlock;
import com.iksxh.create_nuclear_industry.heat.NuclearHeatExchangerBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 验证锅炉实际tick的护目镜镜像经初始标签及更新包往返，并在停机归零。 */
@GameTestHolder("create_nuclear_industry_goggle_sync")
@PrefixGameTestTemplate(false)
public final class ExtensionGoggleSyncGameTests {
    private static final BlockPos CENTER = new BlockPos(10, 2, 3);
    private static final BlockPos CONTROL = CENTER.offset(0, 1, -2);
    private static final BlockPos SECTION = CENTER.offset(1, 0, 0);
    private static final BlockPos SOURCE = SECTION.below();

    private ExtensionGoggleSyncGameTests() {}

    @GameTest(template = "boiler_empty", timeoutTicks = 60)
    public static void boilerProductionAndVentSnapshotRoundTripThenClear(GameTestHelper helper) {
        buildBoiler(helper);
        helper.onEachTick(() -> {
            var source = (NuclearHeatExchangerBlockEntity) helper.getBlockEntity(SOURCE);
            if (!source.current() || !source.canTick()) return;
            source.fluidPort(Direction.EAST).fill(new FluidStack(
                    ModFluids.HOT_COMPOUND_COOLANT_SOURCE.get(), 36), IFluidHandler.FluidAction.EXECUTE);
            source.fluidPort(Direction.WEST).drain(4000, IFluidHandler.FluidAction.EXECUTE);
        });
        helper.runAfterDelay(4, () -> {
            BoilerControllerBlockEntity owner = owner(helper);
            var saved = owner.ledger().save();
            saved.putInt("Water", 1000);
            saved.putInt("Steam", 14399);
            saved.putDouble("WarmHu", 3600);
            saved.putBoolean("Ready", true);
            owner.ledger().load(saved);
        });
        BoilerControllerBlockEntity[] packetMirror = {null};
        helper.runAfterDelay(8, () -> {
            BoilerControllerBlockEntity owner = owner(helper);
            require(helper, owner.ledger().produced() > 0 && owner.ledger().vented() > 0,
                    "锅炉真实服务端tick未同时产汽和排汽：产汽=" + owner.ledger().produced()
                            + " 排汽=" + owner.ledger().vented());

            BoilerControllerBlockEntity initialTagMirror = mirror(owner);
            initialTagMirror.handleUpdateTag(owner.getUpdateTag(helper.getLevel().registryAccess()),
                    helper.getLevel().registryAccess());
            assertSnapshot(helper, initialTagMirror, owner.ledger().produced(), owner.ledger().vented());

            packetMirror[0] = mirror(owner);
            packetMirror[0].onDataPacket(null, owner.getUpdatePacket(), helper.getLevel().registryAccess());
            assertSnapshot(helper, packetMirror[0], owner.ledger().produced(), owner.ledger().vented());

            helper.setBlock(CENTER.offset(0, 1, -3), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(22, () -> {
            BoilerControllerBlockEntity owner = owner(helper);
            require(helper, owner.ledger().produced() == 0 && owner.ledger().vented() == 0,
                    "停机后锅炉本tick产汽/排汽未归零：产汽=" + owner.ledger().produced()
                            + " 排汽=" + owner.ledger().vented());
            require(helper, packetMirror[0] != null, "初始数据包没有建立锅炉镜像");
            packetMirror[0].onDataPacket(null, owner.getUpdatePacket(), helper.getLevel().registryAccess());
            assertSnapshot(helper, packetMirror[0], owner.ledger().produced(), owner.ledger().vented());
            helper.succeed();
        });
    }

    private static void assertSnapshot(GameTestHelper helper, BoilerControllerBlockEntity mirror,
                                       int expectedProduced, int expectedVented) {
        List<Component> tooltip = new ArrayList<>();
        require(helper, mirror.addToGoggleTooltip(tooltip, false), "锅炉没有提供护目镜遥测");
        Object[] flow = args(helper, tooltip, "gui.create_nuclear_industry.boiler.flow");
        Object[] vent = args(helper, tooltip, "gui.create_nuclear_industry.boiler.vent");
        int produced = ((Number) flow[1]).intValue();
        int vented = ((Number) vent[0]).intValue();
        require(helper, produced == expectedProduced && vented == expectedVented,
                "客户端只读tooltip镜像计数与服务端账本不符：镜像产汽=" + produced + " 账本产汽="
                        + expectedProduced + " 镜像排汽=" + vented + " 账本排汽=" + expectedVented);
    }

    private static Object[] args(GameTestHelper helper, List<Component> tooltip, String key) {
        for (Component line : tooltip) {
            if (line.getContents() instanceof TranslatableContents translated && translated.getKey().equals(key))
                return translated.getArgs();
        }
        helper.fail("护目镜内容缺少翻译键：" + key);
        return new Object[0];
    }

    private static void buildBoiler(GameTestHelper helper) {
        for (int y = 0; y < 5; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            BlockPos part = CENTER.offset(x, y, z);
            if (part.equals(CONTROL)) continue;
            if (y >= 1 && y <= 3 && Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                helper.setBlock(part, Blocks.AIR);
                continue;
            }
            helper.setBlock(part, BoilerContent.CASING.get());
        }
        helper.setBlock(CONTROL, BoilerContent.CONTROLLER.get().defaultBlockState()
                .setValue(BoilerPartBlock.FACING, Direction.NORTH));
        helper.setBlock(CENTER.offset(2, 1, 0), BoilerContent.WATER_PORT.get().defaultBlockState()
                .setValue(BoilerPartBlock.FACING, Direction.EAST));
        helper.setBlock(CENTER.offset(-2, 3, 0), BoilerContent.STEAM_PORT.get().defaultBlockState()
                .setValue(BoilerPartBlock.FACING, Direction.WEST));
        helper.setBlock(CENTER.above(4), BoilerContent.SAFETY_VALVE.get());
        helper.setBlock(SECTION, BoilerContent.HEAT_SECTION.get());
        helper.setBlock(SOURCE, HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState()
                .setValue(NuclearHeatExchangerBlock.FACING, Direction.WEST));
    }

    private static BoilerControllerBlockEntity owner(GameTestHelper helper) {
        return (BoilerControllerBlockEntity) helper.getBlockEntity(CONTROL);
    }

    private static BoilerControllerBlockEntity mirror(BoilerControllerBlockEntity owner) {
        return new BoilerControllerBlockEntity(owner.getBlockPos(), owner.getBlockState());
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
