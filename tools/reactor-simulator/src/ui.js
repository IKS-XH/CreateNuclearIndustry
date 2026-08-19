import {
  CONFIG_FIELDS,
  DEFAULT_CONFIG,
  DEFAULT_GEOMETRY,
  RULE_VERSION,
  SCHEMA_VERSION,
  TOOL_VERSION,
  cloneConfig,
  formatNumber,
  baseBurnPerFuel,
  validateConfig,
} from "./config.js";
import {
  COLUMN_LABELS,
  createDepthMatrix,
  createMatrix,
  createPreset,
  countColumns,
  internalDimensions,
  normalizeDepths,
  validateDepthMatrix,
  validateGeometry,
  validateLayout,
} from "./layout.js";
import {
  applyControlDepths,
  cloneSnapshot,
  createInitialSnapshot,
  getColumn,
  repairColumn,
  runTicks,
  setControlDepth,
  snapshotToPlain,
  statusLabel,
  summarizeSnapshot,
  stepSnapshot,
} from "./simulation.js";
import {
  buildCsv,
  buildScene,
  downloadText,
  parseSceneText,
  sceneJson,
} from "./report.js";

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];

const state = {
  geometry: { ...DEFAULT_GEOMETRY },
  layout: createPreset(DEFAULT_GEOMETRY, "mixed"),
  depths: null,
  pendingGeometry: { ...DEFAULT_GEOMETRY },
  pendingLayout: null,
  pendingDepths: null,
  config: cloneConfig(DEFAULT_CONFIG),
  initialState: { running: false, scram: false },
  initialSnapshot: null,
  snapshot: null,
  runtime: { running: false, scram: false },
  lastResult: null,
  history: [],
  selectedKey: null,
  brush: "fuel",
  painting: false,
  dirty: false,
  timer: null,
  runTargetTick: null,
};

function setStatus(message = "", tone = "warning") {
  const node = $("#app-status");
  node.textContent = message;
  node.dataset.tone = tone;
}

function markDirty(message = "有未应用的场景修改") {
  state.dirty = true;
  setStatus(`${message}；请暂停后点击“应用并重置”。`);
}

function pauseTimer() {
  if (state.timer != null) {
    clearInterval(state.timer);
    state.timer = null;
  }
  state.runtime.running = false;
}

function pendingGeometryFromForm() {
  return {
    length: Number($("#length-input").value),
    width: Number($("#width-input").value),
    height: Number($("#height-input").value),
  };
}

function updateDimensionHint(geometry) {
  const result = validateGeometry(geometry);
  const hint = $("#dimension-hint");
  if (!result.ok) {
    hint.textContent = result.errors.join("；");
    hint.classList.add("field-error");
    return false;
  }
  const dimensions = internalDimensions(result.geometry);
  hint.textContent = `有效横截面 ${dimensions.columns} × ${dimensions.rows} · 有效高度 ${dimensions.height} · 有效列数 ${dimensions.columns * dimensions.rows}`;
  hint.classList.remove("field-error");
  return true;
}

function renderParameterFields() {
  const root = $("#parameter-fields");
  root.replaceChildren();
  const groups = new Map();
  for (const field of CONFIG_FIELDS) {
    if (!groups.has(field.group)) groups.set(field.group, []);
    groups.get(field.group).push(field);
  }
  for (const [group, fields] of groups.entries()) {
    const section = document.createElement("section");
    section.className = `parameter-group${fields.some((field) => field.frozen) ? " frozen" : ""}`;
    const heading = document.createElement("h3");
    heading.textContent = group;
    section.append(heading);
    for (const field of fields) {
      const row = document.createElement("label");
      row.className = "parameter-row";
      row.title = field.frozen
        ? "设计文档 P1 原型默认；仍可用于网页实验，不会写回游戏配置。"
        : "工具实验默认值；不代表游戏服务器最终平衡。";
      const name = document.createElement("span");
      name.className = "param-name";
      name.innerHTML = `${field.label}<small>${field.unit}${field.frozen ? " · P1 原型" : " · 工具实验默认"}</small>`;
      const input = document.createElement("input");
      input.type = "number";
      input.id = `param-${field.key}`;
      input.dataset.configKey = field.key;
      input.min = String(field.min);
      input.max = String(field.max);
      input.step = String(field.step);
      input.value = String(state.config[field.key]);
      input.setAttribute("aria-label", `${field.label}，单位 ${field.unit}`);
      row.append(name, input);
      section.append(row);
    }
    root.append(section);
  }
  const burnNote = document.createElement("p");
  burnNote.id = "burn-rate-note";
  burnNote.className = "field-hint";
  root.append(burnNote);
  updateBurnRateNote();
}

function updateBurnRateNote() {
  const values = readConfigFromForm();
  const result = validateConfig(values);
  const burn = result.ok ? baseBurnPerFuel(result.value) : baseBurnPerFuel(state.config);
  const note = $("#burn-rate-note");
  if (note) note.textContent = `baseBurnPerFuel = ${formatNumber(burn, 9)} 列等价份/tick（由 burnHoursPerBlock 反推）`;
}

