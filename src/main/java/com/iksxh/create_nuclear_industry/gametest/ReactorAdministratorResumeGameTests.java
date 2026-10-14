package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.automation.*;
import com.iksxh.create_nuclear_industry.automation.gui.*;
import com.iksxh.create_nuclear_industry.blockentity.*;
import com.iksxh.create_nuclear_industry.content.*;
import com.iksxh.create_nuclear_industry.reactor.*;
import com.iksxh.create_nuclear_industry.structure.*;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** 隔离世界的真实BE/NBT重建代表；不把回环重建等同独立进程重启或用户存档验收。 */
@GameTestHolder("cni_reactor_administrator_resume") @PrefixGameTestTemplate(false)
public final class ReactorAdministratorResumeGameTests {
    private static final BlockPos BASE=new BlockPos(3,1,4),OWNER=BASE.offset(2,2,0),PORT=BASE.offset(0,2,2),BURNER=PORT.west();
    private static final CoreColumnPosition FUEL=new CoreColumnPosition(1,1),A=new CoreColumnPosition(1,0),B=new CoreColumnPosition(2,1);
    private ReactorAdministratorResumeGameTests() { }
    private static ReactorInstrumentPortBlockEntity setup(GameTestHelper h) {
        return setup(h,false);
    }
    private static ReactorInstrumentPortBlockEntity setup(GameTestHelper h,boolean emptySecondFuel) {
        var columns=new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());columns.replaceAll((p,v)->ReactorStructureDefinition.ColumnType.EMPTY);
        columns.put(FUEL,ReactorStructureDefinition.ColumnType.FUEL);columns.put(A,ReactorStructureDefinition.ColumnType.CONTROL_ROD);columns.put(B,ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        if(emptySecondFuel)columns.put(new CoreColumnPosition(2,0),ReactorStructureDefinition.ColumnType.FUEL);
        for(var entry:ReactorStructureDefinition.templateFor(columns).entrySet()) {var p=entry.getKey();h.setBlock(BASE.offset(p.x(),p.y(),p.z()),BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.getValue())).defaultBlockState());}
        h.setBlock(PORT,ReactorAutomationContent.PORT.get());h.setBlock(BURNER,AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.SMOULDERING));
        h.assertTrue(BlazeBurnerBlock.tryInsert(h.getBlockState(BURNER),h.getLevel(),h.absolutePos(BURNER),new ItemStack(Items.COAL),false,false,false).getResult().consumesAction(),"普通燃料真实点燃失败");
        var support=BURNER.north();h.setBlock(support,Blocks.STONE);h.setBlock(support.above(),Blocks.REDSTONE_TORCH);
        for(var p:List.of(support.above().north(),support.above().north(2),new BlockPos(1,4,3),new BlockPos(2,4,3),new BlockPos(3,4,3),new BlockPos(4,4,3),new BlockPos(5,4,3))) {h.setBlock(p.below(),Blocks.STONE);h.setBlock(p,Blocks.REDSTONE_WIRE);}
        ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));
        var o=(ReactorInstrumentPortBlockEntity)h.getBlockEntity(OWNER);
        h.assertTrue(((ReactorPortBlockEntity)h.getBlockEntity(BASE.offset(2,4,2))).tryInsertFuel(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())).success(),"真实换料端口入料失败");
        o.setCoolantInventories(o.coolantCapacityMb()/2,o.coolantCapacityMb()/10);return o;
    }
    private static ServerPlayer player(GameTestHelper h) {
        var profile=new GameProfile(UUID.randomUUID(),"ResumeAdmin");var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        p.connection=FakePlayerFactory.get(h.getLevel(),profile).connection;p.setGameMode(GameType.SURVIVAL);
        var pos=h.absolutePos(BURNER);p.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);return p;
    }
    private static void command(GameTestHelper h,ServerPlayer p,ReactorInstrumentPortBlockEntity o,ReactorAdministratorCommands.Action action,ReactorAdministratorCommands.Draft draft,long sequence) {
        var binding=o.automationBinding();var menu=(ReactorAdministratorMenu)p.containerMenu;
        var request=new ReactorAdministratorCommands.Request(menu.containerId,menu.sessionId(),binding.burnerPos(),binding.portPos(),binding.ownerGeneration(),binding.authorityEpoch(),o.automationPort().settingsRevision(),sequence,action,draft);
        var response=ReactorAdministratorCommands.apply(p,request);h.assertTrue(response.accepted(),"当前菜单拒绝："+response.reason());
    }
    /** 真实当前版本save/load和区块实体替换；不写用户世界，不伪装成进程重启。 */
    private static ReactorInstrumentPortBlockEntity reload(GameTestHelper h,ReactorInstrumentPortBlockEntity source,boolean normalSave) {
        if(normalSave)ReactorAutomationResume.prepareNormalShutdown(source);
        var saved=source.saveWithFullMetadata(h.getLevel().registryAccess());var absolute=h.absolutePos(OWNER);
        var next=(ReactorInstrumentPortBlockEntity)BlockEntity.loadStatic(absolute,h.getBlockState(OWNER),saved,h.getLevel().registryAccess());
        h.getLevel().removeBlockEntity(absolute);h.getLevel().setBlockEntity(next);next.onLoad();return next;
    }
    private static void startAfterFive(GameTestHelper h,ReactorInstrumentPortBlockEntity source,ReactorAutomationState.Mode mode) {
        h.runAfterDelay(5,()->{
            var p=player(h);h.assertTrue(ReactorAdministratorMenu.open(p,h.absolutePos(BURNER)),"真实启动菜单未打开");
            command(h,p,source,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(mode,50,2),1);
            command(h,p,source,ReactorAdministratorCommands.Action.START,null,2);p.closeContainer();
        });
    }
    private static void zeroCache(ReactorInstrumentPortBlockEntity o,double depth) {
        var fuels=new TreeMap<>(o.snapshot().fuelColumns());fuels.replaceAll((pos,f)->new FuelColumnState(f.fuelAssembly(),f.integrity(),0,f.fuelBurnRemainder(),0));
        o.setSnapshot(o.snapshot().withColumns(fuels,Map.of(A,new ControlRodColumnState(1,depth,depth,false,0),B,new ControlRodColumnState(1,depth,depth,false,0))));
    }
    @GameTest(template="resume_empty",timeoutTicks=45)
    public static void runtime_native_unload_callback_cannot_use_normal_marker_alone(GameTestHelper h) {
        var source=setup(h);startAfterFive(h,source,ReactorAutomationState.Mode.MANUAL);
        h.runAfterDelay(12,()->{
            h.assertTrue(source.automationState().phase()==ReactorAutomationState.Phase.MANUAL,"运行卸载夹具未启动");
            // 直接走真实BE回调；服务器仍运行，单独事件标记不能授权为正常停服。这不冒称ChunkManager真正卸载。
            ReactorAutomationResume.beginNormalShutdown(h.getLevel().getServer());
            try {h.assertTrue(!ReactorAutomationResume.normalShutdownActive(source),"运行server因单独标记被当作停服");source.onChunkUnloaded();}
            finally {ReactorAutomationResume.endNormalShutdown(h.getLevel().getServer());}
            h.assertTrue(source.automationState().phase()==ReactorAutomationState.Phase.FAULT&&!source.automationState().runPermission()&&source.automationState().reason().equals("sample_lost"),"真实运行卸载回调没有撤权保护");
            var saved=source.saveWithFullMetadata(h.getLevel().registryAccess());
            h.assertTrue(!saved.getCompound(ReactorAutomationResume.KEY).getBoolean("NormalSave")&&source.snapshot().controlRodColumns().values().stream().allMatch(r->r.targetDepth()==1),"运行卸载留下可自动续接检查点或非安全目标");
            System.out.println("[resume-runtime-unload] native BE callback while server running -> sample_lost, no normal checkpoint");h.succeed();
        });
    }
    @GameTest(template="resume_empty",timeoutTicks=75)
    public static void shared_conductor_feedback_has_no_recursive_signal_query(GameTestHelper h) {
        var source=setup(h);startAfterFive(h,source,ReactorAutomationState.Mode.MANUAL);var restored=new ReactorInstrumentPortBlockEntity[1];
        h.runAfterDelay(12,()->{
            ReactorAutomationResume.prepareNormalShutdown(source);var saved=source.saveWithFullMetadata(h.getLevel().registryAccess());var state=h.getBlockState(OWNER);
            // 新仪表与燃烧室共同邻接同一真实石头支撑格；查询仪表输入会回调燃烧室的强信号getter。
            var nextPos=PORT.north();h.setBlock(OWNER,P1Blocks.REACTOR_CASING.get());h.setBlock(nextPos,state);
            restored[0]=(ReactorInstrumentPortBlockEntity)BlockEntity.loadStatic(h.absolutePos(nextPos),state,saved,h.getLevel().registryAccess());
            h.getLevel().removeBlockEntity(h.absolutePos(nextPos));h.getLevel().setBlockEntity(restored[0]);restored[0].onLoad();
            ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(nextPos));
            try {
                restored[0].tickReactor();h.assertTrue(h.getLevel().hasNeighborSignal(h.absolutePos(nextPos)),"共同支撑格没有传递真实高信号");
            } catch(StackOverflowError recursiveSignal) {h.fail("加载反馈getter经共同导体重入红石输入查询");}
        });
        h.runAfterDelay(57,()->{
            h.assertTrue(restored[0].automationState().phase()==ReactorAutomationState.Phase.FAULT&&restored[0].automationState().reason().equals("arming_timeout")&&!ReactorAutomationResume.pending(restored[0]),"真实共同导体高信号被吞掉或无限等待");
            System.out.println("[resume-conductor] native common stone high -> bounded arming_timeout, no recursive getter");h.succeed();
        });
    }
    @GameTest(template="resume_empty",timeoutTicks=70)
    public static void nuclear_resume_owner_first_no_external_fuel_no_cache(GameTestHelper h) {nuclearResume(h,false);}
    @GameTest(template="resume_empty",timeoutTicks=70)
    public static void nuclear_resume_burner_first_no_external_fuel_no_cache(GameTestHelper h) {nuclearResume(h,true);}
    private static void nuclearResume(GameTestHelper h,boolean burnerFirst) {
        var source=setup(h);startAfterFive(h,source,ReactorAutomationState.Mode.MANUAL);
        var restored=new ReactorInstrumentPortBlockEntity[1];var blaze=new BlazeBurnerBlockEntity[1];
        h.runAfterDelay(12,()->{
            h.assertTrue(source.automationState().phase()==ReactorAutomationState.Phase.MANUAL,"零备用夹具未先真实运行");zeroCache(source,.5);
            var b=(BlazeBurnerBlockEntity)h.getBlockEntity(BURNER);var tag=b.saveWithFullMetadata(h.getLevel().registryAccess());tag.putInt("fuelLevel",BlazeBurnerBlockEntity.FuelType.NONE.ordinal());tag.putInt("burnTimeRemaining",0);b.loadWithComponents(tag,h.getLevel().registryAccess());
            h.assertTrue(ReactorAutomationHeatSupply.dutyValid(b),"保存前缺少实际短期核热资格");
            restored[0]=reload(h,source,true);
            var absolute=h.absolutePos(BURNER);blaze[0]=(BlazeBurnerBlockEntity)BlockEntity.loadStatic(absolute,h.getBlockState(BURNER),tag,h.getLevel().registryAccess());
            h.getLevel().removeBlockEntity(absolute);h.getLevel().setBlockEntity(blaze[0]);blaze[0].onLoad();
            ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));
            // 独立高输入把窗口保持到受控16tick；结算冻结，不靠旧KINDLED/票据供能。
            h.setBlock(OWNER.north(),Blocks.REDSTONE_BLOCK);var before=restored[0].snapshot();
            if(burnerFirst)blaze[0].tick();restored[0].tickReactor();if(!burnerFirst)blaze[0].tick();
            h.assertTrue(ReactorAutomationResume.pending(restored[0])&&restored[0].snapshot().coldCoolantMb()==before.coldCoolantMb()&&restored[0].snapshot().hotCoolantMb()==before.hotCoolantMb(),"握手期间结算库存");
            h.assertTrue(restored[0].snapshot().controlRodColumns().equals(before.controlRodColumns())&&restored[0].automationHeatAccount().paidHeatHu()==0&&!ReactorAutomationHeatSupply.displayingNuclear(blaze[0]),"冻结阶段提棒/扣热或免费核燃烧");
            h.assertTrue(ReactorAutomationBlazeBridge.runPermission(h.getLevel(),absolute)==15&&!restored[0].automationState().runPermission(),"未区分当前反馈许可与正式运行权");
        });
        h.runAfterDelay(16,()->{
            var o=restored[0];var b=blaze[0];h.assertTrue(ReactorAutomationResume.pending(o),"受控高信号未保持冻结窗口");h.setBlock(OWNER.north(),Blocks.AIR);
            h.assertTrue(!h.getLevel().hasNeighborSignal(h.absolutePos(OWNER)),"真实反相火把未降沿");
            if(burnerFirst) {b.tick();h.assertTrue(!ReactorAutomationHeatSupply.displayingNuclear(b),"先燃烧室顺序在付款前免费热级");}
            o.tickReactor();b.tick();
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.MANUAL&&o.automationState().runPermission()&&!ReactorAutomationResume.pending(o),"当前付费没有恢复原手动意图");
            h.assertTrue(o.automationHeatAccount().paidHeatHu()==1&&o.automationHeatAccount().generatedExpenseHu()==1&&o.automationHeatAccount().cachedExpenseHu()==0,"首笔核热不是从当前真实裂变支出1HU");
            h.assertTrue(b.getRemainingBurnTime()==0&&ReactorAutomationHeatSupply.displayingNuclear(b),"正常续接要求额外燃料或未消费当前收据");
            System.out.println("[resume-first-payment] order="+(burnerFirst?"burner-first":"owner-first")+" external=0 cache=0 actualCost=1 generated=1");
        });
        h.runAfterDelay(25,()->{
            h.assertTrue(restored[0].automationState().phase()==ReactorAutomationState.Phase.MANUAL&&blaze[0].getRemainingBurnTime()==0,"纯核热续接不能持续");
            h.assertTrue(restored[0].snapshot().controlRodColumns().values().stream().allMatch(r->r.targetDepth()==.5),"续接统一改写保存棒位");h.succeed();
        });
    }
    @GameTest(template="resume_empty",timeoutTicks=65)
    public static void constant_age_autosave_and_non_administrator_contract(GameTestHelper h) {
        var source=setup(h);startAfterFive(h,source,ReactorAutomationState.Mode.CONSTANT);
        h.runAfterDelay(12,()->{
            h.assertTrue(source.automationState().phase()==ReactorAutomationState.Phase.CONSTANT,"恒功率未建立原生联锁");zeroCache(source,.65);
            source.setAutomationState(new ReactorAutomationState(true,ReactorAutomationState.Phase.CONSTANT,"regulating",65,source.automationState().authorityEpoch(),9,0,0,0,ReactorAutomationState.Mode.CONSTANT,true));
            var o=reload(h,source,true);ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));o.tickReactor();
            h.assertTrue(ReactorAutomationResume.pending(o)&&o.snapshot().controlRodColumns().values().stream().allMatch(r->r.actualDepth()==.65&&r.targetDepth()==.65),"恒功率初始化改写原棒位");
            h.assertTrue(!h.getLevel().hasNeighborSignal(h.absolutePos(OWNER)),"保存前恒功率线路不是低反馈");o.tickReactor();
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.CONSTANT&&o.automationState().adjustmentAge()==9&&o.automationState().commandedDepthPercent()==65&&o.automationPort().settings().targetHeatHuPerTick()==2,"恒功率续接丢原调节进度或目标");
            var ordinary=reload(h,o,false);h.assertTrue(!ReactorAutomationResume.pending(ordinary)&&ordinary.automationState().phase()==ReactorAutomationState.Phase.STOPPED&&!ordinary.automationState().runPermission(),"普通autosave恢复旧运行意图");
            ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));ordinary.tickReactor();
            h.setBlock(PORT,P1Blocks.REACTOR_CASING.get());ordinary.setAutomationState(ReactorAutomationState.initial());ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));
            h.setBlock(BURNER.north().above(),Blocks.AIR);ordinary.updateRedstoneScram(false);zeroCache(ordinary,0);
            ordinary.setSnapshot(ordinary.snapshot().withScramState(Map.of(),false));ordinary.tickReactor();
            h.assertTrue(!ordinary.automationState().claimed()&&ordinary.telemetry().totalGeneratedFissionHeatHuPerTick()>0&&ordinary.automationHeatAccount().paidHeatHu()==0,"未任职反应堆被加载机制冻结或付核费");
            System.out.println("[resume-constant] age=9 command=65 target=2 preserved; autosave STOP; unassigned original fission");h.succeed();
        });
    }
    @GameTest(template="resume_empty",timeoutTicks=75)
    public static void current_missing_member_does_not_resume(GameTestHelper h) {protectedResume(h,"missing","binding_lost");}
    @GameTest(template="resume_empty",timeoutTicks=75)
    public static void current_insufficient_cooling_does_not_resume(GameTestHelper h) {protectedResume(h,"cold","cold_shortage");}
    @GameTest(template="resume_empty",timeoutTicks=75)
    public static void current_high_input_times_out_without_frozen_forever(GameTestHelper h) {protectedResume(h,"high","arming_timeout");}
    private static void protectedResume(GameTestHelper h,String failure,String expected) {
        var source=setup(h);startAfterFive(h,source,ReactorAutomationState.Mode.MANUAL);var restored=new ReactorInstrumentPortBlockEntity[1];
        h.runAfterDelay(12,()->{
            restored[0]=reload(h,source,true);
            if(failure.equals("missing"))h.setBlock(BURNER,Blocks.AIR);
            if(failure.equals("cold"))restored[0].setCoolantInventories(0,restored[0].snapshot().hotCoolantMb());
            if(failure.equals("high"))h.setBlock(OWNER.north(),Blocks.REDSTONE_BLOCK);
            ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));restored[0].tickReactor();
        });
        h.runAfterDelay(failure.equals("high")?57:18,()->{
            var o=restored[0];h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.FAULT&&!o.automationState().runPermission()&&!ReactorAutomationResume.pending(o),"当前故障未终止待续接："+failure);
            h.assertTrue(o.automationState().reason().equals(expected),"错误保护原因："+o.automationState().reason());
            h.assertTrue(o.snapshot().controlRodColumns().values().stream().allMatch(r->r.targetDepth()==1),"续接失败未回原安全基准");
            System.out.println("[resume-protection] "+failure+" -> "+expected+", no permission");h.succeed();
        });
    }
    @GameTest(template="resume_empty",timeoutTicks=60)
    public static void pending_menu_stop_cancels_and_apply_is_atomic_rejected(GameTestHelper h) {
        var source=setup(h);startAfterFive(h,source,ReactorAutomationState.Mode.MANUAL);var stopped=new ReactorInstrumentPortBlockEntity[1];
        h.runAfterDelay(12,()->{
            var o=reload(h,source,true);h.setBlock(OWNER.north(),Blocks.REDSTONE_BLOCK);ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));o.tickReactor();
            var p=player(h);h.assertTrue(ReactorAdministratorMenu.open(p,h.absolutePos(BURNER)),"等待期间不能查看/停堆");var binding=o.automationBinding();var menu=(ReactorAdministratorMenu)p.containerMenu;var settings=o.automationPort().settings();
            var apply=new ReactorAdministratorCommands.Request(menu.containerId,menu.sessionId(),binding.burnerPos(),binding.portPos(),binding.ownerGeneration(),binding.authorityEpoch(),o.automationPort().settingsRevision(),1,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,30,3));
            h.assertTrue(!ReactorAdministratorCommands.apply(p,apply).accepted()&&o.automationPort().settings().equals(settings)&&ReactorAutomationResume.pending(o),"未授当前权时APPLY部分提交或取消原意图");
            command(h,p,o,ReactorAdministratorCommands.Action.STOP,null,2);h.assertTrue(!ReactorAutomationResume.pending(o)&&o.automationState().phase()==ReactorAutomationState.Phase.STOPPED&&!o.automationState().runPermission(),"原STOP未取消待续接");p.closeContainer();
            o.updateRedstoneScram(h.getLevel().hasNeighborSignal(h.absolutePos(OWNER)));h.assertTrue(o.snapshot().scramRequested(),"停堆夹具未先保存真实高输入的SCRAM");
            var next=reload(h,o,true);next.updateRedstoneScram(false);
            h.assertTrue(next.automationState().phase()==ReactorAutomationState.Phase.STOPPED&&!ReactorAutomationResume.pending(next)&&next.snapshot().scramRequested(),"停堆安全高读档改为FAULT或初始化释放SCRAM");
            stopped[0]=next;next.tickReactor();
            h.assertTrue(next.automationState().phase()==ReactorAutomationState.Phase.STOPPED&&next.snapshot().scramRequested(),"已保存STOP在首次结构加载之前变成sample_lost故障");
        });
        h.runAfterDelay(18,()->{
            var next=stopped[0];h.assertTrue(next.structureValid()&&next.automationBinding()!=null&&next.automationState().phase()==ReactorAutomationState.Phase.STOPPED&&!next.automationState().runPermission()&&next.snapshot().scramRequested(),"已保存STOP首次真实重扫/绑定后变成结构故障或释放SCRAM");
            System.out.println("[resume-stop] pending APPLY denied; STOP cancels; saved STOP/SCRAM held before and after actual first scan/binding");h.succeed();
        });
    }
    @GameTest(template="resume_empty",timeoutTicks=80)
    public static void normal_manual_nbt_preserves_rods_and_resumes_without_start(GameTestHelper h) {
        var source=setup(h);var restored=new ReactorInstrumentPortBlockEntity[1];
        h.runAfterDelay(5,()->{
            var p=player(h);h.assertTrue(ReactorAdministratorMenu.open(p,h.absolutePos(BURNER)),"启动菜单未打开");
            command(h,p,source,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,50,3),1);
            command(h,p,source,ReactorAdministratorCommands.Action.START,null,2);p.closeContainer();
        });
        h.runAfterDelay(12,()->{
            h.assertTrue(source.automationState().phase()==ReactorAutomationState.Phase.MANUAL,"保存前未真实运行");
            var rods=Map.of(A,new ControlRodColumnState(1,.3,.4,false,0),B,new ControlRodColumnState(1,.7,.8,false,0));
            var fuels=new TreeMap<>(source.snapshot().fuelColumns());fuels.replaceAll((pos,f)->new FuelColumnState(f.fuelAssembly(),f.integrity(),0,f.fuelBurnRemainder(),0));
            source.setSnapshot(source.snapshot().withColumns(fuels,rods));
            source.setAutomationState(new ReactorAutomationState(true,ReactorAutomationState.Phase.MANUAL,"manual",50,source.automationState().authorityEpoch(),7,0,0,0,ReactorAutomationState.Mode.MANUAL,true));
            var previousGeneration=source.automationGeneration();restored[0]=reload(h,source,true);
            h.assertTrue(restored[0].automationState().phase()==ReactorAutomationState.Phase.ARMING&&!restored[0].automationState().runPermission(),"正常保存未进入无运行授权的原意图交接");
            h.assertTrue(!restored[0].automationGeneration().equals(previousGeneration)&&restored[0].automationBinding()==null,"读档恢复了旧实例授权");
            h.assertTrue(restored[0].snapshot().controlRodColumns().equals(rods),"读NBT改写逐棒actual/target");
            restored[0].tickReactor();h.assertTrue(restored[0].snapshot().controlRodColumns().equals(rods),"等待首次结构加载仍推进或全插棒");
        });
        h.runAfterDelay(24,()->{
            var o=restored[0];h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.MANUAL&&o.automationState().runPermission(),"健康原意图未自动续接");
            h.assertTrue(o.snapshot().controlRodColumns().get(A).targetDepth()==.3&&o.snapshot().controlRodColumns().get(B).targetDepth()==.7,"原逐棒差异被统一或先全插后提棒");
            System.out.println("[administrator-resume] native NBT/BE MANUAL continued, individual rods preserved");h.succeed();
        });
    }
    @GameTest(template="resume_empty",timeoutTicks=50)
    public static void saved_fault_keeps_reason_and_rejects_start(GameTestHelper h) {
        var source=setup(h);
        h.runAfterDelay(5,()->{
            source.setAutomationState(source.automationState().stop("hot_trip",true));var next=reload(h,source,true);
            h.assertTrue(next.automationState().phase()==ReactorAutomationState.Phase.FAULT&&next.automationState().reason().equals("hot_trip")&&!next.automationState().runPermission(),"故障读档变成STOP或丢失保持原因");
            ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));next.tickReactor();
            h.assertTrue(next.automationState().phase()==ReactorAutomationState.Phase.FAULT&&next.automationState().reason().equals("hot_trip"),"首次绑定抹掉保存故障");
            var p=player(h);h.assertTrue(ReactorAdministratorMenu.open(p,h.absolutePos(BURNER)),"故障菜单不可查看");
            var binding=next.automationBinding();var menu=(ReactorAdministratorMenu)p.containerMenu;
            var request=new ReactorAdministratorCommands.Request(menu.containerId,menu.sessionId(),binding.burnerPos(),binding.portPos(),binding.ownerGeneration(),binding.authorityEpoch(),next.automationPort().settingsRevision(),1,ReactorAdministratorCommands.Action.START,null);
            h.assertTrue(!ReactorAdministratorCommands.apply(p,request).accepted(),"保存故障允许不复位直接START");p.closeContainer();h.succeed();
        });
    }
    /** 同次审查的四个真实边界；独立域复用逐字节空模板，原11场景保留但不机械重复运行。 */
    @GameTestHolder("cni_reactor_administrator_resume_review") @PrefixGameTestTemplate(false)
    public static final class ReviewWindows {
        private static final CoreColumnPosition EMPTY_FUEL=new CoreColumnPosition(2,0);
        private static final BlockPos EMPTY_REFUEL=BASE.offset(3,4,1);
        private ReviewWindows() { }
        @GameTest(template="review_empty",timeoutTicks=65)
        public static void sparse_running_layout_preserves_unloaded_fuel_column(GameTestHelper h) {sparse(h,false);}
        @GameTest(template="review_empty",timeoutTicks=65)
        public static void sparse_stopped_layout_preserves_unloaded_fuel_column(GameTestHelper h) {sparse(h,true);}
        private static void sparse(GameTestHelper h,boolean stop) {
            var source=setup(h,true);startAfterFive(h,source,ReactorAutomationState.Mode.MANUAL);var restored=new ReactorInstrumentPortBlockEntity[1];
            h.runAfterDelay(12,()->{
                h.assertTrue(source.automationState().phase()==ReactorAutomationState.Phase.MANUAL&&source.snapshot().fuelColumns().size()==1&&!source.snapshot().fuelColumns().containsKey(EMPTY_FUEL),"真实运行前燃料账本不是合法稀疏表示");
                h.assertTrue(((ReactorPortBlockEntity)h.getBlockEntity(EMPTY_REFUEL)).fuelAssembly().isEmpty(),"未装料真实端口意外有组件");
                if(stop) {
                    var p=player(h);h.assertTrue(ReactorAdministratorMenu.open(p,h.absolutePos(BURNER)),"稀疏STOP菜单未打开");command(h,p,source,ReactorAdministratorCommands.Action.STOP,null,1);p.closeContainer();
                }
                restored[0]=reload(h,source,true);ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));restored[0].tickReactor();
                h.assertTrue(stop?restored[0].automationState().phase()==ReactorAutomationState.Phase.STOPPED:ReactorAutomationResume.pending(restored[0]),"合法未装料FUEL被误判成structure_changed");
            });
            h.runAfterDelay(24,()->{
                var o=restored[0];h.assertTrue(o.structureValid()&&o.automationState().phase()==(stop?ReactorAutomationState.Phase.STOPPED:ReactorAutomationState.Phase.MANUAL),"合法稀疏机组未保持原运行/停止分类");
                h.assertTrue(o.snapshot().fuelColumns().size()==1&&!o.snapshot().fuelColumns().containsKey(EMPTY_FUEL)&&((ReactorPortBlockEntity)h.getBlockEntity(EMPTY_REFUEL)).fuelAssembly().isEmpty(),"加载补造了空FuelColumnState或物品");
                h.assertTrue(o.automationState().runPermission()==!stop,"稀疏机组的运行许可分类错误");
                var acceptedPhase=o.automationState().phase();
                if(stop)checkCorruptLayoutEvidence(h,o);
                System.out.println("[resume-review-sparse] physicalFUEL=2 nuclearState=1 emptyPort=0 acceptedPhaseBeforeCorruptChecks="+acceptedPhase);h.succeed();
            });
        }
        private static void checkCorruptLayoutEvidence(GameTestHelper h,ReactorInstrumentPortBlockEntity stopped) {
            ReactorAutomationResume.prepareNormalShutdown(stopped);var good=stopped.saveWithFullMetadata(h.getLevel().registryAccess());
            for(String kind:List.of("missing","duplicate","wrong_type","outside")) {
                var tag=good.copy();var list=tag.getList("BreakSafetyFuelColumns",net.minecraft.nbt.Tag.TAG_COMPOUND);
                if(kind.equals("missing"))tag.remove("BreakSafetyFuelColumns");
                if(kind.equals("duplicate"))list.add(list.getCompound(0).copy());
                if(kind.equals("wrong_type"))list.getCompound(0).putString("X","0");
                if(kind.equals("outside"))list.getCompound(0).putInt("X",16);
                var absolute=h.absolutePos(OWNER);var next=(ReactorInstrumentPortBlockEntity)BlockEntity.loadStatic(absolute,h.getBlockState(OWNER),tag,h.getLevel().registryAccess());
                h.getLevel().removeBlockEntity(absolute);h.getLevel().setBlockEntity(next);next.onLoad();ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),absolute);next.tickReactor();
                h.assertTrue(next.automationState().phase()==ReactorAutomationState.Phase.FAULT&&!next.automationState().runPermission()&&!ReactorAutomationResume.pending(next)&&next.automationHeatAccount().paidHeatHu()==0,"缺失/坏本版本完整FUEL证据被认可："+kind);
            }
            System.out.println("[resume-review-evidence] missing/duplicate/wrong_type/outside -> FAULT, no receipt");
        }
        @GameTest(template="review_empty",timeoutTicks=50)
        public static void saved_physical_fuel_layout_rejects_true_column_removal(GameTestHelper h) {
            var source=setup(h,true);startAfterFive(h,source,ReactorAutomationState.Mode.MANUAL);
            h.runAfterDelay(12,()->{
                var next=reload(h,source,true);
                // 从未装料的真实整列改为空腔；新几何仍合法，但不能用稀疏核账本掩盖原保存物理布局变化。
                for(int y=1;y<=3;y++)h.setBlock(BASE.offset(3,y,1),Blocks.AIR);h.setBlock(EMPTY_REFUEL,P1Blocks.REACTOR_CASING.get());
                ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));h.assertTrue(next.structureValid(),"删除整列后的几何夹具不合法");next.tickReactor();
                h.assertTrue(next.automationState().phase()==ReactorAutomationState.Phase.FAULT&&next.automationState().reason().equals("structure_changed")&&!ReactorAutomationResume.pending(next)&&!next.automationState().runPermission(),"真实删除未装料列被覆盖后的保存集认可");
                h.assertTrue(next.automationHeatAccount().paidHeatHu()==0&&next.snapshot().controlRodColumns().values().stream().allMatch(r->r.targetDepth()==1),"真实列变化仍首付或未回安全基准");
                System.out.println("[resume-review-layout] savedFUEL=2 currentFUEL=1 -> structure_changed, firstFee=0");h.succeed();
            });
        }
        @GameTest(template="review_empty",timeoutTicks=60)
        public static void feedback_window_fixes_generation_before_extra_cold_port(GameTestHelper h) {
            var source=setup(h);startAfterFive(h,source,ReactorAutomationState.Mode.MANUAL);var restored=new ReactorInstrumentPortBlockEntity[1];
            h.runAfterDelay(12,()->{
                var next=reload(h,source,true);restored[0]=next;h.setBlock(OWNER.north(),Blocks.REDSTONE_BLOCK);
                ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));next.tickReactor();var generation=next.automationGeneration();
                h.assertTrue(ReactorAutomationResume.pending(next)&&ReactorAutomationBlazeBridge.runPermission(h.getLevel(),h.absolutePos(BURNER))==15&&!next.automationState().runPermission(),"未建立真实无正式许可的反馈窗口");
                h.setBlock(BASE.offset(4,2,2),P1Blocks.REACTOR_COLD_PORT.get());ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));
                h.assertTrue(next.structureValid()&&!generation.equals(next.automationGeneration()),"合法额外冷端未触发真实结构代际变化");
                h.assertTrue(next.automationState().phase()==ReactorAutomationState.Phase.FAULT&&next.automationState().reason().equals("structure_changed")&&!ReactorAutomationResume.pending(next)&&ReactorAutomationBlazeBridge.runPermission(h.getLevel(),h.absolutePos(BURNER))==0,"等待期代际变更仍保留旧反馈/绑定");
                h.assertTrue(next.automationHeatAccount().paidHeatHu()==0&&next.snapshot().controlRodColumns().values().stream().allMatch(r->r.targetDepth()==1),"旧代际首付或继续原浅目标");
            });
            h.runAfterDelay(18,()->{
                h.setBlock(OWNER.north(),Blocks.AIR);var o=restored[0];o.tickReactor();h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.FAULT&&!o.automationState().runPermission()&&!ReactorAutomationResume.pending(o),"真实后续低沿让旧代际恢复运行");
                System.out.println("[resume-review-generation] current valid extra cold port -> new generation, feedback revoked before firstFee");h.succeed();
            });
        }
    }
}
