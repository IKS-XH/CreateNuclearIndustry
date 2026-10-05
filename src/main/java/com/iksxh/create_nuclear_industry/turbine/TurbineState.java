package com.iksxh.create_nuclear_industry.turbine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * 服务端唯一排汽周转账本。入口 EXECUTE 将超临界蒸汽等量变为普通蒸汽；
 * 只有出口 EXECUTE 才记实际流量。库存单位 mB，流量单位 mB/t，动力容量单位 SU。
 * 世界、红石与 Create 网络有效性由控制器判定；SIMULATE 始终是纯查询。
 */
public final class TurbineState {
    /** 合法档位的额定量和最高效率；轴长为转子数加两端面。 */
    public record Tier(int rotorCount, int ratedFlowMbPerTick, int diameter, double maxEfficiencyMultiplier) {
        public int length() { return rotorCount + 2; }
        public boolean valid() {
            return rotorCount >= 3 && rotorCount <= 16 && TurbineGeometry.supportedDiameter(diameter)
                    && ratedFlowMbPerTick >= 1 && ratedFlowMbPerTick <= 10_000
                    && Double.isFinite(maxEfficiencyMultiplier) && maxEfficiencyMultiplier > 0
                    && maxEfficiencyMultiplier <= 100;
        }
    }

    /**
     * SERVER 配置快照。跨字段非法、周转容量溢出或高于 Create 转速上限时停机。
     * frontShare 是已有废弃键，不参与动力计算。
     */
    public record Settings(Tier shortTier, Tier mediumTier, Tier longTier, int rpm,
                           double suPerMbPerTick, int smoothingTicks, int inletPortFlowMbPerTick,
                           int exhaustPortFlowMbPerTick, int turnoverTicks,
                           double minEfficiencyMultiplier, double minimumOperatingFlowRatio,
                           double frontShare) {
        public static final Settings DEFAULT = new Settings(new Tier(3, 54, 3, 1.2),
                new Tier(6, 108, 5, 1.5), new Tier(9, 216, 7, 1.8),
                256, 32768, 40, 256, 256, 1, .5, .3, .5);

        public Tier tierForRotors(int rotorCount) {
            if (shortTier != null && shortTier.rotorCount() == rotorCount) return shortTier;
            if (mediumTier != null && mediumTier.rotorCount() == rotorCount) return mediumTier;
            if (longTier != null && longTier.rotorCount() == rotorCount) return longTier;
            return null;
        }

        public boolean valid(int createMaxRpm) {
            return shortTier != null && mediumTier != null && longTier != null
                    && shortTier.valid() && mediumTier.valid() && longTier.valid()
                    && shortTier.rotorCount() != mediumTier.rotorCount()
                    && shortTier.rotorCount() != longTier.rotorCount()
                    && mediumTier.rotorCount() != longTier.rotorCount()
                    && rpm >= 1 && rpm <= 65_536 && createMaxRpm >= rpm
                    && Double.isFinite(suPerMbPerTick) && suPerMbPerTick > 0 && suPerMbPerTick <= 1_000_000
                    && smoothingTicks >= 1 && smoothingTicks <= 1200
                    && inletPortFlowMbPerTick >= 1 && inletPortFlowMbPerTick <= 1_000_000
                    && exhaustPortFlowMbPerTick >= 1 && exhaustPortFlowMbPerTick <= 1_000_000
                    && turnoverTicks >= 1 && turnoverTicks <= 1200
                    && (long) Math.max(shortTier.ratedFlowMbPerTick(),
                            Math.max(mediumTier.ratedFlowMbPerTick(), longTier.ratedFlowMbPerTick()))
                            * turnoverTicks <= Integer.MAX_VALUE
                    && Double.isFinite(minEfficiencyMultiplier) && minEfficiencyMultiplier > 0
                    && minEfficiencyMultiplier <= shortTier.maxEfficiencyMultiplier()
                    && minEfficiencyMultiplier <= mediumTier.maxEfficiencyMultiplier()
                    && minEfficiencyMultiplier <= longTier.maxEfficiencyMultiplier()
                    && Double.isFinite(minimumOperatingFlowRatio)
                    && minimumOperatingFlowRatio > 0 && minimumOperatingFlowRatio < 1
                    && Double.isFinite(frontShare) && frontShare >= 0 && frontShare <= 1
                    && Double.isFinite(10_000D * suPerMbPerTick
                            * Math.max(shortTier.maxEfficiencyMultiplier(),
                                    Math.max(mediumTier.maxEfficiencyMultiplier(),
                                            longTier.maxEfficiencyMultiplier())));
        }
    }

