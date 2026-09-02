const SCHEMA_VERSION_PATTERN = /^\d+\.\d+\.\d+$/;
const REQUIRED_STATE_FIELDS = new Set([
  "timeScale",
  "reactorPower01",
  "controlRodDepth01",
  "primaryCoolantFlow01",
  "heatExchangerLoad01",
  "boilerWaterLevel01",
  "boilerPressure01",
  "steamFlow01",
  "turbineRpm01",
  "shaftLoad01",
  "condenserFlow01",
  "alarmState",
  "scramActive",
]);
const NORMALIZED_FIELDS = new Set([
  "reactorPower01",
  "controlRodDepth01",
  "primaryCoolantFlow01",
  "heatExchangerLoad01",
  "boilerWaterLevel01",
  "boilerPressure01",
  "steamFlow01",
  "turbineRpm01",
  "shaftLoad01",
  "condenserFlow01",
]);
const ALARM_STATES = new Set(["normal", "warning", "critical"]);

function isPlainObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function assert(condition, message) {
  if (!condition) {
    throw new TypeError(message);
  }
}

function assertVector(vector, name, length) {
  assert(Array.isArray(vector) && vector.length === length, `${name} 必须是 ${length} 维数组。`);
  assert(vector.every((value) => Number.isFinite(value)), `${name} 必须只包含有限数值。`);
}

function vectorEquals(left, right) {
  return left.length === right.length && left.every((value, index) => Math.abs(value - right[index]) < 1e-9);
}

function buildDeviceIndex(scene) {
  const deviceIndex = new Map();
  for (const device of scene.devices) {
    assert(isPlainObject(device), "每个设备必须是对象。");
    assert(typeof device.id === "string" && /^[a-z0-9_]+$/.test(device.id), `设备 ID 无效：${device.id}。`);
    assert(!deviceIndex.has(device.id), `设备 ID 重复：${device.id}。`);
    assert(typeof device.registryId === "string" && device.registryId.includes(":"), `${device.id} 缺少带命名空间的 registryId。`);
    assert(typeof device.kind === "string" && device.kind.length > 0, `${device.id} 缺少 kind。`);
    assertVector(device.position, `${device.id}.position`, 3);
    assertVector(device.dimensions, `${device.id}.dimensions`, 3);
    assert(device.dimensions.every((value) => value > 0), `${device.id}.dimensions 必须为正数。`);
    assert(Array.isArray(device.ports) && device.ports.length > 0, `${device.id} 必须包含端口。`);

    const portIds = new Set();
    for (const port of device.ports) {
      assert(isPlainObject(port), `${device.id} 的端口必须是对象。`);
      assert(typeof port.id === "string" && /^[a-z0-9_]+$/.test(port.id), `${device.id} 的端口 ID 无效。`);
      assert(!portIds.has(port.id), `${device.id} 的端口 ID 重复：${port.id}。`);
      portIds.add(port.id);
      assert(["inlet", "outlet", "interface"].includes(port.direction), `${device.id}.${port.id} 的方向无效。`);
      assert(typeof port.medium === "string" && port.medium.length > 0, `${device.id}.${port.id} 缺少 medium。`);
      assertVector(port.localPosition, `${device.id}.${port.id}.localPosition`, 3);
      assert(port.localPosition.every((value, index) => value >= 0 && value <= device.dimensions[index]), `${device.id}.${port.id} 超出设备局部边界。`);
      assert(typeof port.required === "boolean", `${device.id}.${port.id}.required 必须是布尔值。`);
      assert(["internal", "external", "optional"].includes(port.connectionPolicy), `${device.id}.${port.id} 的 connectionPolicy 无效。`);
      if (port.required) {
        assert(port.connectionPolicy === "internal", `${device.id}.${port.id} 的必需端口必须使用 internal 连接策略。`);
        assert(port.direction !== "interface", `${device.id}.${port.id} 的 interface 端口不能是必需端口。`);
      }
    }
    deviceIndex.set(device.id, { device, ports: new Map(device.ports.map((port) => [port.id, port])) });
  }
  return deviceIndex;
}

