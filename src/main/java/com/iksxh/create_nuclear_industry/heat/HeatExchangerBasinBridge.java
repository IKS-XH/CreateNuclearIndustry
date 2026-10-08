package com.iksxh.create_nuclear_industry.heat;

import com.iksxh.create_nuclear_industry.config.HeatExchangerConfig;
import com.iksxh.create_nuclear_industry.mixin.BasinOperatingAccessor;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.Recipe;

/** 服务端工作盆桥接；需求只复用Create原生匹配，费用只在原生批次提交时锁定。 */
public final class HeatExchangerBasinBridge {
    private record Demand(BlockPos basin, double cost) {}
    private record CandidateSet(List<Recipe<?>> recipes, boolean invalidCost) {}
    private record Payment(HeatExchangerState ledger, BlockPos basin, double cost, HeatLevel level) {}
    private static final Map<NuclearHeatExchangerBlockEntity, CandidateSet> CANDIDATES = new WeakHashMap<>();
    private static final ThreadLocal<BasinBlockEntity> PROBE = new ThreadLocal<>();
    private static final ThreadLocal<Payment> PAYMENT = new ThreadLocal<>();

    private HeatExchangerBasinBridge() {}

    /** Create原生候选过滤器和输入/输出模拟在短作用域内忽略热条件；候选仍由原版决定。 */
    public static boolean matchCandidate(BasinOperatingBlockEntity operator, BasinBlockEntity basin, Recipe<?> recipe) {
        if (basin.getLevel() == null || basin.getLevel().isClientSide) return BasinRecipe.match(basin, recipe);
        NuclearHeatExchangerBlockEntity machine = machineFor(basin);
        if (machine == null || !(operator instanceof MechanicalMixerBlockEntity))
            return BasinRecipe.match(basin, recipe);
        if (!validSource(machine, basin)) return BasinRecipe.match(basin, recipe);
        HeatCondition condition = recipe instanceof BasinRecipe basinRecipe
                ? basinRecipe.getRequiredHeat() : HeatCondition.NONE;
        if (condition == HeatCondition.NONE) {
            boolean matched = BasinRecipe.match(basin, recipe);
            if (matched) addCandidate(machine, recipe, false);
            return matched;
        }

        BasinBlockEntity previous = PROBE.get();
        PROBE.set(basin);
        boolean matched;
        try { matched = BasinRecipe.match(basin, recipe); }
        finally { if (previous == null) PROBE.remove(); else PROBE.set(previous); }
        if (!matched) return false;

        double cost = cost(condition, HeatExchangerConfig.settings());
        if (!Double.isFinite(cost) || cost <= 0 || cost > HeatExchangerConfig.settings().capacity()) {
            addCandidate(machine, null, true);
            return false;
        }
        addCandidate(machine, recipe, false);
        return machine.ledger().reserve() + 1e-9 >= cost;
    }

    /** 仅给盆下的核热换热器返回动态已付热；探测返回最高档，不污染Create缓存。 */
    public static HeatLevel heatLevel(BasinBlockEntity basin) {
        if (PROBE.get() == basin) return HeatLevel.SEETHING;
        Payment payment = PAYMENT.get();
        if (payment != null && payment.basin().equals(basin.getBlockPos())) return payment.level();
        NuclearHeatExchangerBlockEntity machine = machineFor(basin);
        if (machine == null || !validSource(machine, basin)) return null;
        var cfg = HeatExchangerConfig.settings();
        double reserve = machine.ledger().reserve();
        if (reserve >= cfg.basinSuperheatedCost()) return HeatLevel.SEETHING;
        if (reserve >= cfg.basinHeatedCost()) return HeatLevel.FADING;
        return HeatLevel.NONE;
    }

    /** 每个源tick重新走Create候选探测，需求失效时当tick即停止转换。 */
    static void tick(NuclearHeatExchangerBlockEntity machine, HeatExchangerState.Settings settings,
                     HeatExchangerLine line) {
        CANDIDATES.remove(machine);
        BlockPos basinPos = machine.getBlockPos().above();
        if (machine.getLevel() == null || !machine.getLevel().hasChunkAt(basinPos)
                || !(machine.getLevel().getBlockEntity(basinPos) instanceof BasinBlockEntity basin)
                || !basin.canContinueProcessing()) {
            machine.ledger().tickBasin(machine.getLevel().getGameTime(), 0, settings, line);
            machine.updateBasinView(null);
            return;
        }
        BlockPos operatorPos = basinPos.above(2);
        if (!machine.getLevel().hasChunkAt(operatorPos)
                || !(machine.getLevel().getBlockEntity(operatorPos) instanceof MechanicalMixerBlockEntity mixer)
                || mixer.getSpeed() == 0) {
            machine.ledger().tickBasin(machine.getLevel().getGameTime(), 0, settings, line);
            machine.updateBasinView(null);
            return;
        }
        BasinOperatingAccessor access = (BasinOperatingAccessor) mixer;
        Demand demand = null;
        CandidateSet candidates = null;
        if (mixer.isSpeedRequirementFulfilled()) {
            List<Recipe<?>> matching = access.createNuclearIndustry$getMatchingRecipes();
            candidates = CANDIDATES.remove(machine);
            if (candidates != null) matching = candidates.recipes();
            matching.sort((first, second) -> second.getIngredients().size() - first.getIngredients().size());
            if (!matching.isEmpty() && matching.getFirst() instanceof BasinRecipe basinRecipe
                    && basinRecipe.getRequiredHeat() != HeatCondition.NONE)
                demand = new Demand(basin.getBlockPos(), cost(basinRecipe.getRequiredHeat(), settings));
        }
        if (candidates == null) candidates = CANDIDATES.remove(machine);
        boolean invalidCost = candidates != null && candidates.invalidCost();
        double demandHu = demand == null ? (invalidCost ? settings.capacity() + 1 : 0) : demand.cost();
        machine.ledger().tickBasin(machine.getLevel().getGameTime(), demandHu, settings, line);
        machine.updateBasinView(demand == null ? (invalidCost ? null : 0) : demand.cost());
        scheduleIfReady(machine, demand);
    }

