package com.iksxh.create_nuclear_industry.heat;

import net.minecraft.nbt.CompoundTag;

/**
 * 单机冷凝的千分比产物尾量和顶部冷源消耗账本；数量单位均为 mB。
 * 世界检查及方块替换由服务端设备负责，本类型只预算和记录已经成功提交的转换。
 */
public final class CondensationState {
    /** 顶格必须同时符合冷源标签与已定义消耗类型；未知扩展不会获得无限冷量。 */
    public enum Source { NONE, WATER, SNOW, ICE, PACKED_ICE, BLUE_ICE }

    /** 八项服务端参数快照；速率单位 mB/t，回收率为千分比，容量及阶段预算单位 mB。 */
    public record Settings(int rateMbPerTick, int recoveryPermille, int steamCapacityMb,
                           int waterCapacityMb, int snowAfterMb, int iceAfterMb,
                           int packedIceAfterMb, int waterAfterMb) {
        public static final Settings DEFAULT = new Settings(54, 1000, 4000, 4000,
                100000, 100000, 900000, 100000);
        public boolean valid() {
            return rateMbPerTick > 0 && rateMbPerTick <= 1000000
                    && recoveryPermille >= 1 && recoveryPermille <= 1000
                    && steamCapacityMb > 0 && steamCapacityMb <= 1000000
                    && waterCapacityMb > 0 && waterCapacityMb <= 1000000
                    && snowAfterMb > 0 && iceAfterMb > 0 && packedIceAfterMb > 0 && waterAfterMb > 0;
        }
        public int lifetime(Source source) {
            return switch (source) {
                case WATER -> waterAfterMb;
                case SNOW -> snowAfterMb;
                case ICE -> iceAfterMb;
                case PACKED_ICE -> packedIceAfterMb;
                case BLUE_ICE -> Integer.MAX_VALUE;
                default -> 0;
            };
        }
    }

    private int remainder;
    private Source source = Source.NONE;
    private long consumed;

    public int remainder() { return remainder; }
    public long consumed() { return consumed; }
    public Source source() { return source; }

    /** 每次真实服务端转换前核对顶格类型；改变类型才开启新阶段，重新加载不刷新进度。 */
    public int budget(Source current, Settings cfg) {
        if (source != current) { source = current; consumed = 0; }
        if (!cfg.valid() || current == Source.NONE) return 0;
        return current == Source.BLUE_ICE ? cfg.rateMbPerTick()
                : (int) Math.min(cfg.rateMbPerTick(), Math.max(0L, cfg.lifetime(current) - consumed));
    }

    /** 输出为整数 mB；运算采用 long 防止配置最大流量乘千分比溢出。 */
    public int output(int input, Settings cfg) {
        return (int) (((long) input * cfg.recoveryPermille() + remainder) / 1000);
    }

    /** 冷罐必须有空位才允许转换，即使本包暂时只增加小数尾量。 */
    public int limitInput(int available, int outputSpace, Settings cfg) {
        if (!cfg.valid() || outputSpace <= 0) return 0;
        long maximum = ((long) (outputSpace + 1) * 1000 - 1 - remainder) / cfg.recoveryPermille();
        return (int) Math.min(Math.max(0, available), maximum);
    }

    /** 只在输入扣除和产物加入成功后调用；尾量始终处在0～999，不受拓扑分合影响。 */
    public void commit(int input, Settings cfg) {
        remainder = (int) (((long) input * cfg.recoveryPermille() + remainder) % 1000);
        if (source != Source.NONE && source != Source.BLUE_ICE) consumed += input;
    }

    public boolean exhausted(Settings cfg) {
        return source != Source.NONE && source != Source.BLUE_ICE && consumed >= cfg.lifetime(source);
    }

    /** 世界完成融化/蒸发后从零开始新阶段；不会增加机内水量。 */
    public void changedTo(Source next) { source = next; consumed = 0; }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("Remainder", remainder);
        tag.putString("Source", source.name());
        tag.putLong("Consumed", consumed);
        return tag;
    }

    /** 当前格式恢复仅收敛损坏数值，保留正常消耗量及单机尾量。 */
    public void load(CompoundTag tag) {
        remainder = Math.clamp(tag.getInt("Remainder"), 0, 999);
        consumed = Math.max(0, tag.getLong("Consumed"));
        try { source = Source.valueOf(tag.getString("Source")); }
        catch (IllegalArgumentException ignored) { source = Source.NONE; consumed = 0; }
    }
}
