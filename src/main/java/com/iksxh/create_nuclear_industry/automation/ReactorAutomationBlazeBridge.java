package com.iksxh.create_nuclear_industry.automation;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.simibubi.create.content.processing.burner.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import java.util.*;
/** Create桥只消费当前服务端授权；NBT view仅给客户端控件/护目镜，绝不控制真实物理。 */
public final class ReactorAutomationBlazeBridge {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty SIGNAL_SOURCE=net.minecraft.world.level.block.state.properties.BooleanProperty.create("cni_automation");
    public interface Access { CompoundTag cniAutomationView(); void cniAutomationView(CompoundTag view); }
    private ReactorAutomationBlazeBridge() {}
    public static Optional<ReactorAutomationBinding> findBinding(Level level,BlockPos burnerPos) {
        if(level==null || burnerPos==null || level.isClientSide || !level.hasChunkAt(burnerPos)
                || !(level.getBlockEntity(burnerPos) instanceof BlazeBurnerBlockEntity)) return Optional.empty();
        ReactorAutomationBinding found=null;
        for(Direction direction:Direction.values()) {
            BlockPos portPos=burnerPos.relative(direction);
            if(!level.hasChunkAt(portPos) || !(level.getBlockEntity(portPos) instanceof ReactorAutomationPortBlockEntity port)) continue;
            var owner=port.owner(); if(owner==null) continue;
            var binding=ReactorAutomationService.candidate(owner);
            if(binding==null || !binding.burnerPos().equals(burnerPos)) continue;
            if(found!=null && !found.equals(binding)) return Optional.empty(); found=binding;
        }
        return Optional.ofNullable(found);
    }
    public static ReactorInstrumentPortBlockEntity owner(Level level,ReactorAutomationBinding b) {
        return level!=null && b!=null && !level.isClientSide && b.dimension().equals(level.dimension().location().toString())
                && level.hasChunkAt(b.ownerPos()) && level.getBlockEntity(b.ownerPos()) instanceof ReactorInstrumentPortBlockEntity o
                && o.automationGeneration().equals(b.ownerGeneration()) ? o : null;
    }
    public static boolean isDutyValid(BlazeBurnerBlockEntity burner) { return ReactorAutomationHeatSupply.dutyValid(burner); }
    public static boolean dutyLevelValid(BlazeBurnerBlock.HeatLevel heat) {
        return heat==BlazeBurnerBlock.HeatLevel.FADING || heat==BlazeBurnerBlock.HeatLevel.KINDLED || heat==BlazeBurnerBlock.HeatLevel.SEETHING;
    }
    public static int runPermission(Level level,BlockPos pos) {
        var b=findBinding(level,pos).orElse(null); var o=owner(level,b);
        if(o!=null&&ReactorAutomationResume.feedbackPermission(o))return 15;
        if(o==null || !o.structureValid() || o.automationBinding()==null || !o.automationBinding().equals(b)
                || !o.automationState().runPermission() || !(level.getBlockEntity(pos) instanceof BlazeBurnerBlockEntity blaze)
                || !ReactorAutomationService.hasCurrentInstances(o,b) || !isDutyValid(blaze) || o.automationObservation()==null) return 0;
        long age=level.getGameTime()-o.automationObservation().sampleTime();
        try { return age>=0 && age<=com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.parameters().controlLeaseTicks()?15:0; }
        catch(RuntimeException invalidConfig) { return 0; }
    }
    /** 原燃烧室tick末更新派生标记和view。已知熄火即时撤销，未绑定时只移除本扩展。 */
    public static void tick(BlazeBurnerBlockEntity burner) {
        Level level=burner.getLevel(); if(level==null || level.isClientSide) return;
        var b=findBinding(level,burner.getBlockPos()).orElse(null); var o=owner(level,b);
        if(o!=null && o.automationState().claimed() && !ReactorAutomationResume.pending(o) && !isDutyValid(burner)) ReactorAutomationService.revoke(o,"duty_lost");
        var property=SIGNAL_SOURCE;
        var state=burner.getBlockState();
        if(state.hasProperty(property) && state.getValue(property)!=(b!=null)) level.setBlock(burner.getBlockPos(),state.setValue(property,b!=null),3);
        if(!(burner instanceof Access access)) return;
        int previousPermission=access.cniAutomationView().getInt("Permission");
        var view=ReactorAutomationGoggles.view(o,b);
        // 即使核tick未续样本，心跳到期的真实输出下降沿也必须通知反相线路。
        if(previousPermission!=view.getInt("Permission")) notifyPermission(level,burner.getBlockPos());
        if(!view.equals(access.cniAutomationView())) { access.cniAutomationView(view); burner.sendData(); }
        ReactorAutomationBehaviours.refresh(burner,view);
    }
    /** 完整授权提交后才通知红石，重入只能看到已经完成的控制状态。 */
    public static void notifyPermission(ReactorInstrumentPortBlockEntity owner,BlockPos burnerPos) {
        notifyPermission(owner.getLevel(),burnerPos);
    }
    public static void notifyPermission(Level level,BlockPos burnerPos) {
        if(level==null || level.isClientSide || burnerPos==null || !level.hasChunkAt(burnerPos)) return;
        var source=level.getBlockState(burnerPos).getBlock();
        level.updateNeighborsAt(burnerPos,source);
        // 弱信号进入相邻实心支撑格时，支撑格不会自行转发邻居通知；同步通知其火把/中继器。
        for(Direction direction:Direction.values()) {
            var support=burnerPos.relative(direction);
            if(level.hasChunkAt(support) && level.getBlockState(support).isRedstoneConductor(level,support)) level.updateNeighborsAt(support,source);
        }
    }
}
