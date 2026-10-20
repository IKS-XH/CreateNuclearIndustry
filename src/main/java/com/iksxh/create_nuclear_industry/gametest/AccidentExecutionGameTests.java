package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.accident.*;
import com.iksxh.create_nuclear_industry.blockentity.*;
import com.iksxh.create_nuclear_industry.config.*;
import com.iksxh.create_nuclear_industry.content.*;
import com.iksxh.create_nuclear_industry.production.*;
import com.iksxh.create_nuclear_industry.radiation.*;
import com.iksxh.create_nuclear_industry.reactor.*;
import com.iksxh.create_nuclear_industry.storage.*;
import com.iksxh.create_nuclear_industry.structure.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;

/** 第二批唯一隔离真实服域；所有库存为测试专用，绝不读取run/saves或用户世界。 */
@GameTestHolder("cni_accident_execution") @PrefixGameTestTemplate(false)
public final class AccidentExecutionGameTests {
    private static final String TEMPLATE="accident_empty";
    private static final BlockPos BASE=new BlockPos(4,2,4);
    private AccidentExecutionGameTests() {}
    private static boolean skipFixture(GameTestHelper h,String key) {
        var focus=System.getenv("CNI_ACCIDENT_FOCUS");
        if(!"1".equals(System.getenv("CNI_ACCIDENT_RESUME_ONLY"))&&(focus==null||focus.equals(key)))return false;
        System.out.println("[accident] focused validation: SKIP fixture "+key);h.succeed();return true;
    }
    private static boolean resumeOnly(GameTestHelper h) {return skipFixture(h,"business");}
    private static ItemStack fresh() {return new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get());}
    private static ItemStack used() {return FuelUseHistory.prepareBurnCopy(fresh(),.001);}
    private static ReactorInstrumentPortBlockEntity build(GameTestHelper h,BlockPos base,int size) {
        for(int x=0;x<size;x++)for(int y=0;y<size;y++)for(int z=0;z<size;z++) {
            boolean edge=x==0||y==0||z==0||x==size-1||y==size-1||z==size-1;
            h.setBlock(base.offset(x,y,z),edge?P1Blocks.REACTOR_CASING.get():P1Blocks.REACTOR_FUEL_ROD.get());
        }
        for(int x=1;x<size-1;x++)for(int z=1;z<size-1;z++)h.setBlock(base.offset(x,size-1,z),P1Blocks.REACTOR_REFUELING_PORT.get());
        h.setBlock(base.offset(1,1,0),P1Blocks.REACTOR_INSTRUMENT_PORT.get());
        h.setBlock(base.offset(2,1,0),P1Blocks.REACTOR_COLD_PORT.get());h.setBlock(base.offset(3,1,0),P1Blocks.REACTOR_HOT_PORT.get());
        ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(base.offset(1,1,0)));
        var owner=(ReactorInstrumentPortBlockEntity)h.getBlockEntity(base.offset(1,1,0));
        h.assertTrue(owner.structureValid(),"真实反应堆未成型："+owner.structureScan().failureReason());return owner;
    }
    private static ReactorPortBlockEntity port(GameTestHelper h,BlockPos base,int size,int x,int z) {
        return (ReactorPortBlockEntity)h.getBlockEntity(base.offset(x+1,size-1,z+1));
    }
    private static ReactorAccidentLedger trigger(GameTestHelper h,ReactorInstrumentPortBlockEntity owner,ReactorMeltdownEvent.Reason reason) {
        var next=owner.snapshot().withMeltdown(P1ServerConfig.VALUES.meltdownCountdownTicks.get(),true).withMeltdownEventPublished(true);
        owner.setSnapshot(next);var e=new ReactorMeltdownEvent(reason,h.getLevel(),owner.structureOrigin(),owner.getBlockPos(),next);
        NeoForge.EVENT_BUS.post(e);var ledger=ReactorAccidentSavedData.get(h.getLevel()).byGeneration(owner.accidentGeneration());
        h.assertTrue(ledger!=null,"正式事件没有事故任务");h.assertTrue(ReactorAccidentService.begin(e)==ledger,"事件重放另建任务");return ledger;
    }
    /** 本次冷态现场的六燃料/三全插棒布局；只补缺失方块，保留事故后实际幸存端口的BE身份。 */
    private static ReactorInstrumentPortBlockEntity shutdownReactor(GameTestHelper h) {
        var roles=new TreeMap<CoreColumnPosition,ReactorStructureDefinition.ColumnType>();
        for(int x=0;x<3;x++)for(int z=0;z<3;z++)roles.put(new CoreColumnPosition(x,z),z==1
                ?ReactorStructureDefinition.ColumnType.CONTROL_ROD:ReactorStructureDefinition.ColumnType.FUEL);
        for(var entry:ReactorStructureDefinition.templateFor(roles).entrySet()) {
            var local=entry.getKey();var pos=BASE.offset(local.x(),local.y(),local.z());
            var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(entry.getValue()));
            if(!h.getBlockState(pos).is(block))h.setBlock(pos,block);
        }
        var at=BASE.offset(2,2,0);ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(at));
        var owner=(ReactorInstrumentPortBlockEntity)h.getBlockEntity(at);
        h.assertTrue(owner.structureValid(),"事故残余重建没有有效几何");return owner;
    }

    /** 真实事故收据完成后，残余空口不能阻止后建冷堆的原生安全维护；旧锁或每次重扫换代都会使本例失败。 */
    @GameTest(template=TEMPLATE,timeoutTicks=160)
    public static void completedAccidentEmptyPortsRebindForColdShutdown(GameTestHelper h) {
        if(skipFixture(h,"shutdown"))return;
        var old=shutdownReactor(h);var ports=List.copyOf(old.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING));
        var cold=old.boundPorts(ReactorPortBlockEntity.BindingType.COLD_COOLANT).getFirst();
        var staleHandler=ReactorCoolantFluidHandler.forPort(cold);h.assertTrue(staleHandler!=null,"缺少真实旧冷端能力");
        var fuels=new TreeMap<CoreColumnPosition,FuelColumnState>();
        for(int i=0;i<ports.size();i++) {
            var item=i==0?used():fresh();item.set(DataComponents.CUSTOM_NAME,Component.literal("停机原件"+i));
            ports.get(i).setFuelAssembly(item);fuels.put(ports.get(i).boundColumn(),new FuelColumnState(FuelAssemblyState.empty(),0,4));
        }
        old.setSnapshot(old.snapshot().withColumns(fuels,old.snapshot().controlRodColumns())
                .withMeltdown(P1ServerConfig.VALUES.meltdownCountdownTicks.get()-1L,true));
        old.tickReactor();h.assertTrue(old.accidentId()!=null,"正式末tick未触发真实事故");
        var ledger=ReactorAccidentSavedData.get(h.getLevel()).byId(old.accidentId());
        h.runAfterDelay(30,()->{
            h.assertTrue(ledger.complete(),"正式事故尚未完成，不能作为过期锁用例");
            var survivors=ports.stream().filter(p->!p.isRemoved()&&h.getLevel().getBlockEntity(p.getBlockPos())==p).toList();
            h.assertTrue(!survivors.isEmpty(),"32块壳上限未留下真实空换料口");
            var locked=survivors.getFirst();var lockedTag=locked.saveWithFullMetadata(h.getLevel().registryAccess());
            h.assertTrue(lockedTag.hasUUID("AccidentLock")&&ledger.id.equals(lockedTag.getUUID("AccidentLock")),"幸存口不是本事故原始锁");
            var originalGenerations=survivors.stream().map(ReactorPortBlockEntity::accidentGeneration).toList();
            var receipt=ledger.save(h.getLevel().registryAccess()).copy();
            var data=ReactorAccidentSavedData.get(h.getLevel());
            h.getLevel().getDataStorage().set("create_nuclear_industry_accidents",ReactorAccidentSavedData.load(data.save(new CompoundTag(),h.getLevel().registryAccess()),h.getLevel().registryAccess()));
            var waste=ledger.columns.stream().filter(ReactorAccidentLedger.Column::waste).findFirst().orElseThrow();
            var corium=(ReactorCoriumBlockEntity)h.getLevel().getBlockEntity(waste.bottom);
            var recovered=corium.take();h.assertTrue(recovered.is(AccidentContent.ACCIDENT_FUEL_WASTE.get())&&recovered.getCount()==1,"真实残骸未提供唯一回收载荷");
            var next=shutdownReactor(h);h.assertTrue(next!=old&&!next.accidentGeneration().equals(old.accidentGeneration()),"未建立后建仪表世代");
            next.setCoolantInventories(9000,0);
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ShutdownBreak"));
            var event=net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(h.getLevel(),GameType.SURVIVAL,player,next.getBlockPos(),next.getBlockState());
            h.assertTrue(!event.isCanceled(),"已完成事故的空口使冷堆原生维护拆除仍被尚未完全停机取消");
            for(int i=0;i<survivors.size();i++) {
                var p=survivors.get(i);h.assertTrue(p.isBound()&&!p.accidentGeneration().equals(originalGenerations.get(i)),"真实残余空口未以新物理代际重绑");
            }
            h.assertTrue(next.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING).size()==6,"新堆漏绑残余列");
            var generation=locked.accidentGeneration();long binding=locked.bindingGeneration();
            var saved=locked.saveWithFullMetadata(h.getLevel().registryAccess());
            locked.loadWithComponents(saved,h.getLevel().registryAccess());ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),next.getBlockPos());
            h.assertTrue(locked.isBound()&&generation.equals(locked.accidentGeneration())&&binding==locked.bindingGeneration(),"当前格式保存/重扫重复换代或失效绑定");
            h.assertTrue(locked.tryInsertFuel(fresh()).success(),"已恢复残余口不能正式补料");
            h.assertTrue(locked.tryExtractFuel().success(),"冷态恢复口不能正式取料");
            h.assertTrue(staleHandler.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.COMPOUND_COOLANT_SOURCE.get(),1),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)==0,"旧能力在新世代复活");
            verifyShutdownRebindGuards(h,locked,next,old,ledger,lockedTag,saved);
            var currentLedger=ReactorAccidentSavedData.get(h.getLevel()).byId(ledger.id);
            h.assertTrue(receipt.equals(currentLedger.save(h.getLevel().registryAccess()))&&currentLedger.columns.stream().allMatch(c->c.payload().isEmpty()),"重绑改动已读回旧账本或再产托管");
            int returned=0;
            for(var c:ledger.columns)if(!c.waste()) {
                var id=UUID.nameUUIDFromBytes((ledger.id+":"+c.port.asLong()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                h.assertTrue(h.getLevel().getEntity(id) instanceof ItemEntity item&&ItemStack.matches(item.getItem(),c.original()),"已交接全新原件丢失/改变");returned++;
            }
            h.assertTrue(returned==5&&recovered.getCount()==1&&next.snapshot().coldCoolantMb()==9000&&next.snapshot().hotCoolantMb()==0,"旧5原件+1废料或新冷堆工质不守恒");
            h.assertTrue(ledger.complete()&&ReactorAccidentExecutor.tick(h.getLevel())==0,"完成事故重放又执行动作");
            h.assertTrue(locked.tryInsertFuel(fresh()).success(),"安全清空前未装入真实组件");
            var shell=BASE;var worldShell=h.absolutePos(shell);
            var reset=net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(h.getLevel(),GameType.SURVIVAL,player,worldShell,h.getBlockState(shell));
            h.assertTrue(!reset.isCanceled()&&next.snapshot().fuelColumns().isEmpty()&&next.snapshot().coldCoolantMb()==0
                    &&next.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING).stream().allMatch(p->p.fuelAssembly().isEmpty()),"正式外壳拆除未通过FULL_SHUTDOWN_RESET清空真实口");
            h.setBlock(shell,Blocks.AIR);ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),next.getBlockPos());
            h.assertTrue(!next.structureValid(),"原生安全事务后实际拆壳未使结构失效");
            System.out.println("[shutdown03] native final-tick accident -> retained "+survivors.size()+" empty ports -> new cold owner -> CommonHooks maintenance and FULL_SHUTDOWN_RESET allowed; receipts/5 originals/1 recovered waste, guards, save and idempotence passed");h.succeed();
        });
    }

    /** 只在隔离夹具恢复真实旧锁NBT来构造拒绝输入；每次失败必须保留身份、锁和库存。 */
    private static void rejectedShutdownRebind(GameTestHelper h,ReactorPortBlockEntity port,ReactorInstrumentPortBlockEntity owner,
            CompoundTag tag,ReactorPortBlockEntity.BindingType type,CoreColumnPosition column,String reason) {
        port.loadWithComponents(tag,h.getLevel().registryAccess());var before=port.saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue(!port.bindTo(owner,type,column)&&before.equals(port.saveWithFullMetadata(h.getLevel().registryAccess())),"无效重绑提前解锁/改身份："+reason);
    }

    private static void verifyShutdownRebindGuards(GameTestHelper h,ReactorPortBlockEntity port,ReactorInstrumentPortBlockEntity next,
            ReactorInstrumentPortBlockEntity old,ReactorAccidentLedger ledger,CompoundTag locked,CompoundTag restored) {
        var column=port.boundColumn();var type=ReactorPortBlockEntity.BindingType.REFUELING;
        var missing=locked.copy();missing.putUUID("AccidentLock",UUID.randomUUID());rejectedShutdownRebind(h,port,next,missing,type,column,"缺账本");
        var mismatch=locked.copy();mismatch.putUUID("AccidentGeneration",UUID.randomUUID());rejectedShutdownRebind(h,port,next,mismatch,type,column,"物理代际不匹配");
        var occupied=locked.copy();occupied.put("FuelAssembly",fresh().save(h.getLevel().registryAccess()));rejectedShutdownRebind(h,port,next,occupied,type,column,"当前口非空");
        rejectedShutdownRebind(h,port,old,locked,type,column,"原事故所有者");
        rejectedShutdownRebind(h,port,next,locked,ReactorPortBlockEntity.BindingType.COLD_COOLANT,null,"职责错误");
        rejectedShutdownRebind(h,port,next,locked,type,new CoreColumnPosition(column.x(),1),"结构列不占用该口");
        var unfinished=ledger.save(h.getLevel().registryAccess());var id=UUID.randomUUID();unfinished.putUUID("Id",id);unfinished.putUUID("Generation",UUID.randomUUID());unfinished.putBoolean("ShockComplete",false);
        var pendingReceipt=ReactorAccidentLedger.load(unfinished,h.getLevel().registryAccess());ReactorAccidentSavedData.get(h.getLevel()).add(pendingReceipt);
        var pending=locked.copy();pending.putUUID("AccidentLock",id);rejectedShutdownRebind(h,port,next,pending,type,column,"事故未完成");
        pendingReceipt.shockComplete=true;
        port.loadWithComponents(restored,h.getLevel().registryAccess());ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),next.getBlockPos());
        h.assertTrue(port.isBound(),"反例后未恢复新代际口");
    }

    @GameTest(template=TEMPLATE,timeoutTicks=120)
    public static void handoffStopsSameTickAndCanceledBreakDoesNotEnqueue(GameTestHelper h) {
        if(resumeOnly(h))return;
        var owner=build(h,BASE,5);var fuel=port(h,BASE,5,0,0);fuel.setFuelAssembly(used());
        var player=h.makeMockPlayer(GameType.SURVIVAL);var before=ReactorAccidentSavedData.get(h.getLevel()).records().size();
        var canceled=new BlockEvent.BreakEvent(h.getLevel(),owner.getBlockPos(),owner.getBlockState(),player);canceled.setCanceled(true);
        ReactorStructureLifecycle.onBlockBreak(canceled);h.assertTrue(before==ReactorAccidentSavedData.get(h.getLevel()).records().size(),"取消拆除留下事故");
        var fuels=new TreeMap<CoreColumnPosition,FuelColumnState>();
        owner.structureScan().columns().keySet().forEach(c->fuels.put(c,new FuelColumnState(FuelAssemblyState.empty(),0,0)));
        owner.setSnapshot(owner.snapshot().withColumns(fuels,Map.of()).withMeltdown(P1ServerConfig.VALUES.meltdownCountdownTicks.get()-1L,true));
        h.assertTrue(owner.tickReactor(),"正式倒计时末tick没有提交");
        h.assertTrue(owner.accidentId()!=null&&!owner.structureValid()&&owner.automationBinding()==null,"事故没有即时锁堆撤权");
        h.assertTrue(owner.snapshot().coldCoolantMb()==0&&owner.snapshot().hotCoolantMb()==0,"工质没有扣账");
        h.assertTrue(owner.runtimeDescriptor().isEmpty()||!owner.runtimeDescriptor().get().available(),"旧result恢复运行样本");
        h.assertTrue(!owner.tickReactor()&&!fuel.tryInsertFuel(fresh()).success(),"旧端口或旧tick重新运行");
        h.assertTrue(fuel.fuelAssembly().isEmpty(),"原口与托管同时持料");
        System.out.println("[accident] formal same-tick handoff and canceled-break assertions passed");h.succeed();
    }
    @GameTest(template=TEMPLATE,timeoutTicks=180)
    public static void custodyConservesPhysicalStacksAcrossReplay(GameTestHelper h) {
        if(resumeOnly(h))return;
        var owner=build(h,BASE,5);var p0=port(h,BASE,5,0,0);var p1=port(h,BASE,5,1,0);var p2=port(h,BASE,5,2,0);
        p0.setFuelAssembly(used());var named=fresh();named.set(DataComponents.CUSTOM_NAME,Component.literal("全新原件"));p1.setFuelAssembly(named);
        p2.setFuelAssembly(new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get()));owner.setCoolantInventories(123,456);
        var ledger=trigger(h,owner,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);
        h.assertTrue(ledger.columns.size()==3&&ledger.columns.stream().filter(ReactorAccidentLedger.Column::waste).count()==2,"组件按高度/投影复制或漏掉小数历史");
        h.assertTrue(ledger.coldMb==123&&ledger.hotMb==456,"原工质未入账");
        var saved=ReactorAccidentLedger.load(ledger.save(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(saved.columns.size()==3&&saved.columns.stream().allMatch(ReactorAccidentLedger.Column::captured),"当前格式托管保存丢料");
        h.runAfterDelay(30,()->{
            h.assertTrue(ledger.complete(),"小堆事故未完成");int waste=0;
            for(var c:ledger.columns)if(c.waste()) {
                h.assertTrue(h.getLevel().getBlockEntity(c.bottom) instanceof ReactorCoriumBlockEntity,"列底没有残骸");
                var corium=(ReactorCoriumBlockEntity)h.getLevel().getBlockEntity(c.bottom);waste+=corium.payload().getCount();
            }
            h.assertTrue(waste==2&&ledger.columns.stream().allMatch(c->c.payload().isEmpty()),"托管/残骸载荷不守恒");
            var freshColumn=ledger.columns.stream().filter(c->!c.waste()).findFirst().orElseThrow();
            var dropId=UUID.nameUUIDFromBytes((ledger.id+":"+freshColumn.port.asLong()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            h.assertTrue(h.getLevel().getEntity(dropId) instanceof ItemEntity item&&ItemStack.matches(item.getItem(),named),"全新原件未完整返还一次");
            h.assertTrue(owner.prepareDisassemblyPlan(owner.getBlockPos(),owner.getBlockState())==null,"已结算残余仪表仍永久拒拆");
            System.out.println("[accident] physical used/fresh/spent/empty columns conserve 2 waste + 1 exact return; settled debris unlocked");h.succeed();
        });
    }
    @GameTest(template=TEMPLATE,timeoutTicks=230)
    public static void ledgerResumesWithoutInstrumentAndWithoutChunkLoads(GameTestHelper h) {
        if(resumeOnly(h))return;
        var owner=build(h,BASE,16);
        for(var port:owner.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING))port.setFuelAssembly(used());
        var ledger=trigger(h,owner,ReactorMeltdownEvent.Reason.DANGEROUS_DISASSEMBLY);
        var partial=ReactorAccidentSavedData.get(h.getLevel()).save(new CompoundTag(),h.getLevel().registryAccess());
        var restored=ReactorAccidentSavedData.load(partial,h.getLevel().registryAccess()).byId(ledger.id);
        h.assertTrue(restored.columns.size()==196&&!restored.complete(),"16³托管快照没有保存196份组件");
        h.setBlock(BASE.offset(1,1,0),Blocks.AIR);
        long[] forced=h.getLevel().getForcedChunks().toLongArray();
        h.runAfterDelay(120,()->{
            h.assertTrue(ledger.complete(),"仪表先毁后账本不能独立完成");
            h.assertTrue(ledger.columns.stream().allMatch(ReactorAccidentLedger.Column::settled),"16³有丢料/未结算列");
            h.assertTrue(ledger.externalBroken<=32&&ledger.peakActions<=64,"16³破坏/预算越界");
            h.assertTrue(java.util.Arrays.equals(forced,h.getLevel().getForcedChunks().toLongArray()),"事故强载区块");
            System.out.println("[accident] 16cube plan="+ledger.actions.size()+" payloads="+ledger.columns.size()+" actions="+ledger.worldActions+" external="+ledger.externalBroken+" peak="+ledger.peakActions);h.succeed();
        });
    }
    @GameTest(template=TEMPLATE,timeoutTicks=150)
    public static void sharedBudgetFrozenRulesAndBoundedDamage(GameTestHelper h) {
        if(resumeOnly(h))return;
        var a=build(h,BASE,5);var b=build(h,BASE.offset(12,0,0),5);port(h,BASE,5,0,0).setFuelAssembly(used());port(h,BASE.offset(12,0,0),5,0,0).setFuelAssembly(used());
        var la=trigger(h,a,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);var lb=trigger(h,b,ReactorMeltdownEvent.Reason.DANGEROUS_DISASSEMBLY);
        int one=ReactorAccidentExecutor.tick(h.getLevel()),two=ReactorAccidentExecutor.tick(h.getLevel());
        h.assertTrue(one+two<=64,"同维度多事故或同tick重入把预算相乘");
        h.assertTrue(la.parameters.equals(AccidentParameters.DEFAULT)&&lb.parameters.equals(AccidentParameters.DEFAULT),"事故没有冻结参数");
        h.runAfterDelay(30,()->{h.assertTrue(la.complete()&&lb.complete()&&la.externalBroken<=32&&lb.externalBroken<=32,"多事故未公平完成或外部超限");h.succeed();});
    }
    @GameTest(template=TEMPLATE,timeoutTicks=120)
    public static void nativeFinalCancellationAndMultiOwnerFailureDoNotEnqueue(GameTestHelper h) {
        if(resumeOnly(h))return;
        var a=build(h,BASE,5);var b=build(h,BASE.offset(4,0,0),5);
        ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),a.getBlockPos());
        var pa=port(h,BASE,5,0,0);var pb=port(h,BASE.offset(4,0,0),5,0,0);pa.setFuelAssembly(used());pb.setFuelAssembly(used());
        var broken=h.absolutePos(BASE.offset(4,2,2));var state=h.getLevel().getBlockState(broken);
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"AccidentBreak"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        int before=ReactorAccidentSavedData.get(h.getLevel()).records().size();
        java.util.function.Consumer<BlockEvent.BreakEvent> cancel=e->{if(e.getPos().equals(broken))e.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST,false,cancel);
        try {
            var event=net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(h.getLevel(),GameType.SURVIVAL,player,broken,state);
            h.assertTrue(event.isCanceled()&&a.accidentId()==null&&b.accidentId()==null,"同LOWEST晚取消仍接管事故");
            h.assertTrue(ReactorAccidentSavedData.get(h.getLevel()).records().size()==before&&!pa.fuelAssembly().isEmpty()&&!pb.fuelAssembly().isEmpty(),"取消事件遗留任务/吞料");
        } finally { NeoForge.EVENT_BUS.unregister(cancel); }
        java.util.function.Consumer<ReactorMeltdownEvent> failure=e->{if(e.instrumentPort().equals(b.getBlockPos()))throw new IllegalStateException("本批验证第二所有者监听失败");};
        NeoForge.EVENT_BUS.addListener(failure);
        try {
            var event=net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(h.getLevel(),GameType.SURVIVAL,player,broken,state);
            h.assertTrue(event.isCanceled()&&ReactorAccidentSavedData.get(h.getLevel()).records().size()==before,"多所有者失败留下孤立事故");
            h.assertTrue(!pa.fuelAssembly().isEmpty()&&!pb.fuelAssembly().isEmpty()&&a.accidentId()==null&&b.accidentId()==null,"失败事务没有保留真实物料");
        } finally { NeoForge.EVENT_BUS.unregister(failure); }
        var success=net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(h.getLevel(),GameType.SURVIVAL,player,broken,state);
        h.assertTrue(!success.isCanceled()&&a.accidentId()!=null&&b.accidentId()!=null,"原生危险拆除没有原子接管两个所有者");
        h.assertTrue(pa.fuelAssembly().isEmpty()&&pb.fuelAssembly().isEmpty(),"危险拆除同时保留原组件");
        System.out.println("[accident] native post final LOWEST cancellation, second-owner failure rollback, and successful dangerous handoff passed");h.succeed();
    }
    /** 夹具只解除FakePlayer固定免伤和出生保护，不建立真人连接，不称多人实测。 */
    private static final class ShockPlayer extends net.neoforged.neoforge.common.util.FakePlayer {
        ShockPlayer(net.minecraft.server.level.ServerLevel level) {
            super(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"AccidentShock"));
            try {var f=net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");f.setAccessible(true);f.setInt(this,0);}
            catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source){return false;}
    }
    @GameTest(template=TEMPLATE,timeoutTicks=130)
    public static void shockIsOnceAndDisabledDestructionFreezesAllRules(GameTestHelper h) {
        if(resumeOnly(h))return;
        var owner=build(h,BASE,5);port(h,BASE,5,0,0).setFuelAssembly(used());
        var player=new ShockPlayer(h.getLevel());player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);player.setPos(Vec3.atCenterOf(h.absolutePos(BASE.offset(2,2,2))));player.setHealth(20);
        h.getLevel().players().add(player);
        var l=trigger(h,owner,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);
        h.runAfterDelay(3,()->{
            try {
                h.assertTrue(player.getHealth()==12&&l.shocked.contains(player.getUUID()),"近距冲击未限8HP并记玩家收据：health="+player.getHealth()+" receipt="+l.shocked.size());
                ReactorAccidentExecutor.tick(h.getLevel());h.assertTrue(player.getHealth()==12,"同事故分批/重入重复伤害");
                var loaded=ReactorAccidentLedger.load(l.save(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(loaded.shocked.contains(player.getUUID()),"冲击收据没有保存");
            } finally {h.getLevel().players().remove(player);}
        });
        var nextBase=BASE.offset(16,0,0);var next=build(h,nextBase,5);port(h,nextBase,5,0,0).setFuelAssembly(used());next.setCoolantInventories(111,222);
        ReactorAccidentLedger disabled;
        try {
            var field=AccidentConfig.class.getDeclaredField("current");field.setAccessible(true);var original=field.get(null);
            try {field.set(null,new AccidentParameters(false,2,32,8,64));disabled=trigger(h,next,ReactorMeltdownEvent.Reason.DANGEROUS_DISASSEMBLY);}
            finally {field.set(null,original);}
        } catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        h.assertTrue(!disabled.parameters.destruction()&&disabled.actions.stream().noneMatch(a->a.external),"关闭破坏仍排壳/外部动作");
        h.runAfterDelay(30,()->{h.assertTrue(disabled.complete()&&disabled.externalBroken==0&&disabled.shocked.isEmpty()&&next.snapshot().coldCoolantMb()==0,"关闭外部破坏同时取消了内部报废/损失或仍有冲击");h.succeed();});
    }
    @GameTest(template=TEMPLATE,timeoutTicks=200)
    public static void actualKineticsPreserveWasteSpentAndManufactureRecipes(GameTestHelper h) {
        if(resumeOnly(h))return;
        var machines=new ArrayList<ShieldedAssemblyBlockEntity>();
        for(int mode=0;mode<3;mode++) {
            var pos=new BlockPos(12+mode*10,3,28);var block=FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get();
            h.setBlock(pos,block.defaultBlockState().setValue(ShieldedAssemblyBlock.EXPANDED,true));block.setPlacedBy(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),null,ItemStack.EMPTY);
            var machine=(ShieldedAssemblyBlockEntity)h.getBlockEntity(pos);machines.add(machine);
            if(mode<2) {
                h.assertTrue(machine.insert(new ItemStack(mode==0?AccidentContent.ACCIDENT_FUEL_WASTE.get():ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get())).isEmpty(),"真实机器拒收封装载荷");
                machine.insert(new ItemStack(SpentFuelStorageContent.VITRIFICATION_MEDIUM.get()));machine.insert(new ItemStack(SpentFuelStorageContent.LEAD_SHIELDING_CASK.get()));
            } else {
                machine.insert(new ItemStack(FuelProcessingContent.SINTERED_FUEL_PELLET.get(),8));machine.insert(new ItemStack(BasicMaterialContent.FUEL_CLADDING_TUBE.get(),4));
                machine.insert(new ItemStack(BasicMaterialContent.SOLDER_INGOT.get(),2));machine.insert(new ItemStack(BasicMaterialContent.STEEL_GRATE.get()));
            }
            h.setBlock(pos.below(),com.simibubi.create.AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(com.simibubi.create.content.kinetics.motor.CreativeMotorBlock.FACING,Direction.UP));
            ((com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)h.getBlockEntity(pos.below())).generatedSpeed.setValue(256);
        }
        h.runAfterDelay(25,()->{
            var m=machines.getFirst();int progress=m.state().progress();h.assertTrue(progress>0&&progress<12800,"真实Create动力没有推进新封装");
            var saved=m.savePortableData();m.loadPortableData(saved);h.assertTrue(m.recipeReady()&&m.state().progress()==progress,"新封装中途恢复串配方");
            m.updateFromNetwork(0,2048,2);m.tick();h.assertTrue(m.state().progress()==progress,"停机/过载仍计新封装工时");m.updateFromNetwork(100000,1024,2);
        });
        h.runAfterDelay(140,()->{
            for(int i=0;i<2;i++)h.assertTrue(SpentFuelPayload.isValid(machines.get(i).state().output()),"新/原封装真实动力没有完成");
            h.assertTrue(machines.get(2).state().output().is(ModItems.FRESH_FUEL_ASSEMBLY.get())&&machines.get(2).state().output().getDamageValue()==0,"原制造真实动力失效");
            for(var m:machines)for(int i=0;i<4;i++)h.assertTrue(m.state().input(i).isEmpty(),"真实完工未原子扣料");
            System.out.println("[accident] actual Create motor: waste/spent 12800 and manufacture 25600, pause/current-format resume passed");h.succeed();
        });
    }
    @GameTest(template=TEMPLATE,timeoutTicks=200)
    public static void wasteSealingAndSourceOwnershipStayAtomic(GameTestHelper h) {
        if(skipFixture(h,"ownership"))return;
        var pos=new BlockPos(28,3,8);var block=FuelProcessingContent.SHIELDED_ASSEMBLY_STATION.get();
        h.setBlock(pos,block.defaultBlockState().setValue(ShieldedAssemblyBlock.EXPANDED,true));block.setPlacedBy(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos),null,ItemStack.EMPTY);
        var machine=(ShieldedAssemblyBlockEntity)h.getBlockEntity(pos);h.assertTrue(machine.complete(),"真实八格装配台未成型");
        var coriumPos=new BlockPos(24,3,8);h.setBlock(coriumPos,AccidentContent.REACTOR_CORIUM.get());var corium=(ReactorCoriumBlockEntity)h.getBlockEntity(coriumPos);
        var waste=new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get());h.assertTrue(corium.accept(UUID.randomUUID(),h.absolutePos(coriumPos),waste),"真实残骸拒绝单件载荷");
        double[] source={0};RadiationSourceAdapters.read(corium,s->source[0]+=RadiationItemSources.describe(s.stack(),RadiationParameters.DEFAULT).dosePerSecond());h.assertTrue(source[0]==4,"残骸真实源不是4");
        h.getLevel().destroyBlock(h.absolutePos(coriumPos),true);var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(coriumPos)).inflate(1));
        var drop=drops.stream().filter(e->e.getItem().is(AccidentContent.ACCIDENT_FUEL_WASTE.get())).findFirst().orElseThrow();var actual=drop.getItem().copy();drop.discard();
        var protection=new ItemEntity(h.getLevel(),h.absolutePos(coriumPos).getX(),h.absolutePos(coriumPos).getY(),h.absolutePos(coriumPos).getZ(),actual.copy());h.getLevel().addFreshEntity(protection);
        for(var damage:List.of(h.getLevel().damageSources().inFire(),h.getLevel().damageSources().lava(),h.getLevel().damageSources().cactus(),h.getLevel().damageSources().explosion(null,null)))
            h.assertTrue(!protection.hurt(damage,100)&&protection.isAlive(),"事故废料被原版伤害删除");
        h.assertTrue(protection.getAge()==-32768,"事故废料没有无限寿命保护");protection.discard();
        var input=machine.itemPort(h.absolutePos(pos.above()),Direction.UP);h.assertTrue(input!=null&&input.insertItem(0,actual,false).isEmpty(),"装配台拒绝真实废料");
        h.assertTrue(input.insertItem(1,new ItemStack(SpentFuelStorageContent.VITRIFICATION_MEDIUM.get()),false).isEmpty(),"固化材料拒绝");
        h.assertTrue(input.insertItem(2,new ItemStack(SpentFuelStorageContent.LEAD_SHIELDING_CASK.get()),false).isEmpty(),"空桶拒绝");
        h.assertTrue(machine.recipeReady()&&machine.state().work()==12800,"新封装没有12800工时");
        var recipe=(ShieldedAssemblyRecipe)h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("create_nuclear_industry:shielded_assembly/sealed_accident_fuel_waste")).orElseThrow().value();
        for(int i=0;i<50;i++)machine.state().advance(128,recipe.actualResult(machine.state().input(0)));
        var checkpoint=machine.savePortableData();machine.loadPortableData(checkpoint);h.assertTrue(machine.recipeReady()&&machine.state().progress()==6400,"封装中途正常格式丢进度");
        for(int i=0;i<50;i++)machine.state().advance(128,recipe.actualResult(machine.state().input(0)));
        var sealed=machine.state().extractOutput(1,false);h.assertTrue(SpentFuelPayload.isValid(sealed),"封装未产合法单件桶");
        h.assertTrue(Math.abs(RadiationItemSources.describe(sealed,RadiationParameters.DEFAULT).dosePerSecond()-.2)<1e-12,"桶重复/漏屏蔽");
        var rackPos=pos.offset(6,0,0);h.setBlock(rackPos,SpentFuelStorageContent.DRY_STORAGE_RACK.get());var rack=(DryStorageBlockEntity)h.getBlockEntity(rackPos);h.assertTrue(rack.insertOne(sealed),"架拒收新废料桶");
        source[0]=0;RadiationSourceAdapters.read(rack,s->source[0]+=RadiationItemSources.describe(s.stack(),RadiationParameters.DEFAULT).dosePerSecond()*(s.rackShielded()?0:1));h.assertTrue(source[0]==0,"完整架不屏蔽废料");
        h.assertTrue(SpentFuelPayload.isValid(SpentFuelPayload.seal(new ItemStack(ModItems.COOLED_SPENT_FUEL_ASSEMBLY.get()))),"旧乏燃料封装失效");
        System.out.println("[accident] corium mine -> assembly 12800/save-resume -> cask .2 -> rack 0; original payload still valid");h.succeed();
    }
    @GameTest(template=TEMPLATE,timeoutTicks=130)
    public static void adjacentReactorKeepsItsOwnFuelAndReceivesStructureInvalidation(GameTestHelper h) {
        if(skipFixture(h,"ownership"))return;
        var a=build(h,BASE,5);var b=build(h,BASE.offset(4,0,0),5);
        ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),a.getBlockPos());
        port(h,BASE,5,0,0).setFuelAssembly(used());var otherPort=port(h,BASE.offset(4,0,0),5,0,0);var original=used();
        original.set(DataComponents.CUSTOM_NAME,Component.literal("邻堆唯一原件"));otherPort.setFuelAssembly(original);
        var ledger=trigger(h,a,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);
        var shared=h.absolutePos(BASE.offset(4,2,2));
        h.assertTrue(ledger.actions.stream().anyMatch(action->action.pos.equals(shared)&&action.external),"共享壳没有进入本次排定目标，不能要求邻堆损坏");
        h.assertTrue(ledger.knownOwners.contains(b.accidentGeneration()),"创建时邻堆世代没有冻结");
        h.assertTrue(ReactorAccidentLedger.load(ledger.save(h.getLevel().registryAccess()),h.getLevel().registryAccess()).knownOwners.contains(b.accidentGeneration()),"已存在邻堆世代保存丢失");
        var newBase=BASE.offset(18,0,0);var c=build(h,newBase,5);port(h,newBase,5,0,0).setFuelAssembly(used());
        var beforeNew=trigger(h,c,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);var d=build(h,newBase.offset(4,0,0),5);
        var newShared=h.absolutePos(newBase.offset(4,2,2));
        h.assertTrue(!beforeNew.knownOwners.contains(d.accidentGeneration()),"后建机组错误写入冻结邻堆名单");
        h.runAfterDelay(30,()->{
            h.assertTrue(b.accidentId()==null&&ReactorAccidentSavedData.get(h.getLevel()).byGeneration(b.accidentGeneration())==null,"邻堆被直接设为事故");
            h.assertTrue(!b.structureValid(),"flags18后没有向邻堆失效实际破损结构");
            h.assertTrue(h.getLevel().getBlockState(shared).isAir(),"排定共享壳没有实际删除");
            var retained=otherPort.fuelAssembly();
            // 共享壳真正破裂前邻堆可正常燃耗；只核对真实单件身份/原件标记，不把合法耐久变化当吞料。
            h.assertTrue(!retained.isEmpty()&&retained.getCount()==1&&retained.is(ModItems.FRESH_FUEL_ASSEMBLY.get())
                    &&original.get(DataComponents.CUSTOM_NAME).equals(retained.get(DataComponents.CUSTOM_NAME)),"本事故吞掉邻堆真实组件");
            h.assertTrue(RadiationItemSources.describe(otherPort.fuelAssembly(),RadiationParameters.DEFAULT).dosePerSecond()==1,"邻堆源被变成事故废料或漏掉");
            h.assertTrue(d.structureValid()&&d.accidentId()==null&&h.getLevel().getBlockState(newShared).is(P1Blocks.REACTOR_CASING.get()),"旧任务按位置破坏事故后新建机组");
            System.out.println("[accident] adjacent reactor own fuel preserved, shared wall damage invalidates structure without forcing meltdown");h.succeed();
        });
    }
    @GameTest(template=TEMPLATE,batch="accident_persistence",timeoutTicks=180)
    public static void normalStopPendingCustodyResumesFromSavedWorld(GameTestHelper h) {
        if(System.getenv("CNI_ACCIDENT_FOCUS")!=null){System.out.println("[accident] focused validation: SKIP persistence fixture");h.succeed();return;}
        var data=ReactorAccidentSavedData.get(h.getLevel());
        var prior=data.records().stream().filter(l->l.coldMb==77711&&l.hotMb==1234&&!l.complete()).findFirst();
        if(prior.isPresent()) {
            var ledger=prior.get();var column=ledger.columns.getFirst();
            h.assertTrue(column.captured()&&!column.settled()&&column.payload().getCount()==1,"正常停服后托管丢失或错误结算");
            // 测试控制端模拟玩家重新到场，显式载入自己的独立测试区块；生产执行器不持有票据。
            h.getLevel().getChunkSource().getChunk(column.bottom.getX()>>4,column.bottom.getZ()>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);
            h.assertTrue(h.getLevel().getBlockState(column.bottom).is(Blocks.BEDROCK),"原生区块没有保存阻塞目标");
            var before=RadiationSourceIndex.get(h.getLevel()).sampleAt(h.getLevel(),Vec3.atCenterOf(column.bottom),RadiationParameters.DEFAULT);
            h.assertTrue(before.complete()&&before.fieldDosePerSecond()==4,"原生重进后托管源漏计");
            h.getLevel().setBlock(column.bottom,Blocks.AIR.defaultBlockState(),3);
            h.runAfterDelay(8,()->{
                h.assertTrue(ledger.complete()&&h.getLevel().getBlockEntity(column.bottom) instanceof ReactorCoriumBlockEntity,"原生重进后未续接残骸");
                var after=RadiationSourceIndex.get(h.getLevel()).sampleAt(h.getLevel(),Vec3.atCenterOf(column.bottom),RadiationParameters.DEFAULT);
                h.assertTrue(after.complete()&&after.fieldDosePerSecond()==4,"托管移交残骸源强不连续");
                System.out.println("[accident] NATIVE_RESTART_RESUME_OK id="+ledger.id+" before=4 after=4 one payload");h.succeed();
            });return;
        }
        h.assertTrue(!"1".equals(System.getenv("CNI_ACCIDENT_RESUME_ONLY")),"续接专轮没有读到前次正常停服的待办账本");
        var owner=build(h,BASE,5);port(h,BASE,5,0,0).setFuelAssembly(used());owner.setCoolantInventories(77711,1234);
        h.setBlock(BASE.offset(1,1,1),Blocks.BEDROCK);
        var ledger=trigger(h,owner,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);var column=ledger.columns.getFirst();
        h.runAfterDelay(30,()->{
            h.assertTrue(!ledger.complete()&&column.captured()&&!column.settled()&&column.payload().getCount()==1,"异常目标吞掉真实托管");
            h.assertTrue(h.getLevel().getBlockState(column.bottom).is(Blocks.BEDROCK),"事故修改不可破坏目标");
            System.out.println("[accident] NATIVE_SAVE_PENDING id="+ledger.id+" payload=1 blocked=bedrock");h.succeed();
        });
    }
    private static void awaitNativeUnload(GameTestHelper h,java.util.concurrent.atomic.AtomicInteger count,int attempts,Runnable ready) {
        h.getLevel().getChunkSource().tick(()->true,false);
        if(count.get()>0){ready.run();return;}
        if(attempts<=0){System.out.println("[accident] physical ChunkEvent.Unload NOT observed; this fixture proves actual FULL unavailability only");ready.run();return;}
        h.runAfterDelay(20,()->awaitNativeUnload(h,count,attempts-1,ready));
    }
    @GameTest(template=TEMPLATE,batch="accident_chunk",timeoutTicks=2600)
    public static void realFullChunkUnavailabilityRetainsCustodyThenResumes(GameTestHelper h) {
        if(skipFixture(h,"chunks"))return;
        var anchor=h.absolutePos(BlockPos.ZERO);
        var base=new BlockPos(24-(anchor.getX()&15),2,24-(anchor.getZ()&15));
        var owner=build(h,base,16);for(var p:owner.boundPorts(ReactorPortBlockEntity.BindingType.REFUELING))p.setFuelAssembly(used());
        var ledger=trigger(h,owner,ReactorMeltdownEvent.Reason.COUNTDOWN_COMPLETE);
        var ownerChunk=new net.minecraft.world.level.ChunkPos(owner.getBlockPos());var far=h.absolutePos(base.offset(14,1,14));
        var ticket=net.minecraft.server.level.TicketType.<net.minecraft.world.level.ChunkPos>create("cni_accident_probe",Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong));
        h.getLevel().getChunkSource().addRegionTicket(ticket,ownerChunk,0,ownerChunk);
        var unloaded=new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Consumer<net.neoforged.neoforge.event.level.ChunkEvent.Unload> listener=e->{
            if(e.getLevel()==h.getLevel()&&e.getChunk().getPos().x==(far.getX()>>4)&&e.getChunk().getPos().z==(far.getZ()>>4))unloaded.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(listener);
        for(long packed:h.getLevel().getForcedChunks().toLongArray()){var c=new net.minecraft.world.level.ChunkPos(packed);h.getLevel().setChunkForced(c.x,c.z,false);}
        for(int i=0;i<3;i++)h.getLevel().getChunkSource().tick(()->true,false);
        h.assertTrue(!h.getLevel().hasChunkAt(far),"真实远端FULL没有撤销，不能冒称卸载场景");
        h.runAfterDelay(355,()->awaitNativeUnload(h,unloaded,10,()->{
            try {
                h.assertTrue(!ledger.complete()&&ledger.columns.stream().anyMatch(c->!c.settled()),"不可用远端被跳过或吞掉");
                h.assertTrue(!h.getLevel().hasChunkAt(far),"生产事故执行器强载远端");
                var min=h.absolutePos(base);for(int x=min.getX()>>4;x<=(min.getX()+15)>>4;x++)for(int z=min.getZ()>>4;z<=(min.getZ()+15)>>4;z++)h.getLevel().setChunkForced(x,z,true);
                h.runAfterDelay(130,()->{
                    h.assertTrue(ledger.complete()&&ledger.columns.stream().allMatch(ReactorAccidentLedger.Column::settled),"真实卸载重载后事故不能续接");
                    h.getLevel().getChunkSource().removeRegionTicket(ticket,ownerChunk,0,ownerChunk);
                    System.out.println("[accident] FULL_CHUNK_AVAILABILITY_RESUME_OK physicalUnloadEvents="+unloaded.get()+" payloads=196 no executor chunk ticket");h.succeed();
                });
            } finally {NeoForge.EVENT_BUS.unregister(listener);}
        }));
    }
}
