package com.iksxh.create_nuclear_industry.gametest;

import com.iksxh.create_nuclear_industry.automation.*;
import com.iksxh.create_nuclear_industry.automation.gui.*;
import com.iksxh.create_nuclear_industry.blockentity.*;
import com.iksxh.create_nuclear_industry.content.*;
import com.iksxh.create_nuclear_industry.reactor.*;
import com.iksxh.create_nuclear_industry.structure.*;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity;
import com.simibubi.create.content.processing.burner.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** 四个隔离真实代表；原生表updateFromNetwork控制输入，不冒称已搭热—汽—动力完整机组。 */
@GameTestHolder("cni_reactor_load_follow") @PrefixGameTestTemplate(false)
public final class ReactorLoadFollowGameTests {
    private static final BlockPos BASE=new BlockPos(3,1,4),OWNER=BASE.offset(2,2,0),PORT=BASE.offset(0,2,2),BURNER=PORT.west(),GAUGE=new BlockPos(1,1,4);
    private static final CoreColumnPosition FUEL=new CoreColumnPosition(1,1),A=new CoreColumnPosition(1,0),B=new CoreColumnPosition(2,1);
    private ReactorLoadFollowGameTests() { }
    private static ReactorInstrumentPortBlockEntity setup(GameTestHelper h) {
        return setup(h,BlockPos.ZERO);
    }
    private static ReactorInstrumentPortBlockEntity setup(GameTestHelper h,BlockPos offset) {
        var base=BASE.offset(offset);var port=PORT.offset(offset);var burner=BURNER.offset(offset);var owner=OWNER.offset(offset);
        var columns=new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());columns.replaceAll((p,v)->ReactorStructureDefinition.ColumnType.EMPTY);
        columns.put(FUEL,ReactorStructureDefinition.ColumnType.FUEL);columns.put(A,ReactorStructureDefinition.ColumnType.CONTROL_ROD);columns.put(B,ReactorStructureDefinition.ColumnType.CONTROL_ROD);
        for(var e:ReactorStructureDefinition.templateFor(columns).entrySet()) {var p=e.getKey();h.setBlock(base.offset(p.x(),p.y(),p.z()),BuiltInRegistries.BLOCK.get(ResourceLocation.parse(e.getValue())).defaultBlockState());}
        h.setBlock(port,ReactorAutomationContent.PORT.get());h.setBlock(burner,AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.SMOULDERING));
        h.assertTrue(BlazeBurnerBlock.tryInsert(h.getBlockState(burner),h.getLevel(),h.absolutePos(burner),new ItemStack(Items.COAL),false,false,false).getResult().consumesAction(),"原生燃料未点燃");
        var support=burner.north();h.setBlock(support,Blocks.STONE);h.setBlock(support.above(),Blocks.REDSTONE_TORCH);
        for(var p:List.of(support.above().north(),support.above().north(2),new BlockPos(1,4,3).offset(offset),new BlockPos(2,4,3).offset(offset),new BlockPos(3,4,3).offset(offset),new BlockPos(4,4,3).offset(offset),new BlockPos(5,4,3).offset(offset))) {h.setBlock(p.below(),Blocks.STONE);h.setBlock(p,Blocks.REDSTONE_WIRE);}
        h.setBlock(GAUGE.offset(offset),AllBlocks.STRESSOMETER.getDefaultState());
        ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(owner));var o=(ReactorInstrumentPortBlockEntity)h.getBlockEntity(owner);
        h.assertTrue(((ReactorPortBlockEntity)h.getBlockEntity(base.offset(2,4,2))).tryInsertFuel(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())).success(),"真实端口装料失败");
        o.setCoolantInventories(o.coolantCapacityMb()/2,o.coolantCapacityMb()/10);return o;
    }
    private static ServerPlayer player(GameTestHelper h) {
        var profile=new GameProfile(UUID.randomUUID(),"LoadFollowAdmin");var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        p.connection=FakePlayerFactory.get(h.getLevel(),profile).connection;p.setGameMode(GameType.SURVIVAL);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(AllItems.WRENCH.get()));move(p,h.absolutePos(BURNER));return p;
    }
    private static void move(ServerPlayer p,BlockPos pos) {p.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);}
    /** 走客户端Ctrl意图对应的同一正式服务，真实玩家持物/距离/世代；不直接写Link。 */
    private static void bind(GameTestHelper h,ServerPlayer p,ReactorInstrumentPortBlockEntity o,long seq) {
        move(p,h.absolutePos(GAUGE));h.assertTrue(ReactorStressGaugeService.apply(p,new ReactorStressGaugeService.Intent(true,h.absolutePos(GAUGE),BlockPos.ZERO,new UUID(0,0),0,seq)).equals("gauge_selected"),"选择真实表失败");
        var b=o.automationBinding();move(p,b.burnerPos());h.assertTrue(ReactorStressGaugeService.apply(p,new ReactorStressGaugeService.Intent(false,b.burnerPos(),b.portPos(),b.ownerGeneration(),b.authorityEpoch(),seq+1)).equals("gauge_bound"),"绑定真实表失败");
    }
    private static ReactorAdministratorCommands.Response command(ServerPlayer p,ReactorInstrumentPortBlockEntity o,ReactorAdministratorCommands.Action action,ReactorAdministratorCommands.Draft draft,long seq) {
        var b=o.automationBinding();var menu=(ReactorAdministratorMenu)p.containerMenu;
        return ReactorAdministratorCommands.apply(p,new ReactorAdministratorCommands.Request(menu.containerId,menu.sessionId(),b.burnerPos(),b.portPos(),b.ownerGeneration(),b.authorityEpoch(),o.automationPort().settingsRevision(),seq,action,draft));
    }
    private static ServerPlayer start(GameTestHelper h,ReactorInstrumentPortBlockEntity o) {
        var p=player(h);bind(h,p,o,1);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        h.assertTrue(ReactorAdministratorMenu.open(p,h.absolutePos(BURNER)),"真实菜单未打开");
        h.assertTrue(command(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.LOAD_FOLLOW,100,0,20,2),1).accepted(),"LOAD草稿未确认");
        var response=command(p,o,ReactorAdministratorCommands.Action.START,null,2);h.assertTrue(response.accepted(),"LOAD启动被拒："+response.reason());return p;
    }
    /** 正式采样仍只读原生表；此受控夹具给Create已加载表注入网络结算，不用getSpeed反推容量。 */
    private static StressGaugeBlockEntity network(GameTestHelper h,float capacity,float load) {
        var g=(StressGaugeBlockEntity)h.getBlockEntity(GAUGE);g.setNetwork(h.absolutePos(GAUGE).asLong());g.setSpeed(16);g.updateFromNetwork(capacity,load,2);return g;
    }
    private static void setDepth(ReactorInstrumentPortBlockEntity o,double depth) {
        var rods=new TreeMap<>(o.snapshot().controlRodColumns());rods.replaceAll((p,r)->new ControlRodColumnState(r.integrity(),depth,depth,r.jammed(),0));
        var fuels=new TreeMap<>(o.snapshot().fuelColumns());fuels.replaceAll((p,f)->new FuelColumnState(f.fuelAssembly(),f.integrity(),0,f.fuelBurnRemainder(),0));o.setSnapshot(o.snapshot().withColumns(fuels,rods));
    }
    @GameTest(template="load_follow_empty",timeoutTicks=80)
    public static void binding_and_controls(GameTestHelper h) {
        var o=setup(h);var second=setup(h,new BlockPos(10,0,0));var player=new ServerPlayer[1];var hiddenPreserved=new boolean[1];var secondRejected=new boolean[1];
        h.runAfterDelay(5,()->{
            network(h,0,0);
            player[0]=player(h);var p=player[0];bind(h,p,o,1);var revision=o.automationPort().settingsRevision();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            h.assertTrue(ReactorAdministratorMenu.open(p,h.absolutePos(BURNER)),"真实菜单未打开");
            var cache=com.iksxh.create_nuclear_industry.automation.client.ReactorAdministratorLayout.DraftFields.confirmed(ReactorAdministratorProjection.from(o,o.automationBinding()).data());
            var other=player(h);other.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);move(other,o.automationBinding().burnerPos());
            h.assertTrue(ReactorAdministratorMenu.open(other,o.automationBinding().burnerPos()),"第二真实菜单未打开");
            h.assertTrue(command(other,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,100,0,37,3),1).accepted(),"另一玩家隐藏偏好未确认");other.closeContainer();
            var latest=ReactorAdministratorProjection.from(o,o.automationBinding()).data();
            cache=cache.synchronize(com.iksxh.create_nuclear_industry.automation.client.ReactorAdministratorLayout.DraftFields.confirmed(latest),ReactorAutomationState.Mode.MANUAL,false);
            var edit=new com.iksxh.create_nuclear_industry.automation.client.ReactorAdministratorLayout.DraftFields("80",cache.heat(),cache.reserve(),cache.minimum());
            h.assertTrue(command(p,o,ReactorAdministratorCommands.Action.APPLY,edit.toDraft(ReactorAutomationState.Mode.MANUAL),1).accepted(),"已收新版本的首次棒深编辑未应用");
            hiddenPreserved[0]=o.automationPort().settings().reservePercent()==37&&o.automationPort().settings().minimumHeatHuPerTick()==3;
            revision=o.automationPort().settingsRevision();var confirmed=o.automationPort().settings();
            var bad=command(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.LOAD_FOLLOW,80,500,100,2),2);
            h.assertTrue(!bad.accepted()&&o.automationPort().settingsRevision()==revision&&o.automationPort().settings().equals(confirmed),"非法完整草稿部分提交");
            h.assertTrue(command(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.LOAD_FOLLOW,80,0,37,2),3).accepted(),"合法原子草稿被拒");
            h.assertTrue(command(p,o,ReactorAdministratorCommands.Action.START,null,4).accepted(),"C0预热启动被拒");
            h.assertTrue(o.automationPort().settings().reservePercent()==37&&o.automationPort().gaugeLink().position().equals(h.absolutePos(GAUGE)),"已确认表/目标没有保留");
            var contender=player(h);bind(h,contender,second,1);contender.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            h.assertTrue(ReactorAdministratorMenu.open(contender,second.automationBinding().burnerPos()),"第二台正式菜单未打开");
            h.assertTrue(command(contender,second,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.LOAD_FOLLOW,100,0,20,2),1).accepted(),"第二位设置未确认");
            var secondStart=command(contender,second,ReactorAdministratorCommands.Action.START,null,2);secondRejected[0]=!secondStart.accepted()&&secondStart.reason().equals("stress_network_busy");contender.closeContainer();
            System.out.println("[load-review-native] hiddenPreserved="+hiddenPreserved[0]+" secondStartAccepted="+secondStart.accepted()+" secondReason="+secondStart.reason());
        });
        h.runAfterDelay(30,()->{
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.LOAD_WARMUP&&o.automationLoadFollowSession()!=null&&!o.automationLoadFollowSession().established(),"C0被当成100%余量或未进入预热");
            var p=player[0];p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(AllItems.WRENCH.get()));move(p,h.absolutePos(BURNER));var b=o.automationBinding();
            h.assertTrue(ReactorStressGaugeService.apply(p,new ReactorStressGaugeService.Intent(false,b.burnerPos(),b.portPos(),b.ownerGeneration(),b.authorityEpoch(),3)).equals("binding_running"),"运行中可改绑");
            var menu=(ReactorAdministratorMenu)p.containerMenu;
            var oldStop=new ReactorAdministratorCommands.Request(menu.containerId,menu.sessionId(),b.burnerPos(),b.portPos(),b.ownerGeneration(),0,0,5,ReactorAdministratorCommands.Action.STOP,null);
            h.assertTrue(ReactorAdministratorCommands.apply(p,oldStop).accepted()&&o.automationState().phase()==ReactorAutomationState.Phase.STOPPED,"旧显示版本屏蔽当前菜单STOP");
            h.assertTrue(o.automationLoadFollowSession()==null&&o.snapshot().controlRodColumns().values().stream().allMatch(r->r.targetDepth()==1),"STOP未清PI和全插目标");
            var view=ReactorAdministratorProjection.from(o,o.automationBinding()).data();h.assertTrue(view.getInt("ReserveTarget")==37&&view.getInt("MinimumHeat")==2,"GUI投影丢确认数值");
            h.assertTrue(hiddenPreserved[0]&&secondRejected[0],"共享生产草稿/正式C0网权缺口：hiddenPreserved="+hiddenPreserved[0]+", secondRejected="+secondRejected[0]);h.succeed();
        });
    }
    @GameTest(template="load_follow_empty",timeoutTicks=980)
    public static void real_network_response(GameTestHelper h) {
        var o=setup(h);
        h.runAfterDelay(5,()->{network(h,100,80);start(h,o).closeContainer();});
        h.runAfterDelay(35,()->{
            setDepth(o,.5);var state=o.automationState();o.setAutomationState(new ReactorAutomationState(true,ReactorAutomationState.Phase.LOAD_FOLLOW,"regulating",50,state.authorityEpoch(),9,0,0,0,ReactorAutomationState.Mode.LOAD_FOLLOW,true));
            ReactorAutomationService.beginLoadStart(o);var g=network(h,100,200);
            h.assertTrue(g.getSpeed()==0&&g.getNetworkCapacity()==100&&g.getNetworkStress()==200,"原生过载不是保留C/L的停转");
            var sample=ReactorStressGaugeService.sample(o,h.getLevel().getGameTime());h.assertTrue(sample.reserveFraction().orElseThrow()==-1,"停转误判信号无效");
        });
        h.runAfterDelay(640,()->{
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.LOAD_FOLLOW&&o.automationState().commandedDepthPercent()<50,"过载余量未使目标增功率");network(h,100,0);
        });
        h.runAfterDelay(920,()->{
            h.assertTrue(o.automationState().commandedDepthPercent()>0&&o.automationState().commandedDepthPercent()<100,"健康空载没有降功率或完全停堆");
            h.assertTrue(o.automationLoadFollowSession().pi().targetHeatHuPerTick()>=2,"空载最低毛HU失守");network(h,0,0);o.tickReactor();
        });
        // 世界实体本tick已跑过，显式tick不得突破原去重；改变传感器后检查下一真实回合的保护。
        h.runAfterDelay(923,()->{h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.FAULT&&o.automationState().reason().equals("stress_capacity_lost"),"运行失容量重新预热："+o.automationState());h.succeed();});
    }
    @GameTest(template="load_follow_empty",timeoutTicks=75)
    public static void blocked_return(GameTestHelper h) {
        var o=setup(h);h.runAfterDelay(5,()->{network(h,100,80);start(h,o).closeContainer();});
        h.runAfterDelay(30,()->{o.setCoolantInventories(o.coolantCapacityMb()/100,o.coolantCapacityMb()*96/100);o.tickReactor();
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.FAULT&&!o.automationState().runPermission()&&o.automationLoadFollowSession()==null,"堵塞未覆盖正最低HU保护");
            h.assertTrue(o.snapshot().controlRodColumns().values().stream().allMatch(r->r.targetDepth()==1),"热液堵塞未全插目标");o.setCoolantInventories(o.coolantCapacityMb()/2,o.coolantCapacityMb()/10);});
        h.runAfterDelay(50,()->{h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.FAULT&&!o.automationState().runPermission(),"库存恢复自动重启");h.succeed();});
    }
    @GameTest(template="load_follow_empty",timeoutTicks=110)
    public static void lifecycle_isolation(GameTestHelper h) {
        var source=setup(h);var restored=new ReactorInstrumentPortBlockEntity[1];var burner=new BlazeBurnerBlockEntity[1];
        h.runAfterDelay(5,()->{network(h,100,80);start(h,source).closeContainer();});
        h.runAfterDelay(35,()->{
            setDepth(source,.5);var state=source.automationState();source.setAutomationState(new ReactorAutomationState(true,ReactorAutomationState.Phase.LOAD_FOLLOW,"regulating",50,state.authorityEpoch(),6,0,0,0,ReactorAutomationState.Mode.LOAD_FOLLOW,true));
            var b=(BlazeBurnerBlockEntity)h.getBlockEntity(BURNER);var tag=b.saveWithFullMetadata(h.getLevel().registryAccess());tag.putInt("fuelLevel",BlazeBurnerBlockEntity.FuelType.NONE.ordinal());tag.putInt("burnTimeRemaining",0);b.loadWithComponents(tag,h.getLevel().registryAccess());
            h.assertTrue(ReactorAutomationHeatSupply.dutyValid(b),"保存前无真实已付核热");ReactorAutomationResume.prepareNormalShutdown(source);var saved=source.saveWithFullMetadata(h.getLevel().registryAccess());
            restored[0]=(ReactorInstrumentPortBlockEntity)BlockEntity.loadStatic(h.absolutePos(OWNER),h.getBlockState(OWNER),saved,h.getLevel().registryAccess());h.getLevel().removeBlockEntity(h.absolutePos(OWNER));h.getLevel().setBlockEntity(restored[0]);restored[0].onLoad();
            burner[0]=(BlazeBurnerBlockEntity)BlockEntity.loadStatic(h.absolutePos(BURNER),h.getBlockState(BURNER),tag,h.getLevel().registryAccess());h.getLevel().removeBlockEntity(h.absolutePos(BURNER));h.getLevel().setBlockEntity(burner[0]);burner[0].onLoad();
            network(h,0,0);ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));h.setBlock(OWNER.north(),Blocks.REDSTONE_BLOCK);var o=restored[0];burner[0].tick();o.tickReactor();
            h.assertTrue(ReactorAutomationResume.pending(o)&&!o.automationState().runPermission()&&o.snapshot().controlRodColumns().values().stream().allMatch(r->r.actualDepth()==.5&&r.targetDepth()==.5),"首核交接等待容量/免费许可/改棒位");
        });
        h.runAfterDelay(40,()->{
            h.setBlock(OWNER.north(),Blocks.AIR);var o=restored[0];h.assertTrue(!h.getLevel().hasNeighborSignal(h.absolutePos(OWNER)),"原生反相线未低");o.tickReactor();burner[0].tick();
            h.assertTrue(!ReactorAutomationResume.pending(o)&&o.automationState().phase()==ReactorAutomationState.Phase.LOAD_FOLLOW&&o.automationLoadFollowSession().loading()&&!o.automationLoadFollowSession().established(),"正常续接C0回到新预热/首付循环");
            h.assertTrue(o.automationHeatAccount().paidHeatHu()==1&&burner[0].getRemainingBurnTime()==0,"首笔真实核费未支付或要求备用燃料");
        });
        h.runAfterDelay(55,()->{
            var o=restored[0];h.assertTrue(o.snapshot().controlRodColumns().values().stream().allMatch(r->r.actualDepth()==.5&&r.targetDepth()==.5)&&o.automationHeatAccount().paidHeatHu()==1&&!o.automationLoadFollowSession().pi().initialized(),"加载C0没有保棒位/真实核费或偷跑PI");
            network(h,100,80);
        });
        h.runAfterDelay(75,()->{
            var o=restored[0];h.assertTrue(o.automationLoadFollowSession().established()&&o.automationLoadFollowSession().pi().initialized(),"新容量未建立新PI");
            var network=ReactorStressGaugeService.sample(o,h.getLevel().getGameTime()).networkId();
            h.assertTrue(!ReactorStressGaugeService.claim(source,network),"当前同世界/同网登记允许另一个owner身份并持权");
            h.assertTrue(!new ReactorStressGaugeService.Registry().claim(h.getLevel(),null,o),"空网可持权");
            // 同坐标换真实表实例，旧网络ID/数值即使完全相同也不能继承当前会话。
            h.setBlock(GAUGE,Blocks.AIR);h.setBlock(GAUGE,AllBlocks.STRESSOMETER.getDefaultState());var g=(StressGaugeBlockEntity)h.getBlockEntity(GAUGE);g.setNetwork(network);g.setSpeed(16);g.updateFromNetwork(100,80,2);o.tickReactor();
        });
        h.runAfterDelay(78,()->{var o=restored[0];h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.FAULT&&o.automationLoadFollowSession()==null,"同坐标新表继承旧积分/网权："+o.automationState());h.succeed();});
    }
}
