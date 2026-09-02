import * as THREE from "../../vendor/three.module.js";
import { assertPlantState } from "../state/plant-state.js";

export const REACTOR_SIZE = Object.freeze([5, 5, 5]);

const REACTOR_COLORS = Object.freeze({
  stableShell: 0x3d4644,
  warningShell: 0x765c36,
  overheatShell: 0x713c31,
  stableGlow: 0x7fa89a,
  warningGlow: 0xf2c14e,
  overheatGlow: 0xf06e5a,
  rod: 0x29302f,
  brass: 0xc0934d,
  copper: 0x98563f,
  glass: 0x315151,
  portCold: 0x62b1b0,
  portHot: 0xc4774c,
  interface: 0xc0934d,
});

const ROD_POSITIONS = Object.freeze([
  [1.65, 1.65],
  [3.35, 1.65],
  [1.65, 3.35],
  [3.35, 3.35],
]);

function isReactorDevice(device) {
  return device
    && device.kind === "experimental_reactor"
    && Array.isArray(device.dimensions)
    && device.dimensions.length === 3
    && device.dimensions.every((value, index) => value === REACTOR_SIZE[index]);
}

function findPort(device, portId) {
  const port = device.ports?.find((item) => item.id === portId);
  if (!port) {
    throw new TypeError(`实验反应堆缺少端口：${portId}。`);
  }
  return port;
}

function addSelectable(group, object, deviceId, pickables) {
  object.userData.deviceId = deviceId;
  group.add(object);
  if (pickables) {
    pickables.push(object);
  }
  return object;
}

function makeBox(width, height, depth, material, name) {
  const mesh = new THREE.Mesh(new THREE.BoxGeometry(width, height, depth), material);
  mesh.name = name;
  return mesh;
}

function makePortSocket(port, color, name) {
  const socket = new THREE.Mesh(
    new THREE.CylinderGeometry(0.34, 0.34, 0.16, 12),
    new THREE.MeshStandardMaterial({ color, emissive: color, emissiveIntensity: 0.18, metalness: 0.18, roughness: 0.42 }),
  );
  socket.name = name;
  socket.position.set(...port.localPosition);
  if (port.localPosition[0] === 0 || port.localPosition[0] === REACTOR_SIZE[0]) {
    socket.rotation.z = Math.PI / 2;
  }
  return socket;
}

function makeFlowCurve() {
  return new THREE.CatmullRomCurve3([
    new THREE.Vector3(0.18, 1, 2.5),
    new THREE.Vector3(1.15, 1.15, 2.5),
    new THREE.Vector3(2.5, 2.45, 2.5),
    new THREE.Vector3(3.85, 3.55, 2.5),
    new THREE.Vector3(4.82, 4, 2.5),
  ]);
}

function createFlowParticles(group, deviceId) {
  const material = new THREE.MeshStandardMaterial({
    color: REACTOR_COLORS.portCold,
    emissive: REACTOR_COLORS.portCold,
    emissiveIntensity: 0.55,
    metalness: 0.04,
    roughness: 0.3,
  });
  const particles = [];
  for (let index = 0; index < 6; index += 1) {
    const particle = new THREE.Mesh(new THREE.SphereGeometry(0.11, 8, 6), material);
    particle.name = `${deviceId}:coolant-flow-${index}`;
    particle.userData.deviceId = deviceId;
    group.add(particle);
    particles.push(particle);
  }
  return particles;
}

/**
 * 校验实验反应堆的游戏结构边界。此模块只消费场景 JSON，不创建 Minecraft 注册或服务端状态。
 * @param {object} device 场景中的反应堆设备定义。
 * @returns {true} 结构满足固定 5×5×5 合同，否则抛出中文异常。
 */
export function assertReactorDevice(device) {
  if (!isReactorDevice(device)) {
    throw new TypeError("实验反应堆必须严格使用 5×5×5 尺寸。");
  }
  findPort(device, "cold_coolant_inlet");
  findPort(device, "hot_coolant_outlet");
  findPort(device, "instrument_port");
  findPort(device, "refuel_port");
  return true;
}

/**
 * 计算反应堆视觉状态。`alarmState=critical` 只代表过热警示视觉，不推导未冻结的温度或功率公式。
 * @param {object} state 标准化机组状态。
 * @returns {{mode:string, controlRodTarget01:number, coolantFlow01:number, flowActive:boolean, pulseRate:number, glowColor:number, shellColor:number}}
 */
