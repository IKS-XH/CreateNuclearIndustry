package com.iksxh.create_nuclear_industry.reactor;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

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

    /** 保存现场149列精确尾热在真实冷液存在时可安全停机，缓存和工质不因修复被删除。 */
    @Test
    void saved149ColumnResidualReachesShutdownAndSurvivesReload() {
        ReactorSnapshot snapshot = saved149ColumnSnapshot();
        assertEquals(149, snapshot.fuelColumns().size());
        assertFalse(ReactorFullShutdownAssessment.assess(snapshot, PARAMETERS).fullyStopped());
        var original = snapshot;
        for (int tick = 0; tick < 20; tick++) {
            var result = ReactorServerTick.advance(snapshot, PARAMETERS,
                    coolantInput(128D, 128D, 658_000L));
            snapshot = result.snapshot();
            assertEquals(0D, result.fission().generatedHeatHu());
            assertEquals(0D, result.fission().plannedFuelBurnUnits());
            assertEquals(0D, result.effectiveCoolingHeatHu());
            assertEquals(original.coldCoolantMb(), snapshot.coldCoolantMb());
            assertEquals(original.hotCoolantMb(), snapshot.hotCoolantMb());
            assertTrue(ReactorFullShutdownAssessment.assess(snapshot, PARAMETERS).fullyStopped());
            for (var entry : original.fuelColumns().entrySet()) {
                var next = snapshot.fuelColumns().get(entry.getKey());
                assertEquals(entry.getValue().cachedHeatHu(), next.cachedHeatHu());
                assertEquals(entry.getValue().fuelBurnRemainder(), next.fuelBurnRemainder());
                assertEquals(entry.getValue().fuelAssembly(), next.fuelAssembly());
            }
        }
        var reloaded = ReactorSnapshotNbtCodec.decode(ReactorSnapshotNbtCodec.encode(snapshot));
        assertEquals(snapshot.withoutFuelAssemblies(), reloaded);
        assertTrue(ReactorFullShutdownAssessment.assess(reloaded, PARAMETERS).fullyStopped());
    }

    /** 单列低于epsilon也不能漏标：三列总量高于完全停机门，必须按真实正余热分配。 */
    @Test
    void subEpsilonColumnsDoNotCreateAnAggregateShutdownDeadZone() {
        var fuel = new TreeMap<CoreColumnPosition, FuelColumnState>();
        for (int x = 0; x < 3; x++) fuel.put(new CoreColumnPosition(x, 0),
                new FuelColumnState(FuelAssemblyState.empty(), 1D, 7.5E-13D));
        var previous = new ReactorSnapshot(fuel, Map.of(), 1L, 0L, 0L, false);
        assertFalse(ReactorFullShutdownAssessment.assess(previous, PARAMETERS).fullyStopped());
        var result = ReactorServerTick.advance(previous, PARAMETERS, coolantInput(0D, 0D, 1L));
        assertTrue(ReactorFullShutdownAssessment.assess(result.snapshot(), PARAMETERS).fullyStopped());
        assertEquals(previous.coldCoolantMb(), result.snapshot().coldCoolantMb());
        assertEquals(0D, result.effectiveCoolingHeatHu());
        for (var entry : fuel.entrySet()) assertEquals(entry.getValue().cachedHeatHu(),
                result.snapshot().fuelColumns().get(entry.getKey()).cachedHeatHu());
    }

    /** 全棒插入也不代替冷却：没有冷液的现场尾热仍须拒绝安全拆除。 */
    @Test
    void zeroColdKeepsSavedResidualUnsafe() {
        var previous = saved149ColumnSnapshot().withCoolantInventories(0L, 658_000L);
        var result = ReactorServerTick.advance(previous, PARAMETERS, coolantInput(0D, 0D, 658_000L));
        assertFalse(ReactorFullShutdownAssessment.assess(result.snapshot(), PARAMETERS).fullyStopped());
        assertEquals(0D, result.coolant().quantizedHeatRemainderHu());
        assertEquals(previous.withoutFuelAssemblies(), result.snapshot().withoutFuelAssemblies());
    }

    /** 由2026-10-16保存副本投影；每行是列坐标、完整度、缓存HU、燃耗尾数、安全HU、物品耐久。 */
    private static ReactorSnapshot saved149ColumnSnapshot() {
        String rows = """
                0 0 1.0 2.604139126560767e-12 0.9331907248814808 0.0 1 207 216000
                0 1 1.0 3.113953539468639e-12 0.04243246999572747 0.0 1 50338 216000
                0 3 1.0 3.1885605267234496e-12 0.5378711028017111 0.0 1 60744 216000
                0 4 1.0 3.1441516057384433e-12 0.04659919447295913 0.0 1 50939 216000
                0 5 1.0 3.1938895972416503e-12 0.5105996803735469 0.0 1 61345 216000
                0 8 1.0 3.1885605267234496e-12 0.36527665799414244 0.0 1 61584 216000
                0 9 1.0 3.1441516057384433e-12 0.1983991980546289 0.0 1 49766 216000
                0 10 1.0 3.1956659540810506e-12 0.03745257063662777 0.0 1 62188 216000
                0 12 1.0 2.6858515411731787e-12 0.6672281738851407 0.0 1 2238 216000
                1 1 1.0 3.5633718198369024e-12 0.23536026832512036 0.0 1 94596 216000
                1 2 1.0 3.2436275887448573e-12 0.16239470858071847 0.0 1 63041 216000
                1 3 1.0 3.581135388230905e-12 0.41694369378439333 0.0 1 96033 216000
                1 5 1.0 3.6202152386977104e-12 0.7994563932071799 0.0 1 98518 216000
                1 6 1.0 3.625544309215911e-12 0.6759723067141987 0.0 1 98790 216000
                1 7 1.0 3.2525093729418586e-12 0.94537946490974 0.0 1 64569 216000
                1 8 1.0 3.581135388230905e-12 0.2546388499130856 0.0 1 97628 216000
                1 10 1.0 3.623767952376511e-12 0.14812909672145125 0.0 1 99911 216000
                1 11 1.0 3.652189661806915e-12 0.1530297809458201 0.0 1 99657 216000
                1 12 1.0 3.518962898851896e-12 0.3942939126755327 0.0 1 79231 216000
                1 13 1.0 3.1814550993658486e-12 0.724752073887899 0.0 1 49356 216000
                2 0 1.0 2.6929569685307797e-12 0.9086500382746028 0.0 1 4459 216000
                2 1 1.0 3.561595462997502e-12 0.015751105642173968 0.0 1 94602 216000
                2 3 1.0 3.6628478028433165e-12 0.10938912919465693 0.0 1 99558 216000
                2 4 1.0 3.671729587040318e-12 0.6662462240171614 0.0 1 100140 216000
                2 5 1.0 3.666400516522117e-12 0.3800951624031512 0.0 1 100257 216000
                2 6 1.0 3.668176873361517e-12 0.295315325471897 0.0 1 100535 216000
                2 8 1.0 3.664624159682717e-12 0.6184573425566775 0.0 1 100967 216000
                2 9 1.0 3.673505943879718e-12 0.8572678494018096 0.0 1 101528 216000
                2 10 1.0 3.668176873361517e-12 0.4216251184010318 0.0 1 101637 216000
                2 11 1.0 3.6699532302009175e-12 0.24884818281613974 0.0 1 101847 216000
                2 13 1.0 3.140598892059643e-12 0.7705843781287864 0.0 1 48783 216000
                3 1 1.0 3.657518732325116e-12 0.09268484189713888 0.0 1 98634 216000
                3 2 1.0 3.671729587040318e-12 0.35238284542846543 0.0 1 99538 216000
                3 3 1.0 3.673505943879718e-12 0.2487907442240136 0.0 1 99876 216000
                3 4 1.0 3.671729587040318e-12 0.626498252670677 0.0 1 100188 216000
                3 6 1.0 3.671729587040318e-12 0.6716471285380052 0.0 1 100765 216000
                3 7 1.0 3.673505943879718e-12 0.7709569427860554 0.0 1 101028 216000
                3 8 1.0 3.673505943879718e-12 0.2846940866392611 0.0 1 101275 216000
                3 9 1.0 3.673505943879718e-12 0.25189174388595603 0.0 1 101576 216000
                3 11 1.0 3.668176873361517e-12 0.7850117908258518 0.0 1 101936 216000
                3 12 1.0 3.6202152386977104e-12 0.6464921976605442 0.0 1 100513 216000
                3 13 1.0 3.1938895972416503e-12 0.6369756482030944 0.0 1 62575 216000
                4 0 1.0 3.1938895972416503e-12 0.5658285672601462 0.0 1 52662 216000
                4 1 1.0 3.625544309215911e-12 0.8883865234766866 0.0 1 96540 216000
                4 2 1.0 3.668176873361517e-12 0.5916690969517919 0.0 1 99345 216000
                4 4 1.0 3.673505943879718e-12 0.19634136421614556 0.0 1 100232 216000
                4 5 1.0 3.673505943879718e-12 0.9268224178756928 0.0 1 100479 216000
                4 6 1.0 3.673505943879718e-12 0.7556671319191892 0.0 1 100756 216000
                4 7 1.0 3.673505943879718e-12 0.8911942325581705 0.0 1 101030 216000
                4 9 1.0 3.673505943879718e-12 0.6397853841721588 0.0 1 101616 216000
                4 10 1.0 3.671729587040318e-12 0.6131084066823096 0.0 1 101844 216000
                4 11 1.0 3.668176873361517e-12 0.4448857572379712 0.0 1 101929 216000
                4 12 1.0 3.625544309215911e-12 0.0006335610778798184 0.0 1 100487 216000
                5 0 1.0 3.1441516057384433e-12 0.595087218781087 0.0 1 51703 216000
                5 2 1.0 3.671729587040318e-12 0.36178415243781004 0.0 1 99599 216000
                5 3 1.0 3.671729587040318e-12 0.7907353191655933 0.0 1 99912 216000
                5 4 1.0 3.671729587040318e-12 0.09887807038139229 0.0 1 100203 216000
                5 5 1.0 3.673505943879718e-12 0.986849356193396 0.0 1 100476 216000
                5 7 1.0 3.673505943879718e-12 0.2240350703628966 0.0 1 101065 216000
                5 8 1.0 3.673505943879718e-12 0.036750128229064316 0.0 1 101312 216000
                5 9 1.0 3.673505943879718e-12 0.008598052540074619 0.0 1 101590 216000
                5 10 1.0 3.673505943879718e-12 0.6774123137343775 0.0 1 101857 216000
                5 12 1.0 3.2525093729418586e-12 0.6774417550131471 0.0 1 65786 216000
                6 0 1.0 3.1885605267234496e-12 0.9106363085042712 0.0 1 52558 216000
                6 1 1.0 3.581135388230905e-12 0.11184874179592086 0.0 1 94537 216000
                6 2 1.0 3.664624159682717e-12 0.5970653626150302 0.0 1 99154 216000
                6 3 1.0 3.671729587040318e-12 0.807881073407378 0.0 1 99871 216000
                6 5 1.0 3.673505943879718e-12 0.7117225944114711 0.0 1 100510 216000
                6 6 1.0 3.673505943879718e-12 0.5649850115022428 0.0 1 100757 216000
                6 7 1.0 3.673505943879718e-12 0.34118874297140067 0.0 1 101036 216000
                6 8 1.0 3.673505943879718e-12 0.9857080762351575 0.0 1 101308 216000
                6 10 1.0 3.671729587040318e-12 0.26033528992606847 0.0 1 101860 216000
                6 11 1.0 3.664624159682717e-12 0.38753861310596527 0.0 1 101782 216000
                6 12 1.0 3.581135388230905e-12 0.008139661880569804 0.0 1 98854 216000
                6 13 1.0 3.1885605267234496e-12 0.8644243401070781 0.0 1 62406 216000
                7 1 1.0 3.2507330161024584e-12 0.11709225439426163 0.0 1 62951 216000
                7 3 1.0 3.671729587040318e-12 0.08544415684125362 0.0 1 99948 216000
                7 4 1.0 3.673505943879718e-12 0.3411570588642192 0.0 1 100200 216000
                7 5 1.0 3.673505943879718e-12 0.5205668767345379 0.0 1 100481 216000
                7 6 1.0 3.673505943879718e-12 0.38360999626164904 0.0 1 100754 216000
                7 8 1.0 3.673505943879718e-12 0.5001888841825064 0.0 1 101342 216000
                7 9 1.0 3.671729587040318e-12 0.4982040584665466 0.0 1 101588 216000
                7 10 1.0 3.671729587040318e-12 0.15905499953011493 0.0 1 101859 216000
                7 11 1.0 3.673505943879718e-12 0.5371901147214135 0.0 1 102086 216000
                7 13 1.0 3.1441516057384433e-12 0.5242768936816162 0.0 1 48827 216000
                8 1 1.0 3.625544309215911e-12 0.5089076119858493 0.0 1 97257 216000
                8 2 1.0 3.668176873361517e-12 0.4437641719359604 0.0 1 99380 216000
                8 3 1.0 3.673505943879718e-12 0.1834578081833378 0.0 1 99898 216000
                8 4 1.0 3.673505943879718e-12 0.854911981413661 0.0 1 100180 216000
                8 6 1.0 3.673505943879718e-12 0.681473406224141 0.0 1 100787 216000
                8 7 1.0 3.673505943879718e-12 0.6913634527616974 0.0 1 101034 216000
                8 8 1.0 3.673505943879718e-12 0.5843253625973048 0.0 1 101313 216000
                8 9 1.0 3.673505943879718e-12 0.0483521811189469 0.0 1 101586 216000
                8 11 1.0 3.668176873361517e-12 0.32946892683761675 0.0 1 101993 216000
                8 12 1.0 3.625544309215911e-12 0.44932485374552655 0.0 1 100683 216000
                8 13 1.0 3.1956659540810506e-12 0.9412399539881751 0.0 1 62556 216000
                9 0 1.0 3.1938895972416503e-12 0.12459736031758872 0.0 1 49132 216000
                9 1 1.0 3.6202152386977104e-12 0.7573938865015712 0.0 1 95986 216000
                9 2 1.0 3.6628478028433165e-12 0.8241659045443139 0.0 1 99111 216000
                9 4 1.0 3.6699532302009175e-12 0.3388048435789095 0.0 1 100070 216000
                9 5 1.0 3.671729587040318e-12 0.3614693010038508 0.0 1 100447 216000
                9 6 1.0 3.671729587040318e-12 0.678906840991738 0.0 1 100755 216000
                9 7 1.0 3.673505943879718e-12 0.4488276538460809 0.0 1 101030 216000
                9 9 1.0 3.673505943879718e-12 0.4261731532941049 0.0 1 101618 216000
                9 10 1.0 3.671729587040318e-12 0.7850536146360305 0.0 1 101858 216000
                9 11 1.0 3.671729587040318e-12 0.9975733651076606 0.0 1 102092 216000
                9 12 1.0 3.664624159682717e-12 0.1531056339236656 0.0 1 102072 216000
                10 0 1.0 3.1441516057384433e-12 0.10120676200514822 0.0 1 31986 216000
                10 2 1.0 3.6290970228947117e-12 0.6783360608303948 0.0 1 97976 216000
                10 3 1.0 3.2578384434600594e-12 0.5628390795549922 0.0 1 63774 216000
                10 4 1.0 3.630873379734112e-12 0.4075499555232547 0.0 1 98603 216000
                10 5 1.0 3.668176873361517e-12 0.6481438894219593 0.0 1 100308 216000
                10 7 1.0 3.671729587040318e-12 0.8779968784099932 0.0 1 101055 216000
                10 8 1.0 3.671729587040318e-12 0.8287381291717058 0.0 1 101273 216000
                10 9 1.0 3.673505943879718e-12 0.32693383314621727 0.0 1 101584 216000
                10 10 1.0 3.671729587040318e-12 0.6558128999397841 0.0 1 101836 216000
                10 12 1.0 3.6202152386977104e-12 0.9834756135333045 0.0 1 100744 216000
                10 13 1.0 3.2009950245992513e-12 0.8950813313174937 0.0 1 64807 216000
                11 0 1.0 3.1938895972416503e-12 0.6549110794979551 0.0 1 49125 216000
                11 1 1.0 3.61843888185831e-12 0.9004901458231842 0.0 1 95907 216000
                11 2 1.0 3.661071446003916e-12 0.9351401349972086 0.0 1 99078 216000
                11 5 1.0 3.6699532302009175e-12 0.395269289992898 0.0 1 100438 216000
                11 6 1.0 3.668176873361517e-12 0.6006763007783764 0.0 1 100564 216000
                11 7 1.0 3.671729587040318e-12 0.7627245823178352 0.0 1 100979 216000
                11 8 1.0 3.6628478028433165e-12 0.6043640258339131 0.0 1 100935 216000
                11 10 1.0 3.668176873361517e-12 0.7103108890637753 0.0 1 101636 216000
                11 11 1.0 3.6628478028433165e-12 0.7319978313501316 0.0 1 101613 216000
                11 12 1.0 3.6202152386977104e-12 0.29561897055542197 0.0 1 100697 216000
                11 13 1.0 3.2027713814386516e-12 0.8452034912581183 0.0 1 64805 216000
                12 1 1.0 3.6131098113401094e-12 0.00831385692009512 0.0 1 96660 216000
                12 2 1.0 3.661071446003916e-12 0.37512209913871075 0.0 1 99130 216000
                12 3 1.0 3.6202152386977104e-12 0.9695450623133031 0.0 1 98012 216000
                12 4 1.0 3.6202152386977104e-12 0.6970272447366721 0.0 1 98314 216000
                12 5 1.0 3.6628478028433165e-12 0.9505632441646892 0.0 1 100106 216000
                12 6 1.0 3.625544309215911e-12 0.12347266625034514 0.0 1 98929 216000
                12 8 1.0 3.581135388230905e-12 0.23927995692238113 0.0 1 97675 216000
                12 9 1.0 3.2507330161024584e-12 0.020023484375517597 0.0 1 65034 216000
                12 10 1.0 3.625544309215911e-12 0.02657594974989763 0.0 1 99788 216000
                12 11 1.0 3.61843888185831e-12 0.040344785454337284 0.0 1 98786 216000
                13 0 1.0 2.6059154834001674e-12 0.04566043924613972 0.0 1 208 216000
                13 1 1.0 3.12105896682624e-12 0.14911601849260414 0.0 1 50491 216000
                13 3 1.0 3.2027713814386516e-12 0.34138172273354606 0.0 1 62527 216000
                13 4 1.0 3.2009950245992513e-12 0.32027811331813005 0.0 1 62725 216000
                13 6 1.0 3.1956659540810506e-12 0.31430777245022945 0.0 1 61401 216000
                13 7 1.0 3.1441516057384433e-12 0.4931480688071659 0.0 1 50236 216000
                13 8 1.0 3.19033688356285e-12 0.937416527207299 0.0 1 61722 216000
                13 11 1.0 3.183231456205249e-12 0.5889247995816795 0.0 1 49852 216000
                13 12 1.0 3.057110120607831e-12 0.6115019162197743 0.0 1 46952 216000
                13 13 1.0 4.410694032230822e-12 0.8370462461098835 0.0 1 1971 216000
                """;
        var fuel = new TreeMap<CoreColumnPosition, FuelColumnState>();
        for (String row : rows.strip().split("\\R")) {
            String[] v = row.strip().split("\\s+");
            fuel.put(new CoreColumnPosition(Integer.parseInt(v[0]), Integer.parseInt(v[1])),
                    new FuelColumnState(FuelAssemblyState.installed(Integer.parseInt(v[8]), Integer.parseInt(v[7])),
                            Double.parseDouble(v[2]), Double.parseDouble(v[3]),
                            Double.parseDouble(v[4]), Double.parseDouble(v[5])));
        }
        var rods = new TreeMap<CoreColumnPosition, ControlRodColumnState>();
        var targets = new TreeMap<CoreColumnPosition, Double>();
        for (int x = 0; x < 14; x++) for (int z = 0; z < 14; z++) {
            var pos = new CoreColumnPosition(x, z);
            if (!fuel.containsKey(pos)) { rods.put(pos, ControlRodColumnState.fullyInserted()); targets.put(pos, 1D); }
        }
        return new ReactorSnapshot(fuel, rods, 658_000L, 0L, 0L, false, targets, true, false,
                new com.iksxh.create_nuclear_industry.structure.ReactorDimensions(16, 16, 16));
    }

    /** 默认 0.5 HU/mB 下，三行半插棒堆每 tick 原始约 23.604 HU，只取整成 24 HU/48 mB。 */
    @Test
    void repeatedFractionalFissionUsesOneTotalForColumnsCoolingAndTelemetry() {
        TreeMap<CoreColumnPosition, FuelColumnState> fuels = new TreeMap<>();
        TreeMap<CoreColumnPosition, ControlRodColumnState> controls = new TreeMap<>();
        for (int z = 0; z < 3; z++) {
            for (int x : new int[]{0, 2}) {
                fuels.put(new CoreColumnPosition(x, z), new FuelColumnState(
                        FuelAssemblyState.installed(216_000, 0), 1.0D, 0.0D));
            }
            controls.put(new CoreColumnPosition(1, z),
                    new ControlRodColumnState(1.0D, 0.5D, 0.5D, false, 0.0D));
        }
        ReactorSnapshot snapshot = new ReactorSnapshot(fuels, controls, 1_000L, 0L, 0L, false);
        for (int tick = 0; tick < 10; tick++) {
            ReactorServerTick.Result result = ReactorServerTick.advance(
                    snapshot, PARAMETERS, coolantInput(128.0D, 128.0D, 2_000L));
            ReactorInstrumentTelemetry telemetry = ReactorInstrumentTelemetry.from(result);
            assertEquals(23.604377652134303D, result.fission().rawHeatHu(), 1.0E-12D);
            assertEquals(24.0D, result.fission().generatedHeatHu());
            assertEquals(24.0D, result.fission().columns().values().stream()
                    .mapToDouble(FuelColumnFissionResult::generatedHeatHu).sum(), 1.0E-12D);
            assertEquals(24.0D, telemetry.totalGeneratedFissionHeatHuPerTick());
            assertEquals(48.0D, telemetry.convertedCoolantMbPerTick());
            assertEquals(24.0D, result.effectiveCoolingHeatHu());
            assertEquals(48L, snapshot.coldCoolantMb() - result.snapshot().coldCoolantMb());
            assertEquals(48L, result.snapshot().hotCoolantMb() - snapshot.hotCoolantMb());
            assertEquals(0.00010927952616728845D,
                    result.fission().plannedFuelBurnUnits(), 1.0E-15D);
            for (CoreColumnPosition position : fuels.keySet()) {
                assertEquals(result.fission().columns().get(position).generatedHeatHu(),
                        result.thermal().columns().get(position).generatedHeatHu());
                assertEquals(0.0D, result.snapshot().fuelColumns().get(position).cachedHeatHu(), 1.0E-12D);
                assertEquals(0.0D, result.snapshot().fuelColumns().get(position).quantizedHeatRemainderHu(),
                        1.0E-12D);
                assertEquals(1.0D, result.snapshot().fuelColumns().get(position).integrity());
            }
            snapshot = result.snapshot();
        }
    }

    /** 全插棒只冷却原有 0.75 HU 缓存；转换 1 mB 并保留 0.25 HU，不补成整数热。 */
    @Test
    void fullyInsertedRodsCoolFractionalCacheWithoutRoundingItAgain() {
        ReactorSnapshot snapshot = new ReactorSnapshot(
                Map.of(CENTER, new FuelColumnState(FuelAssemblyState.installed(216_000, 0), 1.0D, 0.75D)),
                Map.of(new CoreColumnPosition(1, 0), ControlRodColumnState.fullyInserted()),
                128L, 0L, 0L, false);
        ReactorServerTick.Result result = ReactorServerTick.advance(
                snapshot, PARAMETERS, coolantInput(128.0D, 128.0D, 1_000L));

        assertEquals(0.0D, result.fission().generatedHeatHu());
        assertEquals(0.0D, result.fission().plannedFuelBurnUnits());
        assertEquals(1.0D, result.coolant().settlement().convertedCoolantMb());
        assertEquals(0.5D, result.effectiveCoolingHeatHu());
        assertEquals(0.25D, result.snapshot().fuelColumns().get(CENTER).cachedHeatHu());
        assertEquals(0.25D, result.snapshot().fuelColumns().get(CENTER).quantizedHeatRemainderHu());
        assertEquals(0.0D, ReactorInstrumentTelemetry.from(result).totalGeneratedFissionHeatHuPerTick());
    }

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
