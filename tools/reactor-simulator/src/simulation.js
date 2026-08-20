import { baseBurnPerFuel, LIMITS } from "./config.js";
import { cellKey, coolantTotalCapacityMb, getNeighbors, listCells, internalDimensions } from "./layout.js";

const EPSILON = 1e-12;

export function clamp01(value) {
  return Number.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
}

function finiteNonNegative(value) {
  return Number.isFinite(value) && value > 0 ? value : 0;
}

function formatCapacity(value) {
  return Number(value ?? 0).toFixed(3);
}

function sortedColumns(snapshot) {
  return [...snapshot.columns].sort((a, b) => a.z - b.z || a.x - b.x);
}

function cloneColumn(column) {
  return { ...column };
}

export function cloneSnapshot(snapshot) {
  return {
    ...snapshot,
    columns: snapshot.columns.map(cloneColumn),
  };
}

export function createInitialSnapshot(geometry, layout, depths, config) {
  const { height } = internalDimensions(geometry);
  const fuelCapacity = config.fuelCapacityPerBlock * height;
  const coolantCapacity = coolantTotalCapacityMb(geometry, layout);
  const columns = listCells(layout).map(({ x, z, type, key }) => {
    if (type === "fuel") {
      return {
        key, x, z, type,
        fuelCapacity,
        fuelRemaining: fuelCapacity,
        integrity: 1,
        cachedHeat: 0,
      };
    }
    if (type === "control_rod") {
      const depth = clamp01(Number(depths?.[z]?.[x] ?? 1));
      return {
        key, x, z, type,
        controlRodIntegrity: 1,
        depth,
        jammed: false,
        jammedDepth: depth,
        cachedHeat: 0,
      };
    }
    return { key, x, z, type, cachedHeat: 0 };
  });
  return {
    tick: 0,
    meltdownProgress: 0,
    meltdownTriggered: false,
    meltdownMelted: false,
    coolantTotalCapacityMb: coolantCapacity,
    coldCoolantMb: coolantCapacity,
    hotCoolantMb: 0,
    columns,
    simulationFailed: null,
  };
}

export function applyControlDepths(snapshot, depths) {
  return {
    ...snapshot,
    columns: snapshot.columns.map((column) => {
      if (column.type !== "control_rod" || column.jammed) return cloneColumn(column);
      return { ...column, depth: clamp01(Number(depths?.[column.z]?.[column.x] ?? column.depth)) };
    }),
  };
}

export function setControlDepth(snapshot, key, depth) {
  return {
    ...snapshot,
    columns: snapshot.columns.map((column) =>
      column.key === key && column.type === "control_rod" && !column.jammed
        ? { ...column, depth: clamp01(Number(depth)) }
        : cloneColumn(column)),
  };
}

export function getColumn(snapshot, key) {
  return snapshot.columns.find((column) => column.key === key) ?? null;
}

export function isEffectiveFuel(column) {
  return column?.type === "fuel" && column.fuelRemaining > EPSILON;
}

function prepareScram(snapshot, runtime = {}) {
  const previous = cloneSnapshot(snapshot);
  const controlRods = previous.columns.filter((column) => column.type === "control_rod");
  const scramAvailable = controlRods.length > 0;
  const requested = Boolean(runtime.scramRequested ?? runtime.scram);
  if (requested && !scramAvailable) {
    return {
      snapshot: previous,
      runtime: {
        ...runtime,
        scram: false,
        scramRequested: false,
        scramActive: false,
        scramAvailable: false,
        scramSavedDepths: null,
        scramReason: "SCRAM_UNAVAILABLE_NO_CONTROL_RODS",
      },
    };
  }
  const scramActive = requested && scramAvailable;
  let scramSavedDepths = runtime.scramSavedDepths ?? null;
  if (scramActive && !runtime.scramActive) {
    scramSavedDepths = Object.fromEntries(controlRods
      .filter((column) => !column.jammed)
      .map((column) => [column.key, column.depth]));
  }
  if (scramActive) {
    previous.columns = previous.columns.map((column) =>
      column.type === "control_rod" && !column.jammed ? { ...column, depth: 1 } : column);
  } else if (runtime.scramActive && scramSavedDepths) {
    previous.columns = previous.columns.map((column) =>
      column.type === "control_rod" && !column.jammed && Number.isFinite(scramSavedDepths[column.key])
        ? { ...column, depth: clamp01(scramSavedDepths[column.key]) }
        : column);
    scramSavedDepths = null;
  }
  return {
    snapshot: previous,
    runtime: {
      ...runtime,
      scram: scramActive,
      scramRequested: scramActive,
      scramActive,
      scramAvailable,
      scramSavedDepths: scramActive ? scramSavedDepths : null,
      scramReason: null,
    },
  };
}