function readConfigFromForm() {
  const config = {};
  for (const field of CONFIG_FIELDS) config[field.key] = Number($(`#param-${field.key}`).value);
  return config;
}

function renderLayoutGrid(target = "pending") {
  const layout = target === "pending" ? state.pendingLayout : state.layout;
  const grid = target === "pending" ? $("#core-grid") : $("#result-grid");
  if (!grid || !layout) return;
  const columns = layout[0]?.length ?? 1;
  grid.style.gridTemplateColumns = `repeat(${columns}, minmax(0, 1fr))`;
  grid.replaceChildren();
  layout.forEach((row, z) => row.forEach((type, x) => {
    const cell = document.createElement(target === "pending" ? "button" : "button");
    cell.type = "button";
    cell.className = `${target === "pending" ? "core-cell" : "result-cell"} ${type}`;
    cell.dataset.x = String(x);
    cell.dataset.z = String(z);
    cell.dataset.key = `${x},${z}`;
    cell.setAttribute("role", "gridcell");
    cell.setAttribute("aria-label", `${x},${z}：${COLUMN_LABELS[type]}`);
    if (target === "pending") {
      cell.innerHTML = `<span class="cell-icon">${type === "fuel" ? "F" : type === "control_rod" ? "C" : "·"}</span><span class="coord">${x},${z}</span>`;
      cell.addEventListener("pointerdown", (event) => {
        event.preventDefault();
        state.painting = true;
        paintPendingCell(x, z);
        cell.setPointerCapture?.(event.pointerId);
      });
      cell.addEventListener("pointerenter", () => { if (state.painting) paintPendingCell(x, z); });
    } else {
      renderResultCell(cell, x, z, type);
      cell.addEventListener("click", () => selectColumn(`${x},${z}`));
    }
    grid.append(cell);
  }));
  if (target === "pending") {
    const counts = countColumns(layout);
    $("#layout-counts").textContent = `F ${counts.fuel} · C ${counts.control_rod} · 空 ${counts.empty}`;
  }
}

function renderResultCell(cell, x, z, type) {
  const column = getColumn(state.snapshot, `${x},${z}`);
  const result = state.lastResult?.columns?.[`${x},${z}`] ?? null;
  const layer = $("#grid-layer").value;
  const selected = state.selectedKey === `${x},${z}`;
  if (selected) cell.classList.add("selected");
  if (!column) return;
  let value = "—";
  let stateText = COLUMN_LABELS[type];
  if (type === "empty") {
    value = "阻断";
    stateText = "不发热 · 不传播";
  } else if (layer === "heat") {
    value = `${formatNumber(result?.generatedHeat ?? 0)} HU/t`;
    cell.style.setProperty("--heat", `${Math.min(100, (result?.generatedHeat ?? 0) / Math.max(1, state.lastResult?.summary?.generatedHeat ?? 1) * 100)}%`);
  } else if (layer === "integrity") {
    const integrity = type === "fuel" ? column.integrity : column.controlRodIntegrity;
    value = `${(integrity * 100).toFixed(1)}%`;
    stateText = integrity < 1 ? "损伤" : "完整";
  } else if (layer === "fuel") {
    value = type === "fuel" ? `${(column.fuelRemaining / column.fuelCapacity * 100).toFixed(1)}%` : "—";
    stateText = type === "fuel" && column.fuelRemaining <= 1e-12 ? "耗尽" : COLUMN_LABELS[type];
  } else if (layer === "netHeat") {
    value = `${formatNumber(result?.netHeatLoad ?? column.cachedHeat)} HU/t`;
    stateText = (result?.damage ?? 0) > 0 ? "超安全阈值" : "安全区";
  } else if (layer === "residual") {
    value = `${formatNumber(column.cachedHeat)} HU`;
    stateText = column.cachedHeat > 0 ? "有余热" : "无余热";
  } else if (layer === "rodDepth") {
    value = type === "control_rod" ? `${(column.depth * 100).toFixed(1)}%` : "—";
    stateText = type === "control_rod" && column.jammed ? "卡死" : COLUMN_LABELS[type];
  } else if (layer === "propagation") {
    value = `${formatNumber(result?.propagationHeatReceived ?? 0)} HU`;
    stateText = result?.propagationHeatReceived > 0 ? "本 tick 触达" : "未触达";
  }
  cell.innerHTML = `<span class="cell-title">${x},${z}</span><span class="cell-value">${value}</span><span class="cell-state">${stateText}</span>`;
  cell.setAttribute("aria-label", `${x},${z}，${COLUMN_LABELS[type]}，${value}，${stateText}`);
}

function paintPendingCell(x, z) {
  if (state.runtime.running) {
    pauseTimer();
    setStatus("布局编辑已暂停运行；点击“应用并重置”后才会进入新场景。");
  }
  const type = state.brush;
  state.pendingLayout[z][x] = type;
  if (type === "control_rod" && state.pendingDepths[z][x] == null) state.pendingDepths[z][x] = 1;
  if (type !== "control_rod") state.pendingDepths[z][x] = null;
  renderLayoutGrid("pending");
  renderRodEditor();
  markDirty("布局已修改");
}

