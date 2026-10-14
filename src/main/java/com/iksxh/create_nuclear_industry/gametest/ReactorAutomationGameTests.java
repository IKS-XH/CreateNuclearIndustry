package com.iksxh.create_nuclear_industry.gametest;
import com.iksxh.create_nuclear_industry.automation.*;
import com.iksxh.create_nuclear_industry.blockentity.*;
import com.iksxh.create_nuclear_industry.content.*;
import com.iksxh.create_nuclear_industry.control.*;
import com.iksxh.create_nuclear_industry.structure.*;
import com.iksxh.create_nuclear_industry.reactor.*;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.*;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.foundation.blockEntity.behaviour.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.lang.reflect.Method;
/** 独立服务端四个真实代表；NBT只代表现行BE合同，不冒充完整保存重进或实际区块卸载。 */
@GameTestHolder("cni_reactor_automation") @PrefixGameTestTemplate(false)
public final class ReactorAutomationGameTests {
    private static final BlockPos BASE=new BlockPos(3,1,4), OWNER=BASE.offset(2,2,0), DRIVE=BASE.offset(2,4,1),
        FUEL=BASE.offset(2,4,2), PORT=BASE.offset(0,2,2), BURNER=PORT.west();
    private static final CoreColumnPosition ROD=new CoreColumnPosition(1,0);
    private ReactorAutomationGameTests() {}
    private static ReactorInstrumentPortBlockEntity setup(GameTestHelper h) {
        var columns=new HashMap<>(ReactorStructureDefinition.defaultColumnLayout()); columns.replaceAll((p,v)->ReactorStructureDefinition.ColumnType.EMPTY);
        columns.put(ROD,ReactorStructureDefinition.ColumnType.CONTROL_ROD); columns.put(new CoreColumnPosition(1,1),ReactorStructureDefinition.ColumnType.FUEL);
        for(var entry:ReactorStructureDefinition.templateFor(columns).entrySet()) {
            var p=entry.getKey(); h.setBlock(BASE.offset(p.x(),p.y(),p.z()),BuiltInRegistries.BLOCK.get(ResourceLocation.parse(entry.getValue())).defaultBlockState());
        }
        h.setBlock(PORT,ReactorAutomationContent.PORT.get()); placeBurner(h,BURNER);
        // 独立石块承载火把，管理员被拆也不会破坏反相元件；真实粉尘强供电石块邻接仪表。
        var support=BURNER.north(); h.setBlock(support,Blocks.STONE); h.setBlock(support.above(),Blocks.REDSTONE_TORCH);
        for(var p:List.of(support.above().north(),support.above().north(2),new BlockPos(1,4,3),new BlockPos(2,4,3),new BlockPos(3,4,3),new BlockPos(4,4,3),new BlockPos(5,4,3))) {
            h.setBlock(p.below(),Blocks.STONE); h.setBlock(p,Blocks.REDSTONE_WIRE);
        }
        ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));
        var o=(ReactorInstrumentPortBlockEntity)h.getBlockEntity(OWNER);
        var fuel=(ReactorPortBlockEntity)h.getBlockEntity(FUEL);
        h.assertTrue(fuel.tryInsertFuel(new ItemStack(ModItems.FRESH_FUEL_ASSEMBLY.get())).success(),"真实换料入口未接受新组件");
        o.setCoolantInventories(o.coolantCapacityMb()/2,o.coolantCapacityMb()/10); return o;
    }
    private static void placeBurner(GameTestHelper h,BlockPos pos) {
        h.setBlock(pos,AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.SMOULDERING));
        feed(h,pos);
    }
    private static void feed(GameTestHelper h,BlockPos pos) {
        var result=BlazeBurnerBlock.tryInsert(h.getBlockState(pos),h.getLevel(),h.absolutePos(pos),new ItemStack(Items.COAL),false,false,false);
        h.assertTrue(result.getResult().consumesAction(),"Create原生喂煤未成功");
    }
    private static ServerPlayer player(GameTestHelper h) {
        var p=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"AutomationProbe")); p.setGameMode(GameType.SURVIVAL);
        var pos=h.absolutePos(BURNER);p.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5); return p;
    }
    private static ReactorAutomationService.Response command(GameTestHelper h,ServerPlayer player,ReactorAutomationService.Command cmd,int value,long seq) {
        var b=ReactorAutomationBlazeBridge.findBinding(h.getLevel(),h.absolutePos(BURNER)).orElseThrow();
        return ReactorAutomationService.applyCommand(player,new ReactorAutomationService.Request(b.burnerPos(),b.portPos(),b.ownerGeneration(),b.authorityEpoch(),cmd,value,seq));
    }
    private static void accepted(GameTestHelper h,ReactorAutomationService.Response r) {h.assertTrue(r.accepted(),"管理员命令拒绝："+r.reason());}
    private static double depth(ReactorInstrumentPortBlockEntity o) {return o.snapshot().controlRodColumns().get(ROD).targetDepth();}
    @GameTest(template="automation_empty",timeoutTicks=100) public static void operator_controls(GameTestHelper h) {
        var o=setup(h); var p=player(h); var pos=h.absolutePos(DRIVE);
        h.runAfterDelay(5,()->{
            h.assertTrue(h.getBlockState(BURNER.west()).isAir(),"反相线路遮挡燃烧室正面控件");
            var blaze=(BlazeBurnerBlockEntity)h.getBlockEntity(BURNER);
            for(var type:ReactorAutomationBehaviours.TYPES) {
                var transform=(ReactorAutomationBehaviours.Transform)blaze.getBehaviour(type).getSlotPositioning();
                var world=h.absolutePos(BURNER);var state=h.getBlockState(BURNER);
                transform.fromSide(Direction.WEST);var center=transform.getLocalOffset(h.getLevel(),world,state);
                h.assertTrue(transform.testHit(h.getLevel(),world,state,center),"显示锚点未同源命中可见面");
                transform.fromSide(Direction.EAST);h.assertTrue(!transform.testHit(h.getLevel(),world,state,center),"隐藏接口面仍命中");
                transform.fromSide(Direction.DOWN);h.assertTrue(!transform.testHit(h.getLevel(),world,state,center),"底面仍命中");
            }
            h.assertTrue(o.automationState().claimed() && o.snapshot().scramRequested(),"未任职或真实反相线路未提供停堆高电平");
            accepted(h,command(h,p,ReactorAutomationService.Command.HEAT,2,1)); accepted(h,command(h,p,ReactorAutomationService.Command.MODE,1,2));
        });
        h.runAfterDelay(11,()->{
            System.out.println("[automation-boundary] phase="+o.automationState().phase()+" reason="+o.automationState().reason()+" permission="+ReactorAutomationBlazeBridge.runPermission(h.getLevel(),h.absolutePos(BURNER))+" source="+h.getBlockState(BURNER).isSignalSource()+" signal="+h.getLevel().hasNeighborSignal(h.absolutePos(OWNER))+" torch="+h.getBlockState(BURNER.north().above())+" supportInput="+h.getLevel().hasNeighborSignal(h.absolutePos(BURNER.north())));
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.MANUAL && ReactorAutomationBlazeBridge.runPermission(h.getLevel(),h.absolutePos(BURNER))==15,"联锁未解除进入手动");
            h.assertTrue(ControlRodSliderService.handle(p,ControlRodSliderPayload.start(pos,1,0,100,42)).accepted(),"手动逐棒拖动未开放");
            accepted(h,command(h,p,ReactorAutomationService.Command.DEPTH,40,3));
            h.assertTrue(depth(o)==.4 && !ControlRodSliderService.handle(p,ControlRodSliderPayload.commit(pos,1,0,0,42)).accepted(),"全堆命令没有撤销旧拖动");
            var stamp=o.automationBinding(); var stale=new ReactorAutomationService.Request(stamp.burnerPos(),stamp.portPos(),stamp.ownerGeneration(),stamp.authorityEpoch()-1,ReactorAutomationService.Command.HEAT,999,4);
            h.assertTrue(!ReactorAutomationService.applyCommand(p,stale).accepted(),"旧代际提交被接受");
            o.setCoolantInventories(o.coolantCapacityMb()*14/100,o.coolantCapacityMb()/10);
            h.assertTrue(!command(h,p,ReactorAutomationService.Command.DEPTH,20,4).accepted(),"手动预警仍允许全堆提棒");
            h.assertTrue(ControlRodSliderService.handle(p,ControlRodSliderPayload.start(pos,1,0,40,44)).accepted(),"预警不能开始安全精修会话");
            h.assertTrue(!ControlRodSliderService.handle(p,ControlRodSliderPayload.commit(pos,1,0,0,44)).accepted(),"旧单棒权威入口绕过预警提棒限制");
            o.setCoolantInventories(o.coolantCapacityMb()/2,o.coolantCapacityMb()/10);
            accepted(h,command(h,p,ReactorAutomationService.Command.DEPTH,100,5)); accepted(h,command(h,p,ReactorAutomationService.Command.MODE,0,6));
        });
        h.runAfterDelay(17,()->accepted(h,command(h,p,ReactorAutomationService.Command.MODE,2,7)));
        h.runAfterDelay(42,()->{
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.CONSTANT && depth(o)<1 && depth(o)>=.97,"绝对HU恒定调节没有按1百分点逐步提棒");
            h.assertTrue(!ControlRodSliderService.handle(p,ControlRodSliderPayload.start(pos,1,0,0,43)).accepted(),"恒定模式逐棒仍可写");
            try {
                var blaze=(BlazeBurnerBlockEntity)h.getBlockEntity(BURNER);
                var ctrl=blaze.getBehaviour(ReactorAutomationBehaviours.TYPES.get(2));
                var packet=new ValueSettingsPacket(h.absolutePos(BURNER),0,999,null,null,Direction.WEST,false,ctrl.netId());
                Method apply=ValueSettingsPacket.class.getDeclaredMethod("applySettings",ServerPlayer.class,com.simibubi.create.foundation.blockEntity.SmartBlockEntity.class);
                apply.setAccessible(true);apply.invoke(packet,p,blaze);
                h.assertTrue(((ReactorAutomationPortBlockEntity)h.getBlockEntity(PORT)).settings().targetHeatHuPerTick()==2,"Create通用包绕过专用绑定戳");
            } catch(ReflectiveOperationException e) {throw new IllegalStateException("真实Create通用包路由失败",e);}
            var settings=((ReactorAutomationPortBlockEntity)h.getBlockEntity(PORT)).settings();
            var other=player(h); var b=o.automationBinding();
            accepted(h,command(h,p,ReactorAutomationService.Command.HEAT,2,8));
            h.assertTrue(!ReactorAutomationService.applyCommand(other,new ReactorAutomationService.Request(b.burnerPos(),b.portPos(),b.ownerGeneration(),b.authorityEpoch(),ReactorAutomationService.Command.HEAT,3,1)).accepted(),"两玩家旧代际竞争未拒绝");
            h.assertTrue(!command(h,p,ReactorAutomationService.Command.HEAT,1000001,9).accepted(),"目标越界被接受");
            p.setGameMode(GameType.SPECTATOR);h.assertTrue(!command(h,p,ReactorAutomationService.Command.MODE,1,10).accepted(),"旁观者可以控制");
            h.assertTrue(((ReactorAutomationPortBlockEntity)h.getBlockEntity(PORT)).settings().equals(settings),"拒绝请求改变了设定");
            var activeEpoch=o.automationState().authorityEpoch();
            h.setBlock(PORT,P1Blocks.REACTOR_CASING.get());
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.FAULT && o.automationState().authorityEpoch()==activeEpoch+1
                    && depth(o)==1 && ReactorAutomationBlazeBridge.runPermission(h.getLevel(),h.absolutePos(BURNER))==0,"运行中拆接口未立即撤权及换epoch");
            h.setBlock(PORT,ReactorAutomationContent.PORT.get());
            var unclaimedReplacement=ReactorAutomationService.candidate(o);
            p.setGameMode(GameType.SURVIVAL);
            h.assertTrue(!ReactorAutomationService.applyCommand(p,new ReactorAutomationService.Request(unclaimedReplacement.burnerPos(),unclaimedReplacement.portPos(),unclaimedReplacement.ownerGeneration(),unclaimedReplacement.authorityEpoch(),ReactorAutomationService.Command.HEAT,3,10)).accepted(),"新接口正式交接前仅凭候选坐标接受命令");
            o.tickReactor();h.assertTrue(!o.automationState().runPermission() && o.automationState().phase()==ReactorAutomationState.Phase.FAULT,"接口原位重装自行恢复运行");h.succeed();
        });
    }
    @GameTest(template="automation_empty",timeoutTicks=220) public static void fault_latch(GameTestHelper h) {
        var o=setup(h);var p=player(h);
        h.runAfterDelay(5,()->accepted(h,command(h,p,ReactorAutomationService.Command.MODE,1,1)));
        h.runAfterDelay(11,()->{accepted(h,command(h,p,ReactorAutomationService.Command.DEPTH,40,2));h.setBlock(BURNER,Blocks.AIR);});
        h.runAfterDelay(16,()->{
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.FAULT && depth(o)==1 && o.snapshot().scramRequested(),"燃烧室真实拆除未由反相线路停堆保持");
            placeBurner(h,BURNER);
        });
        h.runAfterDelay(23,()->{
            h.assertTrue(!command(h,p,ReactorAutomationService.Command.RESET,1,3).accepted(),"健康未满100tick提前复位");
            // 独立SCRAM高/低沿不允许恢复拆除前40%目标。
            o.updateRedstoneScram(false);h.assertTrue(depth(o)==1,"故障后下降沿反弹");o.updateRedstoneScram(true);
            h.assertTrue(!command(h,p,ReactorAutomationService.Command.MODE,1,4).accepted(),"燃料恢复或模式包自动清故障");
            // 夹具让首次急停遇到卡死浅目标；原钢板维修确实保留目标，随后释放必须覆盖新恢复的可动棒。
            o.setSnapshot(o.snapshot().withColumns(o.snapshot().fuelColumns(),Map.of(ROD,new ControlRodColumnState(0,.4,1,true,0))).withScramState(Map.of(),false));
            o.updateRedstoneScram(true);h.assertTrue(o.snapshot().scramSavedTargetDepths().isEmpty(),"夹具没有覆盖首次急停跳过卡死棒的边界");
            var drive=h.absolutePos(DRIVE);p.setPos(drive.getX()+.5,drive.getY()+.5,drive.getZ()+.5);
            var repaired=ControlRodSliderService.repairFromPlayer(p,drive,new ItemStack(ModItems.STEEL_PLATE.get()));
            h.assertTrue(repaired.success() && repaired.jammedCleared() && depth(o)==.4,"原维修入口未解除卡死并保留浅目标");
            o.updateRedstoneScram(false);o.tickReactor();
            var repairedRod=o.snapshot().controlRodColumns().get(ROD);
            System.out.println("[automation-repaired-scram] target="+repairedRod.targetDepth()+" actual="+repairedRod.actualDepth());
            h.assertTrue(repairedRod.targetDepth()==1 && repairedRod.actualDepth()==1,"持锁维修棒在急停降沿/正式tick恢复浅目标");
            // 真实粉尘已拆，维修发生在最后一次低沿之后；同一gameTime再次正式结算也必须先钳住目标。
            h.setBlock(new BlockPos(5,4,3),Blocks.AIR);
            h.assertTrue(!h.getLevel().hasNeighborSignal(h.absolutePos(OWNER)),"维修无下降沿代表的真实线路仍有高电平");
            o.setSnapshot(o.snapshot().withColumns(o.snapshot().fuelColumns(),Map.of(ROD,new ControlRodColumnState(0,.4,1,true,0))).withScramState(Map.of(),false));
            var noEdgeRepair=ControlRodSliderService.repairFromPlayer(p,drive,new ItemStack(ModItems.STEEL_PLATE.get()));
            h.assertTrue(noEdgeRepair.success() && noEdgeRepair.jammedCleared() && depth(o)==.4,"无下降沿夹具原维修没有保留浅目标");
            long safeEpoch=o.automationState().authorityEpoch();o.tickReactor();
            var noEdgeRod=o.snapshot().controlRodColumns().get(ROD);
            System.out.println("[automation-repaired-no-edge] input="+h.getLevel().hasNeighborSignal(h.absolutePos(OWNER))+" target="+noEdgeRod.targetDepth()+" actual="+noEdgeRod.actualDepth()+" epochStable="+(o.automationState().authorityEpoch()==safeEpoch));
            h.assertTrue(noEdgeRod.targetDepth()==1 && noEdgeRod.actualDepth()==1 && o.automationState().authorityEpoch()==safeEpoch,"无下降沿维修后安全基准漏过或推进epoch");
            p.setPos(h.absolutePos(BURNER).getX()+.5,h.absolutePos(BURNER).getY()+.5,h.absolutePos(BURNER).getZ()+.5);
        });
        h.runAfterDelay(125,()->{
            accepted(h,command(h,p,ReactorAutomationService.Command.RESET,1,5));
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.STOPPED && !o.automationState().runPermission() && depth(o)==1,"复位未只返回停堆");
            var burner=(BlazeBurnerBlockEntity)h.getBlockEntity(BURNER);var tag=burner.saveWithoutMetadata(h.getLevel().registryAccess());tag.putInt("burnTimeRemaining",1);burner.loadWithComponents(tag,h.getLevel().registryAccess());
        });
        h.runAfterDelay(130,()->{
            h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.FAULT && !ReactorAutomationBlazeBridge.isDutyValid((BlazeBurnerBlockEntity)h.getBlockEntity(BURNER)),"原生燃烧tick耗尽没有撤销值班");
            feed(h,BURNER); h.assertTrue(o.automationState().phase()==ReactorAutomationState.Phase.FAULT,"补煤自动恢复运行");h.succeed();
        });
    }
    @GameTest(template="automation_empty",timeoutTicks=100) public static void lifecycle_isolation(GameTestHelper h) {
        var o=setup(h);var p=player(h);
        h.runAfterDelay(5,()->{
            var old=o.automationBinding();var stale=new ReactorAutomationService.Request(old.burnerPos(),old.portPos(),old.ownerGeneration(),old.authorityEpoch(),ReactorAutomationService.Command.HEAT,42,1);
            var previousPort=h.getBlockEntity(PORT);
            // 同一服务器回合真实拆装，最终几何不变且中途不强制重扫；旧面板不能继承新BE权限。
            h.setBlock(PORT,P1Blocks.REACTOR_CASING.get());
            h.assertTrue(o.automationState().authorityEpoch()>old.authorityEpoch() && !o.automationState().runPermission(),"安全停堆下拆接口未立即推进epoch及撤权");
            h.setBlock(PORT,ReactorAutomationContent.PORT.get());
            h.assertTrue(h.getBlockEntity(PORT)!=previousPort,"同回合重装没有产生新接口BE");
            o.tickReactor();
            var replacementResult=ReactorAutomationService.applyCommand(p,stale);
            System.out.println("[automation-port-replacement] old="+old+" current="+o.automationBinding()+" accepted="+replacementResult.accepted()+" reason="+replacementResult.reason());
            h.assertTrue(!replacementResult.accepted(),"同回合接口原位重装接受旧面板请求");
            var event=new BlockEvent.BreakEvent(h.getLevel(),h.absolutePos(PORT),h.getBlockState(PORT),p); ReactorStructureLifecycle.onBlockBreak(event);
            h.assertTrue(!event.isCanceled(),"全插入安全接口不能拆除");
            h.setBlock(PORT,P1Blocks.REACTOR_CASING.get());ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));
            h.assertTrue(o.automationState().claimed() && !o.automationState().runPermission(),"接口移除丢失持锁");
            h.setBlock(PORT,ReactorAutomationContent.PORT.get());ReactorStructureLifecycle.rescanInstrumentPortNow(h.getLevel(),h.absolutePos(OWNER));
            o.tickReactor();h.assertTrue(!ReactorAutomationService.applyCommand(p,stale).accepted(),"同坐标重组成型接受旧请求");
            var port=(ReactorAutomationPortBlockEntity)h.getBlockEntity(PORT); port.applySettings(new ReactorAutomationPortBlockEntity.Settings(ReactorAutomationState.Mode.CONSTANT,42,250));
            var tag=port.saveForServerTest(h.getLevel().registryAccess());port.loadForServerTest(tag,h.getLevel().registryAccess());h.assertTrue(port.settings().targetHeatHuPerTick()==250,"设定NBT未保留");
            var ownerTag=o.saveForServerTest(h.getLevel().registryAccess());o.loadForServerTest(ownerTag,h.getLevel().registryAccess());
            h.assertTrue(o.automationState().claimed() && o.automationState().phase()==ReactorAutomationState.Phase.STOPPED && !o.automationState().runPermission(),"BE重载未安全持锁停堆");
            h.assertTrue(!o.automationGeneration().equals(old.ownerGeneration()),"BE重载沿用旧控制代际");h.succeed();
        });
    }
    @GameTest(template="automation_empty",timeoutTicks=80) public static void native_burner(GameTestHelper h) {
        var pos=new BlockPos(8,1,8);h.setBlock(pos,AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.SMOULDERING));
        var burner=(BlazeBurnerBlockEntity)h.getBlockEntity(pos);var point=new AllArmInteractionPointTypes.BlazeBurnerType().createPoint(h.getLevel(),h.absolutePos(pos),h.getBlockState(pos));
        int before=burner.getRemainingBurnTime();var input=new ItemStack(Items.COAL);
        h.assertTrue(point.insert(null,input,true).isEmpty() && burner.getRemainingBurnTime()==before && input.getCount()==1,"原生机械臂模拟改变燃料");
        h.assertTrue(point.insert(null,input,false).isEmpty() && burner.getRemainingBurnTime()>0,"原生机械臂正式投煤失败");
        h.runAfterDelay(4,()->{
            h.assertTrue(ReactorAutomationBlazeBridge.findBinding(h.getLevel(),h.absolutePos(pos)).isEmpty() && ReactorAutomationBlazeBridge.runPermission(h.getLevel(),h.absolutePos(pos))==0,"普通燃烧室错误任职");
            System.out.println("[native-burner] source="+h.getBlockState(pos).isSignalSource()+" marker="+h.getBlockState(pos).getValue(ReactorAutomationBlazeBridge.SIGNAL_SOURCE)+" state="+h.getBlockState(pos)+" heat="+burner.getHeatLevelFromBlock()+" analog="+h.getBlockState(pos).getAnalogOutputSignal(h.getLevel(),h.absolutePos(pos)));
            h.assertTrue(!h.getBlockState(pos).isSignalSource() && h.getBlockState(pos).getAnalogOutputSignal(h.getLevel(),h.absolutePos(pos))==Math.max(0,burner.getHeatLevelFromBlock().ordinal()-1),"原比较器或无绑定信号改变");
            for(var type:ReactorAutomationBehaviours.TYPES) h.assertTrue(!burner.getBehaviour(type).isActive(),"无绑定控件显示");
            var fullInput=new ItemStack(Items.COAL); int remaining=burner.getRemainingBurnTime();
            var fullResult=BlazeBurnerBlock.tryInsert(h.getBlockState(pos),h.getLevel(),h.absolutePos(pos),fullInput,false,false,false);
            h.assertTrue(!fullResult.getResult().consumesAction() && fullInput.getCount()==1 && burner.getRemainingBurnTime()==remaining,"原生满燃料拒绝交互改变");h.succeed();
        });
    }
}
