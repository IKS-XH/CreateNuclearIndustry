import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";
import { assertPlantState, DEFAULT_PLANT_STATE } from "../src/state/plant-state.js";
import { REACTOR_SIZE, advanceControlRodDepth, assertReactorDevice, calculateReactorVisualState, createReactorModel, updateReactorModel } from "../src/equipment/reactor.js";

const toolRoot = dirname(dirname(fileURLToPath(import.meta.url)));

function reactorDevice(dimensions = REACTOR_SIZE) {
  return {
    id: "experimental_reactor",
    kind: "experimental_reactor",
    position: [6, 0, 0],
    dimensions: [...dimensions],
    ports: [
      { id: "cold_coolant_inlet", localPosition: [0, 1, 2.5] },
      { id: "hot_coolant_outlet", localPosition: [5, 4, 2.5] },
      { id: "instrument_port", localPosition: [2.5, 5, 0] },
      { id: "refuel_port", localPosition: [2.5, 5, 5] },
    ],
  };
}

test("实验反应堆模型合同严格保持 5×5×5 和四个可识别端口", () => {
  assert.deepEqual(REACTOR_SIZE, [5, 5, 5]);
  assert.equal(assertReactorDevice(reactorDevice()), true);
  assert.throws(() => assertReactorDevice(reactorDevice([4, 5, 5])), /5×5×5/);
  assert.throws(() => assertReactorDevice({ ...reactorDevice(), ports: [] }), /缺少端口/);
});

test("正常、过热和 SCRAM 视觉状态有明确差异", () => {
  assertPlantState(DEFAULT_PLANT_STATE);
  const stable = calculateReactorVisualState(DEFAULT_PLANT_STATE);
  const overheat = calculateReactorVisualState({ ...DEFAULT_PLANT_STATE, alarmState: "critical" });
  const scram = calculateReactorVisualState({ ...DEFAULT_PLANT_STATE, scramActive: true, controlRodDepth01: 1 });
  assert.equal(stable.mode, "stable");
  assert.equal(overheat.mode, "overheat");
  assert.equal(scram.mode, "scram");
  assert.ok(overheat.pulseRate > stable.pulseRate);
  assert.equal(scram.controlRodTarget01, 1);
  assert.equal(stable.flowActive, true);
});

test("控制棒随深度移动且 SCRAM 追踪更快，暂停帧不移动", () => {
  const normalStep = advanceControlRodDepth(0.18, 1, 0.1, false);
  const scramStep = advanceControlRodDepth(0.18, 1, 0.1, true);
  assert.ok(scramStep > normalStep);
  assert.equal(advanceControlRodDepth(0.18, 1, 0, true), 0.18);
  assert.equal(advanceControlRodDepth(1, 0, 10, false), 0);
});

test("低流量状态会停止反应堆内部流动表现", () => {
  const stopped = calculateReactorVisualState({ ...DEFAULT_PLANT_STATE, primaryCoolantFlow01: 0 });
  assert.equal(stopped.flowActive, false);
  assert.equal(stopped.coolantFlow01, 0);
});

test("反应堆模型装配包含 5×5×5 外壳、核心、控制棒和可见流动粒子", () => {
  const model = createReactorModel(reactorDevice());
  assert.deepEqual([
    model.shell.geometry.parameters.width,
    model.shell.geometry.parameters.height,
    model.shell.geometry.parameters.depth,
  ], [5, 5, 5]);
  assert.ok(model.pickables.length >= 3);
  updateReactorModel(model, DEFAULT_PLANT_STATE, 1, 0.1);
  assert.equal(model.group.userData.visualMode, "stable");
  assert.ok(model.flowParticles.some((particle) => particle.visible));
  assert.ok(model.rodRoot.position.y < 0);
  updateReactorModel(model, { ...DEFAULT_PLANT_STATE, primaryCoolantFlow01: 0 }, 1, 0);
  assert.equal(model.flowParticles.some((particle) => particle.visible), false);
});

test("反应堆实现不引入独立 SCRAM 联锁器或工业仪表", async () => {
  const source = await readFile(join(toolRoot, "src/equipment/reactor.js"), "utf8");
  assert.doesNotMatch(source, /scram_interlock|独立.*联锁器|工业仪表/);
});