export function setScramRequest(snapshot, runtime = {}, requested) {
  return prepareScram(snapshot, { ...runtime, scramRequested: Boolean(requested) });
}

function effectiveControlDepth(column) {
  if (!column || column.type !== "control_rod") return 0;
  return clamp01(column.jammed || column.controlRodIntegrity <= 0
    ? column.jammedDepth
    : column.depth);
}

function snapshotMap(snapshot) {
  return new Map(snapshot.columns.map((column) => [column.key, column]));
}

function neighborColumns(layout, snapshot, column) {
  const map = snapshotMap(snapshot);
  return getNeighbors(layout, column.x, column.z)
    .map((neighbor) => map.get(cellKey(neighbor.x, neighbor.z)))
    .filter(Boolean);
}

function allocateWeighted(weights, caps, budget) {
  const allocation = new Map();
  const remainingWeights = new Map();
  const remainingCaps = new Map();
  for (const [key, weight] of weights.entries()) {
    const safeWeight = finiteNonNegative(weight);
    const safeCap = finiteNonNegative(caps.get(key) ?? 0);
    if (safeWeight > 0 && safeCap > 0) {
      remainingWeights.set(key, safeWeight);
      remainingCaps.set(key, safeCap);
      allocation.set(key, 0);
    }
  }
  let remainingBudget = finiteNonNegative(budget);
  while (remainingBudget > EPSILON && remainingWeights.size > 0) {
    const totalWeight = [...remainingWeights.values()].reduce((sum, value) => sum + value, 0);
    if (totalWeight <= EPSILON) break;
    const capped = [];
    for (const key of [...remainingWeights.keys()]) {
      const proposed = remainingBudget * remainingWeights.get(key) / totalWeight;
      const cap = remainingCaps.get(key);
      if (proposed >= cap - EPSILON) {
        allocation.set(key, allocation.get(key) + cap);
        remainingBudget -= cap;
        capped.push(key);
      }
    }
    if (capped.length > 0) {
      for (const key of capped) {
        remainingWeights.delete(key);
        remainingCaps.delete(key);
      }
      continue;
    }
    for (const key of remainingWeights.keys()) {
      allocation.set(key, allocation.get(key)
        + Math.min(remainingCaps.get(key), remainingBudget * remainingWeights.get(key) / totalWeight));
    }
    break;
  }
  return allocation;
}

function allocateCooling(generatedHeat, thermal, budget) {
  const allocation = allocateWeighted(generatedHeat, thermal, budget);
  const used = [...allocation.values()].reduce((sum, value) => sum + value, 0);
  const cachedWeights = new Map();
  const cachedCaps = new Map();
  for (const [key, total] of thermal.entries()) {
    const cached = Math.max(0, total - (generatedHeat.get(key) ?? 0));
    const remainingCap = Math.max(0, total - (allocation.get(key) ?? 0));
    if (cached > EPSILON && remainingCap > EPSILON) {
      cachedWeights.set(key, cached);
      cachedCaps.set(key, remainingCap);
    }
  }
  const cachedAllocation = allocateWeighted(cachedWeights, cachedCaps, Math.max(0, budget - used));
  for (const [key, value] of cachedAllocation.entries()) allocation.set(key, (allocation.get(key) ?? 0) + value);
  return allocation;
}