function renderRodEditor() {
  const root = $("#rod-editor");
  root.replaceChildren();
  state.pendingLayout.forEach((row, z) => row.forEach((type, x) => {
    if (type !== "control_rod") return;
    const key = `${x},${z}`;
    const wrapper = document.createElement("label");
    wrapper.className = "rod-row";
    wrapper.innerHTML = `<span>棒 [${key}]</span>`;
    const input = document.createElement("input");
    input.type = "range";
    input.min = "0";
    input.max = "1";
    input.step = "0.01";
    input.value = String(state.pendingDepths[z][x] ?? 1);
    input.setAttribute("aria-label", `控制棒 ${key} 目标插入深度`);
    const output = document.createElement("output");
    output.textContent = `${(Number(input.value) * 100).toFixed(0)}%`;
    input.addEventListener("input", () => {
      if (state.runtime.running) {
        pauseTimer();
        setStatus("控制棒编辑已暂停运行；应用后重置以避免混用快照。");
      }
      state.pendingDepths[z][x] = Number(input.value);
      output.textContent = `${(Number(input.value) * 100).toFixed(0)}%`;
      const keyColumn = `${x},${z}`;
      if (state.layout.some((row) => row.some((cell) => cell === "control_rod")) && getColumn(state.snapshot, keyColumn)) {
        state.snapshot = setControlDepth(state.snapshot, keyColumn, Number(input.value));
      }
      markDirty("控制棒深度已修改");
      renderResultGridAndDetail();
    });
    wrapper.append(input, output);
    root.append(wrapper);
  }));
  if (!root.children.length) root.innerHTML = `<p class="muted">当前没有控制棒列；无控制棒是合法实验场景。</p>`;
}

function currentSummary() {
  return state.lastResult?.summary ?? summarizeSnapshot(state.snapshot, null, state.config, state.geometry);
}

function formatTime(seconds) {
  if (!Number.isFinite(seconds)) return "—";
  const whole = Math.max(0, Math.floor(seconds));
  const hours = Math.floor(whole / 3600);
  const minutes = Math.floor((whole % 3600) / 60);
  const rest = whole % 60;
  return `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(rest).padStart(2, "0")}`;
}

function renderOverview() {
  const summary = currentSummary();
  let status = statusLabel(state.snapshot, state.runtime, state.config);
  if (state.lastResult?.summary?.countdownPaused) status = "暂停倒计时";
  $("#status-pill").textContent = status;
  $("#status-pill").dataset.status = status;
  const metrics = [
    ["有效尺寸", `${internalDimensions(state.geometry).columns} × ${internalDimensions(state.geometry).rows} × ${internalDimensions(state.geometry).height}`, "列 × 列 × 高"],
    ["燃料 / 控制棒 / 空列", `${summary.fuelColumns} / ${summary.controlRodColumns} / ${summary.emptyColumns}`, "列"],
    ["当前 tick / 游戏时间", `${summary.tick} / ${formatTime(summary.gameSeconds)}`, "tick / h:m:s"],
    ["总发热", `${formatNumber(summary.generatedHeat ?? 0)} `, "HU/t"],
    ["总燃耗", `${formatNumber(summary.fuelConsumed ?? summary.plannedBurn ?? 0, 9)} `, "列等价份/t"],
    ["总余热", `${formatNumber(summary.totalResidualHeat ?? 0)} `, "HU"],
    ["冷却需求 / 换热转换", `${formatNumber(summary.coolantDemand ?? 0)} / ${formatNumber(summary.convertedCoolant ?? 0)}`, "mB/t"],
    ["冷端输入 / 热端输出", `${formatNumber(summary.coldInAccepted ?? summary.coldIn ?? 0)} / ${formatNumber(summary.hotOutActual ?? summary.hotOut ?? 0)}`, "mB/t"],
    ["内存冷 / 热冷却剂", `${formatNumber(summary.coldCoolantMb ?? 0)} / ${formatNumber(summary.hotCoolantMb ?? 0)}`, "mB"],
    ["总容量 / 剩余空间", `${formatNumber(summary.coolantTotalCapacityMb ?? 0)} / ${formatNumber(summary.coolantCapacityHeadroomMb ?? 0)}`, "mB"],
    ["最高净热负荷", `${formatNumber(summary.maxNetHeatLoad ?? 0)} `, "HU/t"],
    ["最低完整度", `${((summary.minIntegrity ?? 1) * 100).toFixed(2)}%`, "比例"],
    ["损坏 / 卡死 / 耗尽", `${summary.damagedColumns ?? 0} / ${summary.jammedColumns ?? 0} / ${summary.exhaustedFuelColumns ?? 0}`, "列"],
    ["融毁倒计时", `${summary.meltdownProgress ?? 0} / ${state.config.meltdownCountdownTicks}`, "tick"],
  ];
  $("#overview-grid").innerHTML = metrics.map(([label, value, unit]) => `<div class="metric"><span class="metric-label">${label}</span><span class="metric-value">${value}<small>${unit}</small></span></div>`).join("");
  const bottlenecks = summary.bottlenecks?.length ? summary.bottlenecks : ["尚未产生冷却账本"];
  $("#bottleneck-list").innerHTML = `<strong>当前端口 / 总流量瓶颈：</strong> ${bottlenecks.join("、")} · <strong>内存上限：</strong> ${formatNumber(summary.coolantTotalCapacityMb ?? 0)} mB`;
}

