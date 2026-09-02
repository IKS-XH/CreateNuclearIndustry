const DEVICE_LABELS = Object.freeze({
  primary_coolant_pump: "主冷却泵",
  primary_coolant_inlet_valve: "一回路冷端隔离阀",
  experimental_reactor: "实验反应堆",
  primary_coolant_hot_valve: "一回路热端隔离阀",
  nuclear_heat_exchanger: "核热模式换热器",
  high_pressure_boiler: "专用高压锅炉",
  supercritical_turbine: "超临界汽轮机",
  steam_inlet_valve: "蒸汽入口阀",
  condenser_heat_exchanger: "冷凝模式换热器",
  condensate_return_valve: "冷凝回水阀",
  feedwater_pump: "给水泵",
  create_main_shaft_gearbox: "Create 齿轮箱",
  create_stress_load: "Create 应力负载",
});

const MEDIUM_LABELS = Object.freeze({
  cold_compound_coolant: "冷复合冷却剂",
  hot_compound_coolant: "热复合冷却剂",
  nuclear_heat: "核热",
  water: "水/回水",
  supercritical_steam: "超临界蒸汽",
  steam: "普通蒸汽",
  rotary_stress: "旋转应力",
  control: "控制接口",
  maintenance: "维护接口",
  cooling_service: "冷源接口",
});

export function getDeviceLabel(device) {
  return DEVICE_LABELS[device.id] ?? DEVICE_LABELS[device.kind] ?? device.kind;
}

export function getMediumLabel(medium) {
  return MEDIUM_LABELS[medium] ?? medium;
}

/**
 * 生成紧凑流向说明。该文本只来自场景端口合同，不把尚未冻结的热工数值写进设备模型。
 * @param {object} device 场景设备。
 * @returns {string} 本地化工质和端口方向摘要。
 */
export function formatFlowDescription(device) {
  const ports = device.ports.filter((port) => port.required && ["inlet", "outlet"].includes(port.direction));
  if (ports.length === 0) {
    return "流向：无固定内部工质";
  }
  const flow = ports.map((port) => `${port.direction === "inlet" ? "入口" : "出口"} ${getMediumLabel(port.medium)}`);
  return `流向：${flow.join(" · ")}`;
}

function percent(value) {
  return `${Math.round(value * 100)}%`;
}

/**
 * 把标准化状态映射成选择面板中的可读状态。状态层仍是唯一数值来源，渲染层不反推热力学量。
 * @param {object} device 场景设备。
 * @param {object} state 标准化机组状态。
 * @returns {string} 本地化状态摘要。
 */
export function getDeviceStatus(device, state) {
  if (state.scramActive && device.kind === "experimental_reactor") {
    return "SCRAM · 控制棒插入";
  }
  if (state.alarmState === "critical") {
    return "临界警报 · 占位表现";
  }
  if (state.alarmState === "warning") {
    return "运行预警 · 占位表现";
  }
  switch (device.kind) {
    case "experimental_reactor":
      return `稳定运行 · 功率 ${percent(state.reactorPower01)}`;
    case "main_coolant_pump":
      return `运行中 · 一回路流量 ${percent(state.primaryCoolantFlow01)}`;
    case "feedwater_pump":
      return `运行中 · 给水回路 ${percent(state.boilerWaterLevel01)}`;
    case "nuclear_heat_exchanger":
      return device.mode === "condensing"
        ? `冷凝运行 · 回水 ${percent(state.condenserFlow01)}`
        : `换热运行 · 负荷 ${percent(state.heatExchangerLoad01)}`;
    case "high_pressure_boiler":
      return `运行中 · 液位 ${percent(state.boilerWaterLevel01)} · 压力 ${percent(state.boilerPressure01)}`;
    case "supercritical_turbine":
      return `旋转中 · 转速 ${percent(state.turbineRpm01)}`;
    case "create_stress_load":
      return `旋转应力 · 负载 ${percent(state.shaftLoad01)}`;
    default:
      return "通流占位 · 等待正式模型";
  }
}
