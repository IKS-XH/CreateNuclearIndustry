package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.automation.ReactorAutomationPortBlockEntity;
import com.iksxh.create_nuclear_industry.automation.ReactorAutomationBlazeBridge;
import com.iksxh.create_nuclear_industry.automation.ReactorAutomationState;
import com.iksxh.create_nuclear_industry.automation.ReactorAutomationResume;
import com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorCommands;
import com.iksxh.create_nuclear_industry.automation.gui.ReactorAdministratorMenu;
import com.iksxh.create_nuclear_industry.blockentity.ControlRodDriveBlockEntity;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderPayload;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderService;
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
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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

    /** 事故正式托管后即使几何零接口，原事故代际/物料账和管理员安全锁也不能被退出分支释放。 */
    @GameTest(templateNamespace = "cni_reactor_automation_retrofit", template = "retrofit_empty", timeoutTicks = 100)
    public static void administrator_release_keeps_accident_takeover(GameTestHelper helper) {
        build(helper, 6);
        helper.setBlock(PORT, ReactorAutomationContent.PORT.get());
        installBurner(helper);
        helper.runAfterDelay(5, () -> {
            var owner = owner(helper);
            var fuel = (ReactorPortBlockEntity) helper.getBlockEntity(BASE.offset(2, 5, 3));
            helper.assertTrue(fuel.tryInsertFuel(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())).success(), "事故前未插入真实燃料");
            owner.tickReactor();
            helper.assertTrue(owner.automationState().claimed(), "事故前没有管理员接管");
            var next = owner.snapshot().withMeltdown(com.iksxh.create_nuclear_industry.config.P1ServerConfig.VALUES.meltdownCountdownTicks.get(), true)
                    .withMeltdownEventPublished(true);
            owner.setSnapshot(next);
            var event = new com.iksxh.create_nuclear_industry.reactor.ReactorMeltdownEvent(
                    com.iksxh.create_nuclear_industry.reactor.ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE,
                    helper.getLevel(), owner.structureOrigin(), owner.getBlockPos(), next);
            NeoForge.EVENT_BUS.post(event);
            var ledger = com.iksxh.create_nuclear_industry.accident.ReactorAccidentSavedData.get(helper.getLevel()).byGeneration(owner.accidentGeneration());
            helper.assertTrue(ledger != null && owner.accidentId().equals(ledger.id) && ledger.columns.size() == 1
                    && ledger.columns.getFirst().captured() && fuel.fuelAssembly().isEmpty(), "正式事故没有原子转移唯一真实组件");
            long epoch = owner.automationState().authorityEpoch();
            helper.setBlock(PORT, P1Blocks.REACTOR_CASING.get());
            var geometry = ReactorStructureScanner.scanInstrumentPort(helper.getLevel(), owner.getBlockPos());
            ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), owner.getBlockPos());
            com.iksxh.create_nuclear_industry.automation.ReactorAutomationService.releaseIfPortAbsent(owner);
            helper.assertTrue(geometry.valid() && geometry.contract().ports().getOrDefault(
                    com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition.PortType.AUTOMATION, java.util.List.of()).isEmpty(), "事故退出反例没有物理零接口几何");
            helper.assertTrue(owner.accidentId().equals(ledger.id) && !owner.structureValid() && !owner.tickReactor()
                    && owner.automationState().claimed() && owner.automationState().rodsLocked()
                    && owner.automationState().authorityEpoch() == epoch && !owner.automationState().runPermission(), "零接口扫描释放事故接管或重启旧核结算");
            helper.assertTrue(com.iksxh.create_nuclear_industry.accident.ReactorAccidentService.begin(event) == ledger
                    && ledger.columns.getFirst().captured(), "退出分支改变事故账或重建物料托管");
            System.out.println("[admin-release-accident] zeroPortGeometry=true accidentLockRetained=true custodyRetained=true epochStable=true");
            helper.succeed();
        });
    }

    /** 任职后真实安全拆接口补壳，同一仪表必须恢复正式逐棒入口和客户端锁投影。 */
    @GameTest(templateNamespace = "cni_reactor_automation_retrofit", template = "retrofit_empty", timeoutTicks = 100)
    public static void administrator_release_after_safe_port_removal(GameTestHelper helper) {
        build(helper, 6);
        var player = sliderPlayer(helper);
        var drivePos = helper.absolutePos(BASE.offset(2, 5, 2));
        helper.assertTrue(ControlRodSliderService.handle(player, ControlRodSliderPayload.start(drivePos, -1, -1, 100, 7100)).accepted(), "原局部控制没有建立旧拖动前置");
        helper.setBlock(PORT, ReactorAutomationContent.PORT.get());
        installBurner(helper);
        helper.runAfterDelay(5, () -> {
            var owner = owner(helper);
            var fuel = (ReactorPortBlockEntity) helper.getBlockEntity(BASE.offset(2, 5, 3));
            helper.assertTrue(fuel.tryInsertFuel(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())).success(), "任职拆除前没有真实全新燃料");
            owner.setCoolantInventories(owner.coolantCapacityMb() / 2, 0);
            owner.tickReactor();
            helper.assertTrue(owner.automationState().claimed() && owner.automationState().rodsLocked()
                    && !owner.automationState().runPermission(), "未取得安全停机管理员接管前置");
            helper.assertTrue(owner.snapshot().fuelColumns().values().stream().anyMatch(FuelColumnState::hasUsableFuel)
                    && owner.snapshot().fuelColumns().values().stream().allMatch(f -> f.cachedHeatHu() == 0), "全新燃料安全无热前置失败");
            var burnerPos = helper.absolutePos(PORT.north());
            player.setPos(burnerPos.getX() + .5, burnerPos.getY() + .5, burnerPos.getZ() + .5);
            helper.assertTrue(ReactorAdministratorMenu.open(player, burnerPos), "真实管理员菜单没有打开");
            var menu = (ReactorAdministratorMenu) player.containerMenu;
            var binding = owner.automationBinding();
            var stale = new ReactorAdministratorCommands.Request(menu.containerId, menu.sessionId(), binding.burnerPos(), binding.portPos(),
                    binding.ownerGeneration(), binding.authorityEpoch(), owner.automationPort().settingsRevision(), 1,
                    ReactorAdministratorCommands.Action.START, null);
            owner.recordAutomationDisplayHeat(99, 5);
            var event = breakEvent(helper, PORT);
            helper.assertTrue(!event.isCanceled(), "安全停机接口拆除被拒绝");
            helper.setBlock(PORT, Blocks.AIR);
            var invalid = ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), owner.getBlockPos());
            helper.assertTrue(!invalid.valid() && owner.automationState().claimed(), "未补壳的无效结构提前释放接管");
            helper.setBlock(PORT, P1Blocks.REACTOR_CASING.get());
            var formed = ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), owner.getBlockPos());
            helper.assertTrue(formed.valid() && owner(helper) == owner, "原仪表没有正式有效重成型");
            var drive = (ControlRodDriveBlockEntity) helper.getBlockEntity(BASE.offset(2, 5, 2));
            player.setPos(drivePos.getX() + .5, drivePos.getY() + .5, drivePos.getZ() + .5);
            helper.assertTrue(!ControlRodSliderService.handle(player, ControlRodSliderPayload.commit(drivePos, -1, -1, 0, 7100)).accepted(), "接管前旧逐棒会话在退出后复活");
            var start = ControlRodSliderService.handle(player, ControlRodSliderPayload.start(drivePos, -1, -1, 100, 7101));
            System.out.println("[admin-release-real] formed=" + formed.valid() + " sameOwner=" + (owner(helper) == owner)
                    + " claimed=" + owner.automationState().claimed() + " phase=" + owner.automationState().phase()
                    + " slider=" + start.status() + " reason=" + start.reason() + " displayLocked=" + drive.automationLocked());
            helper.assertTrue(start.accepted(), "同一仪表补壳重成型后正式Slider仍只读：" + start.reason());
            helper.assertTrue(!owner.automationState().claimed() && !drive.automationLocked()
                    && !drive.getUpdateTag(helper.getLevel().registryAccess()).getBoolean("AutomationLocked"), "退出未同步客户端逐棒锁");
            helper.assertTrue(owner.automationBinding() == null && owner.automationBurner() == null && owner.automationPort() == null
                    && owner.automationObservation() == null && owner.automationAverageHeat() == 0, "真实退出仍残留旧绑定/样本/显示窗口");
            helper.assertTrue(ControlRodSliderService.handle(player, ControlRodSliderPayload.commit(drivePos, -1, -1, 75, 7101)).accepted(), "退出后新逐棒会话不能提交");
            helper.assertTrue(owner.snapshot().controlRodColumns().values().stream().allMatch(r -> r.targetDepth() == .75), "逐棒提交没有写入唯一核快照");
            long epoch = owner.automationState().authorityEpoch();
            ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), owner.getBlockPos());
            owner.tickReactor();
            helper.assertTrue(owner.automationState().authorityEpoch() == epoch && !owner.automationState().claimed(), "重复重扫或tick反复推进退出epoch");
            helper.assertTrue(ReactorAutomationBlazeBridge.runPermission(helper.getLevel(), helper.absolutePos(PORT.north())) == 0, "退出后残留燃烧室运行许可");
            helper.assertTrue(ControlRodSliderService.handle(player, ControlRodSliderPayload.start(drivePos, -1, -1, 100, 7102)).accepted()
                    && ControlRodSliderService.handle(player, ControlRodSliderPayload.commit(drivePos, -1, -1, 100, 7102)).accepted(), "重装前逐棒不能安全全插");
            helper.setBlock(PORT, ReactorAutomationContent.PORT.get());
            ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), owner.getBlockPos());
            helper.assertTrue(fuel.tryInsertFuel(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())).success(), "重装前补新燃料失败");
            owner.setCoolantInventories(owner.coolantCapacityMb() / 2, 0);
            owner.tickReactor();
            helper.assertTrue(owner.automationState().claimed() && owner.automationState().phase() == ReactorAutomationState.Phase.STOPPED
                    && !owner.automationState().runPermission() && owner.automationState().authorityEpoch() > epoch, "重装没有安全建立新停机会话");
            helper.assertTrue(!menu.stillValid(player) && !ReactorAdministratorCommands.apply(player, stale).accepted(), "重装接受旧GUI会话");
            ControlRodSliderService.clearSession(player);
            player.closeContainer();
            helper.succeed();
        });
    }

    /** 当前格式的无接口接管保存经首扫退出，核账本、保存SCRAM和卡死保护保持。 */
    @GameTest(templateNamespace = "cni_reactor_automation_retrofit", template = "retrofit_empty", timeoutTicks = 100)
    public static void administrator_release_saved_claim_preserves_protections(GameTestHelper helper) {
        build(helper, 6);
        helper.setBlock(PORT, ReactorAutomationContent.PORT.get());
        installBurner(helper);
        helper.runAfterDelay(5, () -> {
            var original = owner(helper);
            var fuelPort = (ReactorPortBlockEntity) helper.getBlockEntity(BASE.offset(2, 5, 3));
            helper.assertTrue(fuelPort.tryInsertFuel(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())).success(), "保存前没有真实燃料");
            original.tickReactor();
            helper.assertTrue(original.automationState().claimed(), "保存前没有接管");
            // 此处直接换块只为构造当前版本异常保存；不宣称带核热可以通过玩家拆除门。
            helper.setBlock(PORT, P1Blocks.REACTOR_CASING.get());
            var rodPos = original.snapshot().controlRodColumns().keySet().iterator().next();
            var fuelPos = original.snapshot().fuelColumns().keySet().iterator().next();
            var fuel = original.snapshot().fuelColumns().get(fuelPos);
            original.setSnapshot(original.snapshot().withColumns(Map.of(fuelPos, new FuelColumnState(fuel.fuelAssembly(), .9, 23)),
                    Map.of(rodPos, new ControlRodColumnState(.2, 1, .4, true, 0))).withCoolantInventories(1234, 56).withScramState(Map.of(rodPos, 1.), true));
            original.recordAutomationDisplayHeat(99, 5);
            var saved = original.saveForServerTest(helper.getLevel().registryAccess());
            var restored = new ReactorInstrumentPortBlockEntity(original.getBlockPos(), original.getBlockState());
            restored.setLevel(helper.getLevel());
            restored.loadForServerTest(saved, helper.getLevel().registryAccess());
            helper.getLevel().setBlockEntity(restored);
            restored.onLoad();
            helper.assertTrue(restored.automationState().claimed() && !restored.tickReactor(), "未验证加载提前解锁或结算");
            var scan = ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), restored.getBlockPos());
            var snapshot = restored.snapshot();
            helper.assertTrue(scan.valid() && !restored.automationState().claimed(), "当前保存无接口接管经正式核验仍锁死");
            helper.assertTrue(snapshot.coldCoolantMb() == 1234 && snapshot.hotCoolantMb() == 56
                    && snapshot.fuelColumns().get(fuelPos).cachedHeatHu() == 23
                    && snapshot.fuelColumns().get(fuelPos).hasUsableFuel(), "退出改写燃料、积热或工质账本");
            helper.assertTrue(snapshot.scramRequested() && snapshot.scramSavedTargetDepths().equals(Map.of(rodPos, 1.))
                    && snapshot.controlRodColumns().get(rodPos).jammed() && snapshot.controlRodColumns().get(rodPos).actualDepth() == .4
                    && snapshot.controlRodColumns().get(rodPos).targetDepth() == 1, "退出清SCRAM/卡死或恢复危险旧目标");
            helper.assertTrue(restored.automationBinding() == null && restored.automationPort() == null && restored.automationBurner() == null
                    && restored.automationObservation() == null && restored.automationResumeSession() == null
                    && restored.automationNormalStopCheckpoint() == null && restored.automationAverageHeat() == 0, "退出残留旧实例、观测或续接意图");
            var player = sliderPlayer(helper);
            var drivePos = helper.absolutePos(BASE.offset(2, 5, 2));
            helper.assertTrue(!ControlRodSliderService.handle(player, ControlRodSliderPayload.start(drivePos, -1, -1, 0, 7201)).accepted(), "退出绕过保存SCRAM");
            restored.updateRedstoneScram(true); restored.updateRedstoneScram(false);
            helper.assertTrue(!ControlRodSliderService.handle(player, ControlRodSliderPayload.start(drivePos, -1, -1, 0, 7202)).accepted(), "退出绕过卡死");
            long epoch = restored.automationState().authorityEpoch();
            ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), restored.getBlockPos());
            helper.assertTrue(restored.automationState().authorityEpoch() == epoch, "已退出保存状态重复扫描不幂等");
            // 本版本正常运行信封遇到物理零接口同样取消续接；不能先执行保存的浅目标。
            var pending = saved.copy();
            pending.put("ReactorSnapshot", ReactorSnapshotNbtCodec.encode(snapshot.withColumns(snapshot.fuelColumns(),
                    Map.of(rodPos, new ControlRodColumnState(1, .4, .4, false, 0))).withScramState(Map.of(), false)));
            var running = new ReactorAutomationState(true, ReactorAutomationState.Phase.MANUAL, "manual", 40, 8, 3, 0, 0, 0,
                    ReactorAutomationState.Mode.MANUAL, true);
            pending.put(ReactorAutomationResume.KEY, ReactorAutomationResume.encode(running,
                    new ReactorAutomationPortBlockEntity.Settings(ReactorAutomationState.Mode.MANUAL, 40, 0),
                    helper.absolutePos(PORT), helper.absolutePos(PORT.north()), true));
            restored.loadForServerTest(pending, helper.getLevel().registryAccess());
            helper.assertTrue(restored.automationResumeSession() != null, "本版本运行信封未建立待续接前置");
            ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), restored.getBlockPos());
            var releasedRod = restored.snapshot().controlRodColumns().get(rodPos);
            helper.assertTrue(!restored.automationState().claimed() && restored.automationResumeSession() == null
                    && !restored.automationState().runPermission() && releasedRod.actualDepth() == .4 && releasedRod.targetDepth() == 1,
                    "无接口运行信封残留续接意图或丢失安全全插目标/真实棒位");
            // 先让真实输入稳定三tick，再读取不含保存SCRAM的运行信封，排除只测手工高沿的假阳性。
            helper.setBlock(OWNER.north(), Blocks.REDSTONE_BLOCK);
            long highSince = helper.getLevel().getGameTime();
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(helper.getLevel().getGameTime() - highSince >= 3
                        && helper.getLevel().hasNeighborSignal(restored.getBlockPos()), "首扫前没有稳定真实高输入");
                restored.loadForServerTest(pending, helper.getLevel().registryAccess());
                helper.assertTrue(restored.automationResumeSession() != null && !restored.snapshot().scramRequested(),
                        "高输入用例未建立无保存SCRAM的运行信封");
                ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), restored.getBlockPos());
                var highSnapshot = restored.snapshot();
                var highStart = ControlRodSliderService.handle(player, ControlRodSliderPayload.start(drivePos, -1, -1, 0, 7203));
                var highCommit = ControlRodSliderService.handle(player, ControlRodSliderPayload.commit(drivePos, -1, -1, 0, 7203));
                System.out.println("[admin-release-stable-high] high=" + helper.getLevel().hasNeighborSignal(restored.getBlockPos())
                        + " claimed=" + restored.automationState().claimed() + " pending=" + (restored.automationResumeSession() != null)
                        + " scram=" + highSnapshot.scramRequested() + " start=" + highStart.status() + " commit=" + highCommit.status());
                helper.assertTrue(!restored.automationState().claimed() && restored.automationResumeSession() == null
                        && highSnapshot.scramRequested() && !highStart.accepted() && !highCommit.accepted(),
                        "稳定高输入的无接口运行信封首扫退出后仍接受逐棒提棒");
                helper.assertTrue(highSnapshot.coldCoolantMb() == 1234 && highSnapshot.hotCoolantMb() == 56
                        && highSnapshot.fuelColumns().get(fuelPos).cachedHeatHu() == 23
                        && highSnapshot.fuelColumns().get(fuelPos).hasUsableFuel()
                        && highSnapshot.controlRodColumns().get(rodPos).actualDepth() == 1
                        && highSnapshot.controlRodColumns().get(rodPos).targetDepth() == 1,
                        "当前高输入未沿原SCRAM紧急插入或改写核账本");
                helper.setBlock(OWNER.north(), Blocks.AIR);
                helper.assertTrue(!helper.getLevel().hasNeighborSignal(restored.getBlockPos()) && !restored.snapshot().scramRequested()
                        && restored.snapshot().controlRodColumns().get(rodPos).targetDepth() == 1,
                        "真实低沿没有沿原SCRAM规则释放或反弹到危险旧目标");
                helper.assertTrue(ControlRodSliderService.handle(player, ControlRodSliderPayload.start(drivePos, -1, -1, 100, 7204)).accepted()
                        && ControlRodSliderService.handle(player, ControlRodSliderPayload.commit(drivePos, -1, -1, 75, 7204)).accepted(),
                        "真实低沿解除后原局部逐棒不能恢复");
                ControlRodSliderService.clearSession(player);
                System.out.println("[admin-release-saved] released=true nuclearPreserved=true scramAndJamProtected=true epochStable=true stableHighProtected=true realLowRestored=true");
                helper.succeed();
            });
        });
    }

    /** 卸载回调、缺区块缓存及只拆燃烧室都保持接管，不能借失联释放逐棒。 */
    @GameTest(templateNamespace = "cni_reactor_automation_retrofit", template = "retrofit_empty", timeoutTicks = 100)
    public static void administrator_release_keeps_unavailable_and_burner_fault(GameTestHelper helper) {
        build(helper, 6);
        helper.setBlock(PORT, ReactorAutomationContent.PORT.get());
        installBurner(helper);
        helper.runAfterDelay(5, () -> {
            var owner = owner(helper);
            owner.tickReactor();
            var port = (ReactorAutomationPortBlockEntity) helper.getBlockEntity(PORT);
            helper.assertTrue(owner.automationState().claimed(), "失联前没有接管");
            port.onChunkUnloaded();
            helper.assertTrue(owner.automationState().claimed() && owner.automationState().rodsLocked() && !owner.automationState().runPermission(), "接口卸载回调错误结束接管");
            owner.updateStructureCache(ReactorStructureScanner.WorldScanResult.invalid(
                    com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition.DiagnosticCode.CHUNKS_UNAVAILABLE, "admin release unavailable probe"));
            owner.tickReactor();
            helper.assertTrue(owner.automationState().claimed() && owner.automationState().rodsLocked(), "缺区块未验证结构错误结束接管");
            ReactorStructureLifecycle.rescanInstrumentPortNow(helper.getLevel(), owner.getBlockPos());
            owner.tickReactor();
            helper.setBlock(PORT.north(), Blocks.AIR);
            owner.tickReactor();
            helper.assertTrue(owner.structureValid() && owner.automationState().claimed() && owner.automationState().rodsLocked(), "仅拆燃烧室错误开放逐棒");
            installBurner(helper);
            owner.tickReactor();
            helper.assertTrue(owner.automationState().claimed() && owner.automationState().phase() == ReactorAutomationState.Phase.FAULT
                    && !owner.automationState().runPermission(), "燃烧室重装自动启动或清故障");
            System.out.println("[admin-release-protection] unloadCallback=true chunksUnavailable=true burnerOnly=true faultRetained=true");
            helper.succeed();
        });
    }

    private static void installBurner(GameTestHelper helper) {
        helper.setBlock(PORT.north(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING));
        BlazeBurnerBlock.tryInsert(helper.getBlockState(PORT.north()), helper.getLevel(), helper.absolutePos(PORT.north()), new ItemStack(Items.COAL), false, false, false);
    }

    private static ServerPlayer sliderPlayer(GameTestHelper helper) {
        // 沿现有管理员域保留原生openMenu；FakePlayer本身关闭菜单，只借其隔离连接。
        var profile = new GameProfile(UUID.randomUUID(), "AdminReleaseProbe");
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, ClientInformation.createDefault());
        player.connection = FakePlayerFactory.get(helper.getLevel(), profile).connection;
        player.setGameMode(GameType.SURVIVAL);
        var pos = helper.absolutePos(BASE.offset(2, 5, 2));
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        return player;
    }

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
        // 事故树在原生post返回边界提交维护事务，单独post只会进行只读预检。
        return net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(helper.getLevel(), GameType.SURVIVAL,
                sliderPlayer(helper), absolute, helper.getLevel().getBlockState(absolute));
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
