package com.iksxh.create_nuclear_industry.reactor;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SCRAM 与裂变邻接规则的纯 Java 回归测试。
 *
 * <p>测试只验证确定性服务端 tick 的阶段证据，不启动 Minecraft 世界；控制棒深度单位为
 * {@code [0,1]}，裂变热量单位为 HU，燃耗为燃料块等效量。</p>
 */
class ReactorScramFissionRegressionTest {
    /** 使用正式服务端默认参数，避免测试绕开配置读取后的计算路径。 */
    private static final ReactorSimulationParameters PARAMETERS = ReactorSimulationParameters.defaults();
    /** 固定的燃料列坐标，用于说明控制棒邻接和非邻接两种边界。 */
    private static final CoreColumnPosition CENTER = new CoreColumnPosition(1, 1);

    /** SCRAM 将相邻且未卡死的控制棒完全插入，并停止该相邻燃料列的裂变。 */
    @Test
    void scramFullyInsertsAdjacentMovableRodAndStopsAdjacentHeat() {
        CoreColumnPosition controlPosition = new CoreColumnPosition(1, 0);
        ReactorServerTick.Result result = ReactorServerTick.advance(
                scrammedSnapshot(
                        CENTER,
                        controlPosition,
                        new ControlRodColumnState(1.0D, 0.0D, 0.0D, false, 0.0D)
                ),
                PARAMETERS,
                ReactorServerTick.CoolantInput.none()
        );

        assertEquals(1.0D,
                result.controlSnapshot().controlRodColumns().get(controlPosition).actualDepth(),
                1.0E-12D);
        assertEquals(0.0D, result.fission().columns().get(CENTER).generatedHeatHu(), 1.0E-12D);
        assertEquals(0.0D,
                result.fission().columns().get(CENTER).plannedFuelBurnUnits(),
                1.0E-15D);
    }

    /** 卡死控制棒不能继续插入；其保留的部分插入深度仍允许相邻列产生裂变。 */
    @Test
    void scramKeepsPartiallyInsertedJammedAdjacentRodFission() {
        CoreColumnPosition controlPosition = new CoreColumnPosition(1, 0);
        ReactorServerTick.Result result = ReactorServerTick.advance(
                scrammedSnapshot(
                        CENTER,
                        controlPosition,
                        new ControlRodColumnState(0.0D, 0.35D, 0.35D, true, 0.0D)
                ),
                PARAMETERS,
                ReactorServerTick.CoolantInput.none()
        );

        assertTrue(result.controlSnapshot().controlRodColumns().get(controlPosition).jammed());
        assertEquals(0.35D,
                result.controlSnapshot().controlRodColumns().get(controlPosition).actualDepth(),
                1.0E-12D);
        assertEquals(1.95D, result.fission().columns().get(CENTER).generatedHeatHu(), 1.0E-12D);
        assertTrue(result.fission().columns().get(CENTER).plannedFuelBurnUnits() > 0.0D);
    }

    /** SCRAM 的邻接影响不能越过堆芯坐标关系而全局抑制非相邻燃料列。 */
    @Test
    void scramControlRodDoesNotAffectNonAdjacentFuel() {
        CoreColumnPosition fuelPosition = new CoreColumnPosition(0, 0);
        CoreColumnPosition controlPosition = new CoreColumnPosition(2, 2);
        ReactorServerTick.Result result = ReactorServerTick.advance(
                scrammedSnapshot(
                        fuelPosition,
                        controlPosition,
                        new ControlRodColumnState(1.0D, 0.0D, 0.0D, false, 0.0D)
                ),
                PARAMETERS,
                ReactorServerTick.CoolantInput.none()
        );

        assertEquals(1.0D,
                result.controlSnapshot().controlRodColumns().get(controlPosition).actualDepth(),
                1.0E-12D);
        assertEquals(3.0D, result.fission().columns().get(fuelPosition).generatedHeatHu(), 1.0E-12D);
        assertTrue(result.fission().columns().get(fuelPosition).plannedFuelBurnUnits() > 0.0D);
    }

    /** 构造已请求 SCRAM 的最小快照；卡死棒没有可恢复目标，活动棒保存其目标深度。 */
    private static ReactorSnapshot scrammedSnapshot(
            CoreColumnPosition fuelPosition,
            CoreColumnPosition controlPosition,
            ControlRodColumnState control
    ) {
        Map<CoreColumnPosition, Double> savedTargets = control.jammed()
                ? Map.of()
                : Map.of(controlPosition, control.targetDepth());
        return new ReactorSnapshot(
                Map.of(fuelPosition,
                        new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D)),
                Map.of(controlPosition, control),
                0L,
                0L,
                0L,
                false,
                savedTargets,
                true
        );
    }
}
