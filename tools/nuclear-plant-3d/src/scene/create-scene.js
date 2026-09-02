import * as THREE from "../../vendor/three.module.js";
import { createReactorModel, updateReactorModel } from "../equipment/reactor.js";

const DEVICE_COLORS = Object.freeze({
  main_coolant_pump: 0x397f8a,
  feedwater_pump: 0x438b88,
  experimental_reactor: 0x2e7277,
  nuclear_heat_exchanger: 0x8d7046,
  high_pressure_boiler: 0x806344,
  supercritical_turbine: 0x677782,
  valve: 0x53656d,
  create_gearbox: 0x8c6c45,
  create_stress_load: 0xa67543,
});

const MEDIUM_COLORS = Object.freeze({
  cold_compound_coolant: 0x54c7d0,
  hot_compound_coolant: 0xf28b61,
  nuclear_heat: 0xf1d36b,
  water: 0x58b9b0,
  supercritical_steam: 0xffc267,
  steam: 0xd9e8ec,
  rotary_stress: 0xe49b5c,
});

function createDeviceMaterial(device) {
  return new THREE.MeshStandardMaterial({
    color: DEVICE_COLORS[device.kind] ?? 0x617276,
    metalness: 0.35,
    roughness: 0.62,
    flatShading: true,
  });
}

function worldPortPosition(device, portId) {
  const port = device.ports.find((item) => item.id === portId);
  if (!port) {
    throw new Error(`占位设备缺少端口：${device.id}.${portId}。`);
  }
  return device.position.map((value, index) => value + port.localPosition[index]);
}

function createPlaceholderDevice(device, pickables) {
  const group = new THREE.Group();
  group.name = `Placeholder:${device.id}`;
  group.position.set(...device.position);
  group.userData.deviceId = device.id;
  group.userData.device = device;

  const [width, height, depth] = device.dimensions;
  const body = new THREE.Mesh(new THREE.BoxGeometry(width, height, depth), createDeviceMaterial(device));
  body.name = `${device.id}:body`;
  body.position.set(width / 2, height / 2, depth / 2);
  body.userData.deviceId = device.id;
  group.add(body);
  pickables.push(body);

  const edges = new THREE.LineSegments(
    new THREE.EdgesGeometry(body.geometry),
    new THREE.LineBasicMaterial({ color: 0xc4d0cc, transparent: true, opacity: 0.72 }),
  );
  edges.position.copy(body.position);
  edges.userData.deviceId = device.id;
  group.add(edges);

  const cap = new THREE.Mesh(
    new THREE.BoxGeometry(Math.max(0.55, width * 0.82), 0.12, Math.max(0.55, depth * 0.82)),
    new THREE.MeshStandardMaterial({ color: 0xc5a25e, metalness: 0.35, roughness: 0.48 }),
  );
  cap.name = `${device.id}:identification-cap`;
  cap.position.set(width / 2, height + 0.08, depth / 2);
  cap.userData.deviceId = device.id;
  group.add(cap);
  pickables.push(cap);

  if (device.kind === "experimental_reactor") {
    const window = new THREE.Mesh(
      new THREE.BoxGeometry(0.12, 1.4, Math.min(2.6, depth * 0.58)),
      new THREE.MeshStandardMaterial({ color: 0x74d4db, emissive: 0x1b6b73, emissiveIntensity: 0.35, metalness: 0.08, roughness: 0.25 }),
    );
    window.position.set(0.04, height * 0.52, depth / 2);
    window.userData.deviceId = device.id;
    group.add(window);
    pickables.push(window);
  }

  return group;
}

function createConnectionVisual(connection, deviceMap) {
  const color = MEDIUM_COLORS[connection.medium] ?? 0xb7c7c4;
  const points = connection.path.map((point) => new THREE.Vector3(...point));
  const group = new THREE.Group();
  group.name = `Connection:${connection.id}`;
  const line = new THREE.Line(
    new THREE.BufferGeometry().setFromPoints(points),
    new THREE.LineBasicMaterial({ color, transparent: true, opacity: 0.82 }),
  );
  line.renderOrder = 2;
  group.add(line);

  for (let index = 0; index < points.length - 1; index += 1) {
    const start = points[index];
    const end = points[index + 1];
    const direction = end.clone().sub(start);
    if (direction.lengthSq() < 0.001) {
      continue;
    }
    const arrow = new THREE.Mesh(
      new THREE.ConeGeometry(0.13, 0.38, 6),
      new THREE.MeshStandardMaterial({ color, emissive: color, emissiveIntensity: 0.15, metalness: 0.05, roughness: 0.54 }),
    );
    arrow.position.copy(start).addScaledVector(direction, 0.58);
    arrow.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), direction.normalize());
    arrow.userData.isFlowDecoration = true;
    group.add(arrow);
  }

  const from = deviceMap.get(connection.from.deviceId);
  const to = deviceMap.get(connection.to.deviceId);
  group.userData.connection = connection;
  group.userData.endpointPreview = `${from.id}.${connection.from.portId} → ${to.id}.${connection.to.portId}`;
  return group;
}