export function calculateReactorVisualState(state) {
  assertPlantState(state);
  const mode = state.scramActive ? "scram" : state.alarmState === "critical" ? "overheat" : state.alarmState === "warning" ? "warning" : "stable";
  const flowActive = state.primaryCoolantFlow01 > 0.02;
  const pulseRate = mode === "overheat" ? 4.8 : mode === "scram" ? 1.2 : 2.2 + state.reactorPower01 * 1.6;
  const glowColor = mode === "overheat" ? REACTOR_COLORS.overheatGlow : mode === "warning" ? REACTOR_COLORS.warningGlow : REACTOR_COLORS.stableGlow;
  const shellColor = mode === "overheat" ? REACTOR_COLORS.overheatShell : mode === "warning" ? REACTOR_COLORS.warningShell : REACTOR_COLORS.stableShell;
  return {
    mode,
    controlRodTarget01: state.controlRodDepth01,
    coolantFlow01: state.primaryCoolantFlow01,
    flowActive,
    pulseRate,
    glowColor,
    shellColor,
  };
}

/**
 * 按帧推进控制棒视觉位置。SCRAM 使用更高追踪速率，暂停时传入零秒即可保持位置不变。
 * @param {number} previousDepth 上一帧控制棒深度，范围 [0,1]。
 * @param {number} targetDepth 状态层目标深度，范围 [0,1]。
 * @param {number} deltaSeconds 当前帧推进秒数。
 * @param {boolean} scramActive 是否处于 SCRAM 状态。
 * @returns {number} 当前帧后的平滑深度。
 */
export function advanceControlRodDepth(previousDepth, targetDepth, deltaSeconds, scramActive) {
  if (![previousDepth, targetDepth, deltaSeconds].every(Number.isFinite) || deltaSeconds < 0) {
    throw new RangeError("控制棒动画参数必须是有限值且帧间隔不能为负数。");
  }
  const previous = Math.min(1, Math.max(0, previousDepth));
  const target = Math.min(1, Math.max(0, targetDepth));
  const rate = scramActive ? 14 : 5;
  const blend = 1 - Math.exp(-rate * deltaSeconds);
  return previous + (target - previous) * blend;
}

/**
 * 创建固定 5×5×5 实验反应堆的可评审模型。外壳、观察窗、端口和控制棒均为低多边形视觉提案。
 * @param {object} device 场景设备定义。
 * @returns {{group: THREE.Group, pickables: THREE.Object3D[], flowCurve: THREE.CatmullRomCurve3, visualDepth01: number|null}}
 */
