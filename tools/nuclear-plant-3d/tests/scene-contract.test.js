import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";
import { assertSceneContract } from "../src/scene/scene-contract.js";
import { DEFAULT_PLANT_STATE } from "../src/state/plant-state.js";
import { PRESET_IDS } from "../src/state/presets.js";

const testRoot = dirname(fileURLToPath(import.meta.url));
const toolRoot = dirname(testRoot);
const scene = JSON.parse(await readFile(join(toolRoot, "scenes/standard-running-plant.json"), "utf8"));

function cloneScene() {
  return JSON.parse(JSON.stringify(scene));
}

test("默认场景通过 schema、5×5×5 反应堆和模式互斥合同", () => {
  assert.equal(assertSceneContract(scene), true);
  assert.equal(scene.schemaVersion, "1.0.0");
  assert.deepEqual(scene.devices.find((device) => device.kind === "experimental_reactor").dimensions, [5, 5, 5]);
  assert.deepEqual(scene.devices.filter((device) => device.kind === "nuclear_heat_exchanger").map((device) => device.mode).sort(), ["condensing", "nuclear_heat"]);
  assert.equal(scene.devices.find((device) => device.kind === "high_pressure_boiler").sizeStatus, "visual_proposal");
  assert.equal(scene.devices.find((device) => device.kind === "supercritical_turbine").sizeStatus, "visual_proposal");
  assert.equal(scene.visualPolicy.createOutput, "rotary_stress_only");
  assert.deepEqual(scene.defaultState, DEFAULT_PLANT_STATE);
  assert.deepEqual(scene.presetIds, PRESET_IDS);
});

test("默认场景设备 ID 唯一且每个必需端口都连接到闭合主回路", () => {
  const ids = scene.devices.map((device) => device.id);
  assert.equal(new Set(ids).size, ids.length);
  const connectionIds = scene.connections.map((connection) => connection.id);
  assert.equal(new Set(connectionIds).size, connectionIds.length);
  for (const device of scene.devices) {
    for (const port of device.ports.filter((port) => port.required)) {
      const usageCount = scene.connections.filter((connection) => (
        `${connection.from.deviceId}.${connection.from.portId}` === `${device.id}.${port.id}`
        || `${connection.to.deviceId}.${connection.to.portId}` === `${device.id}.${port.id}`
      )).length;
      assert.equal(usageCount, 1, `${device.id}.${port.id} 应恰好连接一次`);
    }
  }
});

test("场景合同拒绝重复设备、断路、错误端点和混合换热模式", () => {
  const duplicateDevice = cloneScene();
  duplicateDevice.devices[1].id = duplicateDevice.devices[0].id;
  assert.throws(() => assertSceneContract(duplicateDevice), /设备 ID 重复/);

  const brokenConnection = cloneScene();
  brokenConnection.connections = brokenConnection.connections.slice(1);
  assert.throws(() => assertSceneContract(brokenConnection), /必需端口未连接|断开的设备/);

  const wrongEndpoint = cloneScene();
  wrongEndpoint.connections[0].to.portId = "missing_port";
  assert.throws(() => assertSceneContract(wrongEndpoint), /不存在的端口/);

  const mixedMode = cloneScene();
  mixedMode.devices.find((device) => device.id === "condenser_heat_exchanger").mode = "nuclear_heat+condensing";
  assert.throws(() => assertSceneContract(mixedMode), /必须且只能选择一个换热模式/);
});