/**
 * 创建三维可视化的通用占位场景。这里仅表达尺寸、拓扑和工质方向，不宣称任何正式方块或设备已注册。
 * @param {object} sceneData 已通过场景合同校验的场景 JSON。
 * @returns {{scene: THREE.Scene, pickables: THREE.Object3D[], animationIndicator: THREE.Object3D, deviceMap: Map, overviewFocus: THREE.Vector3}}
 */
export function createPlantScene(sceneData) {
  const scene = new THREE.Scene();
  scene.background = new THREE.Color(0x111b1d);
  scene.fog = new THREE.Fog(0x111b1d, 70, 150);

  scene.add(new THREE.HemisphereLight(0xd9e9e7, 0x162124, 2.3));
  const keyLight = new THREE.DirectionalLight(0xffe1ad, 3.2);
  keyLight.position.set(-10, 24, 16);
  scene.add(keyLight);
  const rimLight = new THREE.DirectionalLight(0x6ed0d2, 1.8);
  rimLight.position.set(34, 12, -22);
  scene.add(rimLight);

  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(66, 26),
    new THREE.MeshStandardMaterial({ color: 0x1c2b2d, metalness: 0.04, roughness: 0.92 }),
  );
  floor.rotation.x = -Math.PI / 2;
  floor.position.set(24, -0.08, 2.5);
  scene.add(floor);
  const grid = new THREE.GridHelper(66, 66, 0x436061, 0x263d3f);
  grid.position.set(24, -0.02, 2.5);
  grid.material.transparent = true;
  grid.material.opacity = 0.42;
  scene.add(grid);
  const axes = new THREE.AxesHelper(4);
  axes.position.set(0, 0.02, 0);
  scene.add(axes);

  const deviceMap = new Map(sceneData.devices.map((device) => [device.id, device]));
  const pickables = [];
  const deviceRoot = new THREE.Group();
  deviceRoot.name = "PlaceholderDevices";
  let reactorModel = null;
  for (const device of sceneData.devices) {
    if (device.kind === "experimental_reactor") {
      reactorModel = createReactorModel(device);
      pickables.push(...reactorModel.pickables);
      deviceRoot.add(reactorModel.group);
    } else {
      deviceRoot.add(createPlaceholderDevice(device, pickables));
    }
  }
  scene.add(deviceRoot);

  const connectionRoot = new THREE.Group();
  connectionRoot.name = "ConnectionVisuals";
  for (const connection of sceneData.connections) {
    connectionRoot.add(createConnectionVisual(connection, deviceMap));
  }
  scene.add(connectionRoot);

  const indicator = new THREE.Group();
  indicator.name = "StateAnimationIndicator";
  indicator.position.set(24, 8.5, 2.5);
  const indicatorCore = new THREE.Mesh(
    new THREE.OctahedronGeometry(0.3, 0),
    new THREE.MeshStandardMaterial({ color: 0x69d5d0, emissive: 0x1c8b8c, emissiveIntensity: 0.65, metalness: 0.08, roughness: 0.3 }),
  );
  const indicatorRing = new THREE.Mesh(
    new THREE.TorusGeometry(0.52, 0.035, 6, 24),
    new THREE.MeshStandardMaterial({ color: 0xd2a65b, emissive: 0x6c4920, emissiveIntensity: 0.28, metalness: 0.35, roughness: 0.42 }),
  );
  indicatorRing.rotation.x = Math.PI / 2;
  indicator.add(indicatorCore, indicatorRing);
  scene.add(indicator);

  const minX = Math.min(...sceneData.devices.map((device) => device.position[0]));
  const maxX = Math.max(...sceneData.devices.map((device) => device.position[0] + device.dimensions[0]));
  const overviewFocus = new THREE.Vector3((minX + maxX) / 2, 1.6, 2.5);

  return { scene, pickables, animationIndicator: indicator, deviceMap, overviewFocus, reactorModel };
}

/**
 * 更新占位场景中的唯一演示动画指示器。elapsedSeconds 不前进时，所有几何变换保持不变。
 * @param {{animationIndicator: THREE.Object3D}} resources 场景资源。
 * @param {object} state 标准化机组状态。
 * @param {number} elapsedSeconds 逻辑时间，单位为秒。
 */
export function updatePlantScene(resources, state, elapsedSeconds, deltaSeconds = 0) {
  if (resources.reactorModel) {
    updateReactorModel(resources.reactorModel, state, elapsedSeconds, deltaSeconds);
  }
  const indicator = resources.animationIndicator;
  const core = indicator.children[0];
  const ring = indicator.children[1];
  indicator.rotation.y = elapsedSeconds * 1.25;
  ring.rotation.z = elapsedSeconds * 1.8;
  const pulse = 1 + Math.sin(elapsedSeconds * 3.2) * 0.08;
  core.scale.setScalar(pulse);
  const color = state.alarmState === "critical" ? 0xf06e5a : state.alarmState === "warning" ? 0xf2c14e : 0x69d5d0;
  core.material.color.set(color);
  core.material.emissive.set(color);
  core.material.emissiveIntensity = state.alarmState === "normal" ? 0.65 : 0.9;
}

export { worldPortPosition, MEDIUM_COLORS };
