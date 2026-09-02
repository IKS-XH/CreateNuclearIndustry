import test from "node:test";
import assert from "node:assert/strict";
import { readdir, readFile, mkdir, writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import { parseSceneText } from "../src/report.js";
import { createInitialSnapshot, runTicks } from "../src/simulation.js";

const scenarioDir = resolve(process.cwd(), "scenarios");
const reportDir = resolve(process.cwd(), "../../build/reports/reactor-simulator");

function applyFixtureSnapshot(fresh, raw) {
  if (!raw || !Array.isArray(raw.columns)) return fresh;
  const incoming = new Map(raw.columns.map((column) => [column.key, column]));
  return {
    tick: Number(raw.tick) || 0,
    meltdownProgress: Number(raw.meltdownProgress) || 0,
    meltdownTriggered: Boolean(raw.meltdownTriggered),
    meltdownMelted: Boolean(raw.meltdownMelted),
    coolantTotalCapacityMb: fresh.coolantTotalCapacityMb,
    coldCoolantMb: Number(raw.coldCoolantMb ?? 0),
    hotCoolantMb: Number(raw.hotCoolantMb ?? 0),
    simulationFailed: raw.simulationFailed ? String(raw.simulationFailed) : null,
    columns: fresh.columns.map((column) => {
      const source = incoming.get(column.key);
      if (!source || source.type !== column.type) return { ...column };
      return column.type === "fuel"
        ? { ...column, fuelRemaining: Number(source.fuelRemaining), integrity: Number(source.integrity), cachedHeat: Number(source.cachedHeat) }
        : column.type === "control_rod"
          ? { ...column, controlRodIntegrity: Number(source.controlRodIntegrity), depth: Number(source.depth), jammed: Boolean(source.jammed), jammedDepth: Number(source.jammedDepth), cachedHeat: Number(source.cachedHeat) }
          : { ...column };
    }),
  };
}

test("all replay scenarios run deterministically and write handoff evidence", async () => {
  const files = (await readdir(scenarioDir)).filter((file) => file.endsWith(".json")).sort();
  assert.equal(files.length, 8);
  const rows = [];
  const finalSnapshots = {};
  for (const file of files) {
    const parsed = parseSceneText(await readFile(resolve(scenarioDir, file), "utf8"));
    assert.equal(parsed.ok, true, `${file} must pass scene validation`);
    const scene = parsed.scene;
    const fresh = createInitialSnapshot(scene.geometry, scene.layout.matrix, scene.controlRodDepths, scene.config);
    const initial = applyFixtureSnapshot(fresh, scene.initialSnapshot);
    const replay = runTicks(initial, scene.geometry, scene.layout.matrix, scene.config, scene.initialState, scene.runTicks || 1);
    assert.ok(replay.results.every((result) => result.converged), `${file} feedback must converge`);
    const final = replay.results.at(-1)?.summary ?? {
      tick: replay.snapshot.tick,
      generatedHeat: 0,
      convertedCoolant: 0,
      totalResidualHeat: 0,
      minIntegrity: 1,
      meltdownProgress: replay.snapshot.meltdownProgress,
      ruleConverged: true,
    };
    rows.push({
      scenario: file.replace(/\.json$/, ""),
      ticks: replay.snapshot.tick,
      generatedHeatHuPerT: final.generatedHeat ?? 0,
      convertedCoolantMbPerT: final.convertedCoolant ?? 0,
      residualHeatHu: final.totalResidualHeat ?? 0,
      minimumIntegrity: final.minIntegrity ?? 1,
      meltdownProgress: final.meltdownProgress ?? 0,
      meltdownTriggered: replay.snapshot.meltdownTriggered,
      ruleConverged: final.ruleConverged !== false,
      coolantTotalCapacityMb: final.coolantTotalCapacityMb ?? replay.snapshot.coolantTotalCapacityMb ?? 0,
      coldCoolantMb: final.coldCoolantMb ?? replay.snapshot.coldCoolantMb ?? 0,
      hotCoolantMb: final.hotCoolantMb ?? replay.snapshot.hotCoolantMb ?? 0,
      totalCoolantMb: final.totalCoolantMb ?? 0,
      coolantCapacityHeadroomMb: final.coolantCapacityHeadroomMb ?? 0,
      coolantLedgerError: final.coolantLedgerError ?? 0,
    });
    finalSnapshots[file] = replay.snapshot;
  }
  await mkdir(reportDir, { recursive: true });
  const header = "scenario,ticks,generated_heat_HU_per_t,converted_coolant_mB_per_t,residual_heat_HU,minimum_integrity,meltdown_progress,meltdown_triggered,rule_converged,coolant_total_capacity_mB,cold_inventory_mB,hot_inventory_mB,total_coolant_mB,coolant_headroom_mB,coolant_ledger_error_mB";
  const csv = [header, ...rows.map((row) => [row.scenario, row.ticks, row.generatedHeatHuPerT, row.convertedCoolantMbPerT, row.residualHeatHu, row.minimumIntegrity, row.meltdownProgress, row.meltdownTriggered, row.ruleConverged, row.coolantTotalCapacityMb, row.coldCoolantMb, row.hotCoolantMb, row.totalCoolantMb, row.coolantCapacityHeadroomMb, row.coolantLedgerError].join(","))].join("\n") + "\n";
  await writeFile(resolve(reportDir, "scenario-results.csv"), csv, "utf8");
  await writeFile(resolve(reportDir, "scenario-final-snapshots.json"), JSON.stringify(finalSnapshots, null, 2), "utf8");
  assert.ok(rows.some((row) => row.scenario === "cold-end-limited" && row.convertedCoolantMbPerT === 0));
  assert.ok(rows.some((row) => row.scenario === "hot-end-limited" && row.convertedCoolantMbPerT === 0));
  assert.ok(rows.some((row) => row.scenario === "propagation-paused-meltdown" && row.meltdownTriggered));
  const fcfRow = rows.find((row) => row.scenario === "p1-control-05-fcf");
  assert.ok(fcfRow, "the P1-CONTROL-05 F-C-F scenario must be replayed");
  assert.equal(fcfRow.generatedHeatHuPerT, 0,
    "fully inserted F-C-F rods must suppress all new fission heat");
  const fcfFuelColumns = finalSnapshots["p1-control-05-fcf.json"].columns
    .filter((column) => column.type === "fuel");
  assert.equal(fcfFuelColumns.length, 6);
  assert.ok(fcfFuelColumns.every((column) => column.fuelRemaining === column.fuelCapacity),
    "fully inserted F-C-F rods must not consume fuel");
});