function validateStateContract(scene) {
  assert(isPlainObject(scene.stateContract), "场景缺少 stateContract。");
  assert(SCHEMA_VERSION_PATTERN.test(scene.stateContract.version), "stateContract.version 必须是语义版本。");
  assert(Array.isArray(scene.stateContract.fields), "stateContract.fields 必须是数组。");
  const fields = new Set(scene.stateContract.fields.map((field) => field.name));
  assert(fields.size === scene.stateContract.fields.length, "stateContract 字段名不能重复。");
  for (const name of REQUIRED_STATE_FIELDS) {
    assert(fields.has(name), `stateContract 缺少字段：${name}。`);
  }
  for (const field of scene.stateContract.fields) {
    assert(isPlainObject(field) && typeof field.name === "string", "状态字段定义无效。");
    if (NORMALIZED_FIELDS.has(field.name)) {
      assert(Array.isArray(field.range) && vectorEquals(field.range, [0, 1]), `${field.name} 必须声明 [0,1] 范围。`);
    }
  }
  assert(isPlainObject(scene.defaultState), "场景缺少 defaultState。");
  const defaultFields = new Set(Object.keys(scene.defaultState));
  assert(defaultFields.size === REQUIRED_STATE_FIELDS.size && [...REQUIRED_STATE_FIELDS].every((field) => defaultFields.has(field)), "defaultState 字段必须完整匹配状态合同。");
  for (const field of NORMALIZED_FIELDS) {
    assert(Number.isFinite(scene.defaultState[field]) && scene.defaultState[field] >= 0 && scene.defaultState[field] <= 1, `${field} 默认值必须位于 [0,1]。`);
  }
  assert(scene.defaultState.timeScale >= 0 && scene.defaultState.timeScale <= 4, "timeScale 默认值必须位于 [0,4]。");
  assert(ALARM_STATES.has(scene.defaultState.alarmState), "defaultState.alarmState 无效。");
  assert(typeof scene.defaultState.scramActive === "boolean", "defaultState.scramActive 必须是布尔值。");
}

function validateDeviceRules(scene) {
  const reactors = scene.devices.filter((device) => device.kind === "experimental_reactor");
  assert(reactors.length === 1, "默认场景必须且只能包含一个实验反应堆。");
  assert(vectorEquals(reactors[0].dimensions, [5, 5, 5]), "实验反应堆必须严格为 5×5×5。");
  assert(reactors[0].sizeStatus === "game_contract" && reactors[0].structureContract === "fixed_5x5x5", "实验反应堆必须标记为固定游戏结构合同。");

  const exchangers = scene.devices.filter((device) => device.registryId === "create_nuclear_industry:nuclear_heat_exchanger");
  assert(exchangers.length >= 2, "默认场景至少需要两个核换热器实例。");
  const modes = new Set();
  for (const exchanger of exchangers) {
    assert(exchanger.kind === "nuclear_heat_exchanger", `${exchanger.id} 的换热器 kind 无效。`);
    assert(["nuclear_heat", "condensing"].includes(exchanger.mode), `${exchanger.id} 必须且只能选择一个换热模式。`);
    modes.add(exchanger.mode);
    assert(exchanger.modeExclusiveGroup === "nuclear_heat_exchanger_instance_mode", `${exchanger.id} 缺少互斥模式组。`);
  }
  assert(modes.has("nuclear_heat") && modes.has("condensing"), "默认场景必须同时包含核热和冷凝模式实例。");

  const boiler = scene.devices.find((device) => device.kind === "high_pressure_boiler");
  const turbine = scene.devices.find((device) => device.kind === "supercritical_turbine");
  assert(boiler && boiler.sizeStatus === "visual_proposal" && boiler.visualProposal === true && boiler.structureContract === "not_frozen", "锅炉尺寸必须标记为视觉提案且未冻结。");
  assert(turbine && turbine.sizeStatus === "visual_proposal" && turbine.visualProposal === true && turbine.structureContract === "not_frozen", "汽轮机尺寸必须标记为视觉提案且未冻结。");
  assert(Number.isInteger(turbine.lengthBlocks) && turbine.lengthBlocks >= 6 && turbine.lengthBlocks <= 8, "汽轮机视觉提案长度必须位于 6～8 格。");

  assert(scene.visualPolicy?.createOutput === "rotary_stress_only", "Create 输出只能是 rotary_stress_only。");
  assert(Array.isArray(scene.visualPolicy?.forbiddenEnergyVisuals), "必须声明禁止的电力视觉。");
  assert(scene.visualPolicy.forbiddenEnergyVisuals.includes("FE") && scene.visualPolicy.forbiddenEnergyVisuals.includes("RF"), "必须禁止 FE/RF 视觉。");
}

