package com.iksxh.create_nuclear_industry.automation;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.goggle.GoggleTooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.*;
/** 燃烧室只读同步view；首行遵守图标留白，平均值仅用于显示。 */
public final class ReactorAutomationGoggles {
    private ReactorAutomationGoggles() {}
    public static CompoundTag view(ReactorInstrumentPortBlockEntity o,ReactorAutomationBinding b) {
        var tag=new CompoundTag(); if(o==null || b==null) return tag;
        tag.putInt("Permission",ReactorAutomationBlazeBridge.runPermission(o.getLevel(),b.burnerPos()));
        tag.putString("Dimension",b.dimension()); tag.putLong("Owner",b.ownerPos().asLong()); tag.putUUID("Generation",b.ownerGeneration());
        tag.putLong("Port",b.portPos().asLong()); tag.putLong("Burner",b.burnerPos().asLong()); tag.putLong("Epoch",o.automationState().authorityEpoch());
        try {tag.putInt("MaxHeat",com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig.parameters().maxTargetHeatHuPerTick());}
        catch(IllegalArgumentException invalidConfig) {tag.putInt("MaxHeat",0);}
        tag.putString("Phase",o.automationState().phase().name()); tag.putString("Reason",o.automationState().reason());
        var port=(ReactorAutomationPortBlockEntity)o.getLevel().getBlockEntity(b.portPos());
        if(port!=null) { tag.putInt("Mode",port.settings().mode().ordinal()); tag.putInt("Depth",port.settings().manualDepthPercent()); tag.putInt("Heat",port.settings().targetHeatHuPerTick()); }
        if(port!=null) {
            tag.putInt("ReserveTarget",port.settings().reservePercent());tag.putInt("MinimumHeat",port.settings().minimumHeatHuPerTick());
            if(port.gaugeLink()!=null)tag.putString("Gauge",port.gaugeLink().position().toShortString());
            var session=o.automationLoadFollowSession();var stress=session==null?null:session.lastSample();
            tag.putBoolean("LoadingRebind",session!=null&&session.loading()&&!session.established());
            tag.putBoolean("StressValid",stress!=null&&stress.status()==ReactorStressSample.Status.VALID);
            if(stress!=null&&stress.reserveFraction().isPresent())tag.putDouble("StressReserve",stress.reserveFraction().orElseThrow()*100);
        }
        var sample=o.automationObservation();
        if(sample!=null) {
            tag.putDouble("ActualHeat",sample.generatedHeatHuPerTick()); tag.putDouble("AverageHeat",o.automationAverageHeat());
            tag.putDouble("DepthAverage",o.snapshot().controlRodColumns().values().stream().mapToDouble(r->r.actualDepth()*100).average().orElse(100));
            tag.putDouble("Cold",sample.coldFraction()*100); tag.putDouble("Hot",sample.hotFraction()*100);
            tag.putLong("Controlled",sample.controlledFuelCount()); tag.putLong("Fuel",sample.fuelCount()); tag.putLong("Jammed",sample.jammedCount()); tag.putDouble("Cooling",sample.convertedCoolantMb());
        }
        return tag;
    }
    public static boolean append(List<Component> tooltip,CompoundTag view) {
        if(!view.hasUUID("Generation")) return false;
        tooltip.add(GoggleTooltip.indentFirstLine(Component.translatable("automation.create_nuclear_industry.title")));
        tooltip.add(Component.translatable("automation.create_nuclear_industry.phase."+view.getString("Phase").toLowerCase(Locale.ROOT)));
        tooltip.add(Component.translatable("automation.create_nuclear_industry.power",view.getInt("Heat"),String.format(Locale.ROOT,"%.1f",view.getDouble("ActualHeat")),String.format(Locale.ROOT,"%.1f",view.getDouble("AverageHeat"))));
        tooltip.add(Component.translatable("automation.create_nuclear_industry.reason."+view.getString("Reason")));
        if(view.getInt("Mode")==ReactorAutomationState.Mode.LOAD_FOLLOW.ordinal()) {
            tooltip.add(Component.translatable("automation.create_nuclear_industry.gui.load_summary",view.getBoolean("StressValid")?String.format(Locale.ROOT,"%.1f%%",view.getDouble("StressReserve")):"—",view.getInt("ReserveTarget"),view.getInt("MinimumHeat")));
            tooltip.add(Component.translatable("automation.create_nuclear_industry.gui.gauge",view.getString("Gauge")));
        }
        return true;
    }
}
