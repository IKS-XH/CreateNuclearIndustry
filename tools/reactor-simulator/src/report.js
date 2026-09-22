import {
  DEFAULT_CONFIG,
  DEFAULT_GEOMETRY,
  RULE_VERSION,
  SCHEMA_ID,
  SCHEMA_VERSION,
  TOOL_VERSION,
  requireValidConfig,
  createResultMetadata,
  validateConfig,
} from "./config.js";
import {
  createDepthMatrix,
  createPreset,
  normalizeDepths,
  validateDepthMatrix,
  validateGeometry,
  validateLayout,
} from "./layout.js";
import { snapshotToPlain } from "./simulation.js";

export const INITIAL_STATE_DEFAULT = Object.freeze({ running: false, scram: false });

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

export function buildScene({
  geometry,
  layout,
  controlRodDepths,
  config,
  initialState = INITIAL_STATE_DEFAULT,
  initialSnapshot,
  finalSnapshot,
  history = [],
  summary = null,
  ruleVersion = RULE_VERSION,
  resultMetadata,
}) {
  config = requireValidConfig(config);
  // 历史结果沿用来源标签；导出工具升级不等于用新规则重新运行。
  const metadata = resultMetadata ?? summary?.resultMetadata
    ?? history.at(-1)?.summary?.resultMetadata ?? createResultMetadata(config, ruleVersion);
  return {
    schema: SCHEMA_ID,
    schemaVersion: SCHEMA_VERSION,
    toolVersion: TOOL_VERSION,
    ruleVersion: metadata.ruleVersion,
    resultMetadata: clone(metadata),
    geometry: clone(geometry),
    config,
    layout: { matrix: clone(layout) },
    controlRodDepths: clone(controlRodDepths),
    initialState: {
      running: Boolean(initialState.running),
      scram: Boolean(initialState.scram),
    },
    runTicks: finalSnapshot?.tick ?? 0,
    initialSnapshot: initialSnapshot ? snapshotToPlain(initialSnapshot) : null,
    finalSnapshot: finalSnapshot ? snapshotToPlain(finalSnapshot) : null,
    summary: summary ? clone(summary) : null,
    history: clone(history),
  };
}

export function parseSceneText(text, fallback = {}) {
  let raw;
  try {
    raw = JSON.parse(text);
  } catch (error) {
    return { ok: false, errors: [`JSON 解析失败：${error.message}`] };
  }
  if (!raw || typeof raw !== "object" || Array.isArray(raw)) {
    return { ok: false, errors: ["场景必须是 JSON 对象"] };
  }
  if (raw.schema && raw.schema !== SCHEMA_ID) {
    return { ok: false, errors: [`不支持的场景 schema：${raw.schema}`] };
  }
  if (raw.schemaVersion != null && Number(raw.schemaVersion) !== SCHEMA_VERSION) {
    return { ok: false, errors: [`不支持的场景版本：${raw.schemaVersion}`] };
  }

  const geometryResult = validateGeometry({ ...DEFAULT_GEOMETRY, ...(raw.geometry ?? {}) });
  if (!geometryResult.ok) return { ok: false, errors: geometryResult.errors };
  const geometry = geometryResult.geometry;
  const configResult = validateConfig({ ...DEFAULT_CONFIG, ...(raw.config ?? {}) });
  if (!configResult.ok) return { ok: false, errors: configResult.errors };
  const config = configResult.value;
  const matrix = raw.layout?.matrix ?? raw.layout ?? fallback.layout ?? createPreset(geometry, "mixed");
  const layoutResult = validateLayout(matrix, geometry);
  if (!layoutResult.ok) return { ok: false, errors: layoutResult.errors };
  const layout = clone(matrix);
  const defaultDepths = createDepthMatrix(geometry, layout);
  const depths = normalizeDepths(raw.controlRodDepths ?? defaultDepths, layout);
  const depthResult = validateDepthMatrix(depths, layout, geometry);
  if (!depthResult.ok) return { ok: false, errors: depthResult.errors };
  const initialState = {
    ...INITIAL_STATE_DEFAULT,
    ...(raw.initialState ?? {}),
  };
  const initialSnapshot = raw.initialSnapshot ?? null;
  const finalSnapshot = raw.finalSnapshot ?? null;
  // 无版本的旧文件保留“未知”，不把补默认的续算配置伪装成旧记录实测值。
  const resultMetadata = raw.resultMetadata ?? raw.summary?.resultMetadata
    ?? createResultMetadata(config, raw.ruleVersion ?? "unknown", raw.toolVersion ?? "unknown");
  return {
    ok: true,
    scene: {
      schema: SCHEMA_ID,
      schemaVersion: SCHEMA_VERSION,
      toolVersion: raw.toolVersion ?? "unknown",
      ruleVersion: resultMetadata.ruleVersion,
      resultMetadata: clone(resultMetadata),
      geometry,
      config,
      layout: { matrix: layout },
      controlRodDepths: depths,
      initialState: { running: Boolean(initialState.running), scram: Boolean(initialState.scram) },
      runTicks: Number.isInteger(Number(raw.runTicks)) ? Math.max(0, Number(raw.runTicks)) : 0,
      initialSnapshot,
      finalSnapshot,
      summary: raw.summary ?? null,
      history: Array.isArray(raw.history) ? raw.history : [],
    },
  };
}

