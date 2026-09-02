import { DEFAULT_GEOMETRY, LIMITS } from "./config.js";

export const COLUMN_TYPES = Object.freeze(["fuel", "control_rod", "empty"]);
export const COLUMN_LABELS = Object.freeze({
  fuel: "燃料列",
  control_rod: "控制棒列",
  empty: "空列",
});

export const COOLANT_CAPACITY_PER_NON_FUEL_HEIGHT_MB = 1000;

export function internalDimensions(geometry = DEFAULT_GEOMETRY) {
  return {
    columns: geometry.length - 2,
    rows: geometry.width - 2,
    height: geometry.height - 2,
  };
}

export function createMatrix(geometry = DEFAULT_GEOMETRY, fill = "empty") {
  const { columns, rows } = internalDimensions(geometry);
  return Array.from({ length: rows }, () => Array.from({ length: columns }, () => fill));
}

export function createPreset(geometry = DEFAULT_GEOMETRY, preset = "mixed") {
  const matrix = createMatrix(geometry, "fuel");
  const { columns, rows } = internalDimensions(geometry);
  if (preset === "empty") return matrix.map((row) => row.map(() => "empty"));
  if (preset === "full_fuel") return matrix;

  if (preset === "checker_rods") {
    return matrix.map((row, z) => row.map((_, x) => ((x + z) % 2 === 0 ? "control_rod" : "fuel")));
  }

  if (preset === "center_cross") {
    const centerX = Math.floor(columns / 2);
    const centerZ = Math.floor(rows / 2);
    return matrix.map((row, z) => row.map((_, x) =>
      (x === centerX || z === centerZ) ? "control_rod" : "fuel"));
  }

  if (preset === "fcf") {
    // P1-CONTROL-05 的固定三行布局：每行沿 X 方向为燃料、控制棒、燃料。
    const centerX = Math.floor(columns / 2);
    return matrix.map((row) => row.map((_, x) => x === centerX ? "control_rod" : "fuel"));
  }

  // 初始场景故意保持混合：用于展示燃料、控制棒和空列，不宣称是 P1 结构契约。
  const centerX = Math.floor(columns / 2);
  const centerZ = Math.floor(rows / 2);
  return matrix.map((row, z) => row.map((_, x) => {
    if (x === centerX && z === centerZ) return "control_rod";
    if ((x === 0 || x === columns - 1) && (z === 0 || z === rows - 1)) return "empty";
    return "fuel";
  }));
}

export function createDepthMatrix(geometry = DEFAULT_GEOMETRY, layout = createPreset(geometry)) {
  const { columns, rows } = internalDimensions(geometry);
  return Array.from({ length: rows }, (_, z) => Array.from({ length: columns }, (_, x) =>
    layout[z]?.[x] === "control_rod" ? 1 : null));
}

export function cloneMatrix(matrix) {
  return matrix.map((row) => [...row]);
}

export function validateGeometry(raw = {}) {
  const geometry = {
    length: Number(raw.length),
    width: Number(raw.width),
    height: Number(raw.height),
  };
  const errors = [];
  for (const key of ["length", "width", "height"]) {
    if (!Number.isInteger(geometry[key])) errors.push(`${key} 必须是整数`);
    if (geometry[key] < LIMITS.minDimension || geometry[key] > LIMITS.maxDimension) {
      errors.push(`${key} 必须在 ${LIMITS.minDimension}～${LIMITS.maxDimension} 之间`);
    }
  }
  if (geometry.length >= LIMITS.minDimension && geometry.width >= LIMITS.minDimension) {
    const { columns, rows } = internalDimensions(geometry);
    if (columns * rows > LIMITS.maxFuelColumns) {
      errors.push(`有效列数 ${columns * rows} 超过 UI 上限 ${LIMITS.maxFuelColumns}`);
    }
  }
  if (geometry.height >= LIMITS.minDimension && geometry.height - 2 > LIMITS.maxInternalHeight) {
    errors.push(`有效高度超过 ${LIMITS.maxInternalHeight}`);
  }
  return { ok: errors.length === 0, geometry, errors };
}

