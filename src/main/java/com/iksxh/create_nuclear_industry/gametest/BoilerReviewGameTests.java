package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.boiler.BoilerControllerBlockEntity;
import com.iksxh.create_nuclear_industry.boiler.BoilerPartBlock;
import com.iksxh.create_nuclear_industry.boiler.BoilerStructure;
import com.iksxh.create_nuclear_industry.config.BoilerConfig;
import com.iksxh.create_nuclear_industry.content.BoilerContent;
import com.iksxh.create_nuclear_industry.content.HeatExchangeContent;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 审查整改专用真实世界场景；各自独立批次，避免临时SERVER配置与其他测试并发。 */
@GameTestHolder("create_nuclear_industry_boiler_rework")
@PrefixGameTestTemplate(false)
public final class BoilerReviewGameTests {
    private static final TicketType<ChunkPos> TICKET = TicketType.create("boiler_review", Comparator.comparingLong(ChunkPos::toLong));
    private BoilerReviewGameTests() {}
    /** 仅一底机、一再热段；所有功能部件在控制器一端，远端可保持纯壳/空气/普通隔层。 */
    private static BlockPos buildBox(ServerLevel level, BlockPos min, int width, boolean west) {
        for (int x = 0; x < width; x++) for (int y = 0; y < 5; y++) for (int z = 0; z < 5; z++) {
            boolean air = x > 0 && x < width - 1 && z > 0 && z < 4 && (y == 1 || y == 3);
            level.setBlockAndUpdate(min.offset(x, y, z), (air ? Blocks.AIR : BoilerContent.CASING.get()).defaultBlockState());
        }
        int end = west ? 0 : width - 1, inner = west ? 1 : width - 2;
        Direction facing = west ? Direction.WEST : Direction.EAST;
        BlockPos control = min.offset(end, 1, 1);
        level.setBlockAndUpdate(control, BoilerContent.CONTROLLER.get().defaultBlockState().setValue(BoilerPartBlock.FACING, facing));
        level.setBlockAndUpdate(min.offset(end, 1, 2), BoilerContent.WATER_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, facing));
        level.setBlockAndUpdate(min.offset(end, 1, 3), BoilerContent.HOT_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, facing));
        level.setBlockAndUpdate(min.offset(end, 2, 2), BoilerContent.COLD_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, facing));
        level.setBlockAndUpdate(min.offset(end, 3, 1), BoilerContent.STEAM_PORT.get().defaultBlockState().setValue(BoilerPartBlock.FACING, facing));
        level.setBlockAndUpdate(min.offset(inner, 0, 1), HeatExchangeContent.NUCLEAR_HEAT_EXCHANGER.get().defaultBlockState());
        level.setBlockAndUpdate(min.offset(inner, 2, 1), BoilerContent.HEAT_SECTION.get().defaultBlockState());
        level.setBlockAndUpdate(min.offset(inner, 4, 1), BoilerContent.SAFETY_VALVE.get().defaultBlockState());
        return control;
    }
    @GameTest(template = "boiler_empty", timeoutTicks = 600, batch = "review_chunk")
    public static void distantShellChunkReloadRestoresFailedCache(GameTestHelper h) {
        var level = h.getLevel(); var source = level.getChunkSource();
        var range = BoilerConfig.DIMENSION_RANGE.get(); BoilerConfig.DIMENSION_RANGE.set(List.of(5, 32));
        // 距离测试模板1000区块，避免GameTest自动票据替远端保活。控制器票据32仅保证邻1 FULL。
        ChunkPos center = new ChunkPos(1000, 1000), far = new ChunkPos(1002, 1000);
        source.addRegionTicket(TICKET, center, 1, center); source.addRegionTicket(TICKET, far, 0, far);
        BlockPos min = new BlockPos(center.getMinBlockX() + 15, 80, center.getMinBlockZ() + 4);
        BlockPos control = buildBox(level, min, 32, true), water = control.south();
        var owner = (BoilerControllerBlockEntity) level.getBlockEntity(control);
        int[] stage = {0}; IFluidHandler[] old = {null}; double[] hu = {0};
        Runnable cleanup = () -> {
            BoilerConfig.DIMENSION_RANGE.set(range);
            source.removeRegionTicket(TICKET, center, 1, center); source.removeRegionTicket(TICKET, far, 0, far);
        };
        h.onEachTick(() -> {
            try {
                if (h.getTick() >= 590) h.fail("区块FULL可用性退降/恢复未在期限内完成，阶段=" + stage[0]
                        + ", loaded=" + level.hasChunk(far.x, far.z));
                if (stage[0] == 0) {
                    if (!owner.current() || owner.currentForm() == null) return;
                    h.assertTrue(owner.currentForm().exchangers().size() == 1 && new ChunkPos(owner.currentForm().exchangers().getFirst()).x == center.x + 1, "底机必须仅在中间chunk1");
                    var remote = source.getChunkNow(far.x, far.z);
                    h.assertTrue(remote != null && remote.getBlockEntities().isEmpty(), "远端区块含BE，不能证明纯外壳恢复");
                    owner.tick(); old[0] = level.getCapability(Capabilities.FluidHandler.BLOCK, water, Direction.WEST);
                    h.assertTrue(old[0] != null, "初始水口不可用");
                    old[0].fill(new FluidStack(Fluids.WATER, 123), IFluidHandler.FluidAction.EXECUTE);
                    var saved = owner.ledger().save(); saved.putInt("Steam", 100); saved.putDouble("SteamHu", 80);
                    saved.putInt("Cold", 37); owner.ledger().load(saved); hu[0] = owner.ledger().totalHu();
                    source.removeRegionTicket(TICKET, far, 0, far); stage[0] = 1;
                } else if (stage[0] == 1) {
                    if (level.hasChunk(far.x, far.z)) return;
                    h.assertTrue(owner.current() && level.hasChunk(center.x + 1, center.z), "控制器/中间区块未持续存活");
                    h.assertTrue(owner.currentForm() == null && owner.currentForm() == null, "缺区块未进入失败缓存");
                    h.assertTrue(old[0].fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE) == 0, "失效期旧能力仍接收");
                    // 距2区块仍受生成依赖票据保留，可退出FULL但不触发Unload；必须覆盖此真实状态退降路径。
                    source.addRegionTicket(TICKET, far, 0, far);
                    stage[0] = 2;
                } else if (stage[0] == 2) {
                    if (!level.hasChunk(far.x, far.z) || owner.currentForm() == null) return;
                    h.assertTrue(level.getBlockEntity(control) == owner, "控制器被重载，未覆盖持续存活路径");
                    h.assertTrue(old[0].fill(new FluidStack(Fluids.WATER, 1), IFluidHandler.FluidAction.EXECUTE) == 0, "恢复后旧代次句柄复活");
                    h.assertTrue(owner.ledger().water() == 123 && owner.ledger().steam() == 100 && owner.ledger().cold() == 37
                            && owner.ledger().totalHu() == hu[0], "卸载恢复改变当前质量或潜热账本");
                    h.assertTrue(level.getCapability(Capabilities.FluidHandler.BLOCK, water, Direction.WEST) != null, "新能力未恢复");
                    System.out.println("[boiler-review] R1 chunk2 FULL unavailable/available; controller alive; failed cache recovered; mass/HU conserved");
                    stage[0] = 3; cleanup.run(); h.succeed();
                }
            } catch (Throwable failure) { cleanup.run(); throw failure; }
        });
    }
    @GameTest(template = "boiler_empty", timeoutTicks = 100, batch = "review_overlap")
    public static void controllersBeyond32RejectSharedShellButAllowSeparatedBoxes(GameTestHelper h) {
        var range = BoilerConfig.DIMENSION_RANGE.get(); BoilerConfig.DIMENSION_RANGE.set(List.of(5, 20));
        var level = h.getLevel(); BlockPos min = h.absolutePos(new BlockPos(4, 2, 4));
        var source = level.getChunkSource(); ChunkPos firstChunk = new ChunkPos(min), secondChunk = new ChunkPos(min.offset(38, 0, 0));
        // GameTest模板只有原尺寸，扩展夹具必须明确维持两端可运行，不依赖随机模板起点恰落在票据范围内。
        source.addRegionTicket(TICKET, firstChunk, 2, firstChunk); source.addRegionTicket(TICKET, secondChunk, 2, secondChunk);
        Runnable cleanup = () -> {
            BoilerConfig.DIMENSION_RANGE.set(range);
            source.removeRegionTicket(TICKET, firstChunk, 2, firstChunk); source.removeRegionTicket(TICKET, secondChunk, 2, secondChunk);
        };
        BlockPos a = buildBox(level, min, 20, true), b = buildBox(level, min.offset(19, 0, 0), 20, false);
        int[] stage = {0}; BlockPos[] moved = {null};
        h.onEachTick(() -> {
            try {
                if (h.getTick() >= 90) h.fail("远距重叠夹具未就绪，阶段=" + stage[0]);
                var first = (BoilerControllerBlockEntity) level.getBlockEntity(a);
                if (stage[0] == 0) {
                    var second = (BoilerControllerBlockEntity) level.getBlockEntity(b);
                    if (!first.current() || !second.current()) return;
                    h.assertTrue(b.getX() - a.getX() == 38 && BoilerStructure.inspect(level, a) != null && BoilerStructure.inspect(level, b) != null, "两独立合法炉/38格距离夹具错误");
                    h.assertTrue(first.currentForm() == null && second.currentForm() == null, "控制器超过32格时共享纯外壳面仍成型");
                    moved[0] = buildBox(level, min.offset(20, 0, 0), 20, false); stage[0] = 1;
                } else if (stage[0] == 1) {
                    var second = (BoilerControllerBlockEntity) level.getBlockEntity(moved[0]);
                    if (!second.current()) return;
                    h.assertTrue(first.currentForm() != null && second.currentForm() != null, "闭包围盒不相交的相邻两炉被误拒绝");
                    System.out.println("[boiler-review] R2 38-block controllers shared shell rejected; disjoint adjacent boxes accepted");
                    stage[0] = 2; cleanup.run(); h.succeed();
                }
            } catch (Throwable failure) { cleanup.run(); throw failure; }
        });
    }
}