function validateConnections(scene, deviceIndex) {
  assert(Array.isArray(scene.connections) && scene.connections.length > 0, "场景必须包含 connections。");
  const connectedPorts = new Map();
  const graph = new Map(scene.devices.map((device) => [device.id, new Set()]));
  for (const connection of scene.connections) {
    assert(isPlainObject(connection), "连接必须是对象。");
    assert(typeof connection.id === "string" && connection.id.length > 0, "连接缺少 id。");
    assert(connection.flowDirection === "from_to", `${connection.id} 的 flowDirection 必须是 from_to。`);
    assert(typeof connection.medium === "string" && connection.medium.length > 0, `${connection.id} 缺少 medium。`);
    assert(isPlainObject(connection.from) && isPlainObject(connection.to), `${connection.id} 缺少 from/to。`);
    const fromDevice = deviceIndex.get(connection.from.deviceId);
    const toDevice = deviceIndex.get(connection.to.deviceId);
    assert(fromDevice && toDevice, `${connection.id} 引用了不存在的设备。`);
    const fromPort = fromDevice.ports.get(connection.from.portId);
    const toPort = toDevice.ports.get(connection.to.portId);
    assert(fromPort && toPort, `${connection.id} 引用了不存在的端口。`);
    assert(fromPort.direction === "outlet", `${connection.id} 的 from 端口必须是 outlet。`);
    assert(toPort.direction === "inlet", `${connection.id} 的 to 端口必须是 inlet。`);
    assert(fromPort.medium === connection.medium && toPort.medium === connection.medium, `${connection.id} 的工质必须与两端端口一致。`);
    assert(Array.isArray(connection.path) && connection.path.length >= 2, `${connection.id} 必须包含至少两个路径点。`);
    const fromWorld = fromDevice.device.position.map((value, index) => value + fromPort.localPosition[index]);
    const toWorld = toDevice.device.position.map((value, index) => value + toPort.localPosition[index]);
    assertVector(connection.path[0], `${connection.id}.path[0]`, 3);
    assertVector(connection.path.at(-1), `${connection.id}.path[last]`, 3);
    assert(vectorEquals(connection.path[0], fromWorld), `${connection.id} 的路径起点未对齐 from 端口。`);
    assert(vectorEquals(connection.path.at(-1), toWorld), `${connection.id} 的路径终点未对齐 to 端口。`);

    for (const endpoint of [connection.from, connection.to]) {
      const portKey = `${endpoint.deviceId}.${endpoint.portId}`;
      assert(!connectedPorts.has(portKey), `端口重复连接：${portKey}。`);
      connectedPorts.set(portKey, connection.id);
    }
    graph.get(connection.from.deviceId).add(connection.to.deviceId);
    graph.get(connection.to.deviceId).add(connection.from.deviceId);
  }

  for (const { device, ports } of deviceIndex.values()) {
    for (const port of ports.values()) {
      if (port.required) {
        assert(connectedPorts.has(`${device.id}.${port.id}`), `必需端口未连接：${device.id}.${port.id}。`);
      }
    }
  }

  const root = scene.devices[0].id;
  const visited = new Set([root]);
  const queue = [root];
  while (queue.length > 0) {
    const current = queue.shift();
    for (const neighbor of graph.get(current)) {
      if (!visited.has(neighbor)) {
        visited.add(neighbor);
        queue.push(neighbor);
      }
    }
  }
  assert(visited.size === scene.devices.length, "默认场景存在与主回路断开的设备。");
}

/**
 * 校验默认机组场景合同。该校验只做静态数据检查，不加载模型、不连接 Minecraft 服务端。
 * @param {unknown} scene 待校验的场景 JSON 对象。
 * @returns {true} 合同有效；否则抛出确定性的中文错误。
 */
export function assertSceneContract(scene) {
  assert(isPlainObject(scene), "场景必须是普通对象。");
  assert(SCHEMA_VERSION_PATTERN.test(scene.schemaVersion), "schemaVersion 必须是语义版本。");
  assert(typeof scene.sceneId === "string" && scene.sceneId.length > 0, "sceneId 必须非空。");
  assert(scene.coordinateSystem?.unit === "minecraft_block", "场景单位必须是 minecraft_block。");
  assert(scene.coordinateSystem?.upAxis === "y", "场景上轴必须是 y。");
  assert(scene.coordinateSystem?.originMeaning === "device_min_corner", "场景原点含义必须固定为 device_min_corner。");
  assert(Array.isArray(scene.devices) && scene.devices.length > 0, "场景必须包含设备。");
  validateStateContract(scene);
  const deviceIndex = buildDeviceIndex(scene);
  validateDeviceRules(scene);
  validateConnections(scene, deviceIndex);
  assert(Array.isArray(scene.presetIds) && scene.presetIds.length === 8, "默认场景必须声明八个预设 ID。");
  assert(new Set(scene.presetIds).size === 8, "预设 ID 不能重复。");
  assert(scene.presetIds.includes(scene.defaultPresetId), "defaultPresetId 必须位于 presetIds 中。");
  return true;
}
