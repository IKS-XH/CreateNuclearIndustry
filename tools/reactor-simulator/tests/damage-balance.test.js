import test from "node:test";
import assert from "node:assert/strict";
import { DEFAULT_CONFIG, DEFAULT_GEOMETRY as geometry, RULE_VERSION, validateConfig, baseBurnPerFuel } from "../src/config.js";
import { createDepthMatrix, createPreset } from "../src/layout.js";
import { createInitialSnapshot, stepSnapshot, runTicks } from "../src/simulation.js";
import { buildScene, parseSceneText, buildCsv } from "../src/report.js";

/** 以固定 5×5×5、孤立燃料 (0,0)、足够燃料和冷却隔离一次裂变结算。 */
function fixture(integrity = 0.5, endpoints = [2, 3]) {
  const config = { ...DEFAULT_CONFIG, fuelColumnDamageHeatMultiplier: endpoints[0], fuelColumnDamageBurnMultiplier: endpoints[1] };
  const layout = createPreset(geometry, "empty");
  layout[0][0] = "fuel";
  const depths = createDepthMatrix(geometry, layout);
  const snapshot = createInitialSnapshot(geometry, layout, depths, config);
  snapshot.columns[0].integrity = integrity;
  return { config, geometry, layout, depths, snapshot };
}

function settle(f) { return stepSnapshot(f.snapshot, f.geometry, f.layout, f.config, { running: true }); }
function near(actual, expected, tolerance = 1e-12) {
  assert.ok(Math.abs(actual - expected) <= tolerance, `${actual} != ${expected}，容差 ${tolerance}`);
}

// 预期值取自 02A 默认表和自定义线性合同，不由待测函数生成。
for (const [endpoints, heat, burn] of [
  [[2, 3], [1, 1.25, 1.5, 1.75, 2], [1, 1.5, 2, 2.5, 3]],
  [[1.5, 2.5], [1, 1.125, 1.25, 1.375, 1.5], [1, 1.375, 1.75, 2.125, 2.5]],
  [[3, 5], [1, 1.5, 2, 2.5, 3], [1, 2, 3, 4, 5]],
]) {
  test(`损伤曲线 ${endpoints.join("/")}：五个采样、斜率差、H/B 和一次结算`, () => {
    let previousEfficiency = Infinity;
    for (const [index, integrity] of [1, 0.75, 0.5, 0.25, 0].entries()) {
      const f = fixture(integrity, endpoints);
      const result = settle(f);
      const column = result.columns["0,0"];
      near(column.damageHeatMultiplier, heat[index]);
      near(column.damageBurnMultiplier, burn[index]);
      near(column.generatedHeat, 3 * heat[index]);
      near(column.plannedBurn, 3 / 216000 * burn[index], 1e-15);
      assert.equal(result.nextSnapshot.columns[0].integrity, integrity);
      assert.ok(heat[index] / burn[index] < previousEfficiency);
      previousEfficiency = heat[index] / burn[index];
      if (index > 0) assert.ok(burn[index] - burn[index - 1] > heat[index] - heat[index - 1]);
    }
  });
}

test("非法终点被配置、直接结算、场景导入与导出一致拒绝", () => {
  for (const pair of [[1, 3], [0, 3], [-1, 3], [3, 3], [3, 2], [NaN, 3], [2, Infinity],
    [-Infinity, 3], [null, 3], [2, null], ["", 3], [2, " "], [true, 3], [[], 3], [{}, 3]]) {
    const f = fixture();
    f.config.fuelColumnDamageHeatMultiplier = pair[0];
    f.config.fuelColumnDamageBurnMultiplier = pair[1];
    assert.equal(validateConfig(f.config).ok, false, String(pair));
    assert.throws(() => settle(f), RangeError);
    assert.throws(() => createInitialSnapshot(geometry, f.layout, f.depths, f.config), RangeError);
    assert.equal(parseSceneText(JSON.stringify({ config: f.config })).ok, false);
    assert.throws(() => buildScene({ ...f, controlRodDepths: f.depths }), RangeError);
    assert.throws(() => buildCsv([], f.snapshot, null, RULE_VERSION, f.config), RangeError);
  }
});

test("旧场景缺项分别补默认，再检查联合不变量", () => {
  for (const [config, expected] of [[{}, [2, 3]], [{ fuelColumnDamageHeatMultiplier: 1.5 }, [1.5, 3]],
    [{ fuelColumnDamageBurnMultiplier: 5 }, [2, 5]]]) {
    const result = parseSceneText(JSON.stringify({ config }));
    assert.equal(result.ok, true);
    assert.deepEqual([result.scene.config.fuelColumnDamageHeatMultiplier, result.scene.config.fuelColumnDamageBurnMultiplier], expected);
  }
  assert.equal(parseSceneText('{"config":{"fuelColumnDamageHeatMultiplier":3}}').ok, false);
  assert.equal(parseSceneText('{"config":{"fuelColumnDamageBurnMultiplier":2}}').ok, false);
  const f = fixture();
  delete f.config.fuelColumnDamageHeatMultiplier;
  delete f.config.fuelColumnDamageBurnMultiplier;
  assert.equal(settle(f).columns["0,0"].damageBurnMultiplier, 2);
});

