package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.automation.ReactorAutomationPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ModFluids;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.content.ReactorAutomationContent;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ReactorCoolantFluidHandler;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshot;
import com.iksxh.create_nuclear_industry.reactor.ReactorSnapshotNbtCodec;
import com.iksxh.create_nuclear_industry.structure.ReactorDimensions;
import com.iksxh.create_nuclear_industry.structure.ReactorDisassemblyPlan;
import com.iksxh.create_nuclear_industry.structure.ReactorInstrumentStructureSummary;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureScanner;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/** 独立小堆改装探针；只使用隔离模板，当前版本NBT读取不冒充用户世界完整重启。 */
@GameTestHolder("cni_reactor_automation_retrofit")
@PrefixGameTestTemplate(false)
public final class ReactorAutomationRetrofitGameTests {
    private static final BlockPos BASE = new BlockPos(3, 1, 3);
    private static final BlockPos OWNER = BASE.offset(2, 2, 0);
    private static final BlockPos PORT = BASE.offset(3, 2, 0);
    private static final BlockPos COLD = BASE.offset(0, 2, 2);
    private static final CoreColumnPosition COLUMN = new CoreColumnPosition(0, 0);

    private ReactorAutomationRetrofitGameTests() { }

    /** 旧绑定在安全拆壳与Post重扫之间真实注液，不插入人工重扫来掩盖窗口。 */
    @GameTest(templateNamespace = "cni_reactor_automation_retrofit", template = "retrofit_empty", timeoutTicks = 100)
    public static void safe_break_refill_retrofit(GameTestHelper helper) {
        build(helper, 6);
        helper.runAfterDelay(5, () -> {
            var owner = owner(helper);
            helper.assertTrue(owner.structureValid(), "六格前置结构未成型");
            var cold = (ReactorPortBlockEntity) helper.getBlockEntity(COLD);
            var handler = ReactorCoolantFluidHandler.forPort(cold);
            helper.assertTrue(handler != null, "真实冷端未提供能力");
            var event = breakEvent(helper, PORT);
            helper.assertTrue(!event.isCanceled(), "完全停机拆壳被错误取消");
            helper.setBlock(PORT, Blocks.AIR);
            int accepted = handler.fill(new FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(), 128), IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(accepted == 128, "Post前原有效绑定的真实注液未发生");
            helper.setBlock(PORT, ReactorAutomationContent.PORT.get());
            var geometric = ReactorStructureScanner.scanInstrumentPort(helper.getLevel(), helper.absolutePos(OWNER));
            var scan = ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), helper.absolutePos(OWNER));
            System.out.println("[retrofit-after-break] geometry=" + geometric.valid() + " returned=" + scan.valid()
                    + " owner=" + owner.structureValid() + " dimensions=" + owner.snapshot().dimensions() + " cold=" + owner.snapshot().coldCoolantMb());
            helper.assertTrue(geometric.valid() && scan.valid() && owner.structureValid(), "安全拆壳补冷后改装接口导致仪表尺寸门拒绝");
            helper.assertTrue(owner.snapshot().dimensions().equals(new ReactorDimensions(6, 6, 6))
                    && owner.snapshot().coldCoolantMb() == accepted && owner.snapshot().hotCoolantMb() == 0,
                    "改装丢失实际尺寸或真实补入冷液");
            helper.assertTrue(owner.snapshot().controlRodColumns().values().stream().allMatch(rod -> rod.actualDepth() == 1 && rod.targetDepth() == 1),
                    "安全重建未保持控制棒全插入");
            assertAvailableSample(helper, owner, 6);
            helper.assertTrue(helper.getBlockEntity(PORT) instanceof ReactorAutomationPortBlockEntity, "改装位置没有新接口实体");
            var fuel = (ReactorPortBlockEntity) helper.getBlockEntity(BASE.offset(2, 5, 3));
            helper.assertTrue(fuel.tryInsertFuel(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())).success(), "改装后原换料口未接受新组件");
            helper.setBlock(PORT.north(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING));
            BlazeBurnerBlock.tryInsert(helper.getBlockState(PORT.north()), helper.getLevel(), helper.absolutePos(PORT.north()), new ItemStack(Items.COAL), false, false, false);
            owner.tickReactor();
            helper.assertTrue(owner.automationState().claimed() && !owner.automationState().runPermission(), "改装后燃烧室未安全任职或自行启动");
            helper.succeed();
        });
    }

    /** 当前保存型纯冷液恢复及所有真实核状态拒绝；每个分支都走正式BE读取和重扫。 */
    @GameTest(templateNamespace = "cni_reactor_automation_retrofit", template = "retrofit_empty", timeoutTicks = 100)
    public static void saved_cold_only_and_nuclear_rejections(GameTestHelper helper) {
        build(helper, 6);
        helper.setBlock(PORT, ReactorAutomationContent.PORT.get());
        helper.runAfterDelay(5, () -> {
            var owner = owner(helper);
            var saved = owner.saveForServerTest(helper.getLevel().registryAccess());
            saved.remove("BreakSafetySurface");
            saved.remove("BreakSafetyFuelColumns");
            saved.put("ReactorSnapshot", ReactorSnapshotNbtCodec.encode(ReactorSnapshot.empty().withCoolantInventories(32768, 0)));
            var restored = new ReactorInstrumentPortBlockEntity(owner.getBlockPos(), owner.getBlockState());
            restored.setLevel(helper.getLevel());
            restored.loadForServerTest(saved, helper.getLevel().registryAccess());
            helper.getLevel().setBlockEntity(restored);
            restored.onLoad();
            var accepted = ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), restored.getBlockPos());
            System.out.println("[retrofit-saved-cold] returned=" + accepted.valid() + " owner=" + restored.structureValid()
                    + " dimensions=" + restored.snapshot().dimensions() + " cold=" + restored.snapshot().coldCoolantMb());
            helper.assertTrue(accepted.valid() && restored.structureValid() && restored.snapshot().coldCoolantMb() == 32768,
                    "当前保存型默认五格纯冷液没有正常恢复或冷液被抹除");
            assertAvailableSample(helper, restored, 6);
            long capacity = restored.coolantCapacityMb();
            var cold = ReactorSnapshot.empty().withCoolantInventories(1, 0);
            var cases = new LinkedHashMap<String, ReactorSnapshot>();
            cases.put("热液", cold.withCoolantInventories(1, 1));
            cases.put("燃料列", cold.withColumns(Map.of(COLUMN, FuelColumnState.empty()), Map.of()));
            cases.put("控制棒列", cold.withColumns(Map.of(), Map.of(COLUMN, ControlRodColumnState.fullyInserted())));
            cases.put("SCRAM请求", cold.withScramState(Map.of(), true));
            cases.put("SCRAM保存目标", cold.withColumns(Map.of(), Map.of(COLUMN, ControlRodColumnState.fullyInserted())).withScramState(Map.of(COLUMN, .5), true));
            cases.put("事故已建立", cold.withMeltdown(0, true));
            cases.put("事故进度", cold.withMeltdown(1, true));
            cases.put("事故已发布", cold.withMeltdown(1, true).withMeltdownEventPublished(true));
            cases.put("超过新容量", cold.withCoolantInventories(capacity + 1, 0));
            for (var entry : cases.entrySet()) assertRejected(helper, restored, saved, entry.getValue(), entry.getKey(), false);
            assertRejected(helper, restored, saved, cold, "待迁移燃料", true);
            helper.succeed();
        });
    }

    /** 无核状态的干净重建仍能改尺寸，原五格清空保持原empty语义。 */
    @GameTest(templateNamespace = "cni_reactor_automation_retrofit", template = "retrofit_empty", timeoutTicks = 100)
    public static void clean_resize_and_five_block_reset(GameTestHelper helper) {
        build(helper, 6);
        helper.runAfterDelay(5, () -> {
            var owner = owner(helper);
            owner.setSnapshot(ReactorSnapshot.empty().withDimensions(new ReactorDimensions(6, 6, 6)));
            build(helper, 7);
            helper.assertTrue(owner(helper) == owner && owner.structureValid()
                    && owner.snapshot().dimensions().equals(new ReactorDimensions(7, 7, 7)), "干净无核状态重建不同合法尺寸被拒绝");
            owner.setSnapshot(ReactorSnapshot.empty().withDimensions(new ReactorDimensions(7, 7, 7)));
            build(helper, 5);
            helper.assertTrue(owner(helper) == owner && owner.structureValid(), "原五格重建前置失败");
            var event = breakEvent(helper, PORT);
            helper.assertTrue(!event.isCanceled() && owner.snapshot().equals(ReactorSnapshot.empty()), "原五格完全停机清空语义变化");
            helper.setBlock(PORT, Blocks.AIR);
            helper.setBlock(PORT, ReactorAutomationContent.PORT.get());
            var scan = ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), helper.absolutePos(OWNER));
            helper.assertTrue(scan.valid() && owner.structureValid(), "原五格安全改装失效");
            assertAvailableSample(helper, owner, 5);
            helper.succeed();
        });
    }

    /** 小模板最多七格，旧外壁覆盖为空气，仪表实体保持在同一坐标。 */
    private static void build(GameTestHelper helper, int size) {
        for (int x = 0; x < 7; x++) for (int y = 0; y < 7; y++) for (int z = 0; z < 7; z++) {
            boolean inside = x < size && y < size && z < size;
            boolean boundary = inside && (x == 0 || y == 0 || z == 0 || x == size - 1 || y == size - 1 || z == size - 1);
            var pos = BASE.offset(x, y, z);
            if (!pos.equals(OWNER)) helper.setBlock(pos, boundary ? P1Blocks.REACTOR_CASING.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        for (int y = 1; y < size - 1; y++) {
            helper.setBlock(BASE.offset(2, y, 3), P1Blocks.REACTOR_FUEL_ROD.get());
        }
        helper.setBlock(BASE.offset(2, size - 1, 2), P1Blocks.CONTROL_ROD_DRIVE.get());
        helper.setBlock(BASE.offset(2, size - 1, 3), P1Blocks.REACTOR_REFUELING_PORT.get());
        helper.setBlock(COLD, P1Blocks.REACTOR_COLD_PORT.get());
        helper.setBlock(BASE.offset(size - 1, 2, 2), P1Blocks.REACTOR_HOT_PORT.get());
        helper.setBlock(OWNER, P1Blocks.REACTOR_INSTRUMENT_PORT.get());
        var scan = ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), helper.absolutePos(OWNER));
        System.out.println("[retrofit-build] size=" + size + " raw=" + scan.valid() + " code=" + scan.diagnosticCode()
                + " reason=" + scan.failureReason() + " snapshot=" + owner(helper).snapshot().dimensions());
    }

    private static ReactorInstrumentPortBlockEntity owner(GameTestHelper helper) {
        return (ReactorInstrumentPortBlockEntity) helper.getBlockEntity(OWNER);
    }

    private static BlockEvent.BreakEvent breakEvent(GameTestHelper helper, BlockPos relative) {
        var absolute = helper.absolutePos(relative);
        var event = new BlockEvent.BreakEvent(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), helper.makeMockPlayer(GameType.SURVIVAL));
        NeoForge.EVENT_BUS.post(event);
        return event;
    }

    /** 取生产更新标签，核静态摘要与正式tick，而不是检查源文件字符串。 */
    private static void assertAvailableSample(GameTestHelper helper, ReactorInstrumentPortBlockEntity owner, int size) {
        var summary = ReactorInstrumentStructureSummary.readSyncTag(owner.getUpdateTag(helper.getLevel().registryAccess()).getCompound("InstrumentStructureSummary"));
        helper.assertTrue(summary.valid() && summary.width() == size && summary.height() == size && summary.depth() == size, "实际客户端更新摘要仍不可用");
        owner.tickReactor();
        helper.assertTrue(owner.telemetry().available() && owner.runtimeDescriptor().map(sample -> sample.available()).orElse(false), "正式tick未恢复同龄遥测");
    }

    private static void assertRejected(GameTestHelper helper, ReactorInstrumentPortBlockEntity owner, CompoundTag cleanSaved,
                                       ReactorSnapshot snapshot, String reason, boolean pendingFuel) {
        var tag = cleanSaved.copy();
        var snapshotTag = ReactorSnapshotNbtCodec.encode(snapshot);
        if (pendingFuel) {
            var list = new ListTag();
            var entry = new CompoundTag();
            entry.putInt("X", 0); entry.putInt("Z", 0); entry.putBoolean("Present", true); entry.putInt("MaxDamage", 10);
            list.add(entry); snapshotTag.put("LegacyFuelMigration", list);
        }
        tag.put("ReactorSnapshot", snapshotTag);
        owner.loadForServerTest(tag, helper.getLevel().registryAccess());
        var before = owner.snapshot();
        var raw = ReactorStructureScanner.scanInstrumentPort(helper.getLevel(), owner.getBlockPos());
        var actual = ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), owner.getBlockPos());
        helper.assertTrue(raw.valid() && !actual.valid() && !owner.structureValid(), reason + "没有保持尺寸拒绝或扳手仍报告假成功");
        helper.assertTrue(owner.snapshot().equals(before), reason + "拒绝时改变或丢失权威状态");
        helper.assertTrue(!ReactorInstrumentStructureSummary.readSyncTag(owner.getUpdateTag(helper.getLevel().registryAccess()).getCompound("InstrumentStructureSummary")).valid(), reason + "拒绝后静态摘要仍可用");
        System.out.println("[retrofit-rejection] " + reason + " cold=" + before.coldCoolantMb() + " returned=" + actual.valid());
    }
}