export function createReactorModel(device) {
  assertReactorDevice(device);
  const group = new THREE.Group();
  group.name = `ReactorModel:${device.id}`;
  group.position.set(...device.position);
  group.userData.deviceId = device.id;
  group.userData.device = device;
  const pickables = [];

  const shellMaterial = new THREE.MeshStandardMaterial({
    color: REACTOR_COLORS.stableShell,
    metalness: 0.6,
    roughness: 0.52,
    transparent: true,
    opacity: 0.08,
    depthWrite: false,
    side: THREE.DoubleSide,
  });
  const shell = addSelectable(group, makeBox(5, 5, 5, shellMaterial, `${device.id}:shell`), device.id, pickables);
  shell.renderOrder = 1;

  const shellEdges = new THREE.LineSegments(
    new THREE.EdgesGeometry(shell.geometry),
    new THREE.LineBasicMaterial({ color: REACTOR_COLORS.brass, transparent: true, opacity: 0.96 }),
  );
  shellEdges.name = `${device.id}:shell-edges`;
  shellEdges.position.set(2.5, 2.5, 2.5);
  shellEdges.userData.deviceId = device.id;
  group.add(shellEdges);

  const baseMaterial = new THREE.MeshStandardMaterial({ color: 0x29312f, metalness: 0.64, roughness: 0.62 });
  const wallMaterial = new THREE.MeshStandardMaterial({ color: REACTOR_COLORS.stableShell, metalness: 0.58, roughness: 0.58 });
  const brassMaterial = new THREE.MeshStandardMaterial({ color: REACTOR_COLORS.brass, metalness: 0.78, roughness: 0.34 });
  const copperMaterial = new THREE.MeshStandardMaterial({ color: REACTOR_COLORS.copper, metalness: 0.72, roughness: 0.4 });
  const basePlate = makeBox(4.55, 0.22, 4.55, baseMaterial, `${device.id}:base-plate`);
  const topPlate = makeBox(4.55, 0.22, 4.55, baseMaterial, `${device.id}:top-plate`);
  basePlate.position.set(2.5, 0.11, 2.5);
  topPlate.position.set(2.5, 4.89, 2.5);
  group.add(basePlate, topPlate);

  const casingPanels = [shell];
  for (const [size, position, name] of [
    [[4.6, 4.55, 0.22], [2.5, 2.5, 0.16], `${device.id}:back-casing-panel`],
    [[0.22, 4.55, 4.6], [0.16, 2.5, 2.5], `${device.id}:left-casing-panel`],
    [[0.22, 4.55, 4.6], [4.84, 2.5, 2.5], `${device.id}:right-casing-panel`],
  ]) {
    const panel = makeBox(...size, wallMaterial, name);
    panel.position.set(...position);
    group.add(panel);
    casingPanels.push(panel);
  }

  const postMaterial = brassMaterial;
  for (const [x, z] of [[0.2, 0.2], [4.8, 0.2], [0.2, 4.8], [4.8, 4.8]]) {
    const post = makeBox(0.34, 4.55, 0.34, postMaterial, `${device.id}:corner-post`);
    post.position.set(x, 2.5, z);
    group.add(post);
  }

  const observationWindow = addSelectable(
    group,
    makeBox(2.9, 2.05, 0.12, new THREE.MeshStandardMaterial({
      color: REACTOR_COLORS.glass,
      emissive: REACTOR_COLORS.stableGlow,
      emissiveIntensity: 0.22,
      metalness: 0.04,
      roughness: 0.24,
      transparent: true,
      opacity: 0.9,
    }), `${device.id}:observation-window`),
    device.id,
    pickables,
  );
  observationWindow.position.set(2.5, 2.4, 4.93);

  const windowFrameMaterial = brassMaterial;
  const windowTop = makeBox(3.35, 0.24, 0.18, windowFrameMaterial, `${device.id}:observation-window-top-frame`);
  const windowBottom = makeBox(3.35, 0.24, 0.18, windowFrameMaterial, `${device.id}:observation-window-bottom-frame`);
  const windowLeft = makeBox(0.24, 2.75, 0.18, windowFrameMaterial, `${device.id}:observation-window-left-frame`);
  const windowRight = makeBox(0.24, 2.75, 0.18, windowFrameMaterial, `${device.id}:observation-window-right-frame`);
  windowTop.position.set(2.5, 3.54, 4.9);
  windowBottom.position.set(2.5, 1.26, 4.9);
  windowLeft.position.set(0.82, 2.4, 4.9);
  windowRight.position.set(4.18, 2.4, 4.9);
  group.add(windowTop, windowBottom, windowLeft, windowRight);

  const casingBand = makeBox(4.72, 0.18, 0.2, copperMaterial, `${device.id}:front-copper-band`);
  casingBand.position.set(2.5, 0.72, 4.86);
  group.add(casingBand);

  const core = new THREE.Mesh(
    new THREE.CylinderGeometry(1.25, 1.25, 3.35, 12),
    new THREE.MeshStandardMaterial({ color: REACTOR_COLORS.stableGlow, emissive: REACTOR_COLORS.stableGlow, emissiveIntensity: 0.7, metalness: 0.08, roughness: 0.28 }),
  );
  core.name = `${device.id}:reactor-core-glow`;
  core.position.set(2.5, 2.35, 2.5);
  group.add(core);

  const rodRoot = new THREE.Group();
  rodRoot.name = `${device.id}:control-rod-drive`;
  const rodMaterial = new THREE.MeshStandardMaterial({ color: REACTOR_COLORS.rod, metalness: 0.7, roughness: 0.36 });
  const driveHousing = makeBox(1.2, 0.42, 1.2, brassMaterial, `${device.id}:control-rod-drive-housing`);
  driveHousing.position.set(2.5, 4.78, 2.5);
  rodRoot.add(driveHousing);
  for (const [x, z] of ROD_POSITIONS) {
    const driveAxis = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.08, 2.55, 8), rodMaterial);
    driveAxis.name = `${device.id}:control-rod-drive-axis`;
    driveAxis.position.set(x, 3.35, z);
    rodRoot.add(driveAxis);
  }
  const driveWheel = new THREE.Mesh(new THREE.TorusGeometry(0.62, 0.1, 8, 20), brassMaterial);
  driveWheel.name = `${device.id}:control-rod-handwheel`;
  driveWheel.position.set(2.5, 5.18, 2.5);
  driveWheel.rotation.x = Math.PI / 2;
  rodRoot.add(driveWheel);
  const driveHub = new THREE.Mesh(new THREE.CylinderGeometry(0.16, 0.16, 0.18, 10), copperMaterial);
  driveHub.name = `${device.id}:control-rod-drive-hub`;
  driveHub.position.set(2.5, 5.18, 2.5);
  rodRoot.add(driveHub);
  group.add(rodRoot);

  const coldPort = addSelectable(group, makePortSocket(findPort(device, "cold_coolant_inlet"), REACTOR_COLORS.portCold, `${device.id}:cold-port`), device.id, pickables);
  const hotPort = addSelectable(group, makePortSocket(findPort(device, "hot_coolant_outlet"), REACTOR_COLORS.portHot, `${device.id}:hot-port`), device.id, pickables);
  const instrumentSocket = makePortSocket(findPort(device, "instrument_port"), REACTOR_COLORS.interface, `${device.id}:instrument-port`);
  const refuelSocket = makePortSocket(findPort(device, "refuel_port"), REACTOR_COLORS.interface, `${device.id}:refuel-port`);
  instrumentSocket.scale.setScalar(0.65);
  refuelSocket.scale.setScalar(0.65);
  instrumentSocket.rotation.x = Math.PI / 2;
  refuelSocket.rotation.x = Math.PI / 2;
  group.add(instrumentSocket, refuelSocket);

  const flowParticles = createFlowParticles(group, device.id);
  const flowCurve = makeFlowCurve();
  return {
    group,
    pickables,
    flowCurve,
    flowParticles,
    shell,
    observationWindow,
    core,
    rodRoot,
    coldPort,
    hotPort,
    casingPanels,
    visualDepth01: null,
  };
}

