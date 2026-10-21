package com.iksxh.create_nuclear_industry.accident;

import java.util.*;
import com.iksxh.create_nuclear_industry.blockentity.*;
import com.iksxh.create_nuclear_industry.config.AccidentConfig;
import com.iksxh.create_nuclear_industry.content.AccidentContent;
import com.iksxh.create_nuclear_industry.radiation.*;
import com.iksxh.create_nuclear_industry.reactor.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

/** 唯一事故接管入口；先建持久化任务，真实换料口转移，最后撤销原堆。危险拆除批次先全部通过才接管。 */
public final class ReactorAccidentService {
    private static List<ReactorMeltdownEvent> batch;
    private ReactorAccidentService() {}
    public static void beginBatch() { if(batch!=null)throw new IllegalStateException("事故批次嵌套"); batch=new ArrayList<>(); }
    public static void abortBatch() { batch=null; }
    /** 只在所有者提交、后检与全部事件监听器均成功后执行；此前没有事故账本或世界副作用。 */
    public static void commitBatch() {
        var events=batch;batch=null;if(events==null)return;
        // 其它监听器可能改掉后一个所有者；先核全部事件，再进行任何托管，失败不留孤立任务。
        for(var e:events)if(!canBegin(e))throw new IllegalStateException("危险拆除批次的事故所有者发生变化");
        for(var e:events)begin(e);
    }
    private static boolean canBegin(ReactorMeltdownEvent event) {
        var level=event.level();if(level.getChunkSource().getChunkNow(event.instrumentPort().getX()>>4,event.instrumentPort().getZ()>>4)==null)return false;
        if(!(level.getBlockEntity(event.instrumentPort()) instanceof ReactorInstrumentPortBlockEntity owner))return false;
        if(ReactorAccidentSavedData.get(level).byGeneration(owner.accidentGeneration())!=null)return true;
        return owner.structureValid()&&event.structureOrigin().equals(owner.structureOrigin())&&owner.snapshot().equals(event.snapshot())
                &&owner.prepareAccidentFuelCustody()!=null;
    }
    @SubscribeEvent public static void onMeltdown(ReactorMeltdownEvent event) {
        if(batch!=null){batch.add(event);return;} begin(event);
    }
    public static ReactorAccidentLedger begin(ReactorMeltdownEvent event) {
        var level=event.level();
        if(level.getChunkSource().getChunkNow(event.instrumentPort().getX()>>4,event.instrumentPort().getZ()>>4)==null)return null;
        if(!(level.getBlockEntity(event.instrumentPort()) instanceof ReactorInstrumentPortBlockEntity owner))return null;
        var data=ReactorAccidentSavedData.get(level);var old=data.byGeneration(owner.accidentGeneration());
        if(old!=null)return old;
        if(!owner.structureValid()||!event.structureOrigin().equals(owner.structureOrigin())
                ||!owner.snapshot().equals(event.snapshot()))return null;
        var captures=owner.prepareAccidentFuelCustody();if(captures==null)return null;
        var d=event.snapshot().dimensions();var min=event.structureOrigin();var max=min.offset(d.width()-1,d.height()-1,d.depth()-1);
        var columns=new ArrayList<ReactorAccidentLedger.Column>();var ports=new HashMap<BlockPos,ReactorPortBlockEntity>();
        for(var capture:captures) {
            var port=capture.port();
            var actual=port.fuelAssembly();var column=port.boundColumn();
            ports.put(port.getBlockPos(),port);
            if(actual.isEmpty())continue;
            if(!FuelAssemblyItemCodec.isValidStoredFuel(actual))return null;
            boolean waste=FuelAssemblyItemCodec.isCooledSpentFuel(actual)||FuelUseHistory.isUsed(actual)||actual.getDamageValue()>0
                    ||event.snapshot().fuelColumns().getOrDefault(column,FuelColumnState.empty()).fuelBurnRemainder()>0;
            columns.add(new ReactorAccidentLedger.Column(port.getBlockPos(),min.offset(column.x()+1,1,column.z()+1),port.accidentGeneration(),actual,waste));
        }
        double heat=event.snapshot().fuelColumns().values().stream().mapToDouble(FuelColumnState::cachedHeatHu).sum()
                +event.snapshot().controlRodColumns().values().stream().mapToDouble(ControlRodColumnState::cachedHeatHu).sum()
                +event.snapshot().hotCoolantMb()*com.iksxh.create_nuclear_industry.config.P1ServerConfig.VALUES.coolantAbsorptionHuPerMb.get();
        var p=AccidentConfig.parameters();
        var ledger=new ReactorAccidentLedger(UUID.randomUUID(),owner.accidentGeneration(),event.dimension().location().toString(),event.reason().name(),
                owner.getBlockPos(),min,max,p,event.snapshot().coldCoolantMb(),event.snapshot().hotCoolantMb(),heat,columns,
                ReactorAccidentPlan.create(level,min,max,p,ports.keySet(),owner.getBlockPos()));
        ledger.thermal=new AccidentThermalState(AccidentConfig.thermalParameters());
        // 原破裂区内选定一次性候选；只在初始结算结束后检查支撑和空气，失败也不另选位置复燃。
        for(var action:ledger.actions)if(!action.external&&action.pos.getY()==min.getY()+2)
            ledger.thermal.planFire(action.pos);
        // 只在事故创建时冻结已存在的独立机组，不能把后续新建同位置机组当作旧破坏目标。
        for(var action:ledger.actions)for(var existing:com.iksxh.create_nuclear_industry.structure.ReactorStructureLifecycle.loadedOwnersNear(level,action.pos))
            if(existing.structureValid())ledger.knownOwners.add(existing.accidentGeneration());
        // 服务端单线程内，下面的口和物品已全部只读核验；数据先登记，才让原口不可取。
        data.add(ledger);
        for(var c:columns) {
            var actual=ports.get(c.port).takeForAccident(owner,ledger.id,c.portGeneration,c.original());
            if(actual==null)throw new IllegalStateException("事故端口交接前身份变化");
            c.capture(c.waste()?new ItemStack(AccidentContent.ACCIDENT_FUEL_WASTE.get()):actual);
        }
        if(!owner.lockForAccident(ledger))throw new IllegalStateException("事故仪表交接失败");
        data.setDirty();
        for(var c:columns)RadiationSourceIndex.changed(level,c.port);
        for(var c:columns)RadiationSourceIndex.custodyChanged(level,c.bottom);
        return ledger;
    }
}
