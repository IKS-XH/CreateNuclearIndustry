package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.boiler.BoilerStructure;
import com.iksxh.create_nuclear_industry.config.BoilerConfig;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 隔离服务端尺寸探针：读取正式默认配置和原扫描器，不修改测试配置或强载世界区块。 */
@GameTestHolder("create_nuclear_industry_boiler_size")
@PrefixGameTestTemplate(false)
public final class BoilerSizeLimitGameTests {
    private static final BlockPos BASE = new BlockPos(4, 2, 4);
    private BoilerSizeLimitGameTests() {}

    @GameTest(template = "boiler_size_empty", timeoutTicks = 80)
    public static void originalElevenReferenceKeepsOffCenterPartitionAndRejectsSeventeen(GameTestHelper h) {
        ExtensionBoilerGameTests.elevenBoundaryAndOffCenterPartition(h);
    }

    @GameTest(template = "boiler_size_empty", timeoutTicks = 80)
    public static void sixteenCubeCrossesChunksAndUsesActualPartitionCapacity(GameTestHelper h) {
        var forcedBefore = new HashSet<>(h.getLevel().getForcedChunks());
        ExtensionBoilerGameTests.build(h, 16, 16, 16, 8);
        h.runAfterDelay(4, () -> {
            var owner = ExtensionBoilerGameTests.owner(h);
            var form = owner.currentForm();
            h.assertTrue(BoilerConfig.DIMENSION_RANGE.get().equals(List.of(5, 16)), "服务端必须实际读取未覆盖的默认[5,16]");
            h.assertTrue(form != null && form.width() == 16 && form.height() == 16 && form.depth() == 16, "默认16³未成型");
            h.assertTrue((form.min().getX() >> 4) != (form.max().getX() >> 4)
                    && (form.min().getZ() >> 4) != (form.max().getZ() >> 4), "最大尺寸夹具未实际跨X/Z区块");
            h.assertTrue(form.partitionY() == h.absolutePos(BASE.above(8)).getY()
                    && form.waterCells() == 1372 && form.steamCells() == 1176, "14×14截面的7层水/6层汽格数错误");
            var ledger = owner.ledger();
            h.assertTrue(ledger.settings().equals(BoilerConfig.settings()) && ledger.settings().maxDimension() == 16,
                    "实际账本未使用服务器配置快照");
            h.assertTrue(ledger.waterCapacity() == 2744000 && ledger.steamCapacity() == 2352000, "最大尺寸实际容量错误");
            var steam = h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, h.absolutePos(BASE.offset(0, 9, 1)), Direction.WEST);
            h.assertTrue(ExtensionBoilerGameTests.water(h).getTankCapacity(0) == 2744000
                    && steam != null && steam.getTankCapacity(0) == 2352000, "真实端口未公开分区容量");
            h.assertTrue(ledger.water() == 0 && ledger.steam() == 0 && ledger.hot() == 0 && ledger.cold() == 0
                    && ledger.totalHu() == 0, "扩大几何凭空新增了工质或能量");
            h.assertTrue(forcedBefore.equals(h.getLevel().getForcedChunks()), "尺寸验证新增了强载区块");
            System.out.println("[boiler-size-limit] 16x16x16 config=[5,16] cross-X/Z waterCells=1372 steamCells=1176 waterMb=2744000 steamMb=2352000 forcedChunks=unchanged");
            h.succeed();
        });
    }

    @GameTest(template = "boiler_size_empty", timeoutTicks = 80)
    public static void seventeenWidthIsRejectedByDefaultConfiguration(GameTestHelper h) {
        ExtensionBoilerGameTests.build(h, 17, 16, 16, 8);
        h.runAfterDelay(4, () -> {
            var owner = ExtensionBoilerGameTests.owner(h);
            h.assertTrue(BoilerConfig.settings().valid() && BoilerConfig.settings().maxDimension() == 16, "越界探针配置不是有效默认16");
            h.assertTrue(owner.currentForm() == null, "17格宽度被错误接受");
            h.assertTrue(BoilerStructure.issue(h.getLevel(), owner.getBlockPos()).reason().equals("bounds"), "17格未因尺寸边界拒绝");
            System.out.println("[boiler-size-limit] 17x16x16 rejected=bounds configMax=16");
            h.succeed();
        });
    }
}