function renderWarnings() {
  const list = $("#warnings");
  const warnings = state.lastResult?.warnings ?? [];
  list.innerHTML = warnings.length ? warnings.map((warning) => `<li>${warning}</li>`).join("") : `<li class="muted">暂无警告</li>`;
}

function selectColumn(key) {
  state.selectedKey = key;
  renderResultGridAndDetail();
}

function renderResultGridAndDetail() {
  renderLayoutGrid("result");
  renderColumnDetails();
}

function renderColumnDetails() {
  const key = state.selectedKey;
  const column = key ? getColumn(state.snapshot, key) : null;
  const result = key ? state.lastResult?.columns?.[key] : null;
  $("#selected-cell").textContent = key ? `列 [${key}]` : "未选择";
  const root = $("#column-details");
  if (!column) {
    root.innerHTML = `<p class="muted">点击堆芯网格中的列。</p>`;
    return;
  }
  const neighbors = result?.neighbors ?? [];
  const neighborHtml = neighbors.length
    ? neighbors.map((neighbor) => `<span class="neighbor-chip">${neighbor.direction} [${neighbor.x},${neighbor.z}] · ${COLUMN_LABELS[neighbor.type]}</span>`).join("")
    : `<span class="muted">无四向邻居</span>`;
  if (column.type === "empty") {
    root.innerHTML = `<h3>[${key}] 空列</h3><p class="muted">不持有燃料、控制棒、完整度或余热；不发热、不冷却、不参与损伤，并完全切断四向反馈、控制和余热传播。</p><div class="neighbor-list">${neighborHtml}</div>`;
    return;
  }
  if (column.type === "control_rod") {
    root.innerHTML = `<h3>[${key}] 控制棒列</h3><dl class="detail-list">
      <div><dt>目标/实际插入深度</dt><dd>${(column.depth * 100).toFixed(1)}%${column.jammed ? "（卡死）" : ""}</dd></div>
      <div><dt>控制棒完整度</dt><dd>${(column.controlRodIntegrity * 100).toFixed(2)}%</dd></div>
      <div><dt>卡死前深度</dt><dd>${(column.jammedDepth * 100).toFixed(1)}%</dd></div>
      <div><dt>接收传播热量</dt><dd>${formatNumber(result?.propagationHeatReceived ?? 0)} HU</dd></div>
      <div><dt>当前余热缓存</dt><dd>${formatNumber(column.cachedHeat)} HU</dd></div>
      <div><dt>卡死原因</dt><dd>${column.jammed ? `完整度 ≤ ${state.config.controlRodColumnFailureThreshold}` : "未卡死"}</dd></div>
    </dl><div class="neighbor-list">${neighborHtml}</div>`;
    return;
  }
  const burnRate = result?.plannedBurn ?? 0;
  const ticksToEmpty = burnRate > 0 ? column.fuelRemaining / burnRate : Infinity;
  const integrity = column.integrity;
  root.innerHTML = `<h3>[${key}] 燃料列</h3><dl class="detail-list">
    <div><dt>当前发热</dt><dd>${formatNumber(result?.generatedHeat ?? 0)} HU/t</dd></div>
    <div><dt>控制强度</dt><dd>${formatNumber(result?.controlledIntensity ?? 0, 5)}×</dd></div>
    <div><dt>超频热/燃耗倍率</dt><dd>${formatNumber(result?.heatIntensity ?? 0, 5)}× / ${formatNumber(result?.burnIntensity ?? 0, 5)}×</dd></div>
    <div><dt>损伤倍率</dt><dd>${formatNumber(integrity <= 1e-12 ? 0 : 2 - integrity, 5)}×</dd></div>
    <div><dt>等价燃料剩余</dt><dd>${formatNumber(column.fuelRemaining, 6)} / ${formatNumber(column.fuelCapacity, 6)}</dd></div>
    <div><dt>消耗率</dt><dd>${formatNumber(burnRate, 9)} 份/tick</dd></div>
    <div><dt>预计耗尽</dt><dd>${Number.isFinite(ticksToEmpty) ? `${formatNumber(ticksToEmpty, 1)} tick / ${formatNumber(ticksToEmpty / 20 / 60, 2)} min` : "不消耗"}</dd></div>
    <div><dt>冷却带走 / 净热</dt><dd>${formatNumber(result?.removedHeat ?? 0)} / ${formatNumber(result?.netHeatLoad ?? column.cachedHeat)} HU/t</dd></div>
    <div><dt>完整度 / 损坏度</dt><dd>${(integrity * 100).toFixed(2)}% / ${((1 - integrity) * 100).toFixed(2)}%</dd></div>
    <div><dt>余热缓存</dt><dd>${formatNumber(column.cachedHeat)} HU</dd></div>
  </dl><div class="neighbor-list">${neighborHtml}</div>`;
}

