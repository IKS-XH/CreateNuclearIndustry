package com.iksxh.create_nuclear_industry.heat;

import net.minecraft.nbt.CompoundTag;

/**
 * 服务端唯一换热账本；热/冷库存单位 mB，储备单位 HU，时间单位服务端 game tick。
 * 输入转换和冷罐占位原子提交；普通查询与模拟不结算，客户端只读取保存快照。
 */
public final class HeatExchangerState {
    public static final int CAPACITY = 4000;
    private Settings settings = new Settings(18, 1, 40, .5);
    private int hot, cold, heat = -1, converted;
    private double reserve, flowFraction, lastRate;
    private long lastTick = -1;
    private long noFlowDeadlineTick = -1;
    private boolean legacyDeadlinePending;
    private String status = "no_load";

    /** 服务端一次换热使用的库存边界；返回值是实际等体积转移量，单位 mB。 */
    public interface Exchange {
        int hot();
        int coldSpace();
        int convert(int amount);
    }

    private final Exchange localExchange = new Exchange() {
        @Override public int hot() { return hot; }
        @Override public int coldSpace() { return Math.max(0, settings.coldCapacityMb() - cold); }
        @Override public int convert(int amount) {
            int moved = Math.min(amount, Math.min(hot, Math.max(0, settings.coldCapacityMb() - cold)));
            hot -= moved;
            cold += moved;
            return moved;
        }
    };

    /** 有界配置快照；密度复用 P1 的 HU/mB，不把 Create 数值热等级当作物理单位。 */
    public record Settings(int heatLevel, double huPerLevel, int bufferTicks, double density,
                           int hotCapacityMb, int coldCapacityMb, int maxLineLength) {
        public Settings(int heatLevel, double huPerLevel, int bufferTicks, double density) {
            this(heatLevel, huPerLevel, bufferTicks, density, CAPACITY, CAPACITY, 16);
        }
        public double rate() { return heatLevel * huPerLevel; }
        public double capacity() { return rate() * bufferTicks; }
        public double levelReserve() { return huPerLevel * bufferTicks; }
        public boolean valid() {
            return heatLevel >= 1 && heatLevel <= 18 && Double.isFinite(huPerLevel)
                    && huPerLevel > 0 && huPerLevel <= 1_000_000 && bufferTicks >= 1 && bufferTicks <= 1200
                    && Double.isFinite(density) && density > 0 && density <= 1_000_000
                    && hotCapacityMb >= 1 && hotCapacityMb <= 1_000_000
                    && coldCapacityMb >= 1 && coldCapacityMb <= 1_000_000
                    && maxLineLength >= 1 && maxLineLength <= 64
                    && Double.isFinite(rate() / density) && Double.isFinite(capacity());
        }
    }

    public int hot() { return hot; }
    public int cold() { return cold; }
    public int heat() { return heat; }
    public int converted() { return converted; }
    public double reserve() { return reserve; }
    public String status() { return status; }
    public Settings settings() { return settings; }

    /** 服务端库存能力与热账本共用此快照；密度或窗口变动后丢弃无法复用的尾差及余热历史。 */
    public void setSettings(Settings next) {
        if (!next.valid()) { settings = next; heat = -1; status = "invalid"; return; }
        if (Double.compare(settings.density(), next.density()) != 0
                || settings.bufferTicks() != next.bufferTicks()) {
            flowFraction = 0;
            reserve = 0;
            lastRate = 0;
            converted = 0;
            noFlowDeadlineTick = -1;
            legacyDeadlinePending = false;
            heat = -1;
        }
        settings = next;
    }

    /** 返回最长剩余余热窗口；不将储备除以额定耗热冒充精确运行时间。 */
    public int remainingTicks() {
        if (reserve <= 0 || lastTick < 0 || noFlowDeadlineTick < 0) return 0;
        return (int) Math.clamp(noFlowDeadlineTick - lastTick, 0, Integer.MAX_VALUE);
    }

    /** 只返回实际容量内的接收量；simulate 不修改数量及时间。 */
    public int fillHot(int amount, boolean simulate) {
        int accepted = settings.valid() ? Math.min(Math.max(0, amount),
                Math.max(0, settings.hotCapacityMb() - hot)) : 0;
        if (!simulate) hot += accepted;
        return accepted;
    }

    /** 仅抽取已转换的冷液，热液不向外暴露排出入口。 */
    public int drainCold(int amount, boolean simulate) {
        int taken = Math.min(Math.max(0, amount), cold);
        if (!simulate) cold -= taken;
        return taken;
    }

    /** 仅供已预检的整列换热事务使用；不改变本机已付热或流量分数。 */
    int takeHotForConversion(int amount) {
        int taken = Math.min(Math.max(0, amount), hot);
        hot -= taken;
        return taken;
    }

