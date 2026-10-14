package com.iksxh.create_nuclear_industry.automation.gui;

import com.iksxh.create_nuclear_industry.automation.*;
import com.iksxh.create_nuclear_industry.blockentity.ReactorInstrumentPortBlockEntity;
import com.iksxh.create_nuclear_industry.config.ReactorAutomationConfig;
import com.iksxh.create_nuclear_industry.reactor.FuelColumnState;
import com.iksxh.create_nuclear_industry.structure.ReactorStructureDefinition;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.IntArrayTag;
import java.util.Locale;

/** 有界只读菜单投影；只发送196格以内的数值，不发送物品NBT或核状态快照。 */
public record ReactorAdministratorProjection(CompoundTag data) {
    public ReactorAdministratorProjection {
        if(data==null || data.getList("Columns",Tag.TAG_COMPOUND).size()>196
                || data.getAllKeys().size()>64 || data.getString("Reason").length()>128 || data.getString("Phase").length()>32)
            throw new IllegalArgumentException("管理员显示投影超过预算");
        for(var key:data.getAllKeys()) {
            var value=data.get(key);
            if(key.equals("Columns")) {if(!(value instanceof ListTag list)||!list.isEmpty()&&list.getElementType()!=Tag.TAG_COMPOUND)throw new IllegalArgumentException("堆芯列表类型非法");continue;}
            if(value instanceof NumericTag number) {if(!Double.isFinite(number.getAsDouble()))throw new IllegalArgumentException("显示数值必须有限");}
            else if(value instanceof StringTag string) {if(string.getAsString().length()>128)throw new IllegalArgumentException("显示文字超长");}
            else if(!(key.equals("Generation")&&value instanceof IntArrayTag array&&array.getAsIntArray().length==4))throw new IllegalArgumentException("显示投影禁止嵌套核状态或物品NBT");
        }
        int width=data.getInt("ColumnsX"),rows=data.getInt("ColumnsZ");var cells=new java.util.HashSet<Integer>();
        for(var element:data.getList("Columns",Tag.TAG_COMPOUND)) {
            var c=(CompoundTag)element;int x=c.getInt("X"),z=c.getInt("Z");
            if(width<1||width>14||rows<1||rows>14||x<0||x>=width||z<0||z>=rows||!cells.add(x*14+z)||c.getAllKeys().size()>12)
                throw new IllegalArgumentException("堆芯显示坐标或字段预算非法");
            for(var key:c.getAllKeys()) {
                var value=c.get(key);
                if(value instanceof NumericTag number) {if(!Double.isFinite(number.getAsDouble()))throw new IllegalArgumentException("堆芯数值必须有限");}
                else if(!(value instanceof StringTag string)||string.getAsString().length()>32)throw new IllegalArgumentException("堆芯禁止物品或完整快照NBT");
            }
        }
        data=data.copy();
    }
    @Override public CompoundTag data() { return data.copy(); }
    public static ReactorAdministratorProjection unavailable() { return new ReactorAdministratorProjection(new CompoundTag()); }

