package com.iksxh.create_nuclear_industry.heat;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.iksxh.create_nuclear_industry.production.FuelSinteringBlockEntity;
import net.minecraft.core.BlockPos;

/** 服务端只按顶部盆或烧结炉的存在维持固定热负载；不探测其配方与加工状态。 */
public final class HeatExchangerBasinBridge {
    private HeatExchangerBasinBridge() {}

    /** 仅读取最近实际付款tick的热级；查询不转换工质、不扣账或延长已付视图。 */
    public static HeatLevel heatLevel(BasinBlockEntity basin) {
        NuclearHeatExchangerBlockEntity machine = machineFor(basin);
        if (machine == null) return null;
        if (!validSource(machine, basin)) return HeatLevel.NONE;
        return machine.ledger().basinHeatingAt(basin.getLevel().getGameTime()) ? HeatLevel.SEETHING : HeatLevel.NONE;
    }

    /** 烧结炉读取下方换热器最近实际付款tick的热源，允许一个相邻tick读取以处理执行先后；客户端依赖同步视图，不推算未同步账本。 */
    public static boolean sinteringHeated(FuelSinteringBlockEntity furnace) {
        if (furnace.getLevel() == null || !furnace.current()) return false;
        BlockPos sourcePos = furnace.getBlockPos().below();
        if (!furnace.getLevel().hasChunkAt(sourcePos)
                || !(furnace.getLevel().getBlockEntity(sourcePos) instanceof NuclearHeatExchangerBlockEntity machine))
            return false;
        if (furnace.getLevel().isClientSide) return machine.publishedSinteringHeat();
        return validSource(machine, furnace.getBlockPos())
                && machine.ledger().basinHeatingAt(furnace.getLevel().getGameTime());
    }

    /** 顶部有盆或烧结炉就结算固定费用；搅拌器仅在盆存在时接收唤醒通知。 */
    static void tick(NuclearHeatExchangerBlockEntity machine, HeatExchangerState.Settings settings,
                     HeatExchangerLine line) {
        long now = machine.getLevel().getGameTime();
        BlockPos loadPos = machine.getBlockPos().above();
        boolean loaded = machine.getLevel().hasChunkAt(loadPos);
        boolean hasBasin = loaded && machine.getLevel().getBlockEntity(loadPos) instanceof BasinBlockEntity;
        boolean hasSinteringFurnace = loaded
                && machine.getLevel().getBlockEntity(loadPos) instanceof FuelSinteringBlockEntity;
        boolean eligible = (hasBasin || hasSinteringFurnace) && validSource(machine, loadPos)
                && line != null && !line.conflict();
        machine.ledger().tickBasin(now, eligible, settings, line);
        if (hasSinteringFurnace) machine.updateSinteringView(settings.basinHeatCost());
        else {
            boolean statusChanged = machine.updateBasinView(settings.basinHeatCost());
            scheduleMixerUpdate(machine, statusChanged);
        }
    }

    /** 热源状态变化或首次建立负载时唤醒Create检查，稳定状态交给Create自身内容事件处理。 */
    static void scheduleMixerUpdate(NuclearHeatExchangerBlockEntity machine, boolean statusChanged) {
        if (machine.getLevel() == null || machine.getLevel().isClientSide) return;
        BlockPos basinPos = machine.getBlockPos().above();
        if (!machine.getLevel().hasChunkAt(basinPos)
                || !(machine.getLevel().getBlockEntity(basinPos) instanceof BasinBlockEntity)) {
            machine.observeBasinOperator(null);
            return;
        }
        BlockPos operatorPos = basinPos.above(2);
        MechanicalMixerBlockEntity mixer = machine.getLevel().hasChunkAt(operatorPos)
                && machine.getLevel().getBlockEntity(operatorPos) instanceof MechanicalMixerBlockEntity found
                ? found : null;
        boolean firstConnection = machine.observeBasinOperator(mixer);
        if (mixer != null && (statusChanged || firstConnection))
            mixer.basinChecker.scheduleUpdate();
    }

    private static NuclearHeatExchangerBlockEntity machineFor(BasinBlockEntity basin) {
        if (basin.getLevel() == null || basin.getLevel().isClientSide) return null;
        BlockPos source = basin.getBlockPos().below();
        if (!basin.getLevel().hasChunkAt(source)) return null;
        return basin.getLevel().getBlockEntity(source) instanceof NuclearHeatExchangerBlockEntity machine
                ? machine : null;
    }

    private static boolean validSource(NuclearHeatExchangerBlockEntity machine, BasinBlockEntity basin) {
        return validSource(machine, basin.getBlockPos());
    }

    private static boolean validSource(NuclearHeatExchangerBlockEntity machine, BlockPos basinPos) {
        if (machine.getBlockPos().above().equals(basinPos) == false
                || !machine.current() || !machine.canTick() || machine.inBoiler()
                || machine.ledger().mode() != HeatExchangerMode.NUCLEAR
                || machine.getLevel() == null || machine.getLevel().isClientSide) return false;
        HeatExchangerLine line = HeatExchangerLine.find(machine);
        return line != null && !line.conflict() && line.displayMode() == HeatExchangerMode.NUCLEAR;
    }
}
