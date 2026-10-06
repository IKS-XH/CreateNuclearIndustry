package com.iksxh.create_nuclear_industry.boiler;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** 服务端唯一水汽、冷却剂与 HU 账本。库存单位 mB；模拟纯读，汽输出按实际完整比焓扣账。 */
public final class BoilerState {
    public static final int CAPACITY = 18000, FLOW_LIMIT = 256;
    private static final double EPS = 1e-8;
    /** 几何独立的配置快照；容量乘积在运行前以 long 预检，非法组合不交易。 */
    public record Settings(int minDimension, int maxDimension, int waterCapacityPerCellMb,
            int steamCapacityPerCellMb, int portFlowMbPerTick, double pairHeatHuPerTick,
            double boilingTemperature, double supercriticalTemperature, double wallHeatCapacityHuPerWaterCell,
            double waterSpecificHeatHuPerMb, double steamSpecificHeatHuPerMb, double vaporizationLatentHeatHuPerMb,
            double supercriticalPressure, double outputMinPressure,
            double valveOpenPressure, double valveClosePressure, int valveFlowPerSteamCellMbPerTick,
            double idleWaterCoolingHuPerCellPerTick, double idleSteamCoolingHuPerCellPerTick) {
        public boolean valid() {
            if (minDimension < 5 || minDimension > maxDimension || maxDimension > 32
                    || waterCapacityPerCellMb < 1 || steamCapacityPerCellMb < 1 || portFlowMbPerTick < 1
                    || valveFlowPerSteamCellMbPerTick < 1) return false;
            for (double v : new double[]{pairHeatHuPerTick, boilingTemperature, supercriticalTemperature,
                    wallHeatCapacityHuPerWaterCell, waterSpecificHeatHuPerMb, steamSpecificHeatHuPerMb,
                    vaporizationLatentHeatHuPerMb, supercriticalPressure, valveOpenPressure})
                if (!Double.isFinite(v) || v <= 0 || v > 1e9) return false;
            for (double v : new double[]{outputMinPressure,
                    valveClosePressure, idleWaterCoolingHuPerCellPerTick, idleSteamCoolingHuPerCellPerTick})
                if (!Double.isFinite(v) || v < 0 || v > 1e9) return false;
            long cells = (long) (maxDimension - 2) * (maxDimension - 2) * (maxDimension - 4);
            return supercriticalTemperature > boilingTemperature && valveClosePressure < valveOpenPressure
                    && supercriticalPressure < valveOpenPressure && outputMinPressure <= 1
                    && cells * waterCapacityPerCellMb <= Integer.MAX_VALUE
                    && cells * steamCapacityPerCellMb <= Integer.MAX_VALUE
                    && cells * valveFlowPerSteamCellMbPerTick <= Integer.MAX_VALUE;
        }
        /** 仅配置摘要使用的参考 5³ 容量；实际容量读取账本几何。 */
        public int waterCapacityMb() { return 9 * waterCapacityPerCellMb; }
        public int steamCapacityMb() { return 9 * steamCapacityPerCellMb; }
    }
    public static final Settings DEFAULT = new Settings(5, 11, 2000, 2000, 256, 18, 1, 2,
            1600, .1, .2, .7, .5, .6, .9, .8, 32, .9, .1);
    private Settings settings = DEFAULT;
    private int waterCells = 9, steamCells = 9, exchangers = 1, pairs = 1, hotCapacity = 4000, coldCapacity = 4000;
    private int water, steam, hot, cold, produced, vented;
    private double waterHu, steamHu, processHu, coolantHu, conversionFraction;
    private double minimum = .6;
    private boolean minimumSet, valveOpen, valveBlocked;
    private long lastTick = Long.MIN_VALUE, preparedTick = Long.MIN_VALUE, flowTick = Long.MIN_VALUE;
    private long conversionTick = Long.MIN_VALUE, ventTick = Long.MIN_VALUE, totalVented;
    private final Map<Long, Integer> usedByPort = new HashMap<>();
    public int water() { return water; }
    public int steam() { return steam; }
    public int hot() { return hot; }
    public int cold() { return cold; }
    public int waterCells() { return waterCells; }
    public int steamCells() { return steamCells; }
    public int pairs() { return pairs; }
    public double warmHu() { return waterHu; }
    public double steamHu() { return steamHu; }
    public double processHu() { return processHu; }
    public double coolantHu() { return coolantHu; }
    public double totalHu() { return waterHu + steamHu + processHu + coolantHu; }
    public boolean ready() { return settings.valid() && waterTemperature() + EPS >= targetTemperature(); }
    public boolean valveOpen() { return valveOpen; }
    public boolean valveBlocked() { return valveBlocked; }
    public long totalVented() { return totalVented; }
    public int produced() { return produced; }
    public int vented() { return vented; }
    public Settings settings() { return settings; }
    public int waterCapacity() { return capacity(waterCells, settings.waterCapacityPerCellMb()); }
    public int steamCapacity() { return capacity(steamCells, settings.steamCapacityPerCellMb()); }
    public int hotCapacity() { return hotCapacity; }
    public int coldCapacity() { return coldCapacity; }
    /** 汽种只描述当前已付焓的温压资格，不能反过来改变热工目标或付款。 */
    public boolean supercritical() { return outputQualified() && steamTemperature() + EPS >= settings.supercriticalTemperature()
            && pressure() + EPS >= settings.supercriticalPressure(); }
    public double targetTemperature() { return settings.supercriticalTemperature(); }
    public double boilingEnthalpy() { return settings.waterSpecificHeatHuPerMb() * settings.boilingTemperature() + settings.vaporizationLatentHeatHuPerMb(); }
    public double steamEnthalpy(double t) { return boilingEnthalpy() + settings.steamSpecificHeatHuPerMb() * Math.max(0, t - settings.boilingTemperature()); }
    public double waterHeatCapacity() { return settings.wallHeatCapacityHuPerWaterCell() * waterCells + settings.waterSpecificHeatHuPerMb() * water; }
    public double waterTemperature() { return waterHeatCapacity() > 0 ? waterHu / waterHeatCapacity() : 0; }
    public double steamTemperature() { return steam > 0 ? settings.boilingTemperature()
            + Math.max(0, steamHu / steam - boilingEnthalpy()) / settings.steamSpecificHeatHuPerMb() : 0; }
    public double pressure() { return steamCapacity() > 0 ? (double) steam / steamCapacity() * steamTemperature() / settings.supercriticalTemperature() : 0; }
    public double minimumPressure() { return minimum; }
    public boolean outputQualified() { return settings.valid() && steam > 0 && steamHu + EPS >= steam * boilingEnthalpy(); }