    /** 仅当前服务端所有者/已加载成员派生；HU/t为毛功率，库存为mB，棒深为百分比。 */
    public static ReactorAdministratorProjection from(ReactorInstrumentPortBlockEntity owner,ReactorAutomationBinding binding) {
        if(owner==null || binding==null || owner.getLevel()==null || !owner.structureValid()
                || !ReactorAutomationService.hasCurrentInstances(owner,binding)) return unavailable();
        var tag=ReactorAutomationGoggles.view(owner,binding);
        tag.putBoolean("Valid",true);
        tag.putBoolean("ConfigurationValid",true);
        com.iksxh.create_nuclear_industry.automation.ReactorAutomationParameters p;
        try {p=ReactorAutomationConfig.parameters();ReactorAutomationConfig.loadFollowParameters();}
        catch(IllegalArgumentException invalidConfig) {
            // 配置故障不是关联丢失：保留真实身份/phase供查看和STOP，不伪造保护阈值或普通数据。
            tag.putBoolean("ConfigurationValid",false);tag.putString("Reason","invalid_config");
            return new ReactorAdministratorProjection(tag);
        }
        var snapshot=owner.snapshot(); var dimensions=snapshot.dimensions();
        tag.putInt("Width",dimensions.width()); tag.putInt("Height",dimensions.height()); tag.putInt("SizeZ",dimensions.depth());
        tag.putInt("ColumnsX",dimensions.internalWidth()); tag.putInt("ColumnsZ",dimensions.internalDepth());
        tag.putLong("SettingsRevision",owner.automationPort().settingsRevision());
        tag.putLong("ColdMb",snapshot.coldCoolantMb()); tag.putLong("HotMb",snapshot.hotCoolantMb()); tag.putLong("CapacityMb",owner.coolantCapacityMb());
        tag.putInt("HealthyTicks",owner.automationState().healthyAge()); tag.putInt("ResetTicks",p.resetStableTicks());
        tag.putDouble("StartupCold",p.startupColdFraction()); tag.putDouble("StartupHot",p.startupMaxHotFraction());
        tag.putDouble("WarningCold",p.warningColdFraction()); tag.putDouble("WarningHot",p.warningHotFraction()); tag.putDouble("TripHot",p.tripHotFraction());
        tag.putDouble("MaintenanceCost",p.burnerMaintenanceHuPerTick());
        var heat=owner.automationHeatAccount(); tag.putDouble("Maintenance",heat.paidHeatHu());
        tag.putDouble("CacheExpense",heat.cachedExpenseHu());
        tag.putDouble("CacheHeat",snapshot.fuelColumns().values().stream().mapToDouble(FuelColumnState::cachedHeatHu).sum()
                +snapshot.controlRodColumns().values().stream().mapToDouble(com.iksxh.create_nuclear_industry.reactor.ControlRodColumnState::cachedHeatHu).sum());
        tag.putDouble("NetHeat",Math.max(0,tag.getDouble("ActualHeat")-heat.generatedExpenseHu()));
        var burner=(BlazeBurnerBlockEntity)owner.automationBurner();
        tag.putString("Supply",ReactorAutomationHeatSupply.source(burner)); tag.putInt("ExternalTicks",burner.getRemainingBurnTime());
        tag.putBoolean("Duty",ReactorAutomationBlazeBridge.isDutyValid(burner));
        var sample=owner.automationObservation();
        tag.putBoolean("Sample",sample!=null);
        tag.putBoolean("NextCooling",sample!=null && ReactorAutomationController.coolantCanCarryPendingTick(sample));
        tag.putBoolean("Covered",sample!=null && sample.controlledFuelCount()==sample.fuelCount());
        tag.putBoolean("Jammed",sample!=null && sample.jammedCount()>0);
        tag.putBoolean("SignalHigh",owner.getLevel().hasNeighborSignal(owner.getBlockPos()));
        tag.putDouble("MaxReachable",sample==null?0:java.util.Arrays.stream(sample.curve()).max().orElse(0));
        int usable=0,spent=0,empty=0;
        var columns=new ListTag();
        for(var entry:owner.structureScan().columns().entrySet()) {
            var position=entry.getKey(); var column=new CompoundTag();
            column.putInt("X",position.x()); column.putInt("Z",position.z()); column.putString("Role",entry.getValue().type().name().toLowerCase(Locale.ROOT));
            var fuel=snapshot.fuelColumns().get(position); var rod=snapshot.controlRodColumns().get(position);
            if(entry.getValue().type()==ReactorStructureDefinition.ColumnType.FUEL) {
                if(fuel==null || !fuel.fuelAssembly().present()) empty++; else if(fuel.hasUsableFuel()) usable++; else spent++;
                column.putString("Fuel",fuel==null || !fuel.fuelAssembly().present()?"empty":fuel.hasUsableFuel()?"usable":"spent");
                column.putDouble("Remaining",fuel==null || !fuel.fuelAssembly().present()?0:(double)(fuel.fuelAssembly().maxDamage()-fuel.fuelAssembly().damage())/fuel.fuelAssembly().maxDamage());
                column.putDouble("Integrity",fuel==null?1:fuel.integrity()); column.putDouble("Heat",fuel==null?0:fuel.cachedHeatHu());
                column.putBoolean("Covered",position.cardinalNeighbours(dimensions).stream().anyMatch(snapshot.controlRodColumns()::containsKey));
            } else if(rod!=null) {
                column.putDouble("Integrity",rod.integrity()); column.putDouble("Heat",rod.cachedHeatHu());
                column.putDouble("Actual",rod.actualDepth()*100); column.putDouble("Target",rod.targetDepth()*100); column.putBoolean("Jammed",rod.jammed());
            }
            columns.add(column);
        }
        tag.put("Columns",columns); tag.putInt("Usable",usable); tag.putInt("Spent",spent); tag.putInt("EmptyFuel",empty);
        return new ReactorAdministratorProjection(tag);
    }
}
