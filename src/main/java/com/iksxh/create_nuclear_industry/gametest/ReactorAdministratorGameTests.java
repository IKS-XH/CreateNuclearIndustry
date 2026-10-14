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
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** 原生ServerPlayer菜单和正式核tick代表；无客户端画面，不冒称用户存档或GPU验收。 */
@GameTestHolder("cni_reactor_administrator") @PrefixGameTestTemplate(false)
public final class ReactorAdministratorGameTests {
    private static final BlockPos BASE=new BlockPos(3,1,4), OWNER=BASE.offset(2,2,0), PORT=BASE.offset(0,2,2), BURNER=PORT.west();
    private static final CoreColumnPosition FIRST=new CoreColumnPosition(1,0), SECOND=new CoreColumnPosition(1,2), ROD=new CoreColumnPosition(1,1);
    private ReactorAdministratorGameTests() { }
    private static ReactorInstrumentPortBlockEntity setup(GameTestHelper h) {
        var columns=new HashMap<>(ReactorStructureDefinition.defaultColumnLayout());columns.replaceAll((p,v)->ReactorStructureDefinition.ColumnType.EMPTY);
        columns.put(ROD,ReactorStructureDefinition.ColumnType.CONTROL_ROD);columns.put(FIRST,ReactorStructureDefinition.ColumnType.FUEL);columns.put(SECOND,ReactorStructureDefinition.ColumnType.FUEL);
        for(var entry:ReactorStructureDefinition.templateFor(columns).entrySet()) {var p=entry.getKey();h.setBlock(BASE.offset(p.x(),p.y(),p.z()),BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.getValue())).defaultBlockState());}
        h.setBlock(PORT,ReactorAutomationContent.PORT.get());h.setBlock(BURNER,AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.SMOULDERING));
        h.assertTrue(BlazeBurnerBlock.tryInsert(h.getBlockState(BURNER),h.getLevel(),h.absolutePos(BURNER),new ItemStack(Items.COAL),false,false,false).getResult().consumesAction(),"正常原生点燃失败");
        var support=BURNER.north();h.setBlock(support,Blocks.STONE);h.setBlock(support.above(),Blocks.REDSTONE_TORCH);
        for(var p:List.of(support.above().north(),support.above().north(2),new BlockPos(1,4,3),new BlockPos(2,4,3),new BlockPos(3,4,3),new BlockPos(4,4,3),new BlockPos(5,4,3))) {h.setBlock(p.below(),Blocks.STONE);h.setBlock(p,Blocks.REDSTONE_WIRE);}
        ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));
        var o=(ReactorInstrumentPortBlockEntity)h.getBlockEntity(OWNER);
        for(var pos:List.of(FIRST,SECOND))h.assertTrue(fuel(h,pos).tryInsertFuel(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())).success(),"真实原换料端口入料失败");
        o.setCoolantInventories(o.coolantCapacityMb()/2,o.coolantCapacityMb()/10);return o;
    }
    private static ReactorPortBlockEntity fuel(GameTestHelper h,CoreColumnPosition p) {return (ReactorPortBlockEntity)h.getBlockEntity(BASE.offset(p.x()+1,4,p.z()+1));}
    private static BlazeBurnerBlockEntity burner(GameTestHelper h) {return (BlazeBurnerBlockEntity)h.getBlockEntity(BURNER);}
    /** FakePlayer原类关闭了openMenu；这里保留原生ServerPlayer.openMenu，仅使用隔离无网络连接。 */
    private static ServerPlayer player(GameTestHelper h,String name) {
        var profile=new GameProfile(UUID.randomUUID(),name);var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        p.connection=FakePlayerFactory.get(h.getLevel(),profile).connection;p.setGameMode(GameType.SURVIVAL);
        var pos=h.absolutePos(BURNER);p.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);return p;
    }
    private static ReactorAdministratorCommands.Request request(ServerPlayer p,ReactorInstrumentPortBlockEntity o,ReactorAdministratorCommands.Action a,ReactorAdministratorCommands.Draft d,long seq) {
        var b=o.automationBinding();return new ReactorAdministratorCommands.Request(p.containerMenu.containerId,((ReactorAdministratorMenu)p.containerMenu).sessionId(),b.burnerPos(),b.portPos(),b.ownerGeneration(),b.authorityEpoch(),o.automationPort().settingsRevision(),seq,a,d);
    }
    private static void accepted(GameTestHelper h,ReactorAdministratorCommands.Response r) {h.assertTrue(r.accepted(),"菜单命令拒绝："+r.reason());}
    /** 审查新增的两个启动窗口使用独立域；复用真实菜单/反相线，不重复既有六个代表。 */
    @GameTestHolder("cni_reactor_administrator_startup") @PrefixGameTestTemplate(false)
    public static final class StartupWindows {
        private StartupWindows() { }
        @GameTest(template="startup_empty",timeoutTicks=60)
        public static void arming_mode_change_requires_new_start(GameTestHelper h) {
            var o=setup(h);
            h.runAfterDelay(5,()->{
                var p=player(h,"ModeWindow");h.assertTrue(ReactorAdministratorMenu.open(p,h.absolutePos(BURNER)),"启动窗口菜单未打开");
                accepted(h,ReactorAdministratorCommands.apply(p,request(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,40,4),1)));
                accepted(h,ReactorAdministratorCommands.apply(p,request(p,o,ReactorAdministratorCommands.Action.START,null,2)));
                h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.ARMING,"夹具未实际进入ARMING");
                accepted(h,ReactorAdministratorCommands.apply(p,request(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.CONSTANT,40,4),3)));
                h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.STOPPED&&!o.automationState().runPermission(),"ARMING改模式未撤权回停堆");
                h.assertTrue(o.automationPort().settings().mode()==ReactorAutomationState.Mode.CONSTANT,"安全交接丢失新模式偏好");
                h.assertTrue(o.snapshot().controlRodColumns().values().stream().allMatch(r->r.targetDepth()==1),"ARMING改模式未全插");
            });
            h.runAfterDelay(14,()->{
                h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.STOPPED&&!o.automationState().runPermission(),"模式交接后旧启动意图自动恢复");
                System.out.println("[administrator-startup-mode] ARMING APPLY CONSTANT -> STOPPED, new START required");h.succeed();
            });
        }
        @GameTest(template="startup_empty",timeoutTicks=60)
        public static void arming_warning_holds_until_inventory_recovers(GameTestHelper h) {
            var o=setup(h);
            h.runAfterDelay(5,()->{
                var p=player(h,"WarningWindow");h.assertTrue(ReactorAdministratorMenu.open(p,h.absolutePos(BURNER)),"预警窗口菜单未打开");
                accepted(h,ReactorAdministratorCommands.apply(p,request(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,40,4),1)));
                accepted(h,ReactorAdministratorCommands.apply(p,request(p,o,ReactorAdministratorCommands.Action.START,null,2)));
                o.setCoolantInventories(o.coolantCapacityMb()*14/100,o.coolantCapacityMb()/10);
                var sample=ReactorAutomationService.observe(o,0,0,false,h.getLevel().getGameTime(),h.getLevel().getGameTime());
                h.assertTrue(ReactorAutomationController.warning(sample,com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.parameters())&&ReactorAutomationController.coolantCanCarryNextTick(sample,40),"夹具必须已预警但浅目标仍可冷却");
            });
            for(int tick:List.of(11,12,13))h.runAfterDelay(tick,()->{
                h.assertTrue(!h.getLevel().hasNeighborSignal(h.absolutePos(OWNER)),"真实反相线尚未建立低输入，未到受测交接窗口");
                h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.ARMING&&o.automationState().runPermission(),"预警窗口提前提棒或丢失已确认启动意图");
                h.assertTrue(o.snapshot().controlRodColumns().values().stream().allMatch(r->r.actualDepth()==1&&r.targetDepth()==1),"预警持续期间真实棒体离开全插基准");
                System.out.println("[administrator-startup-warning] tick="+tick+" low=true ARMING rods=100%");
            });
            h.runAfterDelay(14,()->o.setCoolantInventories(o.coolantCapacityMb()/2,o.coolantCapacityMb()/10));
            h.runAfterDelay(18,()->{
                h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.MANUAL&&o.automationState().runPermission(),"预警解除未恢复已确认手动启动");
                h.assertTrue(o.automationPort().settings().manualDepthPercent()==40&&o.snapshot().controlRodColumns().values().stream().allMatch(r->r.targetDepth()==.4&&r.actualDepth()==.4),"预警解除未恢复保存的40%目标");
                System.out.println("[administrator-startup-warning] recovered MANUAL rods=40%");h.succeed();
            });
        }
    }
    @GameTest(template="administrator_empty",timeoutTicks=100)
    public static void real_menu_atomic_sessions(GameTestHelper h) {
        var o=setup(h);
        h.runAfterDelay(5,()->{
            var p=player(h,"AdminOne");var other=player(h,"AdminTwo");
            var absolute=h.absolutePos(BURNER);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(absolute),Direction.WEST,absolute,false);
            h.getBlockState(BURNER).useItemOn(ItemStack.EMPTY,h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            h.assertTrue(p.containerMenu instanceof ReactorAdministratorMenu,"原生空手右击未打开玩家菜单");
            h.assertTrue(ReactorAdministratorMenu.open(other,h.absolutePos(BURNER)),"第二玩家菜单未打开");
            var port=o.automationPort();var before=port.settings();long revision=port.settingsRevision();
            var stale=request(other,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,22,9),1);
            var invalid=ReactorAdministratorCommands.apply(p,request(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.CONSTANT,101,4),1));
            h.assertTrue(!invalid.accepted()&&port.settings().equals(before)&&port.settingsRevision()==revision,"多字段非法草稿部分提交");
            accepted(h,ReactorAdministratorCommands.apply(p,request(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.CONSTANT,40,4),2)));
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.STOPPED&&!o.automationState().runPermission(),"应用设置自动启动");
            h.assertTrue(!ReactorAdministratorCommands.apply(other,stale).accepted()&&port.settings().targetHeatHuPerTick()==4,"第二玩家旧草稿覆盖新设置");
            h.assertTrue(!ReactorAdministratorCommands.apply(p,request(p,o,ReactorAdministratorCommands.Action.RESET,null,3)).accepted(),"运行前复位充当启动");
            accepted(h,ReactorAdministratorCommands.apply(p,request(p,o,ReactorAdministratorCommands.Action.START,null,4)));
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.ARMING,"启动未独立进入联锁");
            try {
                var field=com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.class.getDeclaredField("WARN_COLD");field.setAccessible(true);
                var threshold=(net.neoforged.neoforge.common.ModConfigSpec.DoubleValue)field.get(null);double original=threshold.get();
                try {
                    // 合法单项0.3违反冷液预警<启动的跨项顺序；只改隔离服务端内存，finally恢复，不写用户配置。
                    threshold.set(.3);
                    h.assertTrue(ReactorAdministratorProjection.from(o,o.automationBinding()).data().hasUUID("Generation"),"非法配置故障显示丢失当前身份，客户端无法停堆");
                    for(var a:List.of(ReactorAdministratorCommands.Action.APPLY,ReactorAdministratorCommands.Action.START,ReactorAdministratorCommands.Action.RESET)) {
                        var denied=ReactorAdministratorCommands.apply(other,request(other,o,a,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,10,5),1000+a.ordinal()));
                        h.assertTrue(!denied.accepted(),"非法配置允许非停堆动作："+a);
                    }
                    accepted(h,ReactorAdministratorCommands.apply(p,request(p,o,ReactorAdministratorCommands.Action.STOP,null,5)));
                    h.assertTrue(!o.automationState().runPermission(),"非法配置阻止紧急撤权");
                } finally {threshold.set(original);}
            } catch(ReflectiveOperationException exception) {throw new IllegalStateException("隔离跨项配置夹具失败",exception);}
            var staleStop=new ReactorAdministratorCommands.Request(p.containerMenu.containerId,((ReactorAdministratorMenu)p.containerMenu).sessionId(),o.automationBinding().burnerPos(),o.automationBinding().portPos(),o.automationGeneration(),0,0,6,ReactorAdministratorCommands.Action.STOP,null);
            accepted(h,ReactorAdministratorCommands.apply(p,staleStop));h.assertTrue(!o.automationState().runPermission(),"有效菜单紧急停堆被旧显示版本拒绝");
            h.assertTrue(!ReactorAdministratorCommands.apply(p,staleStop).accepted(),"重复sequence接受了第二次停堆请求");
            var reused=request(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,10,5),6);
            for(int reopen=0;reopen<150;reopen++) {p.closeContainer();ReactorAdministratorMenu.open(p,h.absolutePos(BURNER));if(p.containerMenu.containerId==reused.containerId())break;}
            h.assertTrue(p.containerMenu.containerId==reused.containerId(),"原生containerId未实际循环复用");
            for(var a:List.of(ReactorAdministratorCommands.Action.APPLY,ReactorAdministratorCommands.Action.START,ReactorAdministratorCommands.Action.STOP)) {
                var replay=new ReactorAdministratorCommands.Request(reused.containerId(),reused.sessionId(),reused.burnerPos(),reused.portPos(),reused.ownerGeneration(),reused.authorityEpoch(),reused.settingsRevision(),1000,a,reused.draft());
                h.assertTrue(!ReactorAdministratorCommands.apply(p,replay).accepted(),"containerId复用后旧会话动作被接受："+a);
            }
            var closed=request(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,10,5),6);p.closeContainer();
            h.assertTrue(!ReactorAdministratorCommands.apply(p,closed).accepted(),"关闭菜单后提交被接受");
            ReactorAdministratorMenu.open(p,h.absolutePos(BURNER));var far=request(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,10,5),6);
            p.setPos(p.getX()+100,p.getY(),p.getZ());h.assertTrue(!ReactorAdministratorCommands.apply(p,far).accepted(),"远距菜单提交被接受");
            p.setPos(other.getX(),other.getY(),other.getZ());p.closeContainer();ReactorAdministratorMenu.open(p,h.absolutePos(BURNER));
            var replacement=request(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,10,5),7);
            h.setBlock(PORT,P1Blocks.REACTOR_CASING.get());h.setBlock(PORT,ReactorAutomationContent.PORT.get());o.tickReactor();
            h.assertTrue(!ReactorAdministratorCommands.apply(p,replacement).accepted(),"同坐标新接口接受旧菜单");
            p.closeContainer();ReactorAdministratorMenu.open(p,h.absolutePos(BURNER));
            var swappedBurner=request(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,10,5),8);
            h.setBlock(BURNER,Blocks.AIR);h.setBlock(BURNER,AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.SMOULDERING));o.tickReactor();
            h.assertTrue(!ReactorAdministratorCommands.apply(p,swappedBurner).accepted(),"新燃烧室接受旧菜单");
            for(var type:ReactorAutomationBehaviours.TYPES)h.assertTrue(burner(h).getBehaviour(type)==null,"旧四世界控件仍注册");
            System.out.println("[administrator-menu] atomic/stale/closed/far/replaced/stop verified");h.succeed();
        });
    }
    @GameTest(template="administrator_empty",timeoutTicks=180)
    public static void real_partial_fuel_and_refueling(GameTestHelper h) {
        var o=setup(h);h.runAfterDelay(5,()->{
            var p=player(h,"FuelAdmin");ReactorAdministratorMenu.open(p,h.absolutePos(BURNER));
            accepted(h,ReactorAdministratorCommands.apply(p,request(p,o,ReactorAdministratorCommands.Action.APPLY,new ReactorAdministratorCommands.Draft(ReactorAutomationState.Mode.MANUAL,0,4),1)));
            accepted(h,ReactorAdministratorCommands.apply(p,request(p,o,ReactorAdministratorCommands.Action.START,null,2)));
        });
        h.runAfterDelay(12,()->{
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.MANUAL,"手动启动未完成");
            var nearSpent=fuel(h,FIRST).fuelAssembly().copy();nearSpent.setDamageValue(nearSpent.getMaxDamage()-1);fuel(h,FIRST).setFuelAssembly(nearSpent);
            var previous=o.snapshot();var first=previous.fuelColumns().get(FIRST);
            var columns=new TreeMap<>(previous.fuelColumns());columns.put(FIRST,new FuelColumnState(first.fuelAssembly(),1,first.cachedHeatHu(),.999,first.quantizedHeatRemainderHu()));
            o.setSnapshot(previous.withColumns(columns,Map.of(ROD,new ControlRodColumnState(1,0,0,false,0))));o.tickReactor();
            h.assertTrue(!o.snapshot().fuelColumns().get(FIRST).hasUsableFuel()&&o.snapshot().fuelColumns().get(SECOND).hasUsableFuel(),"正式燃耗未自然耗尽首列");
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.MANUAL&&o.automationState().runPermission(),"部分自然耗尽导致撤权");
            h.assertTrue(fuel(h,FIRST).tryReplaceExhaustedFuel(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())).success(),"真实枯竭快捷换料未成功");o.tickReactor();
            h.assertTrue(o.automationState().runPermission()&&o.snapshot().fuelColumns().get(FIRST).hasUsableFuel(),"补新燃料撤销连续许可");
            for(var position:List.of(FIRST,SECOND)){var last=fuel(h,position).fuelAssembly().copy();last.setDamageValue(last.getMaxDamage()-1);fuel(h,position).setFuelAssembly(last);}
            columns=new TreeMap<>(o.snapshot().fuelColumns());for(var position:List.of(FIRST,SECOND)){var f=columns.get(position);columns.put(position,new FuelColumnState(f.fuelAssembly(),1,f.cachedHeatHu(),.999,f.quantizedHeatRemainderHu()));}
            o.setSnapshot(o.snapshot().withColumns(columns,Map.of(ROD,new ControlRodColumnState(1,0,0,false,0))));o.tickReactor();
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.FAULT&&!o.automationState().runPermission(),"全列耗尽未故障保持");
            h.assertTrue(fuel(h,FIRST).tryReplaceExhaustedFuel(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())).success(),"故障后原换料入口拒绝");o.tickReactor();
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.FAULT&&!o.automationState().runPermission(),"全耗尽补料后自动恢复");
            System.out.println("[administrator-fuel] partial/native-refuel/all-fault verified");
        });
        h.runAfterDelay(125,()->{
            var p=player(h,"ResetAdmin");h.assertTrue(ReactorAdministratorMenu.open(p,h.absolutePos(BURNER)),"真实复位菜单未打开");
            accepted(h,ReactorAdministratorCommands.apply(p,request(p,o,ReactorAdministratorCommands.Action.RESET,null,1)));
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.STOPPED&&!o.automationState().runPermission(),"稳定故障复位自动启动");
            System.out.println("[administrator-reset] healthy="+o.automationState().healthyAge()+" reset-only=STOPPED");h.succeed();
        });
    }
    private static void cached(GameTestHelper h,ReactorInstrumentPortBlockEntity o,double hu) {
        var columns=new TreeMap<>(o.snapshot().fuelColumns());var f=columns.get(FIRST);
        columns.put(FIRST,new FuelColumnState(f.fuelAssembly(),1,hu,0,hu));var other=columns.get(SECOND);columns.put(SECOND,new FuelColumnState(other.fuelAssembly(),1,0,0,0));
        o.setSnapshot(o.snapshot().withColumns(columns,Map.of(ROD,ControlRodColumnState.fullyInserted())).withCoolantInventories(0,o.coolantCapacityMb()));
    }
    @GameTest(template="administrator_empty",timeoutTicks=80)
    public static void real_nuclear_owner_first(GameTestHelper h) { heatOrder(h,false); }
    @GameTest(template="administrator_empty",timeoutTicks=80)
    public static void real_nuclear_burner_first(GameTestHelper h) { heatOrder(h,true); }
    private static void heatOrder(GameTestHelper h,boolean burnerFirst) {
        var o=setup(h);cached(h,o,20);var b=burner(h);int before=b.getRemainingBurnTime();
        if(burnerFirst)b.tick();o.tickReactor();b.tick();
        int expected=before-(burnerFirst?1:0);
        h.assertTrue(b.getRemainingBurnTime()==expected,"真实两种tick顺序没有按整笔核热冻结NORMAL");
        h.assertTrue(Math.abs(o.snapshot().fuelColumns().get(FIRST).cachedHeatHu()-19)<1e-9&&o.automationHeatAccount().paidHeatHu()==1,"真实维护没有从停堆缓存支付1HU");
        h.assertTrue(b.getHeatLevelFromBlock()==BlazeBurnerBlock.HeatLevel.KINDLED,"核热未显示普通KINDLED");
        o.tickReactor();b.tick();
        h.assertTrue(b.getRemainingBurnTime()==expected-1&&Math.abs(o.snapshot().fuelColumns().get(FIRST).cachedHeatHu()-19)<1e-9,"顺序切换或同tick重复调用一笔两用/重复收费");
        System.out.println("[administrator-heat] order="+(burnerFirst?"burner-first":"owner-first")+" cost=1 nativeTicks="+(before-b.getRemainingBurnTime()));
        h.runAfterDelay(1,()->{
            h.assertTrue(ReactorAdministratorProjection.from(o,o.automationBinding()).data().getString("Supply").equals("nuclear"),"下一tick菜单来源未保留已完成的核热供能事实");
            System.out.println("[administrator-next-view] currentConsumed="+ReactorAutomationHeatSupply.displayingNuclear(b)+" supply="+ReactorAutomationHeatSupply.source(b));
            int backup=b.getRemainingBurnTime();ReactorAutomationService.onTickFailure(o,h.getLevel().getGameTime());b.tick();
            h.assertTrue(b.getRemainingBurnTime()==backup-1&&!ReactorAutomationHeatSupply.displayingNuclear(b),"已知失败未撤票恢复外燃料计时");
            // SPECIAL真实计时及到期赠NORMAL5000保持，不通过维护伪造超级加热。
            var tag=b.saveWithoutMetadata(h.getLevel().registryAccess());tag.putInt("fuelLevel",BlazeBurnerBlockEntity.FuelType.SPECIAL.ordinal());tag.putInt("burnTimeRemaining",1);b.loadWithComponents(tag,h.getLevel().registryAccess());b.tick();
            h.assertTrue(b.getActiveFuel()==BlazeBurnerBlockEntity.FuelType.NORMAL&&b.getRemainingBurnTime()==5000,"原SPECIAL到期NORMAL5000规则被破坏");
        });
        h.runAfterDelay(2,()->{
            var special=b.saveWithoutMetadata(h.getLevel().registryAccess());special.putInt("fuelLevel",BlazeBurnerBlockEntity.FuelType.SPECIAL.ordinal());special.putInt("burnTimeRemaining",3);b.loadWithComponents(special,h.getLevel().registryAccess());
            b.updateBlockState();cached(h,o,20);o.tickReactor();b.tick();
            h.assertTrue(b.getRemainingBurnTime()==2&&b.getActiveFuel()==BlazeBurnerBlockEntity.FuelType.SPECIAL&&Math.abs(o.snapshot().fuelColumns().get(FIRST).cachedHeatHu()-20)<1e-9,"SPECIAL冻结计时或重复扣核热");
            h.assertTrue(b.getHeatLevelFromBlock()==BlazeBurnerBlock.HeatLevel.SEETHING,"SPECIAL原生超级加热被降低");
            var none=b.saveWithoutMetadata(h.getLevel().registryAccess());none.putInt("fuelLevel",BlazeBurnerBlockEntity.FuelType.NONE.ordinal());none.putInt("burnTimeRemaining",0);b.loadWithComponents(none,h.getLevel().registryAccess());
            ReactorAutomationHeatSupply.invalidate(o);h.setBlock(BURNER,h.getBlockState(BURNER).setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.KINDLED));
            o.tickReactor();b.tick();
            h.assertTrue(o.automationHeatAccount().paidHeatHu()==0&&!ReactorAutomationHeatSupply.displayingNuclear(b)&&!ReactorAutomationBlazeBridge.isDutyValid(b),"无已付资格的KINDLED自我免费重启");
            var nativePos=new BlockPos(11,1,11);h.setBlock(nativePos,AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.SMOULDERING));
            BlazeBurnerBlock.tryInsert(h.getBlockState(nativePos),h.getLevel(),h.absolutePos(nativePos),new ItemStack(Items.COAL),false,false,false);
            var nativeBurner=(BlazeBurnerBlockEntity)h.getBlockEntity(nativePos);int nativeBefore=nativeBurner.getRemainingBurnTime();nativeBurner.tick();
            h.assertTrue(nativeBurner.getRemainingBurnTime()==nativeBefore-1&&ReactorAutomationBlazeBridge.findBinding(h.getLevel(),h.absolutePos(nativePos)).isEmpty(),"未任职普通燃烧室原计时改变");
            var creative=b.saveWithoutMetadata(h.getLevel().registryAccess());creative.putBoolean("isCreative",true);creative.putInt("fuelLevel",BlazeBurnerBlockEntity.FuelType.NORMAL.ordinal());creative.putInt("burnTimeRemaining",10);b.loadWithComponents(creative,h.getLevel().registryAccess());
            o.tickReactor();b.tick();h.assertTrue(b.getRemainingBurnTime()==10&&o.automationHeatAccount().paidHeatHu()==0,"创造供能被扣维护热或普通时长");
        });
        h.runAfterDelay(3,()->{
            var normal=b.saveWithoutMetadata(h.getLevel().registryAccess());normal.putBoolean("isCreative",false);normal.putInt("fuelLevel",BlazeBurnerBlockEntity.FuelType.NORMAL.ordinal());normal.putInt("burnTimeRemaining",2);b.loadWithComponents(normal,h.getLevel().registryAccess());
            ReactorAutomationHeatSupply.invalidate(o);cached(h,o,.5);o.tickReactor();b.tick();
            h.assertTrue(Math.abs(o.snapshot().fuelColumns().get(FIRST).cachedHeatHu()-.5)<1e-9&&o.automationHeatAccount().paidHeatHu()==0&&b.getRemainingBurnTime()==1,"不足整笔热量被部分扣款或仍冻结燃料");
            h.setBlock(BURNER,Blocks.AIR);h.setBlock(BURNER,AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.KINDLED));o.tickReactor();
            h.assertTrue(!ReactorAutomationHeatSupply.consumeForBurner(burner(h)),"同位置新成员继承旧收据");
            var unchanged=o.snapshot();h.setBlock(BASE.offset(FIRST.x()+1,4,FIRST.z()+1),Blocks.AIR);o.tickReactor();
            h.assertTrue(o.automationHeatAccount().paidHeatHu()==0&&Math.abs(o.snapshot().fuelColumns().get(FIRST).cachedHeatHu()-unchanged.fuelColumns().get(FIRST).cachedHeatHu())<1e-9,"真实燃料端口失联的未提交核tick扣了维护热或出票");
            System.out.println("[administrator-native] SPECIAL/creative/unbound/false-KINDLED/insufficient/replaced verified");h.succeed();
        });
    }
    @GameTest(template="administrator_empty",timeoutTicks=80)
    public static void real_special_initial_tick_no_nuclear_cost(GameTestHelper h) {
        var o=setup(h);var b=burner(h);var tag=b.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.putInt("fuelLevel",BlazeBurnerBlockEntity.FuelType.SPECIAL.ordinal());tag.putInt("burnTimeRemaining",3);b.loadWithComponents(tag,h.getLevel().registryAccess());
        b.updateBlockState();cached(h,o,20);o.tickReactor();b.tick();
        h.assertTrue(o.automationHeatAccount().paidHeatHu()==0&&Math.abs(o.snapshot().fuelColumns().get(FIRST).cachedHeatHu()-20)<1e-9&&b.getRemainingBurnTime()==2,"首次正式SPECIAL核tick收费或冻结");
        var normal=b.saveWithoutMetadata(h.getLevel().registryAccess());normal.putInt("fuelLevel",BlazeBurnerBlockEntity.FuelType.NORMAL.ordinal());normal.putInt("burnTimeRemaining",10);b.loadWithComponents(normal,h.getLevel().registryAccess());o.tickReactor();
        h.assertTrue(o.automationHeatAccount().paidHeatHu()==0&&Math.abs(o.snapshot().fuelColumns().get(FIRST).cachedHeatHu()-20)<1e-9,"同tick零请求已提交后又扣费却无法出票");
        System.out.println("[administrator-special] first formal cost=0 native countdown=1");h.succeed();
    }
    @GameTest(template="administrator_empty",timeoutTicks=80)
    public static void real_control_rod_cache_can_pay_nuclear_duty(GameTestHelper h) {
        var o=setup(h);var b=burner(h);int before=b.getRemainingBurnTime();
        var rod=new ControlRodColumnState(1,1,1,false,2);
        o.setSnapshot(o.snapshot().withColumns(o.snapshot().fuelColumns(),Map.of(ROD,rod)).withCoolantInventories(0,o.coolantCapacityMb()));o.tickReactor();b.tick();
        h.assertTrue(o.automationHeatAccount().paidHeatHu()==1&&o.snapshot().controlRodColumns().get(ROD).equals(new ControlRodColumnState(1,1,1,false,1)),"真实杆缓存维护支付没有守恒或改变机械字段");
        h.assertTrue(b.getRemainingBurnTime()==before&&ReactorAdministratorProjection.from(o,o.automationBinding()).data().getDouble("CacheHeat")==1,"杆余热核值班或菜单总缓存显示错误");
        System.out.println("[administrator-rod-heat] rod cache 2 -> 1, cost=1, normal frozen");h.succeed();
    }
}