test("完整度有限性拒绝、区间外钳制、完整插棒与耗尽边界", () => {
  for (const invalid of [NaN, Infinity, -Infinity]) assert.throws(() => settle(fixture(invalid)), RangeError);
  assert.equal(settle(fixture(-1)).columns["0,0"].damageBurnMultiplier, 3);
  assert.equal(settle(fixture(2)).columns["0,0"].damageHeatMultiplier, 1);
  const f = fixture(0, [3, 5]);
  f.layout[0][1] = "control_rod";
  f.depths = createDepthMatrix(geometry, f.layout);
  f.snapshot = createInitialSnapshot(geometry, f.layout, f.depths, f.config);
  f.snapshot.columns[0].integrity = 0;
  const powers = [];
  for (const depth of [0, 0.5, 1]) {
    f.snapshot.columns[1].depth = depth;
    const result = settle(f).columns["0,0"];
    powers.push(result.generatedHeat);
    near(result.plannedBurn, 3 / 216000 * 5 * (1 - depth), 1e-15);
  }
  assert.deepEqual(powers, [9, 4.5, 0]);
  f.snapshot.columns[1].depth = 0;
  f.snapshot.columns[0].fuelRemaining = 0;
  f.snapshot.columns[0].cachedHeat = 4;
  const result = settle(f);
  assert.equal(result.summary.generatedHeat, 0);
  assert.equal(result.summary.plannedBurn, 0);
  assert.equal(result.columns["1,0"].propagationHeatReceived, 0);
  assert.equal(result.summary.removedHeat, 4);
});

test("超频独立叠乘，热量截断不返还燃耗，缓存余热不再次放大", () => {
  const f = fixture(0.5, [3, 5]);
  f.layout[0][1] = "fuel";
  f.snapshot = createInitialSnapshot(geometry, f.layout, f.depths, f.config);
  const healthy = settle(f);
  f.snapshot.columns.filter((column) => column.type === "fuel").forEach((column) => { column.integrity = 0.5; });
  const damaged = settle(f);
  near(damaged.summary.generatedHeat / healthy.summary.generatedHeat, 2);
  near(damaged.summary.plannedBurn / healthy.summary.plannedBurn, 3);
  f.config.totalHeatMultiplierCap = 1;
  const capped = settle(f);
  assert.equal(capped.summary.generatedHeat, 6);
  assert.equal(capped.summary.plannedBurn, damaged.summary.plannedBurn);
  const cached = fixture();
  cached.snapshot.columns[0].cachedHeat = 4;
  assert.equal(settle(cached).summary.availableHeat, 8.5);
});

test("新燃耗逐 tick 累计并在最后一份燃料耗尽时只扣剩余量", () => {
  const f = fixture(0.5, [1.5, 2.5]);
  const burn = 3 * baseBurnPerFuel(f.config) * 1.75;
  const run = runTicks(f.snapshot, geometry, f.layout, f.config, { running: true }, 1000);
  near(3 - run.snapshot.columns[0].fuelRemaining, burn * 1000, 1e-11);
  near(run.results.reduce((sum, result) => sum + result.summary.fuelConsumed, 0), burn * 1000, 1e-12);
  f.snapshot.columns[0].fuelRemaining = burn * 2.5;
  const exhausted = runTicks(f.snapshot, geometry, f.layout, f.config, { running: true }, 4);
  near(exhausted.results[2].summary.fuelConsumed, burn * 0.5, 1e-15);
  assert.equal(exhausted.snapshot.columns[0].fuelRemaining, 0);
  assert.equal(exhausted.results[3].summary.generatedHeat, 0);
  assert.equal(exhausted.results[3].summary.fuelConsumed, 0);
});

test("合法极大终点的列热量、列燃耗及全堆求和溢出均原子停止", () => {
  const cases = [
    { heat: Number.MAX_VALUE / 2, burn: Number.MAX_VALUE, expected: "列 [0,0] 新生裂变热" },
    { heat: 2, burn: Number.MAX_VALUE, capacity: 1e6, expected: "列 [0,0] 计划燃耗" },
    { heat: 4e307, burn: 5e307, second: true, expected: "全堆新生裂变热总和" },
    { heat: 2, burn: Number.MAX_VALUE, capacity: 40000, second: true, expected: "全堆计划燃耗总和" },
  ];
  for (const entry of cases) {
    const f = fixture(0, [entry.heat, entry.burn]);
    if (entry.capacity) f.config.fuelCapacityPerBlock = entry.capacity;
    if (entry.second) f.layout[2][2] = "fuel";
    f.snapshot = createInitialSnapshot(geometry, f.layout, f.depths, f.config);
    f.snapshot.columns.filter((column) => column.type === "fuel").forEach((column) => { column.integrity = 0; });
    assert.equal(validateConfig(f.config).ok, true);
    const original = structuredClone(f.snapshot);
    const result = settle(f);
    assert.equal(result.converged, false);
    assert.equal(result.runtime.running, false);
    assert.ok(result.warnings[0].includes(entry.expected), result.warnings[0]);
    assert.equal(result.nextSnapshot.tick, original.tick);
    assert.equal(result.nextSnapshot.coldCoolantMb, original.coldCoolantMb);
    assert.equal(result.nextSnapshot.hotCoolantMb, original.hotCoolantMb);
    assert.deepEqual(result.nextSnapshot.columns, original.columns);
    assert.deepEqual(f.snapshot, original);
  }
});