function portLedger(config, availableHeat, snapshot, geometry, layout) {
  const coldPortFlowCap = config.coldPortCount * config.coolantMaxFlowPerPort;
  const hotPortFlowCap = config.hotPortCount * config.coolantMaxFlowPerPort;
  const totalCapacity = coolantTotalCapacityMb(geometry, layout);
  const previousCold = Math.max(0, Math.min(totalCapacity, Number(snapshot.coldCoolantMb ?? totalCapacity)));
  const previousHot = Math.max(0, Math.min(totalCapacity - previousCold, Number(snapshot.hotCoolantMb ?? 0)));
  const inputFlowCap = Math.min(config.actualColdIn, coldPortFlowCap);
  const outputFlowCap = Math.min(config.actualHotOut, hotPortFlowCap);
  const hotOutputBeforeConversion = Math.min(previousHot, outputFlowCap);
  const freeCapacityAfterOutput = Math.max(0, totalCapacity - previousCold - previousHot + hotOutputBeforeConversion);
  const coldInAccepted = Math.min(inputFlowCap, freeCapacityAfterOutput);
  const coldPool = previousCold + coldInAccepted;
  const heatLimitedMb = availableHeat / config.coolantAbsorptionHuPerMb;
  const conversionFlowCap = Math.min(coldPortFlowCap, hotPortFlowCap);
  const maxConverted = Math.max(0, Math.min(
    coldPool,
    conversionFlowCap,
    heatLimitedMb,
  ));
  const hotOutputRemaining = Math.max(0, outputFlowCap - hotOutputBeforeConversion);
  const hotOutputForConversion = Math.min(hotOutputRemaining, maxConverted);
  const plannedHotOut = hotOutputBeforeConversion + hotOutputForConversion;
  const plannedColdAfter = coldPool - maxConverted;
  const plannedHotAfter = previousHot - plannedHotOut + maxConverted;
  const candidates = [
    { key: "coldPool", label: "反应堆内冷却剂库存", value: coldPool },
    { key: "coldPortFlowCap", label: "冷端口数量/单端口上限", value: coldPortFlowCap },
    { key: "hotPortFlowCap", label: "热端口数量/单端口上限", value: hotPortFlowCap },
    { key: "heatLimitedMb", label: "可用热量", value: heatLimitedMb },
  ];
  const bottlenecks = availableHeat <= EPSILON
    ? ["无发热或余热"]
    : candidates.filter((candidate) => Math.abs(candidate.value - maxConverted) <= 1e-9)
      .map((candidate) => candidate.label);
  return {
    totalCapacity,
    previousCold,
    previousHot,
    inputFlowCap,
    coldInAccepted,
    coldPool,
    outputFlowCap,
    hotOutputBeforeConversion,
    hotOutputRemaining,
    maxConverted,
    plannedHotOut,
    plannedColdAfter,
    plannedHotAfter,
    coldPortFlowCap,
    hotPortFlowCap,
    heatLimitedMb,
    bottlenecks,
    candidates,
  };
}

function makeColumnResult(column, details) {
  return {
    ...column,
    ...details,
    neighbors: details.neighbors ?? [],
  };
}

