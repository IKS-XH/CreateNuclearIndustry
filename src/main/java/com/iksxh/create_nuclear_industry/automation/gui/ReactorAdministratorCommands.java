package com.iksxh.create_nuclear_industry.automation.gui;

import com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.Mode;
import com.iksxh.create_nuclear_industry.automation.*;
import com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig;
import com.iksxh.create_nuclear_industry.control.ControlRodSliderService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
import static com.iksxh.create_nuclear_industry.automation.ReactorAutomationState.Phase;

/** 菜单四个独立动作；完整草稿一次验证，应用设置不等于启动。 */
public final class ReactorAdministratorCommands {
    public enum Action { APPLY, START, STOP, RESET }
    /** 完整数值草稿：棒深为百分比，目标为毛HU/t；客户端编辑不授予服务端运行许可。 */
    public record Draft(Mode mode,int depthPercent,int heatHuPerTick,int reservePercent,int minimumHeatHuPerTick) {
        public Draft(Mode mode,int depth,int heat) {this(mode,depth,heat,20,2);}
    }
    /** 每次打开的随机session独立于循环containerId；同一几何/设定也不能复用已关闭菜单。 */
    public record Request(int containerId,UUID sessionId,BlockPos burnerPos,BlockPos portPos,UUID ownerGeneration,long authorityEpoch,
                          long settingsRevision,long clientSequence,Action action,Draft draft) { }
    public record Response(boolean accepted,String reason,ReactorAdministratorProjection projection,long sequence) { }
    private ReactorAdministratorCommands() { }
    public static String validate(Draft draft,int maxHeat) {
        return draft==null || draft.mode()==null || draft.mode()==Mode.STOP || draft.depthPercent()<0 || draft.depthPercent()>100
                || draft.heatHuPerTick()<0 || draft.heatHuPerTick()>maxHeat
                ||draft.reservePercent()<0||draft.reservePercent()>90||draft.minimumHeatHuPerTick()<=0
                ||draft.mode()==Mode.LOAD_FOLLOW&&draft.minimumHeatHuPerTick()>maxHeat?"invalid_value":"";
    }
    /** 唯一GUI写入口：当前玩家菜单和真实实例是权威，客户端owner坐标不能转授权限。 */
    public static Response apply(ServerPlayer player,Request request) {
        if(player==null || request==null || !(player.containerMenu instanceof ReactorAdministratorMenu menu)
                || !player.getServer().isSameThread() || request.containerId()!=menu.containerId || !menu.sessionId().equals(request.sessionId()) || !menu.stillValid(player))
            return new Response(false,"menu_invalid",ReactorAdministratorProjection.unavailable(),request==null?0:request.clientSequence());
        var owner=menu.serverOwner(); var binding=owner.automationBinding(); var port=owner.automationPort();
        String reason="accepted"; boolean accepted=false;
        try {
            if(request.action()==null || request.burnerPos()==null || request.portPos()==null || request.ownerGeneration()==null
                    || !binding.burnerPos().equals(request.burnerPos()) || !binding.portPos().equals(request.portPos())
                    || !binding.ownerGeneration().equals(request.ownerGeneration()) || !menu.takeSequence(request.clientSequence()))
                throw new IllegalArgumentException("menu_invalid");
            // 立即停堆只依赖当前有效菜单身份，不因另一玩家的新版本或显示延迟而拒绝。
            if(request.action()!=Action.STOP && (request.authorityEpoch()!=owner.automationState().authorityEpoch()
                    || request.settingsRevision()!=port.settingsRevision())) throw new IllegalArgumentException("stale_settings");
            ReactorAutomationParameters parameters=null;
            // STOP只依赖真实菜单/结构身份，非法运行配置不能屏蔽紧急撤权；其他动作仍完整校验配置。
            if(request.action()!=Action.STOP) {
                try {parameters=ReactorAutomationConfig.parameters();}
                catch(IllegalArgumentException invalidConfig) {throw new IllegalArgumentException("invalid_config");}
            }
            var state=owner.automationState(); var settings=port.settings();
            var sample=owner.automationObservation();
            if(sample!=null) sample=ReactorAutomationService.observe(owner,sample.generatedHeatHuPerTick(),sample.convertedCoolantMb(),sample.newDamage(),sample.sampleTime(),player.level().getGameTime());
            boolean submitFull=false; int depth=100;
            switch(request.action()) {
                case APPLY -> {
                    reason=validate(request.draft(),parameters.maxTargetHeatHuPerTick());
                    if(!reason.isEmpty()) throw new IllegalArgumentException(reason);
                    var draft=request.draft(); boolean running=state.phase()==Phase.MANUAL || state.phase()==Phase.CONSTANT||state.phase()==Phase.LOAD_WARMUP||state.phase()==Phase.LOAD_FOLLOW;
                    // 建立联锁期间活动意图仍在requestedMode；改模式必须撤销旧启动，保存新偏好后另行START。
                    if(state.phase()==Phase.ARMING && draft.mode()!=state.requestedMode()) {
                        state=state.stop("mode_changed",false);submitFull=true;depth=100;
                    } else if(running) {
                        if(sample==null || !ReactorAutomationController.tripReason(sample,parameters).isEmpty() || sample.signalHigh()) throw new IllegalArgumentException("not_healthy");
                        if(draft.mode()==Mode.MANUAL && state.phase()==Phase.MANUAL) {
                            if(ReactorAutomationController.warning(sample,parameters) && owner.snapshot().controlRodColumns().values().stream()
                                    .anyMatch(rod->!rod.jammed() && draft.depthPercent()/100.0<Math.max(rod.targetDepth(),rod.actualDepth())-1e-9)) throw new IllegalArgumentException("warning");
                            if(!ReactorAutomationController.coolantCanCarryNextTick(sample,draft.depthPercent())) throw new IllegalArgumentException("cold_shortage");
                            submitFull=true; depth=draft.depthPercent();
                            state=new ReactorAutomationState(true,Phase.MANUAL,"manual",depth,state.authorityEpoch(),0,0,0,0,Mode.MANUAL,true);
                        } else if(draft.mode()==Mode.CONSTANT && state.phase()==Phase.CONSTANT) {
                            int desired=ReactorAutomationController.chooseDepth(sample.curve(),draft.heatHuPerTick());
                            int bounded=desired<state.commandedDepthPercent()?Math.max(desired,state.commandedDepthPercent()-parameters.maxLiftPercentPerAdjustment())
                                    :Math.min(desired,state.commandedDepthPercent()+parameters.maxInsertPercentPerAdjustment());
                            if(ReactorAutomationController.warning(sample,parameters) && draft.heatHuPerTick()>settings.targetHeatHuPerTick()) throw new IllegalArgumentException("warning");
                            if(!ReactorAutomationController.coolantCanCarryNextTick(sample,bounded)) throw new IllegalArgumentException("cold_shortage");
                        } else if(draft.mode()==Mode.LOAD_FOLLOW&&state.requestedMode()==Mode.LOAD_FOLLOW) {
                            if(ReactorAutomationController.warning(sample,parameters)&&draft.minimumHeatHuPerTick()>settings.minimumHeatHuPerTick())throw new IllegalArgumentException("warning");
                            var nextSettings=new ReactorAutomationPortBlockEntity.Settings(draft.mode(),draft.depthPercent(),draft.heatHuPerTick(),draft.reservePercent(),draft.minimumHeatHuPerTick());
                            if(!ReactorAutomationService.validLoadBand(sample,nextSettings,parameters))throw new IllegalArgumentException("no_operating_band");
                        } else { state=state.stop("mode_changed",false); submitFull=true; depth=100; }
                    }
                    if(draft.mode()==Mode.CONSTANT && draft.heatHuPerTick()==0) {
                        if(state.phase()!=Phase.FAULT) state=state.stop("stopped",false);
                        submitFull=true; depth=100;
                    }
                    settings=new ReactorAutomationPortBlockEntity.Settings(draft.mode(),draft.depthPercent(),draft.heatHuPerTick(),draft.reservePercent(),draft.minimumHeatHuPerTick());
                    if(port.settingsRevision()==Long.MAX_VALUE) throw new IllegalArgumentException("invalid_value");
                }
                case START -> {
                    if(state.phase()==Phase.FAULT) throw new IllegalArgumentException("reset_required");
                    if(state.phase()!=Phase.STOPPED || settings.mode()==Mode.STOP || settings.mode()==Mode.CONSTANT && settings.targetHeatHuPerTick()==0)
                        throw new IllegalArgumentException("startup_interlock");
                    if(sample==null || !ReactorAutomationController.startupHealthy(sample,parameters) || !sample.signalHigh()) throw new IllegalArgumentException("startup_interlock");
                    if(settings.mode()==Mode.MANUAL && !ReactorAutomationController.coolantCanCarryNextTick(sample,settings.manualDepthPercent())) throw new IllegalArgumentException("cold_shortage");
                    if(settings.mode()==Mode.LOAD_FOLLOW) {
                        String rejected=ReactorAutomationService.validateLoadStart(owner,sample,parameters);
                        if(!rejected.isEmpty())throw new IllegalArgumentException(rejected);
                    }
                    submitFull=true; depth=100;
                    state=new ReactorAutomationState(true,Phase.ARMING,"arming",100,state.authorityEpoch(),0,0,0,0,settings.mode(),true);
                }
                case STOP -> { if(state.phase()!=Phase.FAULT) state=state.stop("stopped",false); submitFull=true; depth=100; }
                case RESET -> {
                    if(state.phase()!=Phase.FAULT || state.healthyAge()<parameters.resetStableTicks() || sample==null
                            || !ReactorAutomationController.startupHealthy(sample,parameters)) throw new IllegalArgumentException("reset_not_stable");
                    state=state.stop("stopped",false); submitFull=true; depth=100;
                }
            }
            var next=new ReactorAutomationState(true,state.phase(),state.reason(),state.commandedDepthPercent(),Math.addExact(owner.automationState().authorityEpoch(),1),
                    state.adjustmentAge(),state.healthyAge(),0,state.armingAge(),state.requestedMode(),state.runPermission());
            // 所有检查结束才一次写设定和目标，完整权威状态提交后最后通知红石。
            if(request.action()==Action.APPLY) port.applySettings(settings);
            if(submitFull) ReactorAutomationService.submitDepth(owner,depth,depth==100);
            owner.setAutomationState(next); owner.setAutomationBinding(binding.withEpoch(next.authorityEpoch()));
            if(request.action()==Action.START&&settings.mode()==Mode.LOAD_FOLLOW)ReactorAutomationService.beginLoadStart(owner);
            ControlRodSliderService.revokeOwnerSessions(owner); ReactorAutomationBlazeBridge.notifyPermission(owner,binding.burnerPos());
            accepted=true; reason="accepted";
        } catch(IllegalArgumentException | ArithmeticException rejected) { reason=rejected.getMessage()==null?"invalid_value":rejected.getMessage(); }
        return new Response(accepted,reason,ReactorAdministratorProjection.from(owner,owner.automationBinding()),request.clientSequence());
    }
}
