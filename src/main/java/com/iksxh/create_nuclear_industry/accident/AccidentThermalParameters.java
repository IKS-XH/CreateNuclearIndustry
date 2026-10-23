package com.iksxh.create_nuclear_industry.accident;

import net.minecraft.nbt.CompoundTag;

/** 事故创建时冻结的固体热危害参数；时间为实际加载tick，HP为直接热伤，不包含普通燃烧。 */
public record AccidentThermalParameters(int solidTicks, int waterRate, int fireLimit, int fireTicks,
        double solidDamageHp,int solidIgniteSeconds) {
    public static final AccidentThermalParameters DEFAULT = new AccidentThermalParameters(2400,4,8,600,2,3);
    public AccidentThermalParameters {
        if(solidTicks<1||waterRate<1||fireLimit<0||fireLimit>8||fireTicks<1||fireTicks>600
                ||!Double.isFinite(solidDamageHp)||solidDamageHp<0||solidDamageHp>Float.MAX_VALUE
                ||solidIgniteSeconds<0||solidIgniteSeconds>Integer.MAX_VALUE/20)
            throw new IllegalArgumentException("事故热参数超出有限热危害边界");
    }
    public CompoundTag save() {
        var t=new CompoundTag();t.putInt("SolidTicks",solidTicks);t.putInt("WaterRate",waterRate);
        t.putInt("FireLimit",fireLimit);t.putInt("FireTicks",fireTicks);t.putDouble("SolidDamageHp",solidDamageHp);
        t.putInt("SolidIgniteSeconds",solidIgniteSeconds);return t;
    }
    public static AccidentThermalParameters load(CompoundTag t) {
        return new AccidentThermalParameters(t.getInt("SolidTicks"),t.getInt("WaterRate"),t.getInt("FireLimit"),t.getInt("FireTicks"),
                t.getDouble("SolidDamageHp"),t.getInt("SolidIgniteSeconds"));
    }
}
