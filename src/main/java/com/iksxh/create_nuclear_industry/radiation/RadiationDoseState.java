package com.iksxh.create_nuclear_industry.radiation;
import net.minecraft.nbt.CompoundTag;
/** 服务端在线剂量D、安全连续tick及高剂量伤害tick；离线不推进。 */
public record RadiationDoseState(double dose,int safeTicks,int damageTicks) {
    public static final String KEY="create_nuclear_industry:RadiationDose";
    public static final RadiationDoseState ZERO=new RadiationDoseState(0,0,0);
    public RadiationDoseState {
        if(!Double.isFinite(dose)||dose<0||safeTicks<0||damageTicks<0)throw new IllegalArgumentException("invalid radiation dose state");
    }
    public CompoundTag save() {var tag=new CompoundTag();tag.putDouble("Dose",dose);tag.putInt("SafeTicks",safeTicks);tag.putInt("DamageTicks",damageTicks);return tag;}
    public static RadiationDoseState load(CompoundTag tag,RadiationParameters p) {
        double dose=tag.getDouble("Dose");if(!Double.isFinite(dose)||dose<0)return ZERO;
        return new RadiationDoseState(Math.min(p.maxDose(),dose),Math.max(0,tag.getInt("SafeTicks")),Math.max(0,tag.getInt("DamageTicks")));
    }
}
