export const TOOL_VERSION = "1.2.0";
export const SCHEMA_ID = "create-nuclear-industry/reactor-simulator";
export const SCHEMA_VERSION = 1;
export const RULE_VERSION = "P1.2-simulator-4";
export const TICKS_PER_SECOND = 20;

export const LIMITS = Object.freeze({
  minDimension: 3,
  maxDimension: 31,
  maxFuelColumns: 841,
  maxInternalHeight: 29,
  maxFeedbackIterations: 256,
  feedbackEpsilon: 1e-9,
});

export const DEFAULT_GEOMETRY = Object.freeze({ length: 5, width: 5, height: 5 });

// These are the P1 prototype values from reactor-local-control-revision-design.md.
// Values not frozen by that document are explicitly marked as tool experiment defaults.
export const DEFAULT_CONFIG = Object.freeze({
  controlResponseExponent: 1.0,
  baseHeatPerFuel: 1.0,
  burnHoursPerBlock: 3.0,
  fuelCapacityPerBlock: 1.0,
  overclockHeatMultiplier: 10.0,
  overclockBurnMultiplier: 10.0,
  overclockFeedbackGain: 0.15,
  overclockFeedbackExponent: 0.5,
  totalHeatMultiplierCap: 20.0,
  coldPortCount: 2,
  hotPortCount: 2,
  coolantMaxFlowPerPort: 128.0,
  coolantAbsorptionHuPerMb: 0.5,
  actualColdIn: 10000000.0,
  actualHotOut: 10000000.0,
  fuelColumnDamageHeatThreshold: 0.25,
  fuelColumnDamageRate: 0.0000005,
  fuelColumnDamageTransferRate: 0.25,
  controlRodColumnFailureThreshold: 0.0,
  meltdownTriggerFraction: 0.20,
  meltdownCountdownTicks: 900,
  safeResidualHeatThreshold: 0.25,
  tickStep: 20,
  runTicks: 1000,
});

export const CONFIG_FIELDS = Object.freeze([
  {
    group: "控制棒",
    key: "controlResponseExponent",
    label: "控制响应指数",
    unit: "无量纲",
    min: 0.01,
    max: 20,
    step: 0.01,
    frozen: false,
  },
  {
    group: "发热与燃耗",
    key: "baseHeatPerFuel",
    label: "baseHeatPerFuel",
    unit: "HU/t",
    min: 0,
    max: 1e6,
    step: 0.1,
    frozen: true,
  },
  {
    group: "发热与燃耗",
    key: "burnHoursPerBlock",
    label: "burnHoursPerBlock",
    unit: "h",
    min: 0.001,
    max: 1e6,
    step: 0.1,
    frozen: true,
  },
  {
    group: "发热与燃耗",
    key: "fuelCapacityPerBlock",
    label: "fuelCapacityPerBlock",
    unit: "列等价份",
    min: 0.000001,
    max: 1e6,
    step: 0.1,
    frozen: false,
  },
  {
    group: "发热与燃耗",
    key: "overclockHeatMultiplier",
    label: "overclockHeatMultiplier",
    unit: "×",
    min: 0,
    max: 1e4,
    step: 0.1,
    frozen: false,
  },
  {
    group: "发热与燃耗",
    key: "overclockBurnMultiplier",
    label: "overclockBurnMultiplier",
    unit: "×",
    min: 0,
    max: 1e4,
    step: 0.1,
    frozen: false,
  },
  {
    group: "发热与燃耗",
    key: "overclockFeedbackGain",
    label: "overclockFeedbackGain",
    unit: "无量纲",
    min: 0,
    max: 1e4,
    step: 0.01,
    frozen: false,
  },
  {
    group: "发热与燃耗",
    key: "overclockFeedbackExponent",
    label: "overclockFeedbackExponent",
    unit: "无量纲",
    min: 0.01,
    max: 20,
    step: 0.01,
    frozen: false,
  },
  {
    group: "发热与燃耗",
    key: "totalHeatMultiplierCap",
    label: "totalHeatMultiplierCap",
    unit: "×",
    min: 0.01,
    max: 1e4,
    step: 0.1,
    frozen: false,
  },
  {
    group: "冷却剂端口",
    key: "coldPortCount",
    label: "冷端口数量",
    unit: "个",
    min: 0,
    max: 100,
    step: 1,
    integer: true,
    sceneInputId: "cold-port-count-input",
    frozen: false,
  },
  {
    group: "冷却剂端口",
    key: "hotPortCount",
    label: "热端口数量",
    unit: "个",
    min: 0,
    max: 100,
    step: 1,
    integer: true,
    sceneInputId: "hot-port-count-input",
    frozen: false,
  },
  {
    group: "冷却剂端口",
    key: "coolantMaxFlowPerPort",
    label: "单端口流量上限",
    unit: "mB/t",
    min: 0,
    max: 1e6,
    step: 1,
    frozen: true,
  },
  {
    group: "冷却剂端口",
    key: "coolantAbsorptionHuPerMb",
    label: "coolantAbsorptionHuPerMb",
    unit: "HU/mB",
    min: 0.000001,
    max: 1e6,
    step: 0.1,
    frozen: true,
  },
  {
    group: "冷却剂端口",
    key: "actualColdIn",
    label: "本 tick 冷端实际输入",
    unit: "mB/t",
    min: 0,
    max: 1e7,
    step: 1,
    frozen: false,
  },
  {
    group: "冷却剂端口",
    key: "actualHotOut",
    label: "本 tick 热端实际接收",
    unit: "mB/t",
    min: 0,
    max: 1e7,
    step: 1,
    frozen: false,
  },
  {
    group: "损伤与融毁",
    key: "fuelColumnDamageHeatThreshold",
    label: "燃料列损伤热阈值",
    unit: "HU/t",
    min: 0,
    max: 1e7,
    step: 0.01,
    frozen: true,
  },
  {
    group: "损伤与融毁",
    key: "fuelColumnDamageRate",
    label: "燃料列损伤速率",
    unit: "1/(tick·HU/t)",
    min: 0,
    max: 1e4,
    step: 0.0000001,
    frozen: true,
  },
  {
    group: "损伤与融毁",
    key: "fuelColumnDamageTransferRate",
    label: "失效列传播率",
    unit: "比例",
    min: 0,
    max: 1,
    step: 0.01,
    frozen: true,
  },
  {
    group: "损伤与融毁",
    key: "controlRodColumnFailureThreshold",
    label: "控制棒卡死阈值",
    unit: "完整度",
    min: 0,
    max: 1,
    step: 0.01,
    frozen: true,
  },
  {
    group: "损伤与融毁",
    key: "meltdownTriggerFraction",
    label: "融毁触达比例",
    unit: "比例",
    min: 0,
    max: 1,
    step: 0.01,
    frozen: true,
  },
  {
    group: "损伤与融毁",
    key: "meltdownCountdownTicks",
    label: "融毁倒计时",
    unit: "ticks",
    min: 1,
    max: 1e7,
    step: 1,
    integer: true,
    frozen: true,
  },
  {
    group: "损伤与融毁",
    key: "safeResidualHeatThreshold",
    label: "余热安全阈值",
    unit: "HU/t",
    min: 0,
    max: 1e7,
    step: 0.01,
    frozen: false,
  },
]);

