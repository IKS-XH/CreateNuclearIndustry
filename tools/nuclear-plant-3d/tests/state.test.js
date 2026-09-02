import assert from "node:assert/strict";
import test from "node:test";
import {
  DEFAULT_PLANT_STATE,
  NORMALIZED_STATE_FIELDS,
  assertPlantState,
  createPlantState,
} from "../src/state/plant-state.js";

test("默认状态包含完整字段且所有 01 字段在 [0,1]", () => {
  assert.equal(assertPlantState(DEFAULT_PLANT_STATE), true);
  for (const field of NORMALIZED_STATE_FIELDS) {
    assert.ok(DEFAULT_PLANT_STATE[field] >= 0 && DEFAULT_PLANT_STATE[field] <= 1, field);
  }
  assert.equal(DEFAULT_PLANT_STATE.alarmState, "normal");
  assert.equal(DEFAULT_PLANT_STATE.scramActive, false);
});

test("状态覆盖创建独立快照并拒绝非法输入", () => {
  const state = createPlantState({ reactorPower01: 0.5, alarmState: "warning" });
  assert.equal(state.reactorPower01, 0.5);
  assert.equal(state.alarmState, "warning");
  assert.throws(() => createPlantState({ boilerPressure01: 1.01 }), /boilerPressure01/);
  assert.throws(() => createPlantState({ turbineRpm01: -0.01 }), /turbineRpm01/);
  assert.throws(() => createPlantState({ alarmState: "unknown" }), /alarmState/);
  assert.throws(() => createPlantState({ scramActive: 1 }), /scramActive/);
  assert.throws(() => createPlantState({ unknownField: 0 }), /未知字段/);
  assert.throws(() => createPlantState({ timeScale: 5 }), /timeScale/);
});

test("状态合同拒绝缺字段、额外字段和非有限数值", () => {
  const missing = { ...DEFAULT_PLANT_STATE };
  delete missing.shaftLoad01;
  assert.throws(() => assertPlantState(missing), /字段必须/);

  const extra = { ...DEFAULT_PLANT_STATE, extra: 0 };
  assert.throws(() => assertPlantState(extra), /字段必须/);

  assert.throws(() => assertPlantState({ ...DEFAULT_PLANT_STATE, reactorPower01: Number.NaN }), /有限数值/);
});
