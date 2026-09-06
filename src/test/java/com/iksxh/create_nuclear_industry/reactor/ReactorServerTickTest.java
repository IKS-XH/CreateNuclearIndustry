package com.iksxh.create_nuclear_industry.reactor;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 正式服务端反应堆单 tick 编排的确定性回归测试。
 *
 * <p>测试以阶段结果证据检查固定顺序：控制状态、裂变、冷却、热负荷与损伤、传播、融毁，
 * 最后才提交燃耗。热量使用 HU，冷却剂库存使用 mB，冷却输入能力使用 mB/t。</p>
 */
class ReactorServerTickTest {
    /** 使用正式默认热工、控制棒、燃耗和融毁参数。 */
    private static final ReactorSimulationParameters PARAMETERS = ReactorSimulationParameters.defaults();
    /** 标准 3×3 堆芯中心列，作为单列 tick 测试的固定坐标。 */
    private static final CoreColumnPosition CENTER = new CoreColumnPosition(1, 1);

    /** 验证一次完整 tick 的热量、冷却、损伤、燃耗和冷/热流体库存提交结果。 */
    @Test
    void oneTickCommitsFuelBurnCoolantAndDamageInTheDocumentedOrder() {
        ReactorSnapshot before = new ReactorSnapshot(
                Map.of(CENTER, new FuelColumnState(
                        FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)),
                Map.of(),
                128L,
                0L,
                0L,
                false
        );
        ReactorServerTick.Result result = ReactorServerTick.advance(
                before,
                PARAMETERS,
                coolantInput(128.0D, 128.0D, 1_000L)
        );

        assertEquals(3.0D, result.fission().generatedHeatHu(), 1.0E-12D);
        assertEquals(6.0D, result.coolant().settlement().convertedCoolantMb(), 1.0E-12D);
        assertEquals(3.0D, result.thermal().columns().get(CENTER).removedHeatHu(), 1.0E-12D);
        assertEquals(1.0D, result.snapshot().fuelColumns().get(CENTER).integrity(), 1.0E-12D);
        assertEquals(0.0D, result.snapshot().fuelColumns().get(CENTER).cachedHeatHu(), 1.0E-12D);
        assertEquals(3, result.snapshot().fuelColumns().get(CENTER).fuelAssembly().damage());
        assertEquals(122L, result.snapshot().coldCoolantMb());
        assertEquals(6L, result.snapshot().hotCoolantMb());
    }

    /** 验证冷却不足时残余热、失效列传播和融毁倒计时按稳定顺序推进。 */
    @Test
    void missingCoolingCreatesResidualHeatDamageAndAStableMeltdownProgression() {
        CoreColumnPosition source = new CoreColumnPosition(1, 1);
        CoreColumnPosition target = new CoreColumnPosition(1, 2);
        ReactorSnapshot before = new ReactorSnapshot(
                Map.of(
                        source, new FuelColumnState(
                                FuelAssemblyState.installed(216_000, 0), 0.0D, 4.0D),
                        target, new FuelColumnState(
                                FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)
                ),
                Map.of(),
                0L,
                0L,
                0L,
                false
        );

        ReactorServerTick.Result result = ReactorServerTick.advance(
                before,
                PARAMETERS,
                coolantInput(128.0D, 128.0D, 1_000L)
        );

        assertTrue(result.thermal().columns().get(source).netHeatLoadHu() > 0.0D);
        assertTrue(result.thermal().columns().get(target).integrityDamage() > 0.0D);
        assertTrue(result.propagation().coveredEffectiveFuelColumns().contains(target));
        assertTrue(result.snapshot().meltdownCountdownStarted());
        assertEquals(1L, result.snapshot().meltdownProgressTicks());
        assertFalse(result.snapshot().fuelColumns().get(source).fuelAssembly().exhausted());
    }

    /** 验证本 tick 耗尽的燃料仍贡献本 tick 热量，但下一 tick 起停止产热。 */
    @Test
    void exhaustedAssemblyStopsGeneratingFromTheFollowingTick() {
        ReactorSimulationParameters oneTickFuel = new ReactorSimulationParameters(
                PARAMETERS.baseHeatPerFuelBlockHuPerTick(),
                3.0D / 72_000.0D,
                PARAMETERS.damageHeatThresholdHuPerTick(),
                PARAMETERS.damageRatePerTickHuLoad(),
                PARAMETERS.damageTransferRate(),
                PARAMETERS.controlRodFailureThreshold(),
                PARAMETERS.meltdownTriggerFraction(),
                PARAMETERS.meltdownCountdownTicks(),
                PARAMETERS.controlResponseExponent(),
                PARAMETERS.overclockHeatMultiplier(),
                PARAMETERS.overclockBurnMultiplier(),
                PARAMETERS.overclockFeedbackGain(),
                PARAMETERS.overclockFeedbackExponent(),
                PARAMETERS.totalHeatMultiplierCap()
        );
        ReactorSnapshot before = ReactorSnapshot.singleFuelColumn(
                CENTER,
                new FuelColumnState(FuelAssemblyState.installed(3, 0), 1.0D, 0.0D)
        );

        ReactorServerTick.Result first = ReactorServerTick.advance(
                before, oneTickFuel, coolantInput(0.0D, 0.0D, 1_000L));
        ReactorServerTick.Result second = ReactorServerTick.advance(
                first.snapshot(), oneTickFuel, coolantInput(0.0D, 0.0D, 1_000L));

        assertTrue(first.snapshot().fuelColumns().get(CENTER).fuelAssembly().exhausted());
        assertTrue(first.fission().generatedHeatHu() > 0.0D);
        assertEquals(0.0D, second.fission().generatedHeatHu(), 1.0E-12D);
    }

    /** 验证小数燃耗余量会穿过热量传播并经过版本化 NBT 往返保持不变。 */
    @Test
    void fractionalFuelBurnSurvivesThermalPropagationAndNbtReload() {
        ReactorSnapshot before = ReactorSnapshot.singleFuelColumn(
                CENTER,
                new FuelColumnState(FuelAssemblyState.installed(100, 0), 1.0D, 0.0D)
        );

        ReactorSnapshot after = ReactorServerTick.advance(
                before,
                PARAMETERS,
                ReactorServerTick.CoolantInput.none()
        ).snapshot();

        assertTrue(after.fuelColumns().get(CENTER).fuelBurnRemainder() > 0.0D);
        CompoundTag encoded = ReactorSnapshotNbtCodec.encode(after);
        assertEquals(after.withoutFuelAssemblies(), ReactorSnapshotNbtCodec.decode(encoded));
    }

    /** 构造同时含冷端输入、热端容量和单位吸热量的服务端冷却观测。 */
    private static ReactorServerTick.CoolantInput coolantInput(
            double coldAvailable,
            double hotAvailable,
            long hotCapacity
    ) {
        return new ReactorServerTick.CoolantInput(
                ReactorCoolantLedger.summarizePorts(
                        java.util.List.of(
                                ReactorCoolantLedger.Port.cold("cold", coldAvailable),
                                ReactorCoolantLedger.Port.hot("hot", hotAvailable)
                        ),
                        128.0D
                ),
                hotCapacity,
                0.5D
        );
    }
}