function csvCell(value) {
  if (value == null) return "";
  const text = typeof value === "number"
    ? value.toLocaleString("en-US", { useGrouping: false, maximumFractionDigits: 20 })
    : String(value);
  return /[",\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
}

function csvRow(values) {
  return values.map(csvCell).join(",");
}

/**
 * 导出逐 tick 和列结果。末尾同时记录实测终点与场景续算终点；旧记录未记录的值留空。
 * 按表头定位字段，保证稀疏的 tick/列/元数据行不会把参数写入其他列。
 */
export function buildCsv(history, finalSnapshot, finalSummary, ruleVersion = RULE_VERSION,
  config = DEFAULT_CONFIG, resultMetadata) {
  config = requireValidConfig(config);
  const metadata = resultMetadata ?? finalSummary?.resultMetadata
    ?? history.at(-1)?.summary?.resultMetadata ?? createResultMetadata(config, ruleVersion);
  const headers = [
    "row_type", "tick", "time_s", "total_heat_HU_per_t", "total_burn_equiv_per_t",
    "coolant_demand_mB_per_t", "cold_in_mB_per_t", "hot_out_mB_per_t", "total_residual_heat_HU",
    "max_net_heat_HU_per_t", "minimum_integrity_ratio", "damaged_columns", "jammed_columns",
    "exhausted_fuel_columns", "meltdown_progress_ticks", "meltdown_status", "rule_version",
    "rule_converged", "column_key", "x", "z", "column_type", "generated_heat_HU_per_t",
    "removed_heat_HU_per_t", "net_heat_HU_per_t", "integrity_ratio", "fuel_remaining_equiv",
    "cached_heat_HU", "control_depth_ratio", "control_rod_integrity_ratio", "jammed",
    "coolant_total_capacity_mB", "cold_inventory_mB", "hot_inventory_mB", "total_coolant_mB",
    "coolant_headroom_mB", "coolant_ledger_error_mB",
    "fuelColumnDamageHeatMultiplier", "fuelColumnDamageBurnMultiplier", "result_tool_version",
    "replay_rule_version", "replay_fuelColumnDamageHeatMultiplier", "replay_fuelColumnDamageBurnMultiplier",
    "damage_heat_multiplier", "damage_burn_multiplier", "source_rule_version", "source_tick",
  ];
  const lines = [csvRow(headers)];
  const append = (row, record = metadata) => {
    const values = {
      ...row,
      rule_version: record.ruleVersion,
      fuelColumnDamageHeatMultiplier: record.fuelColumnDamageHeatMultiplier,
      fuelColumnDamageBurnMultiplier: record.fuelColumnDamageBurnMultiplier,
      result_tool_version: record.toolVersion,
      replay_rule_version: RULE_VERSION,
      replay_fuelColumnDamageHeatMultiplier: config.fuelColumnDamageHeatMultiplier,
      replay_fuelColumnDamageBurnMultiplier: config.fuelColumnDamageBurnMultiplier,
      source_rule_version: record.sourceRuleVersion,
      source_tick: record.sourceTick,
    };
    lines.push(csvRow(headers.map((key) => values[key])));
  };
  const coolant = (summary) => ({
    coolant_total_capacity_mB: summary?.coolantTotalCapacityMb,
    cold_inventory_mB: summary?.coldCoolantMb,
    hot_inventory_mB: summary?.hotCoolantMb,
    total_coolant_mB: summary?.totalCoolantMb,
    coolant_headroom_mB: summary?.coolantCapacityHeadroomMb,
    coolant_ledger_error_mB: summary?.coolantLedgerError,
  });
  append({ row_type: "metadata" });
  for (const entry of history) {
    const summary = entry.summary ?? entry;
    append({
      row_type: "tick", tick: summary.tick, time_s: summary.gameSeconds,
      total_heat_HU_per_t: summary.generatedHeat ?? summary.totalHeat ?? 0,
      total_burn_equiv_per_t: summary.fuelConsumed ?? summary.totalBurn ?? 0,
      coolant_demand_mB_per_t: summary.coolantDemand ?? 0,
      cold_in_mB_per_t: summary.coldIn ?? 0, hot_out_mB_per_t: summary.hotOut ?? 0,
      total_residual_heat_HU: summary.totalResidualHeat ?? 0,
      max_net_heat_HU_per_t: summary.maxNetHeatLoad ?? 0, minimum_integrity_ratio: summary.minIntegrity ?? 1,
      damaged_columns: summary.damagedColumns ?? 0, jammed_columns: summary.jammedColumns ?? 0,
      exhausted_fuel_columns: summary.exhaustedFuelColumns ?? 0,
      meltdown_progress_ticks: summary.meltdownProgress ?? 0,
      meltdown_status: summary.meltdownMelted ? "melted" : summary.meltdownTriggered ? "countdown" : "normal",
      rule_converged: summary.ruleConverged !== false,
      ...coolant(summary),
    }, summary.resultMetadata ?? metadata);
  }
  for (const column of [...(finalSnapshot?.columns ?? [])].sort((a, b) => a.z - b.z || a.x - b.x)) {
    const result = history.at(-1)?.columns?.[column.key] ?? {};
    append({
      row_type: "column", tick: finalSnapshot?.tick ?? 0, time_s: (finalSnapshot?.tick ?? 0) / 20,
      rule_converged: history.at(-1)?.summary?.ruleConverged !== false,
      column_key: column.key, x: column.x, z: column.z, column_type: column.type,
      generated_heat_HU_per_t: result.generatedHeat ?? 0, removed_heat_HU_per_t: result.removedHeat ?? 0,
      net_heat_HU_per_t: result.netHeatLoad ?? column.cachedHeat ?? 0,
      integrity_ratio: column.type === "fuel" ? column.integrity : column.type === "control_rod" ? column.controlRodIntegrity : "",
      fuel_remaining_equiv: column.type === "fuel" ? column.fuelRemaining : "",
      cached_heat_HU: column.cachedHeat ?? 0,
      control_depth_ratio: column.type === "control_rod" ? column.depth : "",
      control_rod_integrity_ratio: column.type === "control_rod" ? column.controlRodIntegrity : "",
      jammed: column.type === "control_rod" ? column.jammed : "",
      damage_heat_multiplier: result.damageHeatMultiplier,
      damage_burn_multiplier: result.damageBurnMultiplier,
      ...coolant(finalSummary),
    });
  }
  return `${lines.join("\n")}\n`;
}

export function sceneJson(scene) {
  return JSON.stringify(scene, null, 2);
}

export function downloadText(filename, text, mime = "text/plain;charset=utf-8") {
  const blob = new Blob([text], { type: mime });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.append(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 0);
}
