package com.iksxh.create_nuclear_industry.heat;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import net.minecraft.core.BlockPos;

/** 服务端只按盆的存在维持固定热负载；不探测配方、输入、过滤或搅拌状态。 */
public final class HeatExchangerBasinBridge {
    private HeatExchangerBasinBridge() {}

    /** 仅读取最近实际付款tick的热级；查询不转换工质、不扣账或延长已付视图。 */
    public static HeatLevel heatLevel(BasinBlockEntity basin) {
        NuclearHeatExchangerBlockEntity machine = machineFor(basin);
        if (machine == null) return null;
        if (!validSource(machine, basin)) return HeatLevel.NONE;
        return machine.ledger().basinHeatingAt(basin.getLevel().getGameTime()) ? HeatLevel.SEETHING : HeatLevel.NONE;
    }

    /** 顶部有盆就结算固定费用；搅拌器仅在存在时接收唤醒通知，不参与负载判定。 */
    static void tick(NuclearHeatExchangerBlockEntity machine, HeatExchangerState.Settings settings,
                     HeatExchangerLine line) {
        long now = machine.getLevel().getGameTime();
        BlockPos basinPos = machine.getBlockPos().above();
        boolean hasBasin = machine.getLevel().hasChunkAt(basinPos)
                && machine.getLevel().getBlockEntity(basinPos) instanceof BasinBlockEntity;
        boolean eligible = hasBasin && validSource(machine, basinPos) && line != null && !line.conflict();
        machine.ledger().tickBasin(now, eligible, settings, line);
        boolean statusChanged = machine.updateBasinView(settings.basinHeatCost());
        scheduleMixerUpdate(machine, statusChanged);
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