function renderTrend() {
  const entries = state.history;
  $("#trend-count").textContent = `${entries.length} 个 tick 采样`;
  const tbody = $("#trend-table tbody");
  tbody.innerHTML = entries.slice(-20).reverse().map((entry) => {
    const summary = entry.summary;
    return `<tr><td>${summary.tick}</td><td>${formatNumber(summary.generatedHeat ?? 0)}</td><td>${formatNumber(summary.convertedCoolant ?? 0)}</td><td>${formatNumber(summary.maxNetHeatLoad ?? 0)}</td><td>${((summary.minIntegrity ?? 1) * 100).toFixed(2)}%</td><td>${summary.meltdownProgress ?? 0}</td></tr>`;
  }).join("");
  drawTrendChart(entries);
}

function drawTrendChart(entries) {
  const canvas = $("#trend-chart");
  const context = canvas.getContext("2d");
  const width = canvas.width;
  const height = canvas.height;
  context.clearRect(0, 0, width, height);
  context.fillStyle = "#11191c";
  context.fillRect(0, 0, width, height);
  const left = 50, right = 20, top = 20, bottom = 30;
  const plotWidth = width - left - right;
  const plotHeight = height - top - bottom;
  context.strokeStyle = "#2a393e";
  context.lineWidth = 1;
  context.font = "11px system-ui";
  context.fillStyle = "#71817b";
  for (let i = 0; i <= 4; i += 1) {
    const y = top + plotHeight * i / 4;
    context.beginPath(); context.moveTo(left, y); context.lineTo(width - right, y); context.stroke();
    context.fillText(`${100 - i * 25}%`, 10, y + 4);
  }
  if (!entries.length) {
    context.fillStyle = "#9caca5";
    context.fillText("运行或单步后显示完整 tick 序列", left + 20, height / 2);
    return;
  }
  const maxPower = Math.max(1, ...entries.flatMap((entry) => [entry.summary.generatedHeat ?? 0, entry.summary.convertedCoolant ?? 0, entry.summary.maxNetHeatLoad ?? 0]));
  const point = (index, value, max = maxPower) => ({
    x: left + (entries.length === 1 ? 0 : plotWidth * index / (entries.length - 1)),
    y: top + plotHeight * (1 - Math.max(0, Math.min(1, value / max))),
  });
  const series = [
    { key: "generatedHeat", color: "#f3bd65", max: maxPower },
    { key: "convertedCoolant", color: "#75e0d1", max: maxPower },
    { key: "maxNetHeatLoad", color: "#ed7e50", max: maxPower },
    { key: "minIntegrity", color: "#78d69a", max: 1 },
  ];
  for (const item of series) {
    context.strokeStyle = item.color;
    context.lineWidth = item.key === "minIntegrity" ? 2 : 2.5;
    context.beginPath();
    entries.forEach((entry, index) => {
      const value = item.key === "minIntegrity" ? entry.summary.minIntegrity ?? 1 : entry.summary[item.key] ?? 0;
      const p = point(index, value, item.max);
      if (index === 0) context.moveTo(p.x, p.y); else context.lineTo(p.x, p.y);
    });
    context.stroke();
  }
  context.fillStyle = "#71817b";
  context.fillText(`tick ${entries[0].summary.tick}`, left, height - 9);
  context.fillText(`tick ${entries.at(-1).summary.tick}`, width - 95, height - 9);
}

function renderFormulaText() {
  $("#formula-text").innerHTML = `<ul>
    <li>外尺寸为 length × width × height；有效区为 (length−2) × (width−2) × (height−2)，每个横截面格是一根垂直列。</li>
    <li>孤立燃料：controlledIntensity = (1 − 相邻控制棒平均有效插入深度)<sup>controlResponseExponent</sup>。</li>
    <li>燃料四向直接相邻时使用从 1.0 开始的同步单调有界反馈，最多 ${256} 轮，变化 ≤ ${1e-9} 收敛；控制棒不进入该簇反馈。</li>
    <li>列发热与燃耗乘以 2 − 完整度；损伤只来自超过安全阈值的净热负荷。完整度归零或燃料耗尽后自下一 tick 停止新裂变。</li>
    <li>反应堆冷却剂总容量 = (空列数量 + 控制棒列数量) × 有效高度 × 1000 mB；内存冷却剂 + 热冷却剂始终不超过该上限。换热转换移动冷却剂状态，外部输入/输出可在内存中暂存。</li>
    <li>燃料组件以每列有效高度份等价容量抽象；不会模拟 Create 流体 capability、管网、区块、NBT 或实际物品事务。</li>
  </ul>`;
}

function renderAll() {
  $("#tool-version").textContent = TOOL_VERSION;
  $("#rule-version").textContent = RULE_VERSION;
  renderLayoutGrid("pending");
  renderRodEditor();
  renderOverview();
  renderWarnings();
  renderResultGridAndDetail();
  renderTrend();
  renderFormulaText();
  updateBurnRateNote();
}

