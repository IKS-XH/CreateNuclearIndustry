/**
 * 机组可视化状态合同。该模块只处理无量纲状态，不读取或修改 Minecraft 服务端状态。
 */

export const STATE_SCHEMA_VERSION = "1.0.0";
export const ALARM_STATES = Object.freeze(["normal", "warning", "critical"]);
export const NUMERIC_STATE_FIELDS = Object.freeze([
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
]);
export const NORMALIZED_STATE_FIELDS = Object.freeze(NUMERIC_STATE_FIELDS.slice(1));
export const BOOLEAN_STATE_FIELDS = Object.freeze(["scramActive"]);
export const ENUM_STATE_FIELDS = Object.freeze(["alarmState"]);

export const DEFAULT_PLANT_STATE = Object.freeze({
  timeScale: 1,
  reactorPower01: 0.92,
  controlRodDepth01: 0.18,
  primaryCoolantFlow01: 0.94,
  heatExchangerLoad01: 0.88,
  boilerWaterLevel01: 0.82,
  boilerPressure01: 0.78,
  steamFlow01: 0.86,
  turbineRpm01: 0.82,
  shaftLoad01: 0.78,
  condenserFlow01: 0.86,
  alarmState: "normal",
  scramActive: false,
});

const STATE_FIELDS = Object.freeze([
  ...NUMERIC_STATE_FIELDS,
  ...ENUM_STATE_FIELDS,
  ...BOOLEAN_STATE_FIELDS,
]);

function isPlainObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

/**
 * 校验完整机组状态。输入必须包含全部合同字段，所有 `01` 数值必须位于 [0,1]。
 * @param {unknown} state 待校验的客户端可视化状态。
 * @returns {true} 状态合法；非法输入抛出带中文原因的异常。
 */
export function assertPlantState(state) {
  if (!isPlainObject(state)) {
    throw new TypeError("机组状态必须是普通对象。");
  }

  const actualFields = Object.keys(state).sort();
  const expectedFields = STATE_FIELDS.slice().sort();
  if (actualFields.length !== expectedFields.length || actualFields.some((field, index) => field !== expectedFields[index])) {
    throw new TypeError("机组状态字段必须与 1.0.0 合同完全一致。");
  }

  for (const field of NUMERIC_STATE_FIELDS) {
    const value = state[field];
    if (!Number.isFinite(value)) {
      throw new TypeError(`${field} 必须是有限数值。`);
    }
    const [minimum, maximum] = field === "timeScale" ? [0, 4] : [0, 1];
    if (value < minimum || value > maximum) {
      throw new RangeError(`${field} 必须位于 [${minimum}, ${maximum}]。`);
    }
  }

  if (!ALARM_STATES.includes(state.alarmState)) {
    throw new TypeError("alarmState 必须是 normal、warning 或 critical。");
  }
  if (typeof state.scramActive !== "boolean") {
    throw new TypeError("scramActive 必须是布尔值。");
  }
  return true;
}

/**
 * 从稳态和覆盖字段创建独立状态快照。该函数不改变调用方对象，供时间线建立确定性关键帧。
 * @param {Partial<typeof DEFAULT_PLANT_STATE>} overrides 要覆盖的合同字段。
 * @returns {object} 经过完整合同校验的机组状态快照。
 */
export function createPlantState(overrides = {}) {
  if (!isPlainObject(overrides)) {
    throw new TypeError("机组状态覆盖值必须是普通对象。");
  }
  const unknownFields = Object.keys(overrides).filter((field) => !STATE_FIELDS.includes(field));
  if (unknownFields.length > 0) {
    throw new TypeError(`机组状态包含未知字段：${unknownFields.join("、")}。`);
  }
  const state = { ...DEFAULT_PLANT_STATE, ...overrides };
  assertPlantState(state);
  return state;
}

/**
 * 复制并校验状态，避免时间线采样结果与关键帧共享可变对象。
 * @param {object} state 已经符合合同的状态。
 * @returns {object} 独立的状态副本。
 */
export function clonePlantState(state) {
  assertPlantState(state);
  return { ...state };
}