    private Settings settings = Settings.DEFAULT;
    private Tier tier;
    private int createMaxRpm, exhaust, inputUsedThisTick, outputUsedThisTick;
    private long flowTick = Long.MIN_VALUE, historyTick = Long.MIN_VALUE, lastOutputTick = Long.MIN_VALUE,
            emittedInWindow;
    private int historyHead;
    private int[] history = new int[Settings.DEFAULT.smoothingTicks()];
    private final Map<Long, Integer> inletUsed = new HashMap<>();
    private final Map<Long, Integer> exhaustUsed = new HashMap<>();

    public Settings settings() { return settings; }
    public Tier tier() { return tier; }
    public int exhaust() { return exhaust; }
    public int processed(long now) { return flowTick == now ? outputUsedThisTick : 0; }
    public long processedInWindow() { return emittedInWindow; }
    public int exhaustCapacity() { return tier == null ? 0 : tier.ratedFlowMbPerTick() * settings.turnoverTicks(); }
    public int ratedFlowMbPerTick() { return tier == null ? 0 : tier.ratedFlowMbPerTick(); }
    public int rpm() { return settings.rpm(); }
    public double averageFlowMbPerTick() { return (double) emittedInWindow / settings.smoothingTicks(); }
    public double minimumFlowMbPerTick() { return ratedFlowMbPerTick() * settings.minimumOperatingFlowRatio(); }
    public boolean hasRecentOutput(long now) { return lastOutputTick != Long.MIN_VALUE
            && now >= lastOutputTick && now - lastOutputTick <= 2; }

    /** 达门槛时返回本机倍率；门槛以下为零，仍允许等体积排汽。 */
    public double efficiencyMultiplier() {
        if (!settings.valid(createMaxRpm) || tier == null) return 0;
        double ratio = Math.clamp(averageFlowMbPerTick() / tier.ratedFlowMbPerTick(), 0, 1);
        if (ratio < settings.minimumOperatingFlowRatio()) return 0;
        return settings.minEfficiencyMultiplier() + (tier.maxEfficiencyMultiplier()
                - settings.minEfficiencyMultiplier()) * (ratio - settings.minimumOperatingFlowRatio())
                / (1 - settings.minimumOperatingFlowRatio());
    }

    /** 配置变更只清动力窗口，不截断已收周转蒸汽。 */
    public void applySettings(Settings next, int rotorCount, int currentCreateMaxRpm) {
        Tier nextTier = next.tierForRotors(rotorCount);
        if (settings.smoothingTicks() != next.smoothingTicks()
                || Double.compare(settings.suPerMbPerTick(), next.suPerMbPerTick()) != 0
                || Double.compare(settings.minEfficiencyMultiplier(), next.minEfficiencyMultiplier()) != 0
                || Double.compare(settings.minimumOperatingFlowRatio(), next.minimumOperatingFlowRatio()) != 0
                || settings.rpm() != next.rpm() || tier == null || nextTier == null
                || !tier.equals(nextTier) || !next.valid(currentCreateMaxRpm)) {
            history = new int[Math.clamp(next.smoothingTicks(), 1, 1200)];
            clearHistory();
        }
        settings = next;
        tier = nextTier;
        createMaxRpm = currentCreateMaxRpm;
    }

    /** 仅换档预检；同档配置缩容不会删除既有蒸汽。 */
    public boolean canFormForNewTier(int rotorCount) {
        Tier candidate = settings.tierForRotors(rotorCount);
        return settings.valid(createMaxRpm) && candidate != null
                && exhaust <= (long) candidate.ratedFlowMbPerTick() * settings.turnoverTicks();
    }

    /**
     * 控制器 tick 只补齐已经结束的世界 tick；当前 tick 在首次真实排汽时才入窗。
     * 因此轴在当 tick 排汽前读取的是前 40 个已完成样本，不会稳定少算一格。
     * 停机清动力但保留蒸汽。
     */
    public int tick(long now, boolean operational) {
        if (!operational || !settings.valid(createMaxRpm) || tier == null) {
            clearHistory();
            historyTick = now;
            return 0;
        }
        if (historyTick < now) advanceHistory(now - 1);
        return now == flowTick ? outputUsedThisTick : 0;
    }

    public void stop() { clearHistory(); }

    public double totalSu() {
        return settings.valid(createMaxRpm) && tier != null
                ? averageFlowMbPerTick() * settings.suPerMbPerTick() * efficiencyMultiplier() : 0;
    }

    /** 进汽真实成交即等量存为待排汽；全机与物理口本 tick 额度同时扣减。 */
    public int fillInput(long portKey, int amount, boolean simulate, long now) {
        int accepted = Math.min(Math.max(0, amount), remainingInput(portKey, now));
        if (!simulate && accepted > 0) {
            advanceFlow(now);
            exhaust += accepted;
            inputUsedThisTick += accepted;
            inletUsed.merge(portKey, accepted, Integer::sum);
        }
        return accepted;
    }