export function stepSnapshot(snapshot, geometry, layout, config, runtime = {}) {
  const prepared = prepareScram(snapshot, runtime);
  const previous = prepared.snapshot;
  const currentRuntime = prepared.runtime;
  const columns = sortedColumns(previous);
  const byKey = new Map(columns.map((column) => [column.key, column]));
  const { height } = internalDimensions(geometry);
  const active = currentRuntime.running !== false && !previous.meltdownMelted;
  const effectiveFuel = new Map(columns.map((column) => [column.key, isEffectiveFuel(column)]));
  const controlledIntensity = new Map();
  const overclocked = new Map();
  const controlMeanDepth = new Map();

  for (const column of columns) {
    if (!effectiveFuel.get(column.key)) continue;
    const controls = neighborColumns(layout, previous, column)
      .filter((neighbor) => neighbor.type === "control_rod");
    const meanDepth = controls.length === 0
      ? 0
      : controls.reduce((sum, control) => sum + effectiveControlDepth(control), 0) / controls.length;
    controlMeanDepth.set(column.key, meanDepth);
    controlledIntensity.set(column.key, active
      ? Math.pow(clamp01(1 - meanDepth), config.controlResponseExponent)
      : 0);
    const hasFuelNeighbour = neighborColumns(layout, previous, column)
      .some((neighbor) => effectiveFuel.get(neighbor.key) === true);
    overclocked.set(column.key, hasFuelNeighbour);
  }

  const heatIntensity = new Map();
  const burnIntensity = new Map();
  for (const column of columns) {
    if (!effectiveFuel.get(column.key)) continue;
    const initial = controlledIntensity.get(column.key);
    heatIntensity.set(column.key, initial);
    burnIntensity.set(column.key, initial);
  }

  let converged = true;
  let iterations = 0;
  if ([...overclocked.values()].some(Boolean)) {
    converged = false;
    for (iterations = 1; iterations <= LIMITS.maxFeedbackIterations; iterations += 1) {
      const nextHeat = new Map(heatIntensity);
      const nextBurn = new Map(burnIntensity);
      for (const column of columns) {
        if (!effectiveFuel.get(column.key)) continue;
        if (!overclocked.get(column.key) || !active) {
          nextHeat.set(column.key, controlledIntensity.get(column.key));
          nextBurn.set(column.key, controlledIntensity.get(column.key));
          continue;
        }
        const signal = neighborColumns(layout, previous, column)
          .filter((neighbor) => neighbor.type === "fuel" && effectiveFuel.get(neighbor.key))
          .reduce((sum, neighbor) => sum + (heatIntensity.get(neighbor.key) ?? 1), 0);
        const activation = clamp01(1 - Math.exp(-config.overclockFeedbackGain
          * Math.pow(Math.max(0, signal), config.overclockFeedbackExponent)));
        const controlFactor = controlledIntensity.get(column.key) ?? 0;
        nextHeat.set(column.key, controlFactor * (1 + (config.overclockHeatMultiplier - 1) * activation));
        nextBurn.set(column.key, controlFactor * (1 + (config.overclockBurnMultiplier - 1) * activation));
      }
      const largestDelta = [...heatIntensity.keys()].reduce((largest, key) => Math.max(
        largest,
        Math.abs((nextHeat.get(key) ?? 0) - (heatIntensity.get(key) ?? 0)),
        Math.abs((nextBurn.get(key) ?? 0) - (burnIntensity.get(key) ?? 0)),
      ), 0);
      heatIntensity.clear();
      burnIntensity.clear();
      for (const [key, value] of nextHeat.entries()) heatIntensity.set(key, value);
      for (const [key, value] of nextBurn.entries()) burnIntensity.set(key, value);
      if (largestDelta <= LIMITS.feedbackEpsilon) {
        converged = true;
        break;
      }
    }
  }

  if (!converged) {
    const failed = { ...previous, simulationFailed: `超频反馈未在 ${LIMITS.maxFeedbackIterations} 轮内收敛` };
    return {
      nextSnapshot: failed,
      converged: false,
      iterations,
      runtime: { ...currentRuntime, running: false },
      summary: summarizeSnapshot(failed, null, config, geometry),
      columns: {},
      warnings: [failed.simulationFailed],
    };
  }

  const rawHeat = new Map();
  const generatedHeat = new Map();
  const plannedBurn = new Map();
  let rawTotalHeat = 0;
  let installedFuelColumns = 0;
  const baseBurn = baseBurnPerFuel(config);
  for (const column of columns) {
    if (column.type === "fuel" && column.fuelRemaining > EPSILON) installedFuelColumns += 1;
    if (!effectiveFuel.get(column.key)) {
      rawHeat.set(column.key, 0);
      plannedBurn.set(column.key, 0);
      continue;
    }
    const damageMultiplier = 2 - clamp01(column.integrity);
    const raw = finiteNonNegative(config.baseHeatPerFuel * height
      * (heatIntensity.get(column.key) ?? 0) * damageMultiplier);
    rawHeat.set(column.key, raw);
    plannedBurn.set(column.key, finiteNonNegative(baseBurn * height
      * (burnIntensity.get(column.key) ?? 0) * damageMultiplier));
    rawTotalHeat += raw;
  }
  const heatCap = config.baseHeatPerFuel * height * installedFuelColumns * config.totalHeatMultiplierCap;
  const heatScale = rawTotalHeat <= EPSILON ? 0 : Math.min(1, heatCap / rawTotalHeat);
  let totalGeneratedHeat = 0;
  for (const column of columns) {
    const generated = finiteNonNegative((rawHeat.get(column.key) ?? 0) * heatScale);
    generatedHeat.set(column.key, generated);
    totalGeneratedHeat += generated;
  }

  const currentThermal = new Map(columns.map((column) => [
    column.key,
    column.type === "empty" ? 0 : finiteNonNegative((generatedHeat.get(column.key) ?? 0) + column.cachedHeat),
  ]));
  const availableHeat = [...currentThermal.values()].reduce((sum, value) => sum + value, 0);
  const ports = portLedger(config, availableHeat, previous, geometry, layout);
  const maxRemovalHeat = ports.maxConverted * config.coolantAbsorptionHuPerMb;
  const currentRemoval = allocateCooling(generatedHeat, currentThermal, maxRemovalHeat);
  const currentRemovedHeat = [...currentRemoval.values()].reduce((sum, value) => sum + value, 0);

  const propagationHeat = new Map();
  const sourceTransfer = new Map();
  for (const source of columns) {
    if (source.type !== "fuel" || !effectiveFuel.get(source.key) || source.integrity > EPSILON) continue;
    const residual = Math.max(0, (generatedHeat.get(source.key) ?? 0) + source.cachedHeat);
    const targets = neighborColumns(layout, previous, source).filter((target) =>
      target.type === "control_rod" || effectiveFuel.get(target.key) === true);
    if (targets.length === 0) continue;
    const transfer = residual * config.fuelColumnDamageTransferRate;
    sourceTransfer.set(source.key, transfer);
    const perTarget = transfer / targets.length;
    for (const target of targets) {
      propagationHeat.set(target.key, (propagationHeat.get(target.key) ?? 0) + perTarget);
    }
  }
  const remainingCoolingBudget = Math.max(0, maxRemovalHeat - currentRemovedHeat);
  const propagationRemoval = allocateWeighted(propagationHeat, propagationHeat, remainingCoolingBudget);
  const propagationRemovedHeat = [...propagationRemoval.values()].reduce((sum, value) => sum + value, 0);
  const removedHeat = currentRemovedHeat + propagationRemovedHeat;
  const convertedCoolant = removedHeat / config.coolantAbsorptionHuPerMb;
  const hotOutputForConversion = Math.min(ports.hotOutputRemaining, convertedCoolant);
  const hotOutActual = ports.hotOutputBeforeConversion + hotOutputForConversion;
  const coldAfter = Math.max(0, Math.min(ports.totalCapacity, ports.coldPool - convertedCoolant));
  const hotAfter = Math.max(0, Math.min(ports.totalCapacity - coldAfter,
    ports.previousHot - hotOutActual + convertedCoolant));
  const coolantLedgerError = Math.max(
    Math.abs((ports.previousCold + ports.coldInAccepted - coldAfter) - convertedCoolant),
    Math.abs((hotAfter + hotOutActual - ports.previousHot) - convertedCoolant),
  );

  const nextColumns = [];
  const resultColumns = {};
  const coveredFuel = new Set();
  for (const column of columns) {
    const received = propagationHeat.get(column.key) ?? 0;
    const receivedRemoval = propagationRemoval.get(column.key) ?? 0;
    if (column.type === "fuel" && received > receivedRemoval && effectiveFuel.get(column.key)) {
      coveredFuel.add(column.key);
    }
    const currentRemoved = currentRemoval.get(column.key) ?? 0;
    const heat = generatedHeat.get(column.key) ?? 0;
    const totalUnremoved = column.type === "empty" ? 0 : Math.max(0,
      heat + column.cachedHeat + received - currentRemoved - receivedRemoval);
    let damage = 0;
    let next;
    if (column.type === "fuel") {
      if (effectiveFuel.get(column.key) && totalUnremoved > config.fuelColumnDamageHeatThreshold) {
        damage = (totalUnremoved - config.fuelColumnDamageHeatThreshold) * config.fuelColumnDamageRate;
      }
      const nextIntegrity = clamp01(column.integrity - damage);
      const consumed = Math.min(column.fuelRemaining, plannedBurn.get(column.key) ?? 0);
      let nextCache = totalUnremoved;
      if (column.integrity <= EPSILON && (sourceTransfer.get(column.key) ?? 0) > 0) {
        nextCache = Math.max(0, nextCache - sourceTransfer.get(column.key));
      }
      next = {
        ...column,
        integrity: nextIntegrity,
        fuelRemaining: Math.max(0, column.fuelRemaining - consumed),
        cachedHeat: nextCache,
      };
    } else if (column.type === "control_rod") {
      if (column.controlRodIntegrity > EPSILON && totalUnremoved > config.fuelColumnDamageHeatThreshold) {
        damage = (totalUnremoved - config.fuelColumnDamageHeatThreshold) * config.fuelColumnDamageRate;
      }
      const nextIntegrity = clamp01(column.controlRodIntegrity - damage);
      const wasJammed = column.jammed || column.controlRodIntegrity <= config.controlRodColumnFailureThreshold;
      const actualDepth = effectiveControlDepth(column);
      const jammed = wasJammed || nextIntegrity <= config.controlRodColumnFailureThreshold;
      next = {
        ...column,
        controlRodIntegrity: nextIntegrity,
        depth: jammed ? actualDepth : clamp01(column.depth),
        jammed,
        jammedDepth: jammed ? (column.jammed ? column.jammedDepth : actualDepth) : column.jammedDepth,
        cachedHeat: totalUnremoved,
      };
    } else {
      next = { ...column, cachedHeat: 0 };
    }
    nextColumns.push(next);
    resultColumns[column.key] = makeColumnResult(column, {
      generatedHeat: heat,
      rawHeat: rawHeat.get(column.key) ?? 0,
      heatIntensity: heatIntensity.get(column.key) ?? 0,
      burnIntensity: burnIntensity.get(column.key) ?? 0,
      controlledIntensity: controlledIntensity.get(column.key) ?? 0,
      controlMeanDepth: controlMeanDepth.get(column.key) ?? null,
      overclocked: overclocked.get(column.key) ?? false,
      plannedBurn: plannedBurn.get(column.key) ?? 0,
      fuelConsumed: column.type === "fuel"
        ? Math.min(column.fuelRemaining, plannedBurn.get(column.key) ?? 0)
        : 0,
      removedHeat: currentRemoved + receivedRemoval,
      currentRemovedHeat: currentRemoved,
      propagationHeatReceived: received,
      propagationHeatRemoved: receivedRemoval,
      netHeatLoad: totalUnremoved,
      damage,
      nextIntegrity: next.integrity ?? next.controlRodIntegrity ?? 1,
      nextFuelRemaining: next.fuelRemaining ?? null,
      nextCachedHeat: next.cachedHeat,
      nextJammed: next.jammed ?? false,
      neighbors: getNeighbors(layout, column.x, column.z),
    });
  }

  const effectiveCount = [...effectiveFuel.values()].filter(Boolean).length;
  const coverage = effectiveCount === 0 ? 0 : coveredFuel.size / effectiveCount;
  const meltdownDanger = coveredFuel.size > 0 && coverage >= config.meltdownTriggerFraction;
  let meltdownTriggered = previous.meltdownTriggered || meltdownDanger;
  let meltdownProgress = previous.meltdownProgress;
  const coolingEffective = removedHeat > EPSILON;
  const pauseCountdown = currentRuntime.scramActive === true && coolingEffective;
  if (meltdownDanger && !pauseCountdown) {
    meltdownProgress = Math.min(config.meltdownCountdownTicks, meltdownProgress + 1);
  }
  const allRepaired = nextColumns.every((column) => column.type === "empty"
    || (column.type === "fuel" ? column.integrity >= 1 - EPSILON : column.controlRodIntegrity >= 1 - EPSILON));
  if (previous.meltdownTriggered && allRepaired) {
    meltdownProgress = 0;
    meltdownTriggered = false;
  }
  const nextSnapshot = {
    tick: previous.tick + 1,
    meltdownProgress,
    meltdownTriggered,
    meltdownMelted: meltdownProgress >= config.meltdownCountdownTicks,
    coolantTotalCapacityMb: ports.totalCapacity,
    coldCoolantMb: coldAfter,
    hotCoolantMb: hotAfter,
    columns: nextColumns,
    simulationFailed: null,
  };
  const summary = summarizeSnapshot(nextSnapshot, resultColumns, config, geometry);
  summary.rawHeat = rawTotalHeat;
  summary.generatedHeat = totalGeneratedHeat;
  summary.heatCap = heatCap;
  summary.heatScale = heatScale;
  summary.plannedBurn = Object.values(resultColumns).reduce((sum, result) => sum + result.plannedBurn, 0);
  summary.fuelConsumed = Object.values(resultColumns).reduce((sum, result) => sum + result.fuelConsumed, 0);
  summary.convertedCoolant = convertedCoolant;
  summary.removedHeat = removedHeat;
  summary.coolantDemand = ports.heatLimitedMb;
  summary.coldIn = ports.coldInAccepted;
  summary.hotOut = hotOutActual;
  summary.coldInAccepted = ports.coldInAccepted;
  summary.hotOutActual = hotOutActual;
  summary.coolantTotalCapacityMb = ports.totalCapacity;
  summary.coldCoolantMb = coldAfter;
  summary.hotCoolantMb = hotAfter;
  summary.totalCoolantMb = coldAfter + hotAfter;
  summary.coolantCapacityHeadroomMb = Math.max(0, ports.totalCapacity - coldAfter - hotAfter);
  summary.coolantLedgerError = coolantLedgerError;
  summary.availableHeat = availableHeat;
  summary.propagationCoverage = coverage;
  summary.meltdownDanger = meltdownDanger;
  summary.countdownPaused = meltdownDanger && pauseCountdown;
  summary.scramAvailable = currentRuntime.scramAvailable === true;
  summary.scramRequested = currentRuntime.scramRequested === true;
  summary.scramActive = currentRuntime.scramActive === true;
  summary.scramReason = currentRuntime.scramReason ?? null;
  summary.scramIncomplete = summary.scramActive && totalGeneratedHeat > EPSILON;
  summary.ruleConverged = true;
  summary.iterations = iterations;
  summary.ports = { ...ports, coldInAccepted: ports.coldInAccepted, hotOutActual };
  summary.bottlenecks = ports.bottlenecks;
  summary.runtime = {
    ...currentRuntime,
    running: currentRuntime.running !== false && !nextSnapshot.meltdownMelted,
    coolingEffective,
  };

  return {
    nextSnapshot,
    converged: true,
    iterations,
    runtime: summary.runtime,
    summary,
    columns: resultColumns,
    warnings: buildWarnings(summary, resultColumns, config),
  };
}