/**
 * 消费标准化状态并更新反应堆视觉部件。冷却剂粒子始终从冷端驶向热端，流量为零时隐藏。
 * @param {ReturnType<typeof createReactorModel>} model 反应堆模型资源。
 * @param {object} state 标准化机组状态。
 * @param {number} elapsedSeconds 逻辑时间，单位为秒。
 * @param {number} deltaSeconds 本帧实际推进的逻辑秒数，暂停时为 0。
 */
export function updateReactorModel(model, state, elapsedSeconds, deltaSeconds) {
  assertPlantState(state);
  if (!Number.isFinite(elapsedSeconds) || elapsedSeconds < 0 || !Number.isFinite(deltaSeconds) || deltaSeconds < 0) {
    throw new RangeError("反应堆动画时间必须是非负有限秒数。");
  }
  const visualState = calculateReactorVisualState(state);
  const previousDepth = model.visualDepth01 === null ? visualState.controlRodTarget01 : model.visualDepth01;
  model.visualDepth01 = advanceControlRodDepth(previousDepth, visualState.controlRodTarget01, deltaSeconds, state.scramActive);
  model.rodRoot.position.y = -model.visualDepth01 * 2.6;

  const pulse = 0.72 + Math.sin(elapsedSeconds * visualState.pulseRate) * (visualState.mode === "overheat" ? 0.22 : 0.09);
  model.core.material.color.set(visualState.glowColor);
  model.core.material.emissive.set(visualState.glowColor);
  model.core.material.emissiveIntensity = Math.max(0.2, pulse);
  model.observationWindow.material.color.set(visualState.glowColor);
  model.observationWindow.material.emissive.set(visualState.glowColor);
  model.observationWindow.material.emissiveIntensity = Math.max(0.18, pulse * 0.7);
  model.shell.material.color.set(visualState.shellColor);
  for (const casingPanel of model.casingPanels) {
    casingPanel.material.color.set(visualState.shellColor);
  }
  model.group.userData.visualMode = visualState.mode;
  model.group.userData.coolantFlow01 = visualState.coolantFlow01;

  const particleVisible = visualState.flowActive;
  const particleSpeed = 0.18 + visualState.coolantFlow01 * 0.32;
  for (let index = 0; index < model.flowParticles.length; index += 1) {
    const particle = model.flowParticles[index];
    particle.visible = particleVisible;
    if (!particleVisible) {
      continue;
    }
    const offset = index / model.flowParticles.length;
    const progress = (elapsedSeconds * particleSpeed + offset) % 1;
    particle.position.copy(model.flowCurve.getPointAt(progress));
    particle.material.color.set(visualState.mode === "overheat" ? REACTOR_COLORS.overheatGlow : REACTOR_COLORS.portCold);
    particle.material.emissive.set(particle.material.color);
  }
}
