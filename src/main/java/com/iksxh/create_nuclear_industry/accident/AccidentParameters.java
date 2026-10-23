package com.iksxh.create_nuclear_industry.accident;

import net.minecraft.nbt.CompoundTag;

/** 服务端事故任务冻结的整组规则；距离为格、冲击为生命值，硬上限不能由配置突破。 */
public record AccidentParameters(boolean destruction, int extension, int externalBlocks,
        double shockHealthPoints, int worldActionsPerTick) {
    public static final AccidentParameters DEFAULT = new AccidentParameters(true, 2, 32, 8, 64);
    public AccidentParameters {
        if (extension < 0 || extension > 2 || externalBlocks < 0 || externalBlocks > 32
                || !Double.isFinite(shockHealthPoints) || shockHealthPoints < 0 || shockHealthPoints > 8
                || worldActionsPerTick < 1 || worldActionsPerTick > 64)
            throw new IllegalArgumentException("事故参数超过已批准边界");
    }
    public CompoundTag save() {
        var tag = new CompoundTag(); tag.putBoolean("Destruction", destruction);
        tag.putInt("Extension", extension); tag.putInt("External", externalBlocks);
        tag.putDouble("Shock", shockHealthPoints); tag.putInt("Budget", worldActionsPerTick); return tag;
    }
    public static AccidentParameters load(CompoundTag tag) {
        return new AccidentParameters(tag.getBoolean("Destruction"), tag.getInt("Extension"),
                tag.getInt("External"), tag.getDouble("Shock"), tag.getInt("Budget"));
    }
}
