package com.iksxh.create_nuclear_industry.automation;

import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.content.ReactorAutomationContent;
import com.iksxh.create_nuclear_industry.reactor.*;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import java.util.*;
import static com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.*;

/**
 * 服务端游戏线程的本版本正常停服检查点与有限加载交接。
 * NBT只保存模式、目标、棒位调节进度和坐标；新实例不恢复旧授权、观察样本或付热凭据。
 * 等待时间使用当前世界gameTime（tick），预算用原HU热账本和mB冷却库存；等待不推进核结算。
 */
public final class ReactorAutomationResume {
    public static final String KEY="AutomationResume";
    private static final Set<MinecraftServer> NORMAL_STOPPING=Collections.newSetFromMap(new WeakHashMap<>());
    private ReactorAutomationResume() { }
    /** 只能由锁定NeoForge正常ServerStoppingEvent进入；崩溃的isStopped本身不证明正常退出。 */
    public static void beginNormalShutdown(MinecraftServer server) {NORMAL_STOPPING.add(server);}
    public static void endNormalShutdown(MinecraftServer server) {NORMAL_STOPPING.remove(server);}
    public static boolean normalShutdownActive(ReactorInstrumentPortBlockEntity owner) {
        var server=owner.getLevel()==null?null:owner.getLevel().getServer();
        return owner.automationState().claimed()&&server!=null&&server.isStopped()&&NORMAL_STOPPING.contains(server);
    }
    /** 正常退出事件在最终保存/卸载之前捕获；后续解绑不覆盖该非权威检查点，普通autosave不带续接标记。 */
    public static void prepareNormalShutdown(ReactorInstrumentPortBlockEntity owner) {
        var session=owner.automationResumeSession();var binding=owner.automationBinding();
        if(session!=null)owner.setAutomationNormalStopCheckpoint(encode(session.intent,session.settings,session.port,session.burner,true));
        else {
            var settings=owner.automationPort()==null?new ReactorAutomationPortBlockEntity.Settings(Mode.MANUAL,100,0):owner.automationPort().settings();
            boolean normal=owner.automationState().claimed()&&binding!=null&&ReactorAutomationService.hasCurrentInstances(owner,binding)
                    &&owner.automationObservation()!=null&&owner.automationBurner() instanceof BlazeBurnerBlockEntity b&&ReactorAutomationHeatSupply.dutyValid(b);
            var port=binding==null?owner.getBlockPos():binding.portPos();var burner=binding==null?port.west():binding.burnerPos();
            owner.setAutomationNormalStopCheckpoint(encode(owner.automationState(),settings,port,burner,normal));
        }
        owner.setChanged();
    }
    /** 持久化仅含模式/目标/调节位置和成员坐标；不写generation、epoch、租约或热源资格。 */
    public static CompoundTag encode(ReactorAutomationState state,ReactorAutomationPortBlockEntity.Settings settings,BlockPos port,BlockPos burner,boolean normalSave) {
        var tag=new CompoundTag();tag.putInt("Version",1);tag.putString("Phase",state.phase().name());tag.putString("Reason",state.reason());
        tag.putInt("CommandedDepth",state.commandedDepthPercent());tag.putInt("AdjustmentAge",state.adjustmentAge());tag.putInt("ArmingAge",state.armingAge());tag.putInt("Mode",state.requestedMode().ordinal());
        tag.putInt("SettingMode",settings.mode().ordinal());tag.putInt("SettingDepth",settings.manualDepthPercent());tag.putInt("SettingHeat",settings.targetHeatHuPerTick());
        tag.putLong("Port",port.asLong());tag.putLong("Burner",burner.asLong());tag.putBoolean("NormalSave",normalSave);return tag;
    }
    /** NBT只读分类结果：state没有运行许可；session只表示待重新核验意图，savedScram保留初始低沿锁。 */
    public record Loaded(ReactorAutomationState state,Session session,boolean savedScram) { }
    private static ReactorAutomationState inactive(Phase phase,String reason,boolean claimed) {return new ReactorAutomationState(claimed,phase,reason,100,1,0,0,0,0,Mode.STOP,false);}
    /** 只读本版本信封；正常运行只还原无许可的ARMING，保存故障/急停不能借初始化变成可直接START。 */
    public static Loaded decode(CompoundTag tag,boolean claimed,ReactorSnapshot snapshot) {
        if(!claimed)return new Loaded(ReactorAutomationState.initial(),null,false);
        boolean scram=snapshot.scramRequested();
        if(tag==null)return new Loaded(inactive(scram?Phase.FAULT:Phase.STOPPED,scram?"external_scram":"stopped",true),null,scram);
        try {
            for(String key:List.of("Version","CommandedDepth","AdjustmentAge","ArmingAge","Mode","SettingMode","SettingDepth","SettingHeat"))if(!tag.contains(key,Tag.TAG_INT))throw new IllegalArgumentException();
            if(!tag.contains("Phase",Tag.TAG_STRING)||!tag.contains("Reason",Tag.TAG_STRING)||!tag.contains("Port",Tag.TAG_LONG)||!tag.contains("Burner",Tag.TAG_LONG)||!tag.contains("NormalSave",Tag.TAG_BYTE)||tag.getAllKeys().size()!=13||tag.getInt("Version")!=1)throw new IllegalArgumentException();
            var phase=Phase.valueOf(tag.getString("Phase"));var reason=tag.getString("Reason");
            int mode=tag.getInt("Mode"),settingMode=tag.getInt("SettingMode"),depth=tag.getInt("CommandedDepth"),age=tag.getInt("AdjustmentAge"),arming=tag.getInt("ArmingAge");
            if(reason.length()>128||mode<0||mode>=Mode.values().length||settingMode<0||settingMode>=Mode.values().length||age<0||age>1000000||arming<0||arming>1000000)throw new IllegalArgumentException();
            var settings=new ReactorAutomationPortBlockEntity.Settings(Mode.values()[settingMode],tag.getInt("SettingDepth"),tag.getInt("SettingHeat"));
            var intent=new ReactorAutomationState(true,phase,reason,depth,1,age,0,0,arming,Mode.values()[mode],false);
            var port=BlockPos.of(tag.getLong("Port"));var burner=BlockPos.of(tag.getLong("Burner"));
            if(port.getY()!=burner.getY()||port.distManhattan(burner)!=1)throw new IllegalArgumentException();
            if(phase==Phase.FAULT)return new Loaded(inactive(Phase.FAULT,reason,true),null,scram);
            // 人工STOP自然使反相安全线变高；保存该SCRAM不把明确STOP升级为故障，也不释放其快照。
            if(phase==Phase.STOPPED)return new Loaded(inactive(Phase.STOPPED,"stopped",true),null,scram);
            if(scram)return new Loaded(inactive(Phase.FAULT,"external_scram",true),null,true);
            if(!tag.getBoolean("NormalSave"))return new Loaded(inactive(Phase.STOPPED,"stopped",true),null,false);
            if(settings.mode()==Mode.STOP||intent.requestedMode()!=settings.mode()||phase==Phase.MANUAL&&settings.mode()!=Mode.MANUAL||phase==Phase.CONSTANT&&settings.mode()!=Mode.CONSTANT||settings.mode()==Mode.CONSTANT&&settings.targetHeatHuPerTick()==0)throw new IllegalArgumentException();
            var session=new Session(intent,settings,port,burner);
            return new Loaded(new ReactorAutomationState(true,Phase.ARMING,"arming",depth,1,age,0,0,arming,settings.mode(),false),session,false);
        } catch(IllegalArgumentException corrupt) {return new Loaded(inactive(Phase.FAULT,"invalid_value",true),null,scram);}
    }
    /** 单个新仪表实例的交接状态；成员引用和预算只在当前世界核验，绝不编码到NBT。 */
    public static final class Session {
        private final ReactorAutomationState intent;
        private final ReactorAutomationPortBlockEntity.Settings settings;
        private final BlockPos port,burner;
        private long firstTime=Long.MIN_VALUE,lastTime=Long.MIN_VALUE;
        private boolean feedback,firstSettlement,nuclearRequired;
        private double cost;
        private Object burnerInstance,portInstance;
        private Set<CoreColumnPosition> fuelLayout;
        private UUID structureGeneration;
        private ReactorAutomationBinding currentBinding;
        private Session(ReactorAutomationState intent,ReactorAutomationPortBlockEntity.Settings settings,BlockPos port,BlockPos burner) {this.intent=intent;this.settings=settings;this.port=port;this.burner=burner;}
        public ReactorAutomationState intent() {return intent;}
        /** 待续接的保存偏好，仅用于当前实例重新核验，不含运行许可。 */
        public ReactorAutomationPortBlockEntity.Settings settings() {return settings;}
        /** 本次新实例的初始化tick窗口；重复调用不延长，逆行时间拒绝，离线墙钟不计入。 */
        public boolean withinWait(long time,int timeoutTicks) {
            if(time<0||timeoutTicks<=0||lastTime!=Long.MIN_VALUE&&time<lastTime)return false;
            if(firstTime==Long.MIN_VALUE)firstTime=time;lastTime=time;
            return time-firstTime<timeoutTicks;
        }
    }
    public static boolean pending(ReactorInstrumentPortBlockEntity owner) {return owner.automationResumeSession()!=null;}
    /** 检查点或停止状态的保存副本；仅正常停服捕获的副本带NormalSave，autosave不能令失联自动恢复。 */
    public static CompoundTag checkpointForSave(ReactorInstrumentPortBlockEntity owner) {
        var normal=owner.automationNormalStopCheckpoint();if(normal!=null)return normal.copy();
        var s=owner.automationResumeSession();if(s!=null)return encode(s.intent,s.settings,s.port,s.burner,false);
        var b=owner.automationBinding();var port=b==null?owner.getBlockPos():b.portPos();
        var settings=owner.automationPort()==null?new ReactorAutomationPortBlockEntity.Settings(Mode.MANUAL,100,0):owner.automationPort().settings();
        return encode(owner.automationState(),settings,port,b==null?port.west():b.burnerPos(),false);
    }
    /** 完整物理FUEL布局来自本版本保存证据，核账本允许未装料列缺key；控制棒全映射仍必须相等。 */
    public static boolean matchingColumns(ReactorInstrumentPortBlockEntity owner) {
        if(!owner.structureValid()||!owner.snapshot().dimensions().equals(owner.structureScan().dimensions()))return false;
        var fuels=new HashSet<CoreColumnPosition>();var rods=new HashSet<CoreColumnPosition>();
        owner.structureScan().columns().forEach((p,c)->{if(c.type()==ReactorStructureDefinition.ColumnType.FUEL)fuels.add(p);if(c.type()==ReactorStructureDefinition.ColumnType.CONTROL_ROD)rods.add(p);});
        var session=owner.automationResumeSession();var saved=session!=null&&session.fuelLayout!=null?session.fuelLayout:owner.automationLoadedFuelLayout();
        if(!matchesSavedLayout(fuels,rods,saved,owner.snapshot()))return false;
        if(session!=null) {
            if(session.structureGeneration!=null&&!session.structureGeneration.equals(owner.automationGeneration()))return false;
            session.fuelLayout=Set.copyOf(saved);session.structureGeneration=owner.automationGeneration();
        }
        return true;
    }
    /** 只核集合归属，不补造未装料核状态；缺失完整保存布局不能以任意subset替代。 */
    static boolean matchesSavedLayout(Set<CoreColumnPosition> fuels,Set<CoreColumnPosition> rods,Set<CoreColumnPosition> saved,ReactorSnapshot snapshot) {
        return saved!=null&&fuels.equals(saved)&&saved.containsAll(snapshot.fuelColumns().keySet())&&rods.equals(snapshot.controlRodColumns().keySet());
    }
    /** 首扫已验证的当前代际一旦固定，等待期重扫不能再借“初次加载”绕过撤权。 */
    public static boolean hasVerifiedStructure(ReactorInstrumentPortBlockEntity owner) {var s=owner.automationResumeSession();return s!=null&&s.structureGeneration!=null;}
    public static void fail(ReactorInstrumentPortBlockEntity owner,String reason) {
        var session=owner.automationResumeSession();
        owner.setAutomationResumeSession(null);owner.setAutomationNormalStopCheckpoint(null);ReactorAutomationHeatSupply.invalidate(owner);ReactorAutomationService.revoke(owner,reason);
        // 临时反馈的15独立于state.runPermission；撤销时不能只比较false→false而漏掉真实下降沿通知。
        if(session!=null&&session.feedback)ReactorAutomationBlazeBridge.notifyPermission(owner,session.burner);
    }
    private static boolean externalDuty(BlazeBurnerBlockEntity b) {return b.isCreative()?ReactorAutomationBlazeBridge.dutyLevelValid(b.getHeatLevelFromBlock()):b.getActiveFuel()!=BlazeBurnerBlockEntity.FuelType.NONE&&b.getRemainingBurnTime()>0;}
    private static ReactorAutomationObservation currentBudget(ReactorInstrumentPortBlockEntity owner,long time) {
        var snapshot=owner.snapshot();var parameters=owner.automationSimulationParameters();
        // 反馈getter可能经共同导体被红石输入查询重入；预算入口不再查询红石。正式核tick另读真实输入。
        // duty仅作为尚未首次付费的预算前提，不发布样本或授热；当前事务仍须真实足额支付。
        return new ReactorAutomationObservation(snapshot,parameters,owner.automationCoolantInput(),0,0,false,
                ReactorAutomationService.hasCurrentInstances(owner,owner.automationBinding()),true,false,time,time,
                owner.automationPort().settings(),owner.automationCurve().curve(snapshot,parameters),
                com.iksxh.create_nuclear_industry.config.P1ServerConfig.VALUES.coolantAbsorptionHuPerMb.get()>0);
    }
    /** 预算与首付提交都核当前owner/候选/绑定及具体成员，旧代际不能因同坐标仍有BE被认可。 */
    private static String currentIdentityFailure(ReactorInstrumentPortBlockEntity owner,Session s) {
        if(s.structureGeneration==null||!s.structureGeneration.equals(owner.automationGeneration()))return "structure_changed";
        if(s.currentBinding==null||owner.getLevel()==null||owner.getLevel().getBlockEntity(owner.getBlockPos())!=owner
                ||!s.currentBinding.equals(owner.automationBinding())||!s.currentBinding.equals(ReactorAutomationService.candidate(owner))
                ||!ReactorAutomationService.hasCurrentInstances(owner,s.currentBinding)||owner.automationPort()!=s.portInstance||owner.automationBurner()!=s.burnerInstance)return "binding_lost";
        return "";
    }
    private static String checkBudget(ReactorInstrumentPortBlockEntity owner,Session s,ReactorAutomationParameters p,long time) {
        String identityFailure=currentIdentityFailure(owner,s);if(!identityFailure.isEmpty())return identityFailure;
        if(!owner.automationPort().settings().equals(s.settings))return "stale_settings";
        if(owner.snapshot().scramRequested())return "external_scram";
        var sample=currentBudget(owner,time);String trip=ReactorAutomationController.tripReason(sample,p);if(!trip.isEmpty())return trip;
        var pending=ReactorAutomationController.warning(sample,p)?ReactorAutomationController.safetySnapshot(sample.snapshot(),0):sample.snapshot();
        if(!ReactorAutomationController.coolantCanCarrySnapshot(sample,pending))return "cold_shortage";
        var burner=(BlazeBurnerBlockEntity)s.burnerInstance;s.nuclearRequired=!externalDuty(burner);s.cost=p.burnerMaintenanceHuPerTick();
        if(s.nuclearRequired) {
            var advanced=ReactorControlRodTick.advance(pending);var fission=ReactorFissionCalculator.calculate(advanced,sample.reactorParameters());
            if(ReactorMaintenanceHeat.settle(advanced,fission,s.cost).paidHeatHu()<s.cost)return "duty_lost";
        }
        return "";
    }
    /** 首次核事务之前的有限轮询；等待冻结整机，当前保护通过才发新反馈，真实低沿后允许一次原棒结算。 */
    public static boolean allowSettlement(ReactorInstrumentPortBlockEntity owner,ReactorAutomationParameters p,long time,boolean fuelReady) {
        var s=owner.automationResumeSession();if(s==null)return true;
        if(!s.withinWait(time,p.armingTimeoutTicks())) {fail(owner,"arming_timeout");return false;}
        if(!owner.structureValid()) {
            var code=owner.structureScan().diagnosticCode();
            if(owner.structureScanCount()>0&&code!=ReactorStructureDefinition.DiagnosticCode.CHUNKS_UNAVAILABLE)fail(owner,"structure_changed");
            return false;
        }
        if(!matchingColumns(owner)) {fail(owner,"structure_changed");return false;}
        if(!fuelReady)return false;
        var level=owner.getLevel();
        if(!level.hasChunkAt(s.port)||!level.hasChunkAt(s.burner))return false;
        if(!level.getBlockState(s.port).is(ReactorAutomationContent.PORT.get())||!level.getBlockState(s.burner).is(AllBlocks.BLAZE_BURNER.get())) {fail(owner,"binding_lost");return false;}
        var binding=ReactorAutomationService.candidate(owner);
        if(binding==null)return false;
        if(!binding.portPos().equals(s.port)||!binding.burnerPos().equals(s.burner)) {fail(owner,"binding_lost");return false;}
        if(s.portInstance==null) {
            s.portInstance=level.getBlockEntity(s.port);s.burnerInstance=level.getBlockEntity(s.burner);
            s.currentBinding=binding;
            owner.setAutomationBinding(binding);owner.setAutomationPort((ReactorAutomationPortBlockEntity)s.portInstance);owner.setAutomationBurner(s.burnerInstance);
        }
        String bad=checkBudget(owner,s,p,time);if(!bad.isEmpty()) {fail(owner,bad);return false;}
        if(!s.feedback) {
            s.feedback=true;
            var state=level.getBlockState(s.burner);if(state.hasProperty(ReactorAutomationBlazeBridge.SIGNAL_SOURCE)&&!state.getValue(ReactorAutomationBlazeBridge.SIGNAL_SOURCE))level.setBlock(s.burner,state.setValue(ReactorAutomationBlazeBridge.SIGNAL_SOURCE,true),3);
            ReactorAutomationBlazeBridge.notifyPermission(owner,s.burner);return false;
        }
        if(level.hasNeighborSignal(owner.getBlockPos()))return false;
        var sample=currentBudget(owner,time);
        if(ReactorAutomationController.warning(sample,p))owner.setSnapshot(ReactorAutomationController.safetySnapshot(owner.snapshot(),0));
        s.firstSettlement=true;return true;
    }
    /** 新生成的加载反馈只驱动反相线；不供核热、不恢复旧样本/15，也不跳过后续实际高信号保护。 */
    public static boolean feedbackPermission(ReactorInstrumentPortBlockEntity owner) {
        var s=owner.automationResumeSession();if(s==null||!s.feedback||owner.getLevel()==null)return false;
        try {var p=com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.parameters();return s.withinWait(owner.getLevel().getGameTime(),p.armingTimeoutTicks())&&checkBudget(owner,s,p,owner.getLevel().getGameTime()).isEmpty();}
        catch(RuntimeException badConfig) {return false;}
    }
    public static boolean firstSettlement(ReactorInstrumentPortBlockEntity owner) {var s=owner.automationResumeSession();return s!=null&&s.firstSettlement;}
    public static boolean requiresNewNuclearPayment(ReactorInstrumentPortBlockEntity owner) {var s=owner.automationResumeSession();return s!=null&&s.firstSettlement&&s.nuclearRequired;}
    /** 只在原核事务已提交且新付款出票之后完成；无备用燃料者付费失败立即撤权，不能继续预算许可。 */
    public static void finishPayment(ReactorInstrumentPortBlockEntity owner,ReactorServerTick.Result result) {
        var s=owner.automationResumeSession();if(s==null||!s.firstSettlement)return;
        String identityFailure=currentIdentityFailure(owner,s);if(!identityFailure.isEmpty()) {fail(owner,identityFailure);return;}
        if(s.nuclearRequired&&result.maintenance().paidHeatHu()<s.cost) {fail(owner,"duty_lost");return;}
        var old=s.intent;owner.setAutomationResumeSession(null);
        var next=new ReactorAutomationState(true,old.phase(),old.reason(),old.commandedDepthPercent(),Math.addExact(owner.automationState().authorityEpoch(),1),old.adjustmentAge(),0,0,old.armingAge(),old.requestedMode(),true);
        owner.setAutomationState(next);owner.setAutomationBinding(owner.automationBinding().withEpoch(next.authorityEpoch()));
    }
}