function commitPendingScene() {
  pauseTimer();
  const geometryResult = validateGeometry(pendingGeometryFromForm());
  const configResult = validateConfig(readConfigFromForm());
  const layoutResult = validateLayout(state.pendingLayout, geometryResult.geometry);
  const depthResult = validateDepthMatrix(state.pendingDepths, state.pendingLayout, geometryResult.geometry);
  const errors = [...geometryResult.errors, ...configResult.errors, ...layoutResult.errors, ...depthResult.errors];
  $("#layout-errors").textContent = errors.join("；");
  if (errors.length) {
    setStatus(`无法应用场景：${errors[0]}`);
    return false;
  }
  state.geometry = geometryResult.geometry;
  state.layout = state.pendingLayout.map((row) => [...row]);
  state.depths = normalizeDepths(state.pendingDepths, state.layout);
  state.config = configResult.value;
  state.initialState = {
    running: $("#initial-running").checked,
    scram: $("#initial-scram").checked,
  };
  state.initialSnapshot = createInitialSnapshot(state.geometry, state.layout, state.depths, state.config);
  state.snapshot = cloneSnapshot(state.initialSnapshot);
  state.runtime = { ...state.initialState, running: false };
  $("#scram-toggle").checked = state.runtime.scram;
  state.lastResult = null;
  state.history = [];
  state.selectedKey = null;
  state.dirty = false;
  $("#length-input").value = state.geometry.length;
  $("#width-input").value = state.geometry.width;
  $("#height-input").value = state.geometry.height;
  setStatus("场景已应用并重置。可以运行、单步或切换 SCRAM。", "success");
  renderAll();
  return true;
}

function resetToDefaults() {
  pauseTimer();
  state.geometry = { ...DEFAULT_GEOMETRY };
  state.layout = createPreset(state.geometry, "mixed");
  state.depths = createDepthMatrix(state.geometry, state.layout);
  state.pendingGeometry = { ...state.geometry };
  state.pendingLayout = state.layout.map((row) => [...row]);
  state.pendingDepths = state.depths.map((row) => [...row]);
  state.config = cloneConfig(DEFAULT_CONFIG);
  for (const field of CONFIG_FIELDS) $(`#param-${field.key}`).value = state.config[field.key];
  $("#length-input").value = state.geometry.length;
  $("#width-input").value = state.geometry.width;
  $("#height-input").value = state.geometry.height;
  $("#initial-running").checked = false;
  $("#initial-scram").checked = false;
  commitPendingScene();
}

function resetSimulation() {
  pauseTimer();
  state.snapshot = cloneSnapshot(state.initialSnapshot);
  state.runtime = { ...state.initialState, running: false };
  $("#scram-toggle").checked = state.runtime.scram;
  state.lastResult = null;
  state.history = [];
  state.selectedKey = null;
  setStatus("模拟状态已重置，配置与布局未改变。", "success");
  renderAll();
}

function advanceBy(count, forceRunning = false) {
  if (state.dirty) {
    setStatus("存在未应用修改；请先暂停并点击“应用并重置”。");
    return false;
  }
  const runState = { ...state.runtime, running: forceRunning || state.runtime.running };
  for (let index = 0; index < count; index += 1) {
    const result = stepSnapshot(state.snapshot, state.geometry, state.layout, state.config, runState);
    state.lastResult = result;
    state.snapshot = result.nextSnapshot;
    state.history.push({ summary: result.summary, columns: result.columns });
    runState.running = result.runtime.running;
    if (!result.converged || state.snapshot.meltdownMelted) {
      runState.running = false;
      break;
    }
  }
  state.runtime = { ...state.runtime, ...runState };
  renderOverview();
  renderWarnings();
  renderResultGridAndDetail();
  renderTrend();
  return state.runtime.running;
}

function startRunning() {
  if (state.dirty) {
    setStatus("有未应用修改；请先暂停并点击“应用并重置”。");
    return;
  }
  if (state.snapshot.meltdownMelted) {
    setStatus("当前场景已融毁；只能导出、导入或重置后继续。");
    return;
  }
  state.runtime.running = true;
  state.runTargetTick = state.snapshot.tick + Math.max(1, Number($("#run-ticks").value) || 1);
  if (state.timer == null) {
    state.timer = setInterval(() => {
      const remaining = state.runTargetTick - state.snapshot.tick;
      const step = Math.min(Number($("#tick-step").value), remaining);
      if (step <= 0 || !advanceBy(step, true)) {
        pauseTimer();
        renderAll();
      }
      if (state.snapshot.tick >= state.runTargetTick) {
        pauseTimer();
        setStatus(`完成 ${state.runTargetTick} tick 运行。`, "success");
        renderAll();
      }
    }, 30);
  }
  setStatus(`运行中：目标 tick ${state.runTargetTick}。`, "success");
  renderAll();
}