export function validateLayout(matrix, geometry) {
  const errors = [];
  const { columns, rows } = internalDimensions(geometry);
  if (!Array.isArray(matrix) || matrix.length !== rows) {
    errors.push(`布局矩阵必须有 ${rows} 行（z 方向）`);
    return { ok: false, errors };
  }
  for (let z = 0; z < rows; z += 1) {
    if (!Array.isArray(matrix[z]) || matrix[z].length !== columns) {
      errors.push(`布局矩阵第 ${z} 行必须有 ${columns} 列（x 方向）`);
      continue;
    }
    for (let x = 0; x < columns; x += 1) {
      if (!COLUMN_TYPES.includes(matrix[z][x])) {
        errors.push(`布局 [${x},${z}] 的列类型无效：${String(matrix[z][x])}`);
      }
    }
  }
  const fuelColumns = matrix.flat().filter((type) => type === "fuel").length;
  if (fuelColumns > LIMITS.maxFuelColumns) errors.push("燃料列数超过性能上限");
  return { ok: errors.length === 0, errors, fuelColumns };
}

export function validateDepthMatrix(depths, layout, geometry) {
  const { columns, rows } = internalDimensions(geometry);
  const errors = [];
  if (!Array.isArray(depths) || depths.length !== rows) {
    errors.push(`控制棒深度矩阵必须有 ${rows} 行`);
    return { ok: false, errors };
  }
  for (let z = 0; z < rows; z += 1) {
    if (!Array.isArray(depths[z]) || depths[z].length !== columns) {
      errors.push(`控制棒深度矩阵第 ${z} 行必须有 ${columns} 列`);
      continue;
    }
    for (let x = 0; x < columns; x += 1) {
      const depth = depths[z][x];
      if (layout[z]?.[x] === "control_rod") {
        if (!Number.isFinite(Number(depth)) || Number(depth) < 0 || Number(depth) > 1) {
          errors.push(`控制棒 [${x},${z}] 深度必须在 0～1 之间`);
        }
      }
    }
  }
  return { ok: errors.length === 0, errors };
}

export function cellKey(x, z) {
  return `${x},${z}`;
}

export function parseCellKey(key) {
  const [x, z] = String(key).split(",").map(Number);
  return { x, z };
}

export function getNeighbors(matrix, x, z) {
  const directions = [
    { dx: 0, dz: -1, direction: "北" },
    { dx: 0, dz: 1, direction: "南" },
    { dx: -1, dz: 0, direction: "西" },
    { dx: 1, dz: 0, direction: "东" },
  ];
  return directions
    .map(({ dx, dz, direction }) => ({
      x: x + dx,
      z: z + dz,
      direction,
      type: matrix[z + dz]?.[x + dx] ?? null,
    }))
    .filter((neighbor) => neighbor.type !== null);
}

export function listCells(matrix) {
  const cells = [];
  for (let z = 0; z < matrix.length; z += 1) {
    for (let x = 0; x < matrix[z].length; x += 1) {
      cells.push({ x, z, type: matrix[z][x], key: cellKey(x, z) });
    }
  }
  return cells;
}

export function countColumns(matrix) {
  return matrix.flat().reduce((counts, type) => {
    counts[type] = (counts[type] ?? 0) + 1;
    return counts;
  }, { fuel: 0, control_rod: 0, empty: 0 });
}

export function coolantTotalCapacityMb(geometry, matrix) {
  const counts = countColumns(matrix);
  const { height } = internalDimensions(geometry);
  return (counts.empty + counts.control_rod) * height * COOLANT_CAPACITY_PER_NON_FUEL_HEIGHT_MB;
}

export function normalizeDepths(depths, layout) {
  return layout.map((row, z) => row.map((type, x) =>
    type === "control_rod" ? Math.max(0, Math.min(1, Number(depths?.[z]?.[x] ?? 1))) : null));
}