    /** 仅供同一次换热事务使用；热液扣量须与全列新增冷液等量。 */
    int putColdFromConversion(int amount) {
        int added = settings.valid() ? Math.min(Math.max(0, amount),
                Math.max(0, settings.coldCapacityMb() - cold)) : 0;
        cold += added;
        return added;
    }

    /**
     * 每个世界 tick 最多执行一次。服务端先按已储HU决定档位并实付，再转换补储；
     * 因此发布热始终有足额已付款，热冷转换体积相等，且储备不超过配置上限。
     * 连续无实际转换达到bufferTicks后撤销供热并散去余热；普通查询和模拟不调用结算。
     */
    public void tick(long now, boolean load, Settings cfg) {
        tick(now, load, cfg, localExchange);
    }

    /** 保持本机已付热与期限，只有热液转冷借用传入的整列库存事务。 */
    public void tick(long now, boolean load, Settings cfg, Exchange exchange) {
        if (now == lastTick) return;
        setSettings(cfg);
        heat = -1;
        converted = 0;
        if (!cfg.valid()) {
            lastTick = now;
            reserve = 0;
            flowFraction = 0;
            lastRate = 0;
            noFlowDeadlineTick = -1;
            legacyDeadlinePending = false;
            status = "invalid";
            return;
        }
        if (legacyDeadlinePending) restoreLegacyDeadline(cfg);
        if (lastTick >= 0 && now < lastTick) {
            // 世界时间回退时不重置期限；无法证明旧余热仍在有效窗口内，故安全散热。
            reserve = 0;
            noFlowDeadlineTick = now;
        }
        if (lastTick >= 0 && now > lastTick && now - lastTick > 1 && lastRate > 0) {
            long missingTicks = now - lastTick - 1;
            reserve = Math.max(0, reserve - missingTicks * lastRate);
        }
        lastTick = now;
        lastRate = cfg.rate();
        // 配置改变仅限缩现存HU，不按新密度重算库存热值，更不补齐新的容量。
        reserve = Math.min(reserve, cfg.capacity());
        if (noFlowDeadlineTick >= 0)
            noFlowDeadlineTick = Math.min(noFlowDeadlineTick, safeAdd(now, cfg.bufferTicks()));
        // 必须先撤销到期旧热，再计算本tick档位；新转换只能支付后续tick，不能复活旧HU。
        if (noFlowDeadlineTick < 0 || now >= noFlowDeadlineTick) reserve = 0;
        if (!load) {
            reserve = Math.max(0, reserve - cfg.rate());
            status = "no_load";
            return;
        }

        int level = Math.min(cfg.heatLevel(), (int) Math.floor(reserve / cfg.levelReserve()));
        double payment = level * cfg.huPerLevel();
        if (level > 0) {
            reserve = Math.max(0, reserve - payment);
            heat = level;
        }

        double requested = cfg.rate() / cfg.density() + flowFraction;
        flowFraction = requested - Math.floor(requested);
        int budget = (int) Math.min(settings.hotCapacityMb(), Math.floor(requested));
        int available = Math.min(exchange.hot(), exchange.coldSpace());
        // 最后不足1mB的空间允许一个整mB完成充热，尾差有界散失；向下取整会让
        // 0.3HU/mB等密度永远充不到最高档阈值。空间已满时仍不转换，热液留在热罐。
        int headroomBudget = (int) Math.min(settings.coldCapacityMb(),
                Math.ceil(Math.max(0, cfg.capacity() - reserve) / cfg.density()));
        converted = exchange.convert(Math.min(budget, Math.min(available, headroomBudget)));
        reserve = Math.min(cfg.capacity(), reserve + converted * cfg.density());
        if (converted > 0) noFlowDeadlineTick = safeAdd(now, cfg.bufferTicks());
        // 与本tick实际档位比较补热，而不是与额定转换上限比较；稳定低档同样属于正常供热。
        status = heat > 0 ? (converted * cfg.density() < payment ? "residual" : "running")
                : exchange.coldSpace() == 0 ? "blocked" : exchange.hot() == 0 ? "empty" : "warming";
    }

    /**
     * 专用锅炉按真实需求从既有余热领取 HU；每源每 tick 仅一次，返回值恰为储备扣量。
     * 转换在领取后发生并仍占用冷罐等量空间，不发布原生 Create 锅炉热级。
     */
    public double claimDedicated(long now, double requestHu, Settings cfg) {
        return claimDedicated(now, requestHu, cfg, localExchange);
    }

