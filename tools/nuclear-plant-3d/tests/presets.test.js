import assert from "node:assert/strict";
import test from "node:test";
import { PRESET_IDS, getPreset, samplePreset } from "../src/state/presets.js";

const EXPECTED_PRESET_IDS = [
  "startup",
  "steady",
  "scram",
  "loss_of_coolant",
  "feedwater_loss",
  "steam_blockage",
  "turbine_trip",
  "load_rejection",
];

test("八个预设 ID 完整且每条时间线确定性有效", () => {
  assert.deepEqual(PRESET_IDS, EXPECTED_PRESET_IDS);
  for (const presetId of PRESET_IDS) {
    const timeline = getPreset(presetId);
    assert.equal(timeline.keyframes[0].atSeconds, 0);
    assert.ok(timeline.keyframes.length >= 2);
    const first = JSON.stringify(samplePreset(presetId, 17.25));
    const second = JSON.stringify(samplePreset(presetId, 17.25));
    assert.equal(first, second, presetId);
    assert.deepEqual(samplePreset(presetId, 0), timeline.keyframes[0].state);
  }
  assert.throws(() => getPreset("not_a_preset"), /未知机组预设/);
});

test("startup 按泵、反应堆、锅炉、汽轮机和主轴顺序推进", () => {
  const atPumpStart = samplePreset("startup", 8);
  const atReactorRise = samplePreset("startup", 20);
  const atSteam = samplePreset("startup", 40);
  const atLoad = samplePreset("startup", 60);
  assert.ok(atPumpStart.primaryCoolantFlow01 > atPumpStart.reactorPower01);
  assert.ok(atReactorRise.reactorPower01 > 0 && atReactorRise.heatExchangerLoad01 > 0);
  assert.ok(atSteam.boilerPressure01 > 0 && atSteam.steamFlow01 > 0 && atSteam.turbineRpm01 > 0);
  assert.ok(atLoad.shaftLoad01 > 0 && atLoad.turbineRpm01 > atSteam.turbineRpm01);
});

test("故障预设保留各自因果链", () => {
  const steady = samplePreset("steady", 10);
  const scram = samplePreset("scram", 35);
  const lossOfCoolant = samplePreset("loss_of_coolant", 30);
  const feedwaterLoss = samplePreset("feedwater_loss", 35);
  const steamBlockage = samplePreset("steam_blockage", 30);
  const turbineTrip = samplePreset("turbine_trip", 35);
  const loadRejection = samplePreset("load_rejection", 12);

  assert.equal(steady.alarmState, "normal");
  assert.equal(scram.scramActive, true);
  assert.ok(scram.primaryCoolantFlow01 > 0.8 && scram.reactorPower01 < steady.reactorPower01);
  assert.ok(lossOfCoolant.primaryCoolantFlow01 < steady.primaryCoolantFlow01 && lossOfCoolant.heatExchangerLoad01 < steady.heatExchangerLoad01);
  assert.ok(feedwaterLoss.boilerWaterLevel01 < steady.boilerWaterLevel01 && feedwaterLoss.alarmState === "critical");
  assert.ok(steamBlockage.boilerPressure01 > steady.boilerPressure01 && steamBlockage.steamFlow01 < steady.steamFlow01);
  assert.ok(turbineTrip.steamFlow01 === 0 && turbineTrip.turbineRpm01 < steady.turbineRpm01);
  assert.ok(loadRejection.shaftLoad01 < steady.shaftLoad01 && loadRejection.turbineRpm01 > steady.turbineRpm01);
});

test("时间线采样在边界外钳制且拒绝非有限时间", () => {
  const timeline = getPreset("startup");
  assert.deepEqual(samplePreset("startup", -1), timeline.keyframes[0].state);
  assert.deepEqual(samplePreset("startup", 999), timeline.keyframes.at(-1).state);
  assert.deepEqual(samplePreset("startup", -0.1), timeline.keyframes[0].state);
  assert.throws(() => samplePreset("startup", Number.NaN), /有限/);
});