export function summarizeSnapshot(snapshot, resultColumns, config, geometry) {
  const columns = snapshot.columns;
  const fuel = columns.filter((column) => column.type === "fuel");
  const rods = columns.filter((column) => column.type === "control_rod");
  const integrityValues = columns.filter((column) => column.type !== "empty").map((column) =>
    column.type === "fuel" ? column.integrity : column.controlRodIntegrity);
  const resultValues = resultColumns ? Object.values(resultColumns) : [];
  const coolantCapacity = Math.max(0, Number(snapshot.coolantTotalCapacityMb ?? 0));
  const coldCoolant = Math.max(0, Math.min(coolantCapacity, Number(snapshot.coldCoolantMb ?? coolantCapacity)));
  const hotCoolant = Math.max(0, Math.min(coolantCapacity - coldCoolant, Number(snapshot.hotCoolantMb ?? 0)));
  return {
    tick: snapshot.tick,
    gameSeconds: snapshot.tick / 20,
    fuelColumns: fuel.length,
    controlRodColumns: rods.length,
    emptyColumns: columns.filter((column) => column.type === "empty").length,
    totalResidualHeat: columns.reduce((sum, column) => sum + (column.cachedHeat ?? 0), 0),
    maxNetHeatLoad: resultValues.reduce((max, result) => Math.max(max, result.netHeatLoad ?? 0), 0),
    minIntegrity: integrityValues.length ? Math.min(...integrityValues) : 1,
    damagedColumns: integrityValues.filter((value) => value < 1 - EPSILON).length,
    jammedColumns: rods.filter((column) => column.jammed).length,
    exhaustedFuelColumns: fuel.filter((column) => column.fuelRemaining <= EPSILON).length,
    meltdownProgress: snapshot.meltdownProgress,
    meltdownCountdownTicks: config.meltdownCountdownTicks,
    meltdownTriggered: snapshot.meltdownTriggered,
    meltdownMelted: snapshot.meltdownMelted,
    effectiveHeight: internalDimensions(geometry).height,
    coolantTotalCapacityMb: coolantCapacity,
    coldCoolantMb: coldCoolant,
    hotCoolantMb: hotCoolant,
    totalCoolantMb: coldCoolant + hotCoolant,
    coolantCapacityHeadroomMb: Math.max(0, coolantCapacity - coldCoolant - hotCoolant),
    coolantLedgerError: 0,
    ruleConverged: true,
    iterations: 0,
  };
}