function singleStep() {
  pauseTimer();
  if (state.dirty) {
    setStatus("有未应用修改；请先点击“应用并重置”。");
    return;
  }
  const count = Number($("#tick-step").value);
  advanceBy(count, true);
  state.runtime.running = false;
  setStatus(`已单步 ${count} tick。`, "success");
  renderAll();
}

function exportCurrentJson() {
  const scene = buildScene({
    geometry: state.geometry,
    layout: state.layout,
    controlRodDepths: state.snapshot.columns.reduce((matrix, column) => {
      if (column.type === "control_rod") matrix[column.z][column.x] = column.depth;
      return matrix;
    }, createDepthMatrix(state.geometry, state.layout)),
    config: state.config,
    initialState: state.initialState,
    initialSnapshot: state.initialSnapshot,
    finalSnapshot: state.snapshot,
    history: state.history,
    summary: currentSummary(),
  });
  downloadText(`reactor-scene-t${state.snapshot.tick}.json`, sceneJson(scene), "application/json;charset=utf-8");
  $("#io-message").textContent = `已导出 tick ${state.snapshot.tick} 场景；包含规则版本 ${RULE_VERSION} 与 ${state.history.length} 条 tick 采样。`;
}

function exportCurrentCsv() {
  const csv = buildCsv(state.history, state.snapshot, currentSummary(), RULE_VERSION);
  downloadText(`reactor-history-t${state.snapshot.tick}.csv`, csv, "text/csv;charset=utf-8");
  $("#io-message").textContent = `已导出 ${state.history.length} 行 tick 总览和 ${state.snapshot.columns.length} 行最终列快照。`;
}

function hydrateImportedSnapshot(raw, geometry, layout, config) {
  if (!raw || !Array.isArray(raw.columns)) return null;
  const fresh = createInitialSnapshot(geometry, layout, createDepthMatrix(geometry, layout), config);
  if (raw.columns.length !== fresh.columns.length) return null;
  const freshByKey = new Map(fresh.columns.map((column) => [column.key, column]));
  const imported = [];
  for (const item of raw.columns) {
    const base = freshByKey.get(item.key);
    if (!base || item.type !== base.type) return null;
    if (item.type === "fuel") {
      if (![item.fuelRemaining, item.integrity, item.cachedHeat].every(Number.isFinite)) return null;
      imported.push({ ...base, fuelRemaining: Math.max(0, Math.min(base.fuelCapacity, Number(item.fuelRemaining))), integrity: Math.max(0, Math.min(1, Number(item.integrity))), cachedHeat: Math.max(0, Number(item.cachedHeat)) });
    } else if (item.type === "control_rod") {
      if (![item.controlRodIntegrity, item.depth, item.jammedDepth, item.cachedHeat].every(Number.isFinite)) return null;
      imported.push({ ...base, controlRodIntegrity: Math.max(0, Math.min(1, Number(item.controlRodIntegrity))), depth: Math.max(0, Math.min(1, Number(item.depth))), jammed: Boolean(item.jammed), jammedDepth: Math.max(0, Math.min(1, Number(item.jammedDepth))), cachedHeat: Math.max(0, Number(item.cachedHeat)) });
    } else imported.push(base);
  }
  const coolantTotalCapacityMb = fresh.coolantTotalCapacityMb;
  const coldCoolantMb = Math.max(0, Math.min(coolantTotalCapacityMb, Number(raw.coldCoolantMb ?? fresh.coldCoolantMb)));
  const hotCoolantMb = Math.max(0, Math.min(coolantTotalCapacityMb - coldCoolantMb, Number(raw.hotCoolantMb ?? fresh.hotCoolantMb)));
  return { tick: Math.max(0, Number(raw.tick) || 0), meltdownProgress: Math.max(0, Number(raw.meltdownProgress) || 0), meltdownTriggered: Boolean(raw.meltdownTriggered), meltdownMelted: Boolean(raw.meltdownMelted), coolantTotalCapacityMb, coldCoolantMb, hotCoolantMb, columns: imported.sort((a, b) => a.z - b.z || a.x - b.x), simulationFailed: raw.simulationFailed ? String(raw.simulationFailed) : null };
}

