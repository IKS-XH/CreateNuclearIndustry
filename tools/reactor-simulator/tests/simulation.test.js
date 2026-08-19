import test from "node:test";
import assert from "node:assert/strict";
import {
  DEFAULT_CONFIG,
  DEFAULT_GEOMETRY,
  LIMITS,
  baseBurnPerFuel,
  cloneConfig,
  validateConfig,
} from "../src/config.js";
import {
  coolantTotalCapacityMb,
  createDepthMatrix,
  createPreset,
  getNeighbors,
  validateGeometry,
  validateLayout,
} from "../src/layout.js";
import {
  createInitialSnapshot,
  repairColumn,
  setControlDepth,
  stepSnapshot,
} from "../src/simulation.js";

const G = DEFAULT_GEOMETRY;

function scene(layout, config = DEFAULT_CONFIG, depths = createDepthMatrix(G, layout)) {
  return createInitialSnapshot(G, layout, depths, config);
}

function tick(layout, config = DEFAULT_CONFIG, snapshot = scene(layout), runtime = { running: true, scram: false }) {
  return stepSnapshot(snapshot, G, layout, config, runtime);
}

test("geometry and layout validation enforce the UI contract", () => {
  assert.equal(validateGeometry({ length: 3, width: 3, height: 3 }).ok, true);
  assert.equal(validateGeometry({ length: 2, width: 3, height: 3 }).ok, false);
  assert.equal(validateGeometry({ length: 31, width: 31, height: 31 }).ok, true);
  assert.equal(validateGeometry({ length: 31, width: 31, height: 32 }).ok, false);
  assert.equal(validateLayout([["fuel"]], { length: 3, width: 3, height: 3 }).ok, true);
  assert.equal(validateLayout([["lava"]], { length: 3, width: 3, height: 3 }).ok, false);
  assert.equal(LIMITS.maxFuelColumns, 841);
});

test("neighbors are four-way only and empty columns remain legal", () => {
  const layout = createPreset(G, "empty");
  layout[1][1] = "fuel";
  layout[0][0] = "fuel";
  const neighbors = getNeighbors(layout, 1, 1);
  assert.deepEqual(neighbors.map((neighbor) => [neighbor.x, neighbor.z]), [[1, 0], [1, 2], [0, 1], [2, 1]]);
  assert.equal(neighbors.some((neighbor) => neighbor.x === 0 && neighbor.z === 0), false);
  const result = tick(layout);
  assert.equal(result.columns["1,1"].overclocked, false, "diagonal fuel must not create a feedback neighbor");
  assert.equal(result.summary.generatedHeat, 6, "the two isolated fuel columns still produce their height-scaled heat");
  const noFuel = tick(createPreset(G, "empty"));
  assert.equal(noFuel.summary.generatedHeat, 0);
  assert.equal(noFuel.summary.fuelConsumed, 0);
});

test("isolated fuel is monotonic with control depth", () => {
  const layout = createPreset(G, "empty");
  layout[1][1] = "fuel";
  layout[1][0] = "control_rod";
  let previous = Infinity;
  for (const depth of [0, .25, .5, .75, 1]) {
    const depths = createDepthMatrix(G, layout);
    depths[1][0] = depth;
    const result = tick(layout, DEFAULT_CONFIG, scene(layout, DEFAULT_CONFIG, depths));
    const heat = result.columns["1,1"].generatedHeat;
    assert.ok(heat <= previous + 1e-12);
    previous = heat;
  }
});

test("adjacent fuel forms deterministic convergent feedback while empty columns isolate it", () => {
  const adjacent = createPreset(G, "empty");
  adjacent[1][1] = "fuel";
  adjacent[1][2] = "fuel";
  const separated = createPreset(G, "empty");
  separated[1][0] = "fuel";
  separated[1][2] = "fuel";
  const first = tick(adjacent);
  const second = tick(adjacent);
  const isolated = tick(separated);
  assert.equal(first.converged, true);
  assert.ok(first.summary.generatedHeat > isolated.summary.generatedHeat);
  assert.deepEqual(first.nextSnapshot, second.nextSnapshot);
  assert.ok(first.summary.iterations <= LIMITS.maxFeedbackIterations);
});