export function buildWarnings(summary, resultColumns, config) {
  const warnings = [];
  if (summary.scramReason === "SCRAM_UNAVAILABLE_NO_CONTROL_RODS") {
    warnings.push("SCRAM_UNAVAILABLE_NO_CONTROL_RODS：当前布局没有控制棒列，SCRAM 请求已原子拒绝");
  }
  if (summary.scramIncomplete) {
    warnings.push("SCRAM_INCOMPLETE：SCRAM 已建立，但仍存在裂变产热；卡死控制棒或燃料超频簇未被停堆请求切断");
  }
  const overclocked = Object.values(resultColumns).filter((column) => column.overclocked);
  if (overclocked.length > 0) {
    warnings.push(`超频簇：${overclocked.map((column) => `[${column.x},${column.z}]`).join("、")}；控制棒深度作为该簇功率门控`);
  }
  if (summary.ports && summary.generatedHeat > 0) {
    for (const bottleneck of summary.bottlenecks ?? []) {
      if (bottleneck !== "无发热或余热") warnings.push(`冷却瓶颈：${bottleneck}`);
    }
  }
  if (summary.availableHeat > EPSILON && (summary.convertedCoolant ?? 0) + EPSILON < (summary.coolantDemand ?? 0)) {
    warnings.push(`冷却能力不足：可用热量需求 ${summary.coolantDemand.toFixed(3)} mB/t，实际仅转换 ${(summary.convertedCoolant ?? 0).toFixed(3)} mB/t`);
  }
  if ((summary.coolantTotalCapacityMb ?? 0) <= EPSILON && summary.availableHeat > EPSILON) {
    warnings.push("反应堆冷却剂总容量为 0 mB：当前布局没有空列或控制棒列可提供内部冷却剂空间");
  } else if ((summary.coolantCapacityHeadroomMb ?? 0) <= EPSILON && (summary.ports?.inputFlowCap ?? 0) > (summary.coldInAccepted ?? 0) + EPSILON) {
    warnings.push(`反应堆冷却剂总容量已满：${formatCapacity(summary.coolantTotalCapacityMb)} mB（冷 ${formatCapacity(summary.coldCoolantMb)} + 热 ${formatCapacity(summary.hotCoolantMb)}）`);
  }
  const damaged = Object.values(resultColumns).filter((column) => column.damage > EPSILON);
  if (damaged.length > 0) {
    warnings.push(`列完整度下降：${damaged.map((column) => `[${column.x},${column.z}]`).join("、")}`);
  } else if ((summary.damagedColumns ?? 0) > 0) {
    warnings.push(`列完整度已下降：当前有 ${summary.damagedColumns} 根列低于 100%`);
  }
  const jammed = Object.values(resultColumns).filter((column) => column.nextJammed && column.type === "control_rod");
  if (jammed.length > 0) {
    warnings.push(`控制棒卡死：${jammed.map((column) => `[${column.x},${column.z}]`).join("、")}；深度保持卡死前实际值`);
  } else if ((summary.jammedColumns ?? 0) > 0) {
    warnings.push(`控制棒已卡死：当前有 ${summary.jammedColumns} 根控制棒保持卡死深度`);
  }
  if (summary.meltdownDanger) warnings.push(`达到融毁触达阈值：传播覆盖 ${(summary.propagationCoverage * 100).toFixed(1)}% 有效燃料列`);
  if (summary.countdownPaused) warnings.push("融毁倒计时暂停：SCRAM 且本 tick 有有效冷却；进度不会回退");
  if (summary.meltdownMelted) warnings.push("已融毁：倒计时达到上限，运行停止");
  return [...new Set(warnings)];
}