async function importScene(file) {
  if (!file) return;
  const text = await file.text();
  const parsed = parseSceneText(text);
  if (!parsed.ok) {
    $("#io-message").textContent = `导入失败：${parsed.errors.join("；")}。当前场景未改变。`;
    return;
  }
  const scene = parsed.scene;
  const importedInitial = hydrateImportedSnapshot(scene.initialSnapshot, scene.geometry, scene.layout.matrix, scene.config);
  const importedFinal = hydrateImportedSnapshot(scene.finalSnapshot, scene.geometry, scene.layout.matrix, scene.config);
  if (scene.initialSnapshot && !importedInitial) {
    $("#io-message").textContent = "导入失败：initialSnapshot 结构不匹配；当前场景未改变。";
    return;
  }
  if (scene.finalSnapshot && !importedFinal) {
    $("#io-message").textContent = "导入失败：finalSnapshot 结构不匹配；当前场景未改变。";
    return;
  }
  pauseTimer();
  state.geometry = scene.geometry;
  state.layout = scene.layout.matrix.map((row) => [...row]);
  state.depths = normalizeDepths(scene.controlRodDepths, state.layout);
  state.config = scene.config;
  state.initialState = scene.initialState;
  state.initialSnapshot = importedInitial ?? createInitialSnapshot(state.geometry, state.layout, state.depths, state.config);
  state.snapshot = importedFinal ?? cloneSnapshot(state.initialSnapshot);
  state.runtime = { ...scene.initialState, running: false };
  state.lastResult = null;
  state.history = Array.isArray(scene.history) ? scene.history : [];
  state.pendingGeometry = { ...state.geometry };
  state.pendingLayout = state.layout.map((row) => [...row]);
  state.pendingDepths = state.depths.map((row) => [...row]);
  state.dirty = false;
  $("#length-input").value = state.geometry.length;
  $("#width-input").value = state.geometry.width;
  $("#height-input").value = state.geometry.height;
  $("#initial-running").checked = state.initialState.running;
  $("#initial-scram").checked = state.initialState.scram;
  for (const field of CONFIG_FIELDS) $(`#param-${field.key}`).value = state.config[field.key];
  $("#io-message").textContent = `已导入 tick ${state.snapshot.tick}；规则版本 ${scene.ruleVersion}。当前状态保持暂停，避免文件自动启动。`;
  renderAll();
}

function wireEvents() {
  $("#apply-reset").addEventListener("click", commitPendingScene);
  $("#reset-defaults").addEventListener("click", resetToDefaults);
  $("#clear-layout").addEventListener("click", () => {
    if (state.runtime.running) pauseTimer();
    state.pendingLayout = createMatrix(state.pendingGeometry, "empty");
    state.pendingDepths = createDepthMatrix(state.pendingGeometry, state.pendingLayout);
    markDirty("布局已清空");
    renderLayoutGrid("pending"); renderRodEditor();
  });
  $$("[data-preset]").forEach((button) => button.addEventListener("click", () => {
    state.pendingLayout = createPreset(state.pendingGeometry, button.dataset.preset);
    state.pendingDepths = createDepthMatrix(state.pendingGeometry, state.pendingLayout);
    markDirty(`已选择${button.textContent}布局`);
    renderLayoutGrid("pending"); renderRodEditor();
  }));
  $$('input[name="brush"]').forEach((input) => input.addEventListener("change", () => { state.brush = input.value; }));
  ["length-input", "width-input", "height-input"].forEach((id) => $("#" + id).addEventListener("input", () => {
    const geometry = pendingGeometryFromForm();
    if (state.runtime.running) pauseTimer();
    if (!updateDimensionHint(geometry)) return;
    state.pendingGeometry = geometry;
    state.pendingLayout = createPreset(geometry, "mixed");
    state.pendingDepths = createDepthMatrix(geometry, state.pendingLayout);
    markDirty("尺寸已修改");
    renderLayoutGrid("pending"); renderRodEditor();
  }));
  $$("#parameter-fields input").forEach((input) => input.addEventListener("input", () => {
    if (state.runtime.running) pauseTimer();
    updateBurnRateNote();
    markDirty("参数已修改");
  }));
  $("#initial-running").addEventListener("change", () => markDirty("初始运行状态已修改"));
  $("#initial-scram").addEventListener("change", () => markDirty("初始 SCRAM 状态已修改"));
  $("#tick-step").addEventListener("change", () => {});
  $("#run-ticks").addEventListener("input", () => {});
  $("#run-button").addEventListener("click", startRunning);
  $("#pause-button").addEventListener("click", () => { pauseTimer(); setStatus("已暂停；当前快照已保留。", "success"); renderAll(); });
  $("#step-button").addEventListener("click", singleStep);
  $("#reset-simulation").addEventListener("click", resetSimulation);
  $("#scram-toggle").addEventListener("change", () => {
    state.runtime.scram = $("#scram-toggle").checked;
    setStatus(state.runtime.scram ? "SCRAM 已启用；运行裂变被切断，余热仍会结算。" : "SCRAM 已解除；控制棒恢复当前快照中的目标深度。", "success");
    renderAll();
  });
  $("#grid-layer").addEventListener("change", renderResultGridAndDetail);
  $("#export-json").addEventListener("click", exportCurrentJson);
  $("#export-csv").addEventListener("click", exportCurrentCsv);
  $("#import-json").addEventListener("change", (event) => importScene(event.target.files[0]));
  window.addEventListener("pointerup", () => { state.painting = false; });
}

function initialize() {
  state.depths = createDepthMatrix(state.geometry, state.layout);
  state.pendingLayout = state.layout.map((row) => [...row]);
  state.pendingDepths = state.depths.map((row) => [...row]);
  state.initialSnapshot = createInitialSnapshot(state.geometry, state.layout, state.depths, state.config);
  state.snapshot = cloneSnapshot(state.initialSnapshot);
  renderParameterFields();
  $("#scram-toggle").checked = false;
  wireEvents();
  updateDimensionHint(state.geometry);
  renderAll();
}

initialize();
