package com.iksxh.create_nuclear_industry.reactor;

/** P1 融毁倒计时状态机；进度只会保持或递增，修复完成时才清零。 */
public final class ReactorMeltdownStateMachine {
    private ReactorMeltdownStateMachine() {
    }

    /**
     * 根据有效燃料列的传播覆盖率、SCRAM 与有效冷却更新融毁状态。危险覆盖率达到
     * 阈值才启动倒计时；SCRAM 或有效冷却会暂停进度，但不会回退已累计 tick。
     */
    public static MeltdownUpdateResult update(
            ReactorSnapshot beforePropagation,
            HeatPropagationResult propagation,
            boolean scramActive,
            boolean effectiveCooling,
            ReactorSimulationParameters parameters
    ) {
        if (beforePropagation == null || propagation == null || parameters == null) {
            throw new IllegalArgumentException("meltdown update inputs are required");
        }

        long effectiveFuelCount = beforePropagation.fuelColumns().values().stream()
                .filter(FuelColumnState::isEffectiveFuel)
                .count();
        long coveredFuelCount = propagation.coveredEffectiveFuelColumns().stream()
                .filter(position -> {
                    FuelColumnState fuel = beforePropagation.fuelColumns().get(position);
                    return fuel != null && fuel.isEffectiveFuel();
                })
                .count();
        double coverage = effectiveFuelCount == 0L
                ? 0.0D
                : (double) coveredFuelCount / effectiveFuelCount;
        boolean danger = coveredFuelCount > 0L && coverage >= parameters.meltdownTriggerFraction();

        ReactorSnapshot propagated = propagation.snapshot();
        if (allColumnsFullyRepaired(propagated)) {
            ReactorSnapshot reset = withMeltdown(propagated, 0L, false);
            return new MeltdownUpdateResult(reset, MeltdownStatus.INACTIVE, coverage, danger);
        }

        boolean started = propagated.meltdownCountdownStarted() || danger;
        long progress = Math.min(propagated.meltdownProgressTicks(), parameters.meltdownCountdownTicks());
        boolean paused = started && (!danger || scramActive || effectiveCooling);
        if (started && danger && !scramActive && !effectiveCooling
                && progress < parameters.meltdownCountdownTicks()) {
            progress++;
        }

        MeltdownStatus status;
        if (!started) {
            status = MeltdownStatus.INACTIVE;
        } else if (progress >= parameters.meltdownCountdownTicks()) {
            status = MeltdownStatus.COMPLETE;
        } else if (paused) {
            status = MeltdownStatus.PAUSED;
        } else {
            status = MeltdownStatus.RUNNING;
        }
        return new MeltdownUpdateResult(
                withMeltdown(propagated, progress, started),
                status,
                coverage,
                danger
        );
    }

    private static boolean allColumnsFullyRepaired(ReactorSnapshot snapshot) {
        return snapshot.fuelColumns().values().stream().allMatch(fuel -> fuel.integrity() == 1.0D)
                && snapshot.controlRodColumns().values().stream().allMatch(control -> control.integrity() == 1.0D);
    }

    private static ReactorSnapshot withMeltdown(ReactorSnapshot snapshot, long progress, boolean started) {
        return snapshot.withMeltdown(progress, started);
    }
}
