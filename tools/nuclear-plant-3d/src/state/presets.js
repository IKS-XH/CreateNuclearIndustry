import { createPlantState } from "./plant-state.js";
import { createTimeline, sampleTimeline } from "./timeline.js";

function frame(atSeconds, label, overrides) {
  return { atSeconds, label, state: createPlantState(overrides) };
}

const steady = createPlantState();

const presetDefinitions = {
  startup: {
    description: "泵先启动，反应堆随后升功率，锅炉预热后汽轮机升速并接入主轴负载。",
    durationSeconds: 75,
    keyframes: [
      frame(0, "全机组待启动", { timeScale: 1, reactorPower01: 0, controlRodDepth01: 1, primaryCoolantFlow01: 0, heatExchangerLoad01: 0, boilerWaterLevel01: 0.76, boilerPressure01: 0, steamFlow01: 0, turbineRpm01: 0, shaftLoad01: 0, condenserFlow01: 0.15 }),
      frame(8, "主冷却泵启动", { reactorPower01: 0, controlRodDepth01: 1, primaryCoolantFlow01: 0.65, heatExchangerLoad01: 0.05, boilerWaterLevel01: 0.78, condenserFlow01: 0.3 }),
      frame(20, "反应堆低功率升温", { reactorPower01: 0.25, controlRodDepth01: 0.76, primaryCoolantFlow01: 0.75, heatExchangerLoad01: 0.18, boilerWaterLevel01: 0.82, boilerPressure01: 0.12, condenserFlow01: 0.5 }),
      frame(40, "锅炉预热并开始出汽", { reactorPower01: 0.68, controlRodDepth01: 0.4, primaryCoolantFlow01: 0.88, heatExchangerLoad01: 0.55, boilerWaterLevel01: 0.85, boilerPressure01: 0.4, steamFlow01: 0.35, turbineRpm01: 0.35, shaftLoad01: 0.05, condenserFlow01: 0.72 }),
      frame(60, "汽轮机升速接载", { reactorPower01: 0.9, controlRodDepth01: 0.2, primaryCoolantFlow01: 0.92, heatExchangerLoad01: 0.82, boilerWaterLevel01: 0.83, boilerPressure01: 0.72, steamFlow01: 0.8, turbineRpm01: 0.78, shaftLoad01: 0.62, condenserFlow01: 0.84 }),
      frame(75, "进入稳定运行", steady),
    ],
  },
  steady: {
    description: "反应堆、换热器、锅炉、汽轮机和冷凝回路保持稳定的示意运行状态。",
    durationSeconds: 60,
    keyframes: [
      frame(0, "稳定运行", steady),
      frame(60, "稳定运行", steady),
    ],
  },
  scram: {
    description: "控制棒快速插入，冷却泵继续运行，锅炉和汽轮机按惯性衰减。",
    durationSeconds: 60,
    keyframes: [
      frame(0, "SCRAM 前稳定运行", steady),
      frame(5, "SCRAM 请求建立", { reactorPower01: 0.75, controlRodDepth01: 0.9, primaryCoolantFlow01: 0.94, heatExchangerLoad01: 0.82, boilerWaterLevel01: 0.78, boilerPressure01: 0.7, steamFlow01: 0.72, turbineRpm01: 0.82, shaftLoad01: 0.7, condenserFlow01: 0.86, alarmState: "warning", scramActive: true }),
      frame(15, "控制棒接近全插入", { reactorPower01: 0.35, controlRodDepth01: 1, primaryCoolantFlow01: 0.92, heatExchangerLoad01: 0.65, boilerWaterLevel01: 0.7, boilerPressure01: 0.55, steamFlow01: 0.5, turbineRpm01: 0.72, shaftLoad01: 0.45, condenserFlow01: 0.8, alarmState: "warning", scramActive: true }),
      frame(35, "余热与机械惯性衰减", { reactorPower01: 0.12, controlRodDepth01: 1, primaryCoolantFlow01: 0.88, heatExchangerLoad01: 0.3, boilerWaterLevel01: 0.58, boilerPressure01: 0.25, steamFlow01: 0.15, turbineRpm01: 0.3, shaftLoad01: 0.08, condenserFlow01: 0.62, alarmState: "normal", scramActive: true }),
      frame(60, "SCRAM 后安全余热运行", { reactorPower01: 0.05, controlRodDepth01: 1, primaryCoolantFlow01: 0.8, heatExchangerLoad01: 0.1, boilerWaterLevel01: 0.65, boilerPressure01: 0.12, steamFlow01: 0.05, turbineRpm01: 0.05, shaftLoad01: 0, condenserFlow01: 0.35, alarmState: "normal", scramActive: true }),
    ],
  },
  loss_of_coolant: {
    description: "一回路流量持续下降，反应堆预警，换热负荷和产汽逐步降低。",
    durationSeconds: 60,
    keyframes: [
      frame(0, "失冷前稳定运行", steady),
      frame(10, "一回路流量下降", { primaryCoolantFlow01: 0.55, heatExchangerLoad01: 0.7, steamFlow01: 0.72, turbineRpm01: 0.78, shaftLoad01: 0.7, alarmState: "warning" }),
      frame(30, "换热能力继续下降", { reactorPower01: 0.78, controlRodDepth01: 0.3, primaryCoolantFlow01: 0.25, heatExchangerLoad01: 0.4, boilerWaterLevel01: 0.76, boilerPressure01: 0.5, steamFlow01: 0.35, turbineRpm01: 0.6, shaftLoad01: 0.45, condenserFlow01: 0.6, alarmState: "critical" }),
      frame(60, "低流量保护状态", { reactorPower01: 0.65, controlRodDepth01: 0.45, primaryCoolantFlow01: 0.1, heatExchangerLoad01: 0.15, boilerWaterLevel01: 0.72, boilerPressure01: 0.28, steamFlow01: 0.1, turbineRpm01: 0.2, shaftLoad01: 0.05, condenserFlow01: 0.35, alarmState: "critical" }),
    ],
  },
  feedwater_loss: {
    description: "给水回路中断，锅炉液位快速降低并表现出干烧风险。",
    durationSeconds: 50,
    keyframes: [
      frame(0, "给水正常", steady),
      frame(8, "给水流量下降", { boilerWaterLevel01: 0.6, boilerPressure01: 0.8, steamFlow01: 0.78, condenserFlow01: 0.55, alarmState: "warning" }),
      frame(20, "锅炉液位偏低", { boilerWaterLevel01: 0.25, boilerPressure01: 0.86, steamFlow01: 0.7, turbineRpm01: 0.76, shaftLoad01: 0.65, condenserFlow01: 0.35, alarmState: "warning" }),
      frame(35, "干烧警示", { boilerWaterLevel01: 0.05, boilerPressure01: 0.93, steamFlow01: 0.58, turbineRpm01: 0.68, shaftLoad01: 0.45, condenserFlow01: 0.22, alarmState: "critical" }),
      frame(50, "给水耗尽保护状态", { boilerWaterLevel01: 0, boilerPressure01: 0.82, steamFlow01: 0.35, turbineRpm01: 0.48, shaftLoad01: 0.2, condenserFlow01: 0.12, alarmState: "critical" }),
    ],
  },
  steam_blockage: {
    description: "蒸汽出口受阻，锅炉压力升高并进入安全阀/旁通表现区间。",
    durationSeconds: 45,
    keyframes: [
      frame(0, "蒸汽通道正常", steady),
      frame(10, "蒸汽流量受限", { boilerPressure01: 0.88, steamFlow01: 0.55, turbineRpm01: 0.78, shaftLoad01: 0.6, alarmState: "warning" }),
      frame(20, "压力继续升高", { boilerPressure01: 0.95, steamFlow01: 0.3, turbineRpm01: 0.66, shaftLoad01: 0.38, condenserFlow01: 0.55, alarmState: "critical" }),
      frame(30, "安全阀与旁通动作区间", { boilerPressure01: 0.98, steamFlow01: 0.12, turbineRpm01: 0.52, shaftLoad01: 0.2, condenserFlow01: 0.42, alarmState: "critical" }),
      frame(45, "压力受控但主回路仍受限", { boilerPressure01: 0.78, steamFlow01: 0.2, turbineRpm01: 0.42, shaftLoad01: 0.12, condenserFlow01: 0.35, alarmState: "warning" }),
    ],
  },
  turbine_trip: {
    description: "汽轮机进汽关闭，制动介入，转子按惯性逐步降速。",
    durationSeconds: 50,
    keyframes: [
      frame(0, "汽轮机跳闸前稳定运行", steady),
      frame(5, "进汽阀关闭", { steamFlow01: 0.05, turbineRpm01: 0.82, shaftLoad01: 0.55, condenserFlow01: 0.78, alarmState: "warning" }),
      frame(15, "转子惯性降速", { steamFlow01: 0, turbineRpm01: 0.7, shaftLoad01: 0.25, condenserFlow01: 0.62, alarmState: "warning" }),
      frame(35, "制动完成主要降速", { steamFlow01: 0, turbineRpm01: 0.2, shaftLoad01: 0, condenserFlow01: 0.42, alarmState: "normal" }),
      frame(50, "汽轮机安全停转", { steamFlow01: 0, turbineRpm01: 0.03, shaftLoad01: 0, condenserFlow01: 0.25, alarmState: "normal" }),
    ],
  },
  load_rejection: {
    description: "外部旋转应力负载突然消失，汽轮机短时超速后调速/制动介入。",
    durationSeconds: 45,
    keyframes: [
      frame(0, "主轴负载稳定", steady),
      frame(5, "负载突然切除", { turbineRpm01: 0.88, shaftLoad01: 0, steamFlow01: 0.88, alarmState: "warning" }),
      frame(12, "短时超速", { turbineRpm01: 0.98, shaftLoad01: 0.02, steamFlow01: 0.72, alarmState: "critical" }),
      frame(25, "调速与制动介入", { turbineRpm01: 0.85, shaftLoad01: 0.05, steamFlow01: 0.7, alarmState: "warning" }),
      frame(45, "负载重新稳定", { turbineRpm01: 0.82, shaftLoad01: 0.75, steamFlow01: 0.84, alarmState: "normal" }),
    ],
  },
};

export const PRESET_IDS = Object.freeze(Object.keys(presetDefinitions));
export const PRESETS = Object.freeze(Object.fromEntries(
  PRESET_IDS.map((id) => [id, createTimeline({ id, ...presetDefinitions[id] })]),
));

/**
 * 获取一个确定性预设时间线。预设 ID 是场景合同的一部分，未知 ID 立即拒绝。
 * @param {string} presetId 预设 ID。
 * @returns {object} 不可变时间线。
 */
export function getPreset(presetId) {
  if (!PRESETS[presetId]) {
    throw new RangeError(`未知机组预设：${presetId}。`);
  }
  return PRESETS[presetId];
}

/**
 * 直接采样预设，供渲染层只读消费，不在设备模型中复制状态规则。
 * @param {string} presetId 预设 ID。
 * @param {number} seconds 时间线秒数。
 * @returns {object} 标准化机组状态。
 */
export function samplePreset(presetId, seconds) {
  return sampleTimeline(getPreset(presetId), seconds);
}
