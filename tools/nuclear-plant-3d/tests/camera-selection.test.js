import assert from "node:assert/strict";
import test from "node:test";
import { calculateSceneOverview } from "../src/scene/camera-controller.js";
import { findDeviceIdFromObject, getNormalizedPointer } from "../src/scene/selection.js";
import { getDeviceLabel, formatFlowDescription } from "../src/ui/labels.js";

const sceneData = {
  devices: [
    { id: "experimental_reactor", kind: "experimental_reactor", position: [6, 0, 0], dimensions: [5, 5, 5], ports: [] },
    { id: "supercritical_turbine", kind: "supercritical_turbine", position: [29, 1, 0], dimensions: [8, 3, 3], ports: [] },
  ],
};

test("场景总览给出斜俯视相机且覆盖最长设备布局", () => {
  const overview = calculateSceneOverview(sceneData, { aspectRatio: 2 });
  assert.deepEqual(overview.min, [6, 0, 0]);
  assert.deepEqual(overview.max, [37, 5, 5]);
  assert.ok(overview.cameraPosition[1] > overview.target[1]);
  assert.ok(overview.cameraPosition[2] > overview.target[2]);
  assert.ok(overview.distance > 14);
});

test("画布坐标转换和设备父级查找保持稳定", () => {
  assert.deepEqual(getNormalizedPointer(50, 25, { left: 0, top: 0, width: 100, height: 50 }), { x: 0, y: 0 });
  const parent = { userData: { deviceId: "experimental_reactor" }, parent: null };
  const mesh = { userData: {}, parent };
  assert.equal(findDeviceIdFromObject(mesh), "experimental_reactor");
});

test("设备详情使用本地化名称和合同端口工质", () => {
  const device = {
    id: "experimental_reactor",
    kind: "experimental_reactor",
    ports: [
      { id: "cold", direction: "inlet", medium: "cold_compound_coolant", required: true },
      { id: "hot", direction: "outlet", medium: "hot_compound_coolant", required: true },
    ],
  };
  assert.equal(getDeviceLabel(device), "实验反应堆");
  assert.match(formatFlowDescription(device), /入口 冷复合冷却剂/);
  assert.match(formatFlowDescription(device), /出口 热复合冷却剂/);
});