    /** 主动推送与被动抽取共用额度；只有 EXECUTE 移走蒸汽才计入窗口。 */
    public int drainExhaust(long portKey, int amount, boolean simulate, long now) {
        int taken = Math.min(Math.max(0, amount), remainingExhaust(portKey, now));
        if (!simulate && taken > 0) {
            advanceFlow(now);
            advanceHistory(now);
            exhaust -= taken;
            outputUsedThisTick += taken;
            exhaustUsed.merge(portKey, taken, Integer::sum);
            history[historyHead] += taken;
            emittedInWindow += taken;
            lastOutputTick = now;
        }
        return taken;
    }

    public int remainingInput(long portKey, long now) {
        if (!settings.valid(createMaxRpm) || tier == null) return 0;
        int used = now == flowTick ? inletUsed.getOrDefault(portKey, 0) : 0;
        int whole = now == flowTick ? inputUsedThisTick : 0;
        return Math.min(Math.max(0, exhaustCapacity() - exhaust),
                Math.min(Math.max(0, tier.ratedFlowMbPerTick() - whole),
                        Math.max(0, settings.inletPortFlowMbPerTick() - used)));
    }

    public int remainingExhaust(long portKey, long now) {
        if (!settings.valid(createMaxRpm) || tier == null) return 0;
        int used = now == flowTick ? exhaustUsed.getOrDefault(portKey, 0) : 0;
        int whole = now == flowTick ? outputUsedThisTick : 0;
        return Math.min(exhaust, Math.min(Math.max(0, tier.ratedFlowMbPerTick() - whole),
                Math.max(0, settings.exhaustPortFlowMbPerTick() - used)));
    }

    private void advanceFlow(long now) {
        if (flowTick == now) return;
        flowTick = now;
        inputUsedThisTick = 0;
        outputUsedThisTick = 0;
        inletUsed.clear();
        exhaustUsed.clear();
    }

    private void advanceHistory(long now) {
        if (historyTick == now) return;
        if (historyTick == Long.MIN_VALUE || now < historyTick || now - historyTick >= history.length) {
            clearHistory();
        } else {
            for (long step = 0; step < now - historyTick; step++) {
                historyHead = (historyHead + 1) % history.length;
                emittedInWindow -= history[historyHead];
                history[historyHead] = 0;
            }
        }
        historyTick = now;
    }

    private void clearHistory() {
        Arrays.fill(history, 0);
        historyHead = 0;
        emittedInWindow = 0;
    }

    /** 保存当前格式周转残留与同 tick 额度；动力窗口不落盘，恢复后不能凭空发电。 */
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("TurnoverSteam", exhaust);
        tag.putLong("FlowTick", flowTick);
        tag.putInt("InputUsed", inputUsedThisTick);
        tag.putInt("OutputUsed", outputUsedThisTick);
        tag.put("InletBudgets", saveBudgets(inletUsed));
        tag.put("ExhaustBudgets", saveBudgets(exhaustUsed));
        return tag;
    }

    public void load(CompoundTag tag) {
        exhaust = Math.max(0, tag.getInt("TurnoverSteam"));
        flowTick = tag.contains("FlowTick") ? tag.getLong("FlowTick") : Long.MIN_VALUE;
        inputUsedThisTick = Math.max(0, tag.getInt("InputUsed"));
        outputUsedThisTick = Math.max(0, tag.getInt("OutputUsed"));
        inletUsed.clear();
        exhaustUsed.clear();
        if (tag.contains("InletBudgets", Tag.TAG_LIST))
            loadBudgets(tag.getList("InletBudgets", Tag.TAG_COMPOUND), inletUsed);
        if (tag.contains("ExhaustBudgets", Tag.TAG_LIST))
            loadBudgets(tag.getList("ExhaustBudgets", Tag.TAG_COMPOUND), exhaustUsed);
        clearHistory();
        historyTick = Long.MIN_VALUE;
        lastOutputTick = Long.MIN_VALUE;
    }

    private static ListTag saveBudgets(Map<Long, Integer> budgets) {
        var list = new ListTag();
        budgets.forEach((port, used) -> {
            var entry = new CompoundTag();
            entry.putLong("Port", port);
            entry.putInt("Used", Math.max(0, used));
            list.add(entry);
        });
        return list;
    }

    private static void loadBudgets(ListTag list, Map<Long, Integer> budgets) {
        for (int index = 0; index < list.size(); index++) {
            var entry = list.getCompound(index);
            int used = Math.max(0, entry.getInt("Used"));
            if (used > 0) budgets.put(entry.getLong("Port"), used);
        }
    }
}
