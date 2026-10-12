package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.blockentity.ReactorPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.P1Blocks;
import com.iksxh.create_nuclear_industry.content.ModItems;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderService;
import com.iksxh.create_nuclear_industry.reactor.CoreColumnPosition;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** 独立命名空间真实控制器探针；不读写候选客户端存档。 */
@GameTestHolder("create_nuclear_industry_variable_reactor")
@PrefixGameTestTemplate(false)
public final class ReactorVariableSizeGameTests {
    private ReactorVariableSizeGameTests() { }

    /** 独立整改域只检验缺FULL期间拆除门，不重跑原两项已通过业务。 */
    @GameTest(templateNamespace = "create_nuclear_industry_variable_reactor_waiting", template = "variable_reactor_empty", timeoutTicks = 500)
    public static void unavailableFrameRejectsOnlyItsPreviouslyVerifiedComponents(GameTestHelper helper) {
        var anchor = helper.absolutePos(BlockPos.ZERO);
        var origin = new BlockPos(24 - (anchor.getX() & 15), 0, 24 - (anchor.getZ() & 15));
        build(helper, origin, 16, 16, 16);
        helper.setBlock(origin.offset(2, 1, 0), P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState());
        helper.setBlock(origin.offset(14, 15, 14), P1Blocks.CONTROL_ROD_DRIVE.get().defaultBlockState());
        ReactorStructureLifecycle.rescanAroundNow(helper.getLevel(), helper.absolutePos(origin.offset(2, 1, 0)));
        helper.runAfterDelay(6, () -> {
            var level = helper.getLevel();
            var initialOwner = owner(helper, origin.offset(1, 1, 0));
            require(helper, initialOwner.structureValid(), "waiting-break fixture did not form");
            var fuel = (ReactorPortBlockEntity) helper.getBlockEntity(origin.offset(1, 15, 1));
            fuel.setFuelAssembly(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get()));
            initialOwner.setSnapshot(initialOwner.snapshot().withCoolantInventories(123, 456));
            // 当前NBT的新实例在首次Post之前尚无扫描结果，即使全部FULL也要保留原组件保护。
            var owner = new ReactorInstrumentPortBlockEntity(initialOwner.getBlockPos(), initialOwner.getBlockState());
            owner.setLevel(level);
            owner.loadForServerTest(initialOwner.saveForServerTest(level.registryAccess()), level.registryAccess());
            level.setBlockEntity(owner);
            owner.onLoad();
            require(helper, level.getBlockEntity(owner.getBlockPos()) == owner
                    && owner.structureScanCount() == 0 && !owner.structureScan().valid()
                    && level.hasChunkAt(helper.absolutePos(origin.offset(15, 1, 15))), "fresh FULL restore precondition failed");
            require(helper, owner.protectsUnavailableComponent(owner.getBlockPos(), owner.getBlockState()), "fresh FULL restore discarded pre-Post protection");
            assertWaitingBreak(helper, owner, fuel, origin.offset(1, 1, 0), true, "fresh all FULL before first Post");
            owner.updateStructureCache(com.iksxh.create_nuclear_industry.structure.ReactorStructureScanner.scanInstrumentPort(level, owner.getBlockPos()));
            require(helper, owner.structureValid() && !owner.protectsUnavailableComponent(owner.getBlockPos(), owner.getBlockState()), "valid scan did not revoke fresh restore protection");
            var ownerChunk = new net.minecraft.world.level.ChunkPos(owner.getBlockPos());
            var ticket = net.minecraft.server.level.TicketType.<net.minecraft.world.level.ChunkPos>create("reactor_waiting_break_owner", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
            level.getChunkSource().addRegionTicket(ticket, ownerChunk, 0, ownerChunk);
            for (long packed : level.getForcedChunks().toLongArray()) {
                var chunk = new net.minecraft.world.level.ChunkPos(packed);
                level.setChunkForced(chunk.x, chunk.z, false);
            }
            // 在同一回调内更新FULL可用性，Post尚未将原有效缓存改为WAITING。
            // 公开读操作的UNKNOWN ticket寿命为1；同回调内清除有限残留，仍未运行Post。
            for (int pass = 0; pass < 3; pass++) level.getChunkSource().tick(() -> true, false);
            require(helper, !level.hasChunkAt(helper.absolutePos(origin.offset(15, 1, 15))), "remote FULL fixture precondition failed");
            require(helper, owner.structureScan().valid(), "pre-Post edge no longer had its verified cache");
            assertWaitingBreak(helper, owner, fuel, origin.offset(2, 1, 0), true, "pre-Post");
            helper.runAfterDelay(2, () -> {
                require(helper, !level.hasChunkAt(helper.absolutePos(origin.offset(14, 15, 14))), "telemetry cleanup forced the remote drive chunk");
                ReactorStructureLifecycle.notifyFluidNetworkAround(level, helper.absolutePos(origin.offset(15, 1, 1)));
                require(helper, !level.hasChunkAt(helper.absolutePos(origin.offset(15, 1, 1))), "fluid notification forced the unloaded endpoint chunk");
                ReactorStructureLifecycle.notifyFluidNetworkAround(level, helper.absolutePos(origin.offset(7, 1, 0)));
                require(helper, !level.hasChunkAt(helper.absolutePos(origin.offset(8, 1, 0))), "loaded endpoint notification forced its unloaded neighbour chunk");
                require(helper, !owner.structureScan().valid(), "waiting state was not entered");
                for (var pos : new BlockPos[]{origin.offset(1, 1, 0), origin.offset(2, 1, 0), origin.offset(1, 15, 1), origin.offset(1, 1, 1)})
                    assertWaitingBreak(helper, owner, fuel, pos, true, "WAITING");
                var unrelated = origin.offset(2, 1, 2);
                helper.setBlock(unrelated, P1Blocks.REACTOR_CASING.get().defaultBlockState());
                assertWaitingBreak(helper, owner, fuel, unrelated, false, "previously empty body");
                helper.setBlock(unrelated, Blocks.AIR.defaultBlockState());
                var saved = owner.snapshot().withoutFuelAssemblies();
                level.getChunkSource().removeRegionTicket(ticket, ownerChunk, 0, ownerChunk);
                helper.runAfterDelay(340, () -> {
                    level.getChunkSource().tick(() -> true, false);
                    require(helper, owner.isRemoved(), "waiting-break owner did not physically unload");
                    level.getChunkSource().addRegionTicket(ticket, ownerChunk, 0, ownerChunk);
                    level.getChunkSource().getChunk(ownerChunk.x, ownerChunk.z, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
                    helper.runAfterDelay(8, () -> {
                        var restored = owner(helper, origin.offset(1, 1, 0));
                        var restoredFuel = (ReactorPortBlockEntity) helper.getBlockEntity(origin.offset(1, 15, 1));
                        require(helper, restored != owner && !level.hasChunkAt(helper.absolutePos(origin.offset(15, 1, 15))), "near-only reload precondition failed");
                        require(helper, !restored.structureValid() && restored.snapshot().withoutFuelAssemblies().equals(saved), "near-only reload lost saved state");
                        System.out.println("variable-reactor near-only owner indexed="+ReactorStructureLifecycle.loadedOwnersNear(level,restored.getBlockPos()).size()+" guard="+restored.protectsUnavailableComponent(restored.getBlockPos(),restored.getBlockState()));
                        assertWaitingBreak(helper, restored, restoredFuel, origin.offset(1, 1, 0), true, "near-only current NBT");
                        assertWaitingBreak(helper, restored, restoredFuel, origin.offset(1, 1, 1), true, "near-only saved fuel body");
                        var absolute = helper.absolutePos(origin);
                        var maximum = absolute.offset(15, 15, 15);
                        for (int x = absolute.getX() >> 4; x <= maximum.getX() >> 4; x++) for (int z = absolute.getZ() >> 4; z <= maximum.getZ() >> 4; z++) level.setChunkForced(x, z, true);
                        helper.runAfterDelay(8, () -> {
                            require(helper, restored.structureValid(), "restoration did not revoke waiting protection");
                            var plan = restored.prepareDisassemblyPlan(restored.getBlockPos(), restored.getBlockState());
                            require(helper, plan != null && plan.action() == com.iksxh.create_nuclear_industry.structure.ReactorDisassemblyPlan.Action.DANGEROUS, "restored loaded reactor lost dangerous break assessment");
                            restoredFuel.setFuelAssembly(ItemStack.EMPTY);
                            restored.setSnapshot(restored.snapshot().withFuelColumn(new CoreColumnPosition(0, 0), FuelColumnState.empty()).withCoolantInventories(0, 0));
                            var resetPos = helper.absolutePos(origin.offset(2, 1, 0));
                            var reset = restored.prepareDisassemblyPlan(resetPos, level.getBlockState(resetPos));
                            require(helper, reset != null && reset.action() == com.iksxh.create_nuclear_industry.structure.ReactorDisassemblyPlan.Action.FULL_SHUTDOWN_RESET, "safe reset precondition failed");
                            require(helper, restored.commitDisassemblyPlan(reset), "safe reset commit failed");
                            restored.rollbackDisassemblyPlan(reset);
                            require(helper, restored.saveForServerTest(level.registryAccess()).contains("BreakSafetySurface"), "rollback discarded saved break protection");
                            System.out.println("variable-reactor reset rollback retained protection=true");
                            level.getChunkSource().removeRegionTicket(ticket, ownerChunk, 0, ownerChunk);
                            helper.succeed();
                        });
                    });
                });
            });
        });
    }

    /** BreakEvent只做规则调用，核对取消结果且不改变热量、库存或真实组件。 */
    private static void assertWaitingBreak(GameTestHelper helper, ReactorInstrumentPortBlockEntity owner, ReactorPortBlockEntity fuel,
                                           BlockPos relative, boolean cancelled, String phase) {
        var before = owner.snapshot();
        var item = fuel.fuelAssembly();
        var pos = helper.absolutePos(relative);
        var event = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(helper.getLevel(), pos,
                helper.getLevel().getBlockState(pos), helper.makeMockPlayer(GameType.SURVIVAL));
        ReactorStructureLifecycle.onBlockBreak(event);
        require(helper, event.isCanceled() == cancelled, phase + " break protection mismatch at " + relative);
        require(helper, owner.snapshot().equals(before) && ItemStack.matches(item, fuel.fuelAssembly()), phase + " break changed authoritative state");
        System.out.println("variable-reactor waiting-break phase=" + phase + " cancelled=" + event.isCanceled() + " unchanged=true");
    }

    @GameTest(template = "variable_reactor_empty", timeoutTicks = 1_000)
    public static void maximumBindingsSaveAndHeightRepairs(GameTestHelper helper) {
        var anchor=helper.absolutePos(BlockPos.ZERO);
        var origin=new BlockPos(24-(anchor.getX()&15),0,24-(anchor.getZ()&15));
        build(helper,origin,16,16,16);
        var nearDrive=origin.offset(1,15,2);
        var remoteDrive=origin.offset(14,15,14);
        helper.setBlock(nearDrive,P1Blocks.CONTROL_ROD_DRIVE.get().defaultBlockState());
        helper.setBlock(remoteDrive,P1Blocks.CONTROL_ROD_DRIVE.get().defaultBlockState());
        helper.setBlock(origin.offset(2,1,0),P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState());
        ReactorStructureLifecycle.rescanAroundNow(helper.getLevel(),helper.absolutePos(remoteDrive));
        helper.runAfterDelay(6,()->{
            var owner=owner(helper,origin.offset(1,1,0));
            require(helper,owner.structureValid(),"16 cubed cross-chunk structure did not form");
            require(helper,owner.snapshot().controlRodColumns().containsKey(new CoreColumnPosition(13,13)),"far control column did not bind");
            var fuelPort=(ReactorPortBlockEntity)helper.getBlockEntity(origin.offset(1,15,1));
            require(helper,fuelPort.isBound(),"maximum-height fuel port did not bind");
            var state=owner.snapshot().withFuelColumn(new CoreColumnPosition(0,0),FuelColumnState.empty().withIntegrity(.5));
            var rods=new java.util.TreeMap<>(state.controlRodColumns());
            rods.put(new CoreColumnPosition(13,13),new ControlRodColumnState(.5,.2,.2,false,0));
            owner.setSnapshot(state.withColumns(state.fuelColumns(),rods).withCoolantInventories(111,222));
            var fuelRepair=fuelPort.tryRepairFuelColumn(new ItemStack(ModItems.STEEL_PLATE.get(),2));
            require(helper,fuelRepair.success(),"maximum-height fuel repair failed");
            require(helper,Math.abs(fuelRepair.nextColumn().integrity()-(.5+.25/14))<1e-12,"fuel repair used fixed height");
            var player=helper.makeMockPlayer(GameType.SURVIVAL);
            var absolute=helper.absolutePos(remoteDrive);
            player.setPos(absolute.getX()+.5,absolute.getY()+.5,absolute.getZ()+2);
            var rodRepair=ControlRodSliderService.repairFromPlayer(player,absolute,new ItemStack(ModItems.STEEL_PLATE.get(),2));
            require(helper,rodRepair.success(),"far control-rod repair failed");
            require(helper,Math.abs(rodRepair.nextColumn().integrity()-(.5+.25/14))<1e-12,"control repair used fixed height");
            owner.tickReactor();
            require(helper,owner.runtimeDescriptor().map(d->d.available()&&d.columns().size()==196).orElse(false),"formal maximum-size tick did not publish its settled descriptor");
            require(helper,owner.snapshot().dimensions().height()==16,"formal tick discarded actual dimensions");
            var saved=owner.saveForServerTest(helper.getLevel().registryAccess());
            var before=owner.snapshot().withoutFuelAssemblies();
            owner.loadForServerTest(saved,helper.getLevel().registryAccess());
            require(helper,owner.snapshot().withoutFuelAssemblies().equals(before),"current saved dimensions/state did not round trip");
            // 另一探针先完成远端拆件，再解除本轮模板保活，避免两个测试互相卸载。
            helper.runAfterDelay(24,()->assertRealChunkUnloadAndRecovery(helper,owner,origin));
        });
    }

    /**
     * 第一阶段只保留仪表FULL级ticket，验证远端失去FULL可用性；第二阶段解除全部
     * ticket验证物理卸载及新实例恢复。使用锁定公开API，不手发区块事件。
     */
    private static void assertRealChunkUnloadAndRecovery(GameTestHelper helper,ReactorInstrumentPortBlockEntity owner,BlockPos origin) {
        var level=helper.getLevel();
        var ownerChunk=new net.minecraft.world.level.ChunkPos(owner.getBlockPos());
        var remotePos=helper.absolutePos(origin.offset(15,1,15));
        var remoteChunk=new net.minecraft.world.level.ChunkPos(remotePos);
        require(helper,!ownerChunk.equals(remoteChunk),"maximum probe did not cross a chunk boundary");
        var cold=(ReactorPortBlockEntity)helper.getBlockEntity(origin.offset(2,1,0));
        var stale=com.iksxh.create_nuclear_industry.reactor.ReactorCoolantFluidHandler.forPort(cold);
        require(helper,stale!=null,"cold capability was unavailable before unload");
        var saved=owner.snapshot().withoutFuelAssemblies();
        var unloads=new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Consumer<net.neoforged.neoforge.event.level.ChunkEvent.Unload> listener=event->{
            if(event.getLevel()==level && event.getChunk().getPos().equals(remoteChunk)) {
                unloads.incrementAndGet();
                System.out.println("variable-reactor real remote ChunkEvent.Unload "+remoteChunk+" tick="+helper.getTick());
            }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(listener);
        System.out.println("variable-reactor pre-unload owner="+ownerChunk+" remote="+remoteChunk+" indexed="+ReactorStructureLifecycle.loadedOwnersNear(level,owner.getBlockPos()).size());
        var ticket=net.minecraft.server.level.TicketType.<net.minecraft.world.level.ChunkPos>create("reactor_variable_probe_owner",java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
        level.getChunkSource().addRegionTicket(ticket,ownerChunk,0,ownerChunk);
        for(long packed:level.getForcedChunks().toLongArray()) {
            var chunk=new net.minecraft.world.level.ChunkPos(packed);
            level.setChunkForced(chunk.x,chunk.z,false);
        }
        helper.runAfterDelay(340,()->{
            // 快速GameTest没有正常帧的空闲预算；消费公开卸载队列，等待真实事件而非手发事件。
            level.getChunkSource().tick(()->true,false);
            System.out.println("variable-reactor post-unload ownerRemoved="+owner.isRemoved()+" sameOwner="+(level.getBlockEntity(owner.getBlockPos())==owner)+" indexed="+ReactorStructureLifecycle.loadedOwnersNear(level,owner.getBlockPos()).size()+" remoteEvents="+unloads.get()+" scans="+owner.structureScanCount());
            require(helper,level.hasChunkAt(owner.getBlockPos()),"owner chunk unexpectedly unloaded");
            require(helper,!level.hasChunkAt(remotePos),"remote chunk kept FULL availability");
            require(helper,!owner.structureValid(),"missing remote chunk retained valid structure");
            require(helper,stale.getTankCapacity(0)==0,"old handler remained usable during chunk absence");
            require(helper,owner.snapshot().withoutFuelAssemblies().equals(saved),"chunk absence destroyed actual inventories or simulation state");
            level.getChunkSource().getChunk(remoteChunk.x,remoteChunk.z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);
            // 按真实包围盒显式恢复区块可用性；生产重绑定仍由ChunkEvent/onLoad安排。
            var absolute=helper.absolutePos(origin);
            var maximum=absolute.offset(15,15,15);
            for(int x=absolute.getX()>>4;x<=maximum.getX()>>4;x++) for(int z=absolute.getZ()>>4;z<=maximum.getZ()>>4;z++) level.setChunkForced(x,z,true);
            helper.runAfterDelay(8,()->{
                require(helper,owner.structureValid(),"loaded remote chunks did not rebuild binding");
                require(helper,owner.snapshot().dimensions().height()==16,"restored dimensions were lost");
                require(helper,stale.getTankCapacity(0)==0,"old handler revived after remote reload");
                var newCold=(ReactorPortBlockEntity)helper.getBlockEntity(origin.offset(2,1,0));
                require(helper,newCold==cold,"near port unexpectedly unloaded instead of changing binding generation");
                require(helper,com.iksxh.create_nuclear_industry.reactor.ReactorCoolantFluidHandler.forPort(newCold)!=null,"new cold binding did not recover");
                // FULL33的仪表ticket会让邻区保留生成级对象；解除它才能真正物理卸载整堆。
                level.getChunkSource().removeRegionTicket(ticket,ownerChunk,0,ownerChunk);
                for(long packed:level.getForcedChunks().toLongArray()) {
                    var chunk=new net.minecraft.world.level.ChunkPos(packed);
                    level.setChunkForced(chunk.x,chunk.z,false);
                }
                helper.runAfterDelay(340,()->{
                    level.getChunkSource().tick(()->true,false);
                    System.out.println("variable-reactor physical-unload ownerRemoved="+owner.isRemoved()+" indexed="+ReactorStructureLifecycle.loadedOwnersNear(level,owner.getBlockPos()).size()+" remoteEvents="+unloads.get());
                    require(helper,unloads.get()>0,"physical remote ChunkEvent.Unload never occurred");
                    require(helper,owner.isRemoved(),"physical unload retained old owner instance");
                    require(helper,ReactorStructureLifecycle.loadedOwnersNear(level,owner.getBlockPos()).isEmpty(),"unloaded owner remained indexed");
                    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(listener);
                    for(int x=absolute.getX()>>4;x<=maximum.getX()>>4;x++) for(int z=absolute.getZ()>>4;z<=maximum.getZ()>>4;z++) {
                        level.getChunkSource().getChunk(x,z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);
                        level.setChunkForced(x,z,true);
                    }
                    helper.runAfterDelay(8,()->{
                        var restored=owner(helper,origin.offset(1,1,0));
                        require(helper,restored!=owner&&restored.structureValid(),"fresh physical reload did not form new owner");
                        require(helper,restored.snapshot().withoutFuelAssemblies().equals(saved),"physical save/reload changed authoritative state");
                        require(helper,stale.getTankCapacity(0)==0,"old handler revived after physical reload");
                        System.out.println("variable-reactor reload sameOwner=false indexed="+ReactorStructureLifecycle.loadedOwnersNear(level,restored.getBlockPos()).size()+" dimensions="+restored.snapshot().dimensions());
                        helper.succeed();
                    });
                });
            });
        });
    }

    @GameTest(template = "variable_reactor_empty", timeoutTicks = 160)
    public static void rectangularBindingsAndRemoteMaintenance(GameTestHelper helper) {
        build(helper, BlockPos.ZERO, 6, 8, 7);
        helper.runAfterDelay(6, () -> {
            var owner = owner(helper, new BlockPos(1, 1, 0));
            require(helper, owner.structureValid(), "6x8x7 reactor did not form");
            require(helper, owner.structureScan().columns().size() == 20, "actual rectangular columns were lost");
            var fuelPort = (ReactorPortBlockEntity) helper.getBlockEntity(new BlockPos(1, 7, 1));
            require(helper, fuelPort.isBound() && fuelPort.boundOwner() == owner, "remote fuel port did not bind");
            long scans = owner.structureScanCount();
            helper.runAfterDelay(10, () -> {
                require(helper, scans == owner.structureScanCount(), "normal ticks rescanned geometry");
                helper.setBlock(new BlockPos(5, 7, 6), Blocks.AIR.defaultBlockState());
                ReactorStructureLifecycle.rescanAroundNow(helper.getLevel(), helper.absolutePos(new BlockPos(5, 7, 6)));
                require(helper, !owner.structureValid() && !fuelPort.isBound(), "remote casing loss kept stale ownership");
                helper.succeed();
            });
        });
    }

    /** 有限块阵列，仪表偏心且内侧直接邻接燃料；每列仍只有一个顶部换料端口。 */
    private static void build(GameTestHelper helper, BlockPos origin, int width, int height, int depth) {
        for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) for (int z = 0; z < depth; z++) {
            boolean boundary = x == 0 || y == 0 || z == 0 || x == width - 1 || y == height - 1 || z == depth - 1;
            helper.setBlock(origin.offset(x, y, z), boundary ? P1Blocks.REACTOR_CASING.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
        }
        for (int y = 1; y < height - 1; y++) helper.setBlock(origin.offset(1, y, 1), P1Blocks.REACTOR_FUEL_ROD.get().defaultBlockState());
        helper.setBlock(origin.offset(1, height - 1, 1), P1Blocks.REACTOR_REFUELING_PORT.get().defaultBlockState());
        helper.setBlock(origin.offset(width - 1, 1, 1), P1Blocks.REACTOR_COLD_PORT.get().defaultBlockState());
        helper.setBlock(origin.offset(width - 1, 1, depth - 2), P1Blocks.REACTOR_HOT_PORT.get().defaultBlockState());
        helper.setBlock(origin.offset(1, 1, 0), P1Blocks.REACTOR_INSTRUMENT_PORT.get().defaultBlockState());
        ReactorStructureLifecycle.rescanAroundNow(helper.getLevel(), helper.absolutePos(origin.offset(1, 1, 0)));
    }
    private static ReactorInstrumentPortBlockEntity owner(GameTestHelper helper, BlockPos pos) {
        return (ReactorInstrumentPortBlockEntity) helper.getBlockEntity(pos);
    }
    private static void require(GameTestHelper helper, boolean condition, String message) { if (!condition) helper.fail(message); }
}