test("cooling ledger is conserved and sufficient cooling prevents damage", () => {
  const layout = createPreset(G, "empty");
  layout[1][1] = "fuel";
  const hotConfig = cloneConfig({ ...DEFAULT_CONFIG, baseHeatPerFuel: 500, actualColdIn: 200, actualHotOut: 200 });
  const cooled = tick(layout, hotConfig);
  assert.equal(cooled.summary.convertedCoolant, 200);
  assert.equal(cooled.summary.coldInAccepted, 0, "the default internal cold tank starts full");
  assert.equal(cooled.summary.hotOutActual, 200);
  assert.equal(cooled.summary.coolantLedgerError, 0);
  assert.equal(cooled.summary.generatedHeat, 1500);
  assert.equal(cooled.summary.maxNetHeatLoad, 1300);
  assert.ok(cooled.nextSnapshot.columns.find((column) => column.key === "1,1").integrity < 1);

  const safeConfig = cloneConfig({ ...DEFAULT_CONFIG, baseHeatPerFuel: 1, actualColdIn: 200, actualHotOut: 200 });
  const safe = tick(layout, safeConfig);
  assert.equal(safe.summary.maxNetHeatLoad, 0);
  assert.equal(safe.nextSnapshot.columns.find((column) => column.key === "1,1").integrity, 1);
});

test("internal coolant capacity is derived from non-fuel columns and never exceeded", () => {
  const layout = createPreset(G, "mixed");
  assert.equal(coolantTotalCapacityMb(G, layout), (4 + 1) * 3 * 1000);
  const snapshot = scene(layout);
  assert.equal(snapshot.coolantTotalCapacityMb, 15000);
  assert.equal(snapshot.coldCoolantMb + snapshot.hotCoolantMb, snapshot.coolantTotalCapacityMb);

  const highHeatLayout = createPreset(G, "empty");
  highHeatLayout[1][1] = "fuel";
  const highHeat = {
    ...DEFAULT_CONFIG,
    baseHeatPerFuel: 1000,
    actualColdIn: 200,
    actualHotOut: 0,
    fuelColumnDamageRate: 0,
  };
  let current = scene(highHeatLayout, highHeat);
  let maximumInventory = 0;
  current = { ...current, coolantTotalCapacityMb: 24000, coldCoolantMb: 24000, hotCoolantMb: 0 };
  for (let index = 0; index < 120; index += 1) {
    const result = tick(highHeatLayout, highHeat, current);
    current = result.nextSnapshot;
    const total = current.coldCoolantMb + current.hotCoolantMb;
    maximumInventory = Math.max(maximumInventory, total);
    assert.ok(total <= current.coolantTotalCapacityMb + 1e-9);
    assert.equal(result.summary.coolantLedgerError, 0);
  }
  assert.equal(maximumInventory, current.coolantTotalCapacityMb);
  assert.equal(current.hotCoolantMb, current.coolantTotalCapacityMb);
  assert.equal(current.coldCoolantMb, 0);
});

test("failed fuel propagates north/south/east/west, never diagonally, and control rods terminate propagation", () => {
  const layout = createPreset(G, "empty");
  layout[1][1] = "fuel";
  layout[0][1] = "fuel";
  layout[1][2] = "fuel";
  layout[2][2] = "fuel";
  layout[2][1] = "control_rod";
  let snapshot = scene(layout);
  snapshot = { ...snapshot, coldCoolantMb: 0, hotCoolantMb: 0 };
  snapshot.columns = snapshot.columns.map((column) => column.key === "1,1"
    ? { ...column, integrity: 0, cachedHeat: 4 }
    : column);
  const result = tick(layout, { ...DEFAULT_CONFIG, actualColdIn: 0, actualHotOut: 0 }, snapshot);
  assert.ok(result.columns["1,0"].propagationHeatReceived > 0);
  assert.ok(result.columns["2,1"].propagationHeatReceived > 0);
  assert.equal(result.columns["2,2"].propagationHeatReceived, 0);
  const second = tick(layout, { ...DEFAULT_CONFIG, actualColdIn: 0, actualHotOut: 0 }, result.nextSnapshot);
  assert.equal(second.columns["2,2"].propagationHeatReceived, 0, "damaged control rods do not forward heat");
});

