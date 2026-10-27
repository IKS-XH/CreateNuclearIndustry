package com.iksxh.create_nuclear_industry.boiler;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** 服务端锅炉危险时钟；只计实际调用的加载tick，不补离线间隔，不以结构重查刷新进度。 */
public final class BoilerAccidentState {
    public enum Cause { NONE, DRY, BURST }
    /** 压力为归一值、时长为tick、半径为格、伤害为HP；非法组合禁止新热工交易。 */
    public record Parameters(double ratedPressure,double instantRatio,int pressureTicks,double recoveryRatio,int recoveryTicks,
            int dryTicks,double minimumRadius,double radiusPerRatio,double maximumRadius,double damageHp) {
        public boolean valid() {
            for(double value:new double[]{ratedPressure,instantRatio,recoveryRatio,minimumRadius,radiusPerRatio,maximumRadius,damageHp})
                if(!Double.isFinite(value)||value<=0||value>1e6)return false;
            return instantRatio>1&&recoveryRatio<1&&pressureTicks>0&&recoveryTicks>0&&dryTicks>0
                    &&minimumRadius<=maximumRadius&&maximumRadius<=32&&damageHp<=1024;
        }
        public double radius(double pressure) { return Math.clamp(minimumRadius+radiusPerRatio*(pressure/ratedPressure-1),minimumRadius,maximumRadius); }
    }
    public static final Parameters DEFAULT=new Parameters(1,1.5,200,.95,40,400,2,4,6,16);
    private int dry,pressure,recovery;
    private long lastTick=Long.MIN_VALUE;
    private UUID pending;
    public int dryTicks(){return dry;}
    public int pressureTicks(){return pressure;}
    public int recoveryTicks(){return recovery;}
    public UUID pending(){return pending;}
    /** 最终拆除许可后调用只读判定；超压优先，不把正常热炉一概当危险。 */
    public Cause dangerous(double actualPressure,boolean dryNow,Parameters p) {
        return !p.valid()||pending!=null?Cause.NONE:actualPressure>p.ratedPressure()?Cause.BURST:dryNow?Cause.DRY:Cause.NONE;
    }
    public Cause advance(long now,boolean loaded,double actualPressure,boolean dryNow,Parameters p) {
        if(now==lastTick||!loaded||!p.valid()||pending!=null)return Cause.NONE;
        lastTick=now;
        dry=dryNow?Math.min(p.dryTicks(),dry+1):0;
        if(actualPressure>p.ratedPressure()) {pressure=Math.min(p.pressureTicks(),pressure+1);recovery=0;}
        else if(actualPressure<=p.ratedPressure()*p.recoveryRatio()) {
            recovery=Math.min(p.recoveryTicks(),recovery+1);
            if(recovery>=p.recoveryTicks())pressure=0;
        } else recovery=0;
        if(actualPressure>=p.ratedPressure()*p.instantRatio()||pressure>=p.pressureTicks())return Cause.BURST;
        return dry>=p.dryTicks()?Cause.DRY:Cause.NONE;
    }
    public void submitted(UUID id){if(pending!=null&&!pending.equals(id))throw new IllegalStateException("锅炉已有待提交事故");pending=id;}
    public void finished(UUID id){if(id.equals(pending)){pending=null;dry=pressure=recovery=0;}}
    /** 真实重新落位解除已提交事故的本地等待/已领取时钟；未提交的危险进度及旧现场任务不改。 */
    public void detachPending(){if(pending!=null)finished(pending);}
    public CompoundTag save(){var t=new CompoundTag();t.putInt("DryTicks",dry);t.putInt("PressureTicks",pressure);t.putInt("RecoveryTicks",recovery);t.putLong("LastTick",lastTick);if(pending!=null)t.putUUID("Pending",pending);return t;}
    public void load(CompoundTag t){dry=Math.max(0,t.getInt("DryTicks"));pressure=Math.max(0,t.getInt("PressureTicks"));recovery=Math.max(0,t.getInt("RecoveryTicks"));lastTick=t.contains("LastTick")?t.getLong("LastTick"):Long.MIN_VALUE;pending=t.hasUUID("Pending")?t.getUUID("Pending"):null;}
    public static CompoundTag saveParameters(Parameters p){var t=new CompoundTag();t.putDouble("Rated",p.ratedPressure());t.putDouble("InstantRatio",p.instantRatio());t.putInt("PressureTicks",p.pressureTicks());t.putDouble("RecoveryRatio",p.recoveryRatio());t.putInt("RecoveryTicks",p.recoveryTicks());t.putInt("DryTicks",p.dryTicks());t.putDouble("MinimumRadius",p.minimumRadius());t.putDouble("RadiusPerRatio",p.radiusPerRatio());t.putDouble("MaximumRadius",p.maximumRadius());t.putDouble("DamageHp",p.damageHp());return t;}
    public static Parameters loadParameters(CompoundTag t){return new Parameters(t.getDouble("Rated"),t.getDouble("InstantRatio"),t.getInt("PressureTicks"),t.getDouble("RecoveryRatio"),t.getInt("RecoveryTicks"),t.getInt("DryTicks"),t.getDouble("MinimumRadius"),t.getDouble("RadiusPerRatio"),t.getDouble("MaximumRadius"),t.getDouble("DamageHp"));}
}
