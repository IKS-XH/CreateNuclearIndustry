package com.iksxh.create_nuclear_industry.automation;
import com.iksxh.create_nuclear_industry.reactor.*;
/** 一次成功结算的同龄服务端观测。保护读当前权威库存，显示平均不能延长样本寿命。 */
public record ReactorAutomationObservation(ReactorSnapshot snapshot, ReactorSimulationParameters reactorParameters,
        ReactorServerTick.CoolantInput coolantInput, double generatedHeatHuPerTick, double convertedCoolantMb,
        boolean newDamage, boolean bindingValid, boolean dutyValid, boolean signalHigh, long sampleTime,
        long gameTime, ReactorAutomationPortBlockEntity.Settings settings, double[] curve, boolean configuredAbsorptionPositive) {
    /** 纯夹具构造采用输入吸热值；世界桥单独传入未经原热工夹紧的配置合法性。 */
    public ReactorAutomationObservation(ReactorSnapshot snapshot, ReactorSimulationParameters reactorParameters,
            ReactorServerTick.CoolantInput coolantInput, double generatedHeatHuPerTick, double convertedCoolantMb,
            boolean newDamage, boolean bindingValid, boolean dutyValid, boolean signalHigh, long sampleTime,
            long gameTime, ReactorAutomationPortBlockEntity.Settings settings, double[] curve) {
        this(snapshot,reactorParameters,coolantInput,generatedHeatHuPerTick,convertedCoolantMb,newDamage,
                bindingValid,dutyValid,signalHigh,sampleTime,gameTime,settings,curve,coolantInput.coolantAbsorptionHuPerMb()>0);
    }
    public double effectiveHeatHu() { return snapshot.fuelColumns().values().stream()
            .mapToDouble(f->Math.max(0,f.cachedHeatHu()-f.quantizedHeatRemainderHu())).max().orElse(0); }
    public long fuelCount() { return snapshot.fuelColumns().values().stream().filter(FuelColumnState::hasUsableFuel).count(); }
    public long controlledFuelCount() { return snapshot.fuelColumns().entrySet().stream().filter(e->e.getValue().hasUsableFuel())
            .filter(e->e.getKey().cardinalNeighbours(snapshot.dimensions()).stream().anyMatch(snapshot.controlRodColumns()::containsKey)).count(); }
    public long jammedCount() { return snapshot.controlRodColumns().values().stream().filter(ControlRodColumnState::jammed).count(); }
    public boolean fullyInserted() { return !snapshot.controlRodColumns().isEmpty() && snapshot.controlRodColumns().values().stream()
            .allMatch(r->!r.jammed() && r.actualDepth()>=1-1e-9 && r.targetDepth()>=1-1e-9); }
    public boolean coolingAvailable() { return configuredAbsorptionPositive && coolantInput.coolantCapacityMb()>0 && coolantInput.coolantAbsorptionHuPerMb()>0; }
    public double coldFraction() { return coolingAvailable()?(double)snapshot.coldCoolantMb()/coolantInput.coolantCapacityMb():0; }
    public double hotFraction() { return coolingAvailable()?(double)snapshot.hotCoolantMb()/coolantInput.coolantCapacityMb():1; }
}
