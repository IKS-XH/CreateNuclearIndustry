package com.iksxh.create_nuclear_industry.accident;

import net.minecraft.nbt.CompoundTag;

/** 每团创建时冻结的规则；时间均为加载tick，份额只作用于真实废料，不限制活动距离或总破坏数。 */
public record MoltenCoriumParameters(double wasteFraction,int coolingTicks,int moveTicks,int corrosionTicks,
        double damageHp,int igniteSeconds,AccidentThermalParameters solid) {
    public static final MoltenCoriumParameters DEFAULT=new MoltenCoriumParameters(.5,6000,20,40,4,5,AccidentThermalParameters.DEFAULT);
    public MoltenCoriumParameters {
        if(!Double.isFinite(wasteFraction)||wasteFraction<0||wasteFraction>1||coolingTicks<1||moveTicks<1||corrosionTicks<1
                ||!Double.isFinite(damageHp)||damageHp<0||damageHp>Float.MAX_VALUE||igniteSeconds<0||igniteSeconds>Integer.MAX_VALUE/20||solid==null)
            throw new IllegalArgumentException("熔融物参数非法");
    }
    public int moltenCount(int realWaste){if(realWaste<0)throw new IllegalArgumentException("真实废料不能为负");return (int)Math.ceil(realWaste*wasteFraction);}
    public CompoundTag save(){var t=new CompoundTag();t.putDouble("Fraction",wasteFraction);t.putInt("Cooling",coolingTicks);t.putInt("Move",moveTicks);
        t.putInt("Corrosion",corrosionTicks);t.putDouble("Damage",damageHp);t.putInt("Ignite",igniteSeconds);t.put("Solid",solid.save());return t;}
    public static MoltenCoriumParameters load(CompoundTag t){return new MoltenCoriumParameters(t.getDouble("Fraction"),t.getInt("Cooling"),t.getInt("Move"),
            t.getInt("Corrosion"),t.getDouble("Damage"),t.getInt("Ignite"),AccidentThermalParameters.load(t.getCompound("Solid")));}
}