    static void scheduleIfReady(NuclearHeatExchangerBlockEntity machine, Demand demand) {
        if (demand == null || machine.ledger().reserve() + 1e-9 < demand.cost()) return;
        BlockPos operatorPos = demand.basin().above(2);
        if (machine.getLevel() == null || !machine.getLevel().hasChunkAt(operatorPos)
                || !(machine.getLevel().getBlockEntity(operatorPos) instanceof MechanicalMixerBlockEntity mixer)) return;
        BasinOperatingAccessor access = (BasinOperatingAccessor) mixer;
        mixer.basinChecker.scheduleUpdate();
    }

    /** 原生apply开始前预留整批HU；非核热盆、NONE和压片配方保持Create原行为。 */
    public static boolean beginApply(BasinBlockEntity basin, Recipe<?> recipe) {
        if (PAYMENT.get() != null) return false;
        if (!(recipe instanceof BasinRecipe basinRecipe)) return true;
        HeatCondition condition = basinRecipe.getRequiredHeat();
        if (condition == HeatCondition.NONE) return true;
        NuclearHeatExchangerBlockEntity machine = machineFor(basin);
        // 盆下没有本设备时，Create烈焰人等原生供热路径继续完整执行。
        if (machine == null) return true;
        if (!validSource(machine, basin) || !poweredMixer(basin)) return false;
        var cfg = HeatExchangerConfig.settings();
        double amount = cost(condition, cfg);
        if (!Double.isFinite(amount) || amount <= 0 || amount > cfg.capacity()
                || !machine.ledger().reserveBasinBatch(amount)) return false;
        HeatLevel level = condition == HeatCondition.SUPERHEATED ? HeatLevel.SEETHING : HeatLevel.FADING;
        PAYMENT.set(new Payment(machine.ledger(), basin.getBlockPos().immutable(), amount, level));
        return true;
    }

    /** 仅成功原生提交消费预留；正常失败归还储备，避免Create检查失败收费。 */
    public static void finishApply(boolean success) {
        Payment payment = PAYMENT.get();
        if (payment == null) return;
        PAYMENT.remove();
        if (!success) payment.ledger().releaseBasinBatch(payment.cost());
    }

    private static double cost(HeatCondition condition, HeatExchangerState.Settings cfg) {
        return condition == HeatCondition.SUPERHEATED ? cfg.basinSuperheatedCost() : cfg.basinHeatedCost();
    }

    private static void addCandidate(NuclearHeatExchangerBlockEntity machine, Recipe<?> recipe, boolean invalidCost) {
        CandidateSet current = CANDIDATES.get(machine);
        List<Recipe<?>> recipes = current == null ? new ArrayList<>() : current.recipes();
        if (recipe != null && !recipes.contains(recipe)) recipes.add(recipe);
        CANDIDATES.put(machine, new CandidateSet(recipes, invalidCost || current != null && current.invalidCost()));
    }

    private static NuclearHeatExchangerBlockEntity machineFor(BasinBlockEntity basin) {
        if (basin.getLevel() == null || basin.getLevel().isClientSide) return null;
        BlockPos source = basin.getBlockPos().below();
        if (!basin.getLevel().hasChunkAt(source)) return null;
        return basin.getLevel().getBlockEntity(source) instanceof NuclearHeatExchangerBlockEntity machine
                ? machine : null;
    }

    private static boolean validSource(NuclearHeatExchangerBlockEntity machine, BasinBlockEntity basin) {
        if (machine.getBlockPos().above().equals(basin.getBlockPos()) == false
                || !machine.current() || !machine.canTick() || machine.inBoiler()
                || machine.ledger().mode() != HeatExchangerMode.NUCLEAR) return false;
        HeatExchangerLine line = HeatExchangerLine.find(machine);
        return line != null && !line.conflict() && line.displayMode() == HeatExchangerMode.NUCLEAR;
    }

    private static boolean poweredMixer(BasinBlockEntity basin) {
        BlockPos operatorPos = basin.getBlockPos().above(2);
        if (!basin.getLevel().hasChunkAt(operatorPos)
                || !(basin.getLevel().getBlockEntity(operatorPos) instanceof MechanicalMixerBlockEntity mixer)
                || mixer.getSpeed() == 0) return false;
        return mixer.isSpeedRequirementFulfilled();
    }
}