const fieldByKey = new Map(CONFIG_FIELDS.map((field) => [field.key, field]));

export function baseBurnPerFuel(config) {
  return config.fuelCapacityPerBlock / (config.burnHoursPerBlock * 3600 * TICKS_PER_SECOND);
}

export function cloneConfig(config = DEFAULT_CONFIG) {
  const value = { ...DEFAULT_CONFIG };
  for (const field of CONFIG_FIELDS) {
    if (Object.prototype.hasOwnProperty.call(config, field.key)) value[field.key] = config[field.key];
  }
  return value;
}

export function validateConfig(raw = {}) {
  const value = cloneConfig(raw);
  const errors = [];
  for (const field of CONFIG_FIELDS) {
    const number = Number(value[field.key]);
    if (!Number.isFinite(number)) {
      errors.push(`${field.label} 必须是有限数字`);
      continue;
    }
    if (number < field.min || number > field.max) {
      errors.push(`${field.label} 必须在 ${field.min}～${field.max} ${field.unit} 范围内`);
    }
    if (field.integer && !Number.isInteger(number)) {
      errors.push(`${field.label} 必须是整数`);
    }
    value[field.key] = field.integer ? Math.trunc(number) : number;
  }
  if (value.meltdownTriggerFraction < 0 || value.meltdownTriggerFraction > 1) {
    errors.push("meltdownTriggerFraction 必须在 0～1 之间");
  }
  return { ok: errors.length === 0, value, errors };
}

export function fieldDefinition(key) {
  return fieldByKey.get(key);
}

export function formatNumber(number, digits = 3) {
  if (!Number.isFinite(number)) return "—";
  const absolute = Math.abs(number);
  const decimalDigits = absolute !== 0 && (absolute >= 1e6 || absolute < 1e-4)
    ? Math.max(digits, 15)
    : digits;
  return number.toLocaleString("zh-CN", { maximumFractionDigits: decimalDigits });
}

export function formatDecimal(number, digits = 12) {
  if (!Number.isFinite(number)) return "—";
  return number.toLocaleString("zh-CN", { maximumFractionDigits: digits });
}
