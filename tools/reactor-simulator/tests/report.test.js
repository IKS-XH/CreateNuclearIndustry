import test from "node:test";
import assert from "node:assert/strict";
import { DEFAULT_CONFIG, DEFAULT_GEOMETRY, RULE_VERSION } from "../src/config.js";
import { createDepthMatrix, createPreset } from "../src/layout.js";
import { createInitialSnapshot } from "../src/simulation.js";
import { buildCsv, buildScene, parseSceneText, sceneJson } from "../src/report.js";

test("scene JSON round-trips snapshots and ignores unknown fields", () => {
  const layout = createPreset(DEFAULT_GEOMETRY, "full_fuel");
  const depths = createDepthMatrix(DEFAULT_GEOMETRY, layout);
  const snapshot = createInitialSnapshot(DEFAULT_GEOMETRY, layout, depths, DEFAULT_CONFIG);
  const scene = buildScene({
    geometry: DEFAULT_GEOMETRY,
    layout,
    controlRodDepths: depths,
    config: DEFAULT_CONFIG,
    initialState: { running: false, scram: false },
    initialSnapshot: snapshot,
    finalSnapshot: snapshot,
    history: [{ summary: { tick: 0, gameSeconds: 0, generatedHeat: 0, ruleConverged: true }, columns: {} }],
    summary: { tick: 0 },
  });
  scene.futureUnknownField = { should: "be ignored" };
  const parsed = parseSceneText(sceneJson(scene));
  assert.equal(parsed.ok, true);
  assert.equal(parsed.scene.geometry.length, 5);
  assert.equal(parsed.scene.layout.matrix[0][0], "fuel");
  assert.equal(parsed.scene.finalSnapshot.columns.length, snapshot.columns.length);
  assert.equal(parsed.scene.futureUnknownField, undefined);
});

test("legacy aggregate coolant flow field is ignored on import and omitted on export", () => {
  const layout = createPreset(DEFAULT_GEOMETRY, "empty");
  const scene = buildScene({
    geometry: DEFAULT_GEOMETRY,
    layout,
    controlRodDepths: createDepthMatrix(DEFAULT_GEOMETRY, layout),
    config: { ...DEFAULT_CONFIG, coolantTotalFlowCap: 200 },
  });
  assert.equal(Object.hasOwn(scene.config, "coolantTotalFlowCap"), false);
  const imported = parseSceneText(JSON.stringify({ ...scene, config: { ...scene.config, coolantTotalFlowCap: 999 } }));
  assert.equal(imported.ok, true);
  assert.equal(Object.hasOwn(imported.scene.config, "coolantTotalFlowCap"), false);
});

test("invalid scene does not pass schema/version validation", () => {
  assert.equal(parseSceneText(JSON.stringify({ schema: "other", schemaVersion: 1 })).ok, false);
  assert.equal(parseSceneText(JSON.stringify({ schemaVersion: 999 })).ok, false);
  assert.equal(parseSceneText("not json").ok, false);
});

test("CSV contains unit-bearing tick overview rows and final per-column rows", () => {
  const layout = createPreset(DEFAULT_GEOMETRY, "empty");
  layout[1][1] = "fuel";
  const snapshot = createInitialSnapshot(DEFAULT_GEOMETRY, layout, createDepthMatrix(DEFAULT_GEOMETRY, layout), DEFAULT_CONFIG);
  const csv = buildCsv([
    { summary: { tick: 1, gameSeconds: .05, generatedHeat: 3, fuelConsumed: 1e-7, convertedCoolant: 3, maxNetHeatLoad: 0, minIntegrity: 1, ruleConverged: true }, columns: {} },
  ], snapshot, { tick: 1 }, RULE_VERSION);
  assert.match(csv, /total_heat_HU_per_t/);
  assert.match(csv, /tick,1,0\.05/);
  assert.match(csv, /0\.0000001/);
  assert.doesNotMatch(csv, /1e-7/i);
  assert.ok(csv.includes(`column,0,0,,,,,,,,,,,,,,${RULE_VERSION},true`));
});