    /** 专用锅炉仍只领取本机储备，补储使用整列可用热液和冷罐空位。 */
    public double claimDedicated(long now, double requestHu, Settings cfg, Exchange exchange) {
        if (now == lastTick) return 0;
        setSettings(cfg);
        if (!cfg.valid() || !Double.isFinite(requestHu) || requestHu <= 0) return 0;
        if (legacyDeadlinePending) restoreLegacyDeadline(cfg);
        if (lastTick >= 0 && now < lastTick) {
            reserve = 0;
            noFlowDeadlineTick = now;
        }
        if (lastTick >= 0 && now > lastTick && now - lastTick > 1 && lastRate > 0)
            reserve = Math.max(0, reserve - (now - lastTick - 1) * lastRate);
        lastTick = now;
        lastRate = cfg.rate();
        reserve = Math.min(reserve, cfg.capacity());
        if (noFlowDeadlineTick < 0 || now >= noFlowDeadlineTick) reserve = 0;
        double paid = Math.min(Math.min(requestHu, cfg.rate()), reserve);
        reserve -= paid;
        heat = -1;
        // 专用负载只为本次可完成事务转冷；已有储备先付款，补储不超出实际申请。
        double requested = Math.min(requestHu, cfg.rate()) / cfg.density() + flowFraction;
        flowFraction = requested - Math.floor(requested);
        int budget = (int) Math.min(settings.hotCapacityMb(), Math.floor(requested));
        int headroom = (int) Math.min(settings.coldCapacityMb(), Math.ceil((cfg.capacity() - reserve) / cfg.density()));
        converted = exchange.convert(Math.min(budget, Math.min(Math.min(exchange.hot(), exchange.coldSpace()), headroom)));
        reserve = Math.min(cfg.capacity(), reserve + converted * cfg.density());
        if (converted > 0) noFlowDeadlineTick = safeAdd(now, cfg.bufferTicks());
        status = converted > 0 ? "dedicated" : paid > 0 ? "residual" : exchange.coldSpace() == 0 ? "blocked" : "empty";
        return paid;
    }

    /** 旧格式只存最后tick、额定耗热与储备；据此保守推断期限，绝不从加载时刻重新计时。 */
    private void restoreLegacyDeadline(Settings cfg) {
        legacyDeadlinePending = false;
        if (lastTick < 0 || lastRate <= 0 || reserve <= 0) {
            noFlowDeadlineTick = lastTick;
            reserve = 0;
            return;
        }
        long remaining = (long) Math.min(cfg.bufferTicks(), Math.floor(reserve / lastRate));
        noFlowDeadlineTick = safeAdd(lastTick, remaining);
    }

    private static long safeAdd(long tick, long delta) {
        if (delta > 0 && tick > Long.MAX_VALUE - delta) return Long.MAX_VALUE;
        return tick + delta;
    }

    /** 精确保存HU、分数流量、最后世界tick与绝对断流期限；携物不刷新余热窗口。 */
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("Hot", hot);
        tag.putInt("Cold", cold);
        tag.putDouble("ReserveHu", reserve);
        tag.putDouble("FlowFraction", flowFraction);
        tag.putDouble("LastRate", lastRate);
        tag.putDouble("SettingsDensity", settings.density());
        tag.putInt("SettingsBufferTicks", settings.bufferTicks());
        tag.putLong("LastTick", lastTick);
        // 旧NBT在首次tick前可能再次携物/保存；继续保留缺字段标记，不能丢失待迁移状态。
        if (!legacyDeadlinePending) tag.putLong("NoFlowDeadlineTick", noFlowDeadlineTick);
        return tag;
    }

    /** NBT数值收敛到有限范围；恢复后仍须服务端tick重新验证负载再发热。 */
    public void load(CompoundTag tag) {
        hot = Math.max(0, tag.getInt("Hot"));
        cold = Math.max(0, tag.getInt("Cold"));
        reserve = finite(tag.getDouble("ReserveHu"), 21_600_000_000D);
        flowFraction = finite(tag.getDouble("FlowFraction"), Math.nextDown(1D));
        lastRate = finite(tag.getDouble("LastRate"), 18_000_000D);
        double savedDensity = tag.contains("SettingsDensity") ? tag.getDouble("SettingsDensity") : settings.density();
        int savedBuffer = tag.contains("SettingsBufferTicks") ? tag.getInt("SettingsBufferTicks") : settings.bufferTicks();
        if (Double.isFinite(savedDensity) && savedDensity > 0 && savedBuffer > 0)
            settings = new Settings(settings.heatLevel(), settings.huPerLevel(), savedBuffer,
                    savedDensity, settings.hotCapacityMb(), settings.coldCapacityMb(), settings.maxLineLength());
        lastTick = tag.contains("LastTick") ? Math.max(-1, tag.getLong("LastTick")) : -1;
        legacyDeadlinePending = !tag.contains("NoFlowDeadlineTick");
        noFlowDeadlineTick = legacyDeadlinePending ? -1 : Math.max(-1, tag.getLong("NoFlowDeadlineTick"));
        heat = -1;
        converted = 0;
    }

    private static double finite(double value, double max) {
        return Double.isFinite(value) ? Math.clamp(value, 0, max) : 0;
    }
}