export function statusLabel(snapshot, runtime, config) {
  if (snapshot.simulationFailed) return "规则失败";
  if (snapshot.meltdownMelted) return "已融毁";
  if (snapshot.meltdownTriggered && snapshot.meltdownProgress >= config.meltdownCountdownTicks) return "已融毁";
  if (snapshot.meltdownTriggered && (runtime?.scramActive || runtime?.coolingEffective)) return "暂停倒计时";
  if (snapshot.meltdownTriggered) return "融毁倒计时";
  if (runtime?.scramActive) return "SCRAM";
  if (runtime?.running) return "运行";
  return "暂停";
}

export function repairColumn(snapshot, key, geometry) {
  const internalHeight = internalDimensions(geometry).height;
  return {
    ...snapshot,
    columns: snapshot.columns.map((column) => {
      if (column.key !== key || column.type === "empty") return cloneColumn(column);
      if (column.type === "fuel") {
        return { ...column, integrity: clamp01(column.integrity + 0.25 / internalHeight) };
      }
      if (column.jammed) return cloneColumn(column);
      return { ...column, controlRodIntegrity: clamp01(column.controlRodIntegrity + 0.25 / internalHeight) };
    }),
  };
}

export function snapshotToPlain(snapshot) {
  return JSON.parse(JSON.stringify(snapshot));
}

export function runTicks(snapshot, geometry, layout, config, runtime, count) {
  let current = snapshot;
  let currentRuntime = { ...runtime };
  const results = [];
  for (let index = 0; index < count; index += 1) {
    const result = stepSnapshot(current, geometry, layout, config, currentRuntime);
    results.push(result);
    current = result.nextSnapshot;
    currentRuntime = { ...currentRuntime, ...result.runtime };
    if (!result.converged || current.meltdownMelted) {
      currentRuntime.running = false;
      break;
    }
  }
  return { snapshot: current, runtime: currentRuntime, results };
}
