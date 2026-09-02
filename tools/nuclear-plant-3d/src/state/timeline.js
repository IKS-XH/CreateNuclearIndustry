import {
  BOOLEAN_STATE_FIELDS,
  ENUM_STATE_FIELDS,
  NUMERIC_STATE_FIELDS,
  assertPlantState,
  clonePlantState,
} from "./plant-state.js";

function isPlainObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function freezeKeyframe(keyframe) {
  return Object.freeze({
    atSeconds: keyframe.atSeconds,
    label: keyframe.label,
    state: Object.freeze(clonePlantState(keyframe.state)),
  });
}

/**
 * 创建确定性状态时间线。关键帧按秒排序，第一帧必须从 0 秒开始，时间线不依赖墙上时钟或随机数。
 * @param {{id:string, description:string, durationSeconds:number, keyframes:Array}} definition 时间线定义。
 * @returns {{id:string,description:string,durationSeconds:number,keyframes:Array}} 不可变时间线。
 */
export function createTimeline(definition) {
  if (!isPlainObject(definition) || typeof definition.id !== "string" || definition.id.length === 0) {
    throw new TypeError("时间线必须包含非空 id。");
  }
  if (typeof definition.description !== "string" || definition.description.length === 0) {
    throw new TypeError(`${definition.id} 必须包含中文说明。`);
  }
  if (!Number.isFinite(definition.durationSeconds) || definition.durationSeconds <= 0) {
    throw new RangeError(`${definition.id} 的 durationSeconds 必须是正数。`);
  }
  if (!Array.isArray(definition.keyframes) || definition.keyframes.length < 2) {
    throw new TypeError(`${definition.id} 至少需要两个关键帧。`);
  }

  let previousTime = -1;
  const keyframes = definition.keyframes.map((keyframe, index) => {
    if (!isPlainObject(keyframe) || !Number.isFinite(keyframe.atSeconds) || keyframe.atSeconds < 0) {
      throw new TypeError(`${definition.id} 的第 ${index + 1} 个关键帧时间无效。`);
    }
    if (keyframe.atSeconds <= previousTime) {
      throw new RangeError(`${definition.id} 的关键帧时间必须严格递增。`);
    }
    previousTime = keyframe.atSeconds;
    if (typeof keyframe.label !== "string" || keyframe.label.length === 0) {
      throw new TypeError(`${definition.id} 的关键帧必须包含中文 label。`);
    }
    assertPlantState(keyframe.state);
    return freezeKeyframe(keyframe);
  });

  if (keyframes[0].atSeconds !== 0) {
    throw new RangeError(`${definition.id} 的第一帧必须从 0 秒开始。`);
  }
  if (keyframes.at(-1).atSeconds > definition.durationSeconds) {
    throw new RangeError(`${definition.id} 的最后关键帧不能晚于 durationSeconds。`);
  }

  return Object.freeze({
    id: definition.id,
    description: definition.description,
    durationSeconds: definition.durationSeconds,
    keyframes: Object.freeze(keyframes),
  });
}

function interpolateState(left, right, ratio) {
  const state = {};
  for (const field of NUMERIC_STATE_FIELDS) {
    state[field] = left[field] + (right[field] - left[field]) * ratio;
  }
  for (const field of [...ENUM_STATE_FIELDS, ...BOOLEAN_STATE_FIELDS]) {
    state[field] = ratio >= 1 ? right[field] : left[field];
  }
  return state;
}

/**
 * 在确定性时间线上采样状态。数值字段线性插值，警报和 SCRAM 在关键帧边界切换。
 * @param {object} timeline `createTimeline` 生成的时间线。
 * @param {number} seconds 从时间线起点开始的秒数，超出范围时钳制到首尾状态。
 * @returns {object} 独立的标准化机组状态。
 */
export function sampleTimeline(timeline, seconds) {
  if (!isPlainObject(timeline) || !Array.isArray(timeline.keyframes)) {
    throw new TypeError("采样目标必须是有效时间线。");
  }
  if (!Number.isFinite(seconds)) {
    throw new RangeError("时间线采样秒数必须是有限数值。");
  }

  const keyframes = timeline.keyframes;
  if (seconds <= keyframes[0].atSeconds) {
    return clonePlantState(keyframes[0].state);
  }
  if (seconds >= keyframes.at(-1).atSeconds) {
    return clonePlantState(keyframes.at(-1).state);
  }

  const rightIndex = keyframes.findIndex((keyframe) => keyframe.atSeconds > seconds);
  const left = keyframes[rightIndex - 1];
  const right = keyframes[rightIndex];
  const ratio = (seconds - left.atSeconds) / (right.atSeconds - left.atSeconds);
  return interpolateState(left.state, right.state, ratio);
}