    /** 参数变化保留现存工质与已付热；非法设置由交易入口拒绝。 */
    public void setSettings(Settings next) {
        settings = next;
        if (!minimumSet) minimum = next.outputMinPressure();
    }
    /** 只接受已验证结构的格数与成员数，缩容只停止收量，不裁切旧库存。 */
    public void setGeometry(int waterCells, int steamCells, int exchangers, int sections, int hotPerMachine, int coldPerMachine) {
        this.waterCells = Math.max(0, waterCells); this.steamCells = Math.max(0, steamCells);
        this.exchangers = Math.max(0, exchangers); pairs = Math.min(this.exchangers, Math.max(0, sections));
        hotCapacity = capacity(this.exchangers, hotPerMachine); coldCapacity = capacity(this.exchangers, coldPerMachine);
    }
    private static int capacity(int n, int per) { long value = (long) n * per; return value > 0 && value <= Integer.MAX_VALUE ? (int) value : 0; }
    /** 出汽下限是独立的归一压力[0,1]；即使高于安全阀线也合法，不改任何库存或HU。 */
    public void setMinimumPressure(double value) {
        if (!Double.isFinite(value)) return;
        minimum = Math.clamp(value, 0, 1); minimumSet = true;
    }
    private int budget(BlockPos port, long now) { return settings.valid() ? Math.max(0, settings.portFlowMbPerTick()
            - (now == flowTick ? usedByPort.getOrDefault(port.asLong(), 0) : 0)) : 0; }
    private void spend(BlockPos port, int amount, long now) {
        if (flowTick != now) { usedByPort.clear(); flowTick = now; }
        usedByPort.merge(port.asLong(), amount, Integer::sum);
    }
    public int remainingFill(BlockPos port, long now) { return Math.min(Math.max(0, waterCapacity() - water), budget(port, now)); }
    public int remainingHotFill(BlockPos port, long now) { return Math.min(Math.max(0, hotCapacity - hot), budget(port, now)); }
    public int remainingDrain(BlockPos port, long now) { return Math.min(removableSteam(), budget(port, now)); }
    /** 补冷水不增加 HU，热容增加自然造成混合降温。 */
    public int fillWater(BlockPos port, int amount, boolean simulate, long now) {
        int n = Math.min(Math.max(0, amount), remainingFill(port, now));
        if (!simulate && n > 0) { water += n; spend(port, n, now); } return n;
    }
    public int fillHot(BlockPos port, int amount, boolean simulate, long now) {
        int n = Math.min(Math.max(0, amount), remainingHotFill(port, now));
        if (!simulate && n > 0) { hot += n; spend(port, n, now); } return n;
    }
    public int drainCold(BlockPos port, int amount, boolean simulate, long now) {
        int n = Math.min(Math.max(0, amount), Math.min(cold, budget(port, now)));
        if (!simulate && n > 0) { cold -= n; spend(port, n, now); } return n;
    }
    /** 同口主动、被动抽取共用额度，每次重算整罐保压余量。 */
    public int drainSteam(BlockPos port, int amount, boolean simulate, long now) {
        int n = Math.min(Math.max(0, amount), remainingDrain(port, now));
        if (!simulate && n > 0) { removeSteam(n); spend(port, n, now); } return n;
    }
    private int removableSteam() {
        if (!outputQualified()) return 0;
        double retained = minimumPressure() * steamCapacity() * settings.supercriticalTemperature() / steamTemperature();
        return Math.max(0, steam - (int) Math.min(Integer.MAX_VALUE, Math.ceil(retained - EPS)));
    }
    private void removeSteam(int n) {
        double carried = steamHu / steam * n;
        steam -= n; steamHu = steam == 0 ? 0 : Math.max(0, steamHu - carried);
    }
    /** 成型事务的成员库存溢出预检；当前成员的核热不能被第二份账本复制。 */
    public boolean canImport(int h, int c, double hu) {
        return h >= 0 && c >= 0 && (long) hot + h <= Integer.MAX_VALUE && (long) cold + c <= Integer.MAX_VALUE
                && Double.isFinite(hu) && hu >= 0 && Double.isFinite(coolantHu + hu);
    }
    public void importMember(int h, int c, double hu) {
        if (!canImport(h, c, hu)) throw new IllegalArgumentException("炉内成员库存溢出");
        hot += h; cold += c; coolantHu += hu;
    }
    /** 等量热转冷只入一次 HU 储备；分数仅是速率尾量，不代表已付能量。 */
    public double collectHeat(long now, double rating, double density, double demand) {
        if (conversionTick == now || !settings.valid() || !Double.isFinite(density) || density <= 0 || demand <= 0 || rating <= 0) return 0;
        conversionTick = now;
        double limit = Math.min(demand, Math.min(rating, pairs * settings.pairHeatHuPerTick()));
        double request = limit / density + conversionFraction;
        int n = (int) Math.min(Integer.MAX_VALUE, Math.floor(request));
        conversionFraction = request - Math.floor(request);
        int needed = (int) Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(0, limit - coolantHu) / density));
        n = Math.min(n, Math.min(needed, Math.min(hot, Math.max(0, coldCapacity - cold))));
        hot -= n; cold += n; coolantHu += n * density;
        double paid = Math.min(limit, coolantHu); coolantHu -= paid; return paid;
    }
    /** 满汽仍可再热；无水无汽时停止收热。 */
    public double demand(int ignoredSections) { return demand(true); }
    public double demand(boolean clear) {
        if (!settings.valid() || pairs < 1) return 0;
        double reheat = Math.max(0, steam * steamEnthalpy(targetTemperature()) - steamHu);
        if (!clear) reheat = Math.min(reheat, pressureHeatRoom());
        double warming = water > 0 ? Math.max(0, waterHeatCapacity() * targetTemperature() - waterHu) : 0;
        double production = Math.min(water, Math.max(0, steamCapacity() - steam)) * steamEnthalpy(targetTemperature());
        if (!clear && pressure() >= settings.valveOpenPressure() - EPS) production = 0;
        return Math.min(pairs * settings.pairHeatHuPerTick(), Math.max(0, warming + reheat + production - processHu));
    }
    /** 卸载间隔仅散失显热；不会散掉潜热或重置原有付款。 */
    public void prepare(long now, int ignoredSections) {
        if (preparedTick == now) return;
        preparedTick = now;
        if (lastTick != Long.MIN_VALUE && now > lastTick + 1) cool(now - lastTick - 1);
    }
    private void cool(long ticks) {
        waterHu = Math.max(0, waterHu - ticks * waterCells * settings.idleWaterCoolingHuPerCellPerTick());
        steamHu = Math.max(Math.min(steamHu, steam * boilingEnthalpy()), steamHu - ticks * steamCells * settings.idleSteamCoolingHuPerCellPerTick());
    }
    private double pressureHeatRoom() { return Math.max(0, (settings.valveOpenPressure() - pressure()) * steamCapacity()
            * settings.supercriticalTemperature() * settings.steamSpecificHeatHuPerMb()); }
    /** 结构无效时只结算时间与显热散失，绝不把拆开的储备转为新工质。 */
    public void idle(long now) {
        if (now == lastTick) return;
        prepare(now, pairs); lastTick = now; produced = vented = 0;
        if (settings.valid()) cool(1);
    }
    private double takeWorkingHeat(double amount, boolean residual) {
        double reserve = Math.min(amount, processHu); processHu -= reserve;
        double wall = residual ? Math.min(Math.max(0, amount - reserve), Math.max(0,
                waterHu - waterHeatCapacity() * settings.boilingTemperature())) : 0;
        waterHu -= wall; return reserve + wall;
    }
    /**
     * 先预热水侧、再补已有汽的实际焓、再汽化；水携带热从水侧移入汽侧，只额外付焓差。
     * 红石停止新收热，仍可消耗高于沸点的已付炉体余热。安全阀由控制器输出后单独结算。
     */
    public void tick(long now, int ignoredSections, double paidHu, boolean clear, boolean redstoneStop) {
        if (now == lastTick) return;
        produced = vented = 0;
        if (!settings.valid()) return;
        prepare(now, pairs); lastTick = now;
        double paid = !redstoneStop && Double.isFinite(paidHu) ? Math.max(0, paidHu) : 0;
        processHu += paid;
        if (paid == 0) cool(1);
        double target = targetTemperature();
        if (water > 0 && paid > 0) {
            double warming = Math.min(processHu, Math.max(0, waterHeatCapacity() * target - waterHu));
            waterHu += warming; processHu -= warming;
        }
        double reheat = Math.max(0, steam * steamEnthalpy(target) - steamHu);
        if (!clear) reheat = Math.min(reheat, pressureHeatRoom());
        steamHu += takeWorkingHeat(reheat, paid == 0);
        double tw = waterTemperature();
        if (water > 0 && steam < steamCapacity() && (tw + EPS >= target || paid == 0 && tw + EPS >= settings.boilingTemperature())) {
            double carried = settings.waterSpecificHeatHuPerMb() * tw;
            double temperature = Math.max(target, steamTemperature());
            double enthalpy = steamEnthalpy(temperature), difference = enthalpy - carried;
            double spare = processHu + (paid == 0 ? Math.max(0, waterHu - waterHeatCapacity() * settings.boilingTemperature()) : 0);
            // 余热汽化必须留下剩余水和炉壁在沸点所需的显热，不能将已转移的水热重复支付。
            double cost = paid == 0 ? enthalpy - settings.waterSpecificHeatHuPerMb() * settings.boilingTemperature() : difference;
            int n = Math.min((int) Math.min(Integer.MAX_VALUE, Math.floor((spare + EPS) / Math.max(EPS, cost))),
                    Math.min(water, steamCapacity() - steam));
            if (!clear) n = Math.min(n, (int) Math.max(0, Math.floor((settings.valveOpenPressure() - pressure())
                    * steamCapacity() * settings.supercriticalTemperature() / temperature + EPS)));
            if (n > 0) {
                waterHu -= carried * n; water -= n;
                if (difference > 0) takeWorkingHeat(difference * n, paid == 0); else processHu -= difference * n;
                steam += n; steamHu += enthalpy * n; produced = n;
            }
        }
        valveBlocked = !clear && pressure() + EPS >= settings.valveOpenPressure();
    }
    /** 按炉压启闭，逐次扣实际比焓；无论调用多少次每 tick 只泄放一次。 */
    public void vent(long now, boolean clear) {
        if (ventTick == now || !settings.valid()) return;
        ventTick = now; vented = 0;
        if (pressure() + EPS >= settings.valveOpenPressure()) valveOpen = true;
        if (pressure() <= settings.valveClosePressure() + EPS) valveOpen = false;
        valveBlocked = valveOpen && !clear;
        if (valveOpen && clear && steam > 0) {
            int retained = (int) Math.ceil(settings.valveClosePressure() * steamCapacity() * settings.supercriticalTemperature() / steamTemperature() - EPS);
            vented = Math.min(Math.max(0, steam - retained), capacity(steamCells, settings.valveFlowPerSteamCellMbPerTick()));
            if (vented > 0) { removeSteam(vented); totalVented += vented; }
        }
        if (pressure() <= settings.valveClosePressure() + EPS) valveOpen = false;
    }
    /** 当前版本 NBT 保留全部工质、HU、几何与同 tick 额度；恢复不补量或刷新预算。 */
    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("Water", water); t.putInt("Steam", steam); t.putInt("Hot", hot); t.putInt("Cold", cold);
        t.putDouble("WaterHu", waterHu); t.putDouble("SteamHu", steamHu); t.putDouble("ProcessHu", processHu);
        t.putDouble("CoolantHu", coolantHu); t.putDouble("ConversionFraction", conversionFraction);
        t.putInt("WaterCells", waterCells); t.putInt("SteamCells", steamCells); t.putInt("Exchangers", exchangers);
        t.putInt("Pairs", pairs); t.putInt("HotCapacity", hotCapacity); t.putInt("ColdCapacity", coldCapacity);
        t.putDouble("Minimum", minimum); t.putBoolean("MinimumSet", minimumSet); t.putBoolean("ValveOpen", valveOpen);
        t.putLong("TotalVented", totalVented); t.putLong("LastTick", lastTick); t.putLong("FlowTick", flowTick);
        t.putLong("ConversionTick", conversionTick); t.putLong("VentTick", ventTick);
        ListTag list = new ListTag();
        usedByPort.forEach((port, used) -> { CompoundTag e = new CompoundTag(); e.putLong("Port", port); e.putInt("Used", used); list.add(e); });
        t.put("Budgets", list); return t;
    }
    public void load(CompoundTag t) {
        water = Math.max(0, t.getInt("Water")); steam = Math.max(0, t.getInt("Steam")); hot = Math.max(0, t.getInt("Hot")); cold = Math.max(0, t.getInt("Cold"));
        waterHu = finite(t.getDouble("WaterHu")); steamHu = finite(t.getDouble("SteamHu")); processHu = finite(t.getDouble("ProcessHu")); coolantHu = finite(t.getDouble("CoolantHu"));
        conversionFraction = Math.min(Math.nextDown(1D), finite(t.getDouble("ConversionFraction")));
        if (t.contains("WaterCells")) {
            waterCells = Math.clamp(t.getInt("WaterCells"), 0, 27000); steamCells = Math.clamp(t.getInt("SteamCells"), 0, 27000);
            exchangers = Math.clamp(t.getInt("Exchangers"), 0, 900); pairs = Math.clamp(t.getInt("Pairs"), 0, exchangers);
            hotCapacity = Math.max(0, t.getInt("HotCapacity")); coldCapacity = Math.max(0, t.getInt("ColdCapacity"));
        }
        minimum = t.contains("Minimum") ? Math.clamp(finite(t.getDouble("Minimum")), 0, 1) : settings.outputMinPressure();
        minimumSet = t.getBoolean("MinimumSet");
        valveOpen = t.getBoolean("ValveOpen"); totalVented = Math.max(0, t.getLong("TotalVented"));
        lastTick = t.contains("LastTick") ? t.getLong("LastTick") : Long.MIN_VALUE;
        flowTick = t.contains("FlowTick") ? t.getLong("FlowTick") : Long.MIN_VALUE;
        conversionTick = t.contains("ConversionTick") ? t.getLong("ConversionTick") : Long.MIN_VALUE;
        ventTick = t.contains("VentTick") ? t.getLong("VentTick") : Long.MIN_VALUE;
        preparedTick = Long.MIN_VALUE; usedByPort.clear();
        for (Tag item : t.getList("Budgets", Tag.TAG_COMPOUND)) { CompoundTag e = (CompoundTag) item; usedByPort.put(e.getLong("Port"), Math.max(0, e.getInt("Used"))); }
    }
    private static double finite(double v) { return Double.isFinite(v) ? Math.max(0, v) : 0; }
}