test("control rod jams at actual depth and stays jammed", () => {
  const layout = createPreset(G, "empty");
  layout[1][1] = "fuel";
  layout[1][2] = "control_rod";
  let snapshot = scene(layout);
  snapshot = { ...snapshot, coldCoolantMb: 0, hotCoolantMb: 0 };
  snapshot = setControlDepth(snapshot, "2,1", .35);
  snapshot.columns = snapshot.columns.map((column) => column.key === "1,1"
    ? { ...column, integrity: 0, cachedHeat: 100 }
    : column);
  const aggressive = cloneConfig({ ...DEFAULT_CONFIG, fuelColumnDamageRate: 1, actualColdIn: 0, actualHotOut: 0 });
  const result = tick(layout, aggressive, snapshot);
  const rod = result.nextSnapshot.columns.find((column) => column.key === "2,1");
  assert.equal(rod.controlRodIntegrity, 0);
  assert.equal(rod.jammed, true);
  assert.equal(rod.jammedDepth, .35);
  const changed = setControlDepth(result.nextSnapshot, "2,1", 0);
  assert.equal(changed.columns.find((column) => column.key === "2,1").depth, .35);
});

test("meltdown coverage uses 20 percent effective fuel and SCRAM plus cooling never rewinds progress", () => {
  const layout = createPreset(G, "empty");
  for (let x = 0; x < 5; x += 1) layout[1][x % 3] = "fuel";
  layout[1][0] = "fuel";
  layout[1][1] = "fuel";
  layout[1][2] = "fuel";
  const snapshot = { ...scene(layout), coldCoolantMb: 0, hotCoolantMb: 0 };
  const failed = snapshot.columns.map((column) => column.type === "fuel" && column.key === "0,1"
    ? { ...column, integrity: 0, cachedHeat: 4 }
    : column);
  const result = tick(layout, { ...DEFAULT_CONFIG, actualColdIn: 0, actualHotOut: 0 }, { ...snapshot, columns: failed });
  assert.equal(result.summary.meltdownDanger, true);
  const progress = result.nextSnapshot.meltdownProgress;
  const paused = tick(layout, { ...DEFAULT_CONFIG, actualColdIn: 200, actualHotOut: 200 }, result.nextSnapshot, { running: true, scram: true });
  assert.equal(paused.nextSnapshot.meltdownProgress, progress);
  const resumed = tick(layout, DEFAULT_CONFIG, paused.nextSnapshot, { running: true, scram: false });
  assert.ok(resumed.nextSnapshot.meltdownProgress >= progress);
});

test("height scales heat, fuel capacity, and repair amount while preserving three-hour anchor", () => {
  const low = { length: 3, width: 3, height: 3 };
  const high = { length: 3, width: 3, height: 5 };
  const lowLayout = [["fuel"]];
  const highLayout = [["fuel"]];
  const lowSnapshot = createInitialSnapshot(low, lowLayout, [[null]], DEFAULT_CONFIG);
  const highSnapshot = createInitialSnapshot(high, highLayout, [[null]], DEFAULT_CONFIG);
  const lowResult = stepSnapshot(lowSnapshot, low, lowLayout, { ...DEFAULT_CONFIG, actualColdIn: 1, actualHotOut: 1 }, { running: true });
  const highResult = stepSnapshot(highSnapshot, high, highLayout, { ...DEFAULT_CONFIG, actualColdIn: 5, actualHotOut: 5 }, { running: true });
  assert.equal(highResult.summary.generatedHeat / lowResult.summary.generatedHeat, 3);
  assert.equal(highResult.summary.plannedBurn / lowResult.summary.plannedBurn, 3);
  assert.equal(highSnapshot.columns[0].fuelCapacity / lowSnapshot.columns[0].fuelCapacity, 3);
  const repaired = repairColumn({ ...lowSnapshot, columns: [{ ...lowSnapshot.columns[0], integrity: .5 }] }, "0,0", low);
  assert.equal(repaired.columns[0].integrity, .75);
  assert.equal(baseBurnPerFuel(DEFAULT_CONFIG), 1 / (3 * 3600 * 20));
});

test("config validation rejects non-finite values and keeps prototype defaults explicit", () => {
  assert.equal(validateConfig(DEFAULT_CONFIG).ok, true);
  assert.equal(validateConfig({ ...DEFAULT_CONFIG, baseHeatPerFuel: Number.NaN }).ok, false);
  assert.equal(validateConfig({ ...DEFAULT_CONFIG, meltdownTriggerFraction: 1.1 }).ok, false);
  assert.equal(DEFAULT_CONFIG.baseHeatPerFuel, 1);
  assert.equal(DEFAULT_CONFIG.coolantMaxFlowPerPort, 100);
  assert.equal(DEFAULT_CONFIG.coolantTotalFlowCap, 200);
  assert.equal(DEFAULT_CONFIG.meltdownCountdownTicks, 900);
});
