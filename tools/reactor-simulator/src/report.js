import {
  DEFAULT_CONFIG,
  DEFAULT_GEOMETRY,
  RULE_VERSION,
  SCHEMA_ID,
  SCHEMA_VERSION,
  TOOL_VERSION,
  cloneConfig,
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
}) {
  return {
    schema: SCHEMA_ID,
    schemaVersion: SCHEMA_VERSION,
    toolVersion: TOOL_VERSION,
    ruleVersion: RULE_VERSION,
    geometry: clone(geometry),
    config: cloneConfig(config),
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
  return {
    ok: true,
    scene: {
      schema: SCHEMA_ID,
      schemaVersion: SCHEMA_VERSION,
      toolVersion: raw.toolVersion ?? TOOL_VERSION,
      ruleVersion: raw.ruleVersion ?? RULE_VERSION,
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

export function buildCsv(history, finalSnapshot, finalSummary, ruleVersion = RULE_VERSION) {
  const lines = [];
  lines.push(csvRow([
    "row_type", "tick", "time_s", "total_heat_HU_per_t", "total_burn_equiv_per_t",
    "coolant_demand_mB_per_t", "cold_in_mB_per_t", "hot_out_mB_per_t", "total_residual_heat_HU",
    "max_net_heat_HU_per_t", "minimum_integrity_ratio", "damaged_columns", "jammed_columns",
    "exhausted_fuel_columns", "meltdown_progress_ticks", "meltdown_status", "rule_version",
    "rule_converged", "column_key", "x", "z", "column_type", "generated_heat_HU_per_t",
    "removed_heat_HU_per_t", "net_heat_HU_per_t", "integrity_ratio", "fuel_remaining_equiv",
    "cached_heat_HU", "control_depth_ratio", "control_rod_integrity_ratio", "jammed",
    "coolant_total_capacity_mB", "cold_inventory_mB", "hot_inventory_mB", "total_coolant_mB",
    "coolant_headroom_mB", "coolant_ledger_error_mB",
  ]));
  for (const entry of history) {
    const summary = entry.summary ?? entry;
    lines.push(csvRow([
      "tick", summary.tick, summary.gameSeconds, summary.generatedHeat ?? summary.totalHeat ?? 0,
      summary.fuelConsumed ?? summary.totalBurn ?? 0, summary.coolantDemand ?? 0,
      summary.coldIn ?? 0, summary.hotOut ?? 0, summary.totalResidualHeat ?? 0,
      summary.maxNetHeatLoad ?? 0, summary.minIntegrity ?? 1, summary.damagedColumns ?? 0,
      summary.jammedColumns ?? 0, summary.exhaustedFuelColumns ?? 0, summary.meltdownProgress ?? 0,
      summary.meltdownMelted ? "melted" : summary.meltdownTriggered ? "countdown" : "normal",
      ruleVersion, summary.ruleConverged !== false,
      summary.coolantTotalCapacityMb ?? "", summary.coldCoolantMb ?? "", summary.hotCoolantMb ?? "",
      summary.totalCoolantMb ?? "", summary.coolantCapacityHeadroomMb ?? "", summary.coolantLedgerError ?? "",
    ]));
  }
  for (const column of [...(finalSnapshot?.columns ?? [])].sort((a, b) => a.z - b.z || a.x - b.x)) {
    const result = history.at(-1)?.columns?.[column.key] ?? {};
    lines.push(csvRow([
      "column", finalSnapshot?.tick ?? 0, (finalSnapshot?.tick ?? 0) / 20, "", "", "", "", "", "", "", "", "", "", "", "", "",
      ruleVersion, history.at(-1)?.summary?.ruleConverged !== false, column.key, column.x, column.z,
      column.type, result.generatedHeat ?? 0, result.removedHeat ?? 0, result.netHeatLoad ?? column.cachedHeat ?? 0,
      column.type === "fuel" ? column.integrity : column.type === "control_rod" ? column.controlRodIntegrity : "",
      column.type === "fuel" ? column.fuelRemaining : "", column.cachedHeat ?? 0,
      column.type === "control_rod" ? column.depth : "", column.type === "control_rod" ? column.controlRodIntegrity : "",
      column.type === "control_rod" ? column.jammed : "",
      finalSummary?.coolantTotalCapacityMb ?? "", finalSummary?.coldCoolantMb ?? "", finalSummary?.hotCoolantMb ?? "",
      finalSummary?.totalCoolantMb ?? "", finalSummary?.coolantCapacityHeadroomMb ?? "", finalSummary?.coolantLedgerError ?? "",
    ]));
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
