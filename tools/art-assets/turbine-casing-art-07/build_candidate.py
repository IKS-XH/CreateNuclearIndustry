"""从原创SVG生成汽轮机机壳B方案候选及实际OBJ预览。"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import math
import re
import struct
import zlib
from collections import defaultdict
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
GENERATED = HERE / "generated"
MESH = ROOT / "src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh"
FORMED_RE = re.compile(r"casing_d([357])_(front|middle|rear)_x(-?\d+)_y(-?\d+)\.obj$")
UNFORMED_RE = re.compile(r"turbine_casing_unformed(?:_(up|down|north|south|west|east))?\.obj$")
NAMESPACE = "http://www.w3.org/2000/svg"
PALETTE = {
    "D": "#28353A", "S": "#3E5058", "M": "#5B7078", "H": "#788E94",
    "L": "#A3B7B8", "W": "#D1DFDA", "C": "#A87942", "c": "#715035",
}
OLD_MATERIAL_COLOR = {"inside": "#28353A", "edge": "#3E5058"}
TARGETS = {
    "mtl": "src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh/turbine_casing_07.mtl",
    "panel_png": "src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_casing_07_panel.png",
    "endcap_png": "src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_casing_07_endcap.png",
}


def source_grids() -> dict[str, list[list[str]]]:
    """面板横轴为标准局部Z；端盖以八棱同心法兰表现B方案。"""
    panel = []
    for y in range(16):
        base = {0: "D", 1: "L", 2: "H", 13: "S", 14: "D", 15: "D"}.get(y, "M")
        row = [base] * 16
        for x, color in ((0, "C"), (1, "L"), (2, "H"), (3, "S")):
            if 2 <= y <= 13:
                row[x] = color
        # 小铆点只补充钢束带，不再增加轴向整圈。
        for x in (7, 12):
            if y in (4, 11):
                row[x] = "W"
            elif y in (5, 10):
                row[x] = "D"
        panel.append(row)
    endcap = []
    for y in range(16):
        row = []
        for x in range(16):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            radius = max(dx, dy, (dx + dy) * 0.70710678)
            if radius >= 7.0:
                color = "D"
            elif radius >= 6.1:
                color = "L" if y < 8 else "S"
            elif radius >= 5.4:
                color = "M"
            elif radius >= 4.7:
                color = "D"
            elif radius >= 3.9:
                color = "c" if x < 8 else "C"
            elif radius >= 3.2:
                color = "C" if y < 8 else "c"
            else:
                color = "M" if y < 8 else "S"
            if (x, y) in ((5, 5), (10, 5), (5, 10), (10, 10)):
                color = "W" if (x + y) % 2 else "D"
            row.append(color)
        endcap.append(row)
    return {"panel": panel, "endcap": endcap}


def svg_text(grid: list[list[str]], title: str) -> str:
    rows = [f'<svg xmlns="{NAMESPACE}" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges" data-source="ART-TURBINE-07B">', f"<title>{title}</title>"]
    for y, row in enumerate(grid):
        x = 0
        while x < 16:
            end = x + 1
            while end < 16 and row[end] == row[x]:
                end += 1
            rows.append(f'<rect x="{x}" y="{y}" width="{end-x}" height="1" fill="{PALETTE[row[x]]}"/>')
            x = end
    rows.append("</svg>")
    return "\n".join(rows) + "\n"


def write_sources() -> None:
    for name, title in (("panel", "B方案钢束带与窄铜缝面板"), ("endcap", "B方案八棱钢法兰与铜内嵌端盖")):
        (HERE / f"{name}.svg").write_text(svg_text(source_grids()[name], title), encoding="utf-8", newline="\n")


def png_chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def export_png(grid: list[list[str]]) -> bytes:
    raw = b"".join(b"\x00" + b"".join(bytes.fromhex(PALETTE[key][1:]) + b"\xff" for key in row) for row in grid)
    return b"\x89PNG\r\n\x1a\n" + png_chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)) + png_chunk(b"IDAT", zlib.compress(raw, 9)) + png_chunk(b"IEND", b"")


def svg_pixels(path: Path) -> list[list[str]]:
    root = ET.parse(path).getroot()
    if root.attrib.get("width") != "16" or root.attrib.get("height") != "16" or root.attrib.get("viewBox") != "0 0 16 16":
        raise ValueError(f"SVG画布不是16×16：{path}")
    grid: list[list[str | None]] = [[None] * 16 for _ in range(16)]
    for rect in root:
        if rect.tag == f"{{{NAMESPACE}}}title":
            continue
        if rect.tag != f"{{{NAMESPACE}}}rect":
            raise ValueError(f"SVG含非rect图元：{path}")
        x, y, width, height = (int(rect.attrib[k]) for k in ("x", "y", "width", "height"))
        color = rect.attrib.get("fill", "").upper()
        if color not in set(PALETTE.values()) or min(x, y, width, height) < 0 or width < 1 or height < 1 or x + width > 16 or y + height > 16:
            raise ValueError(f"SVG像素范围/色板错误：{path}")
        code = next(key for key, value in PALETTE.items() if value == color)
        for yy in range(y, y + height):
            for xx in range(x, x + width):
                if grid[yy][xx] is not None:
                    raise ValueError(f"SVG像素重叠：{path}")
                grid[yy][xx] = code
    if any(pixel is None for row in grid for pixel in row):
        raise ValueError(f"SVG有空白像素：{path}")
    return [[pixel for pixel in row if pixel is not None] for row in grid]


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def parse_obj(path: Path) -> dict:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    vertices, uvs, normals, faces = [], [], [], []
    material = ""
    for line_index, line in enumerate(lines):
        parts = line.strip().split()
        if not parts:
            continue
        if parts[0] == "v":
            vertices.append(tuple(map(float, parts[1:4])))
        elif parts[0] == "vt":
            uvs.append(tuple(map(float, parts[1:3])))
        elif parts[0] == "vn":
            normals.append(tuple(map(float, parts[1:4])))
        elif parts[0] == "usemtl":
            material = parts[1]
        elif parts[0] == "f":
            indices = [tuple(int(n) if n else 0 for n in token.split("/")) for token in parts[1:]]
            faces.append({"line": line_index, "material": material, "indices": indices})
    return {"path": path, "lines": lines, "vertices": vertices, "uvs": uvs, "normals": normals, "faces": faces}


def formed_details(path: Path) -> tuple[int, str, int, int] | None:
    match = FORMED_RE.fullmatch(path.name)
    return (int(match[1]), match[2], int(match[3]), int(match[4])) if match else None


def loose_direction(path: Path) -> str | None:
    match = UNFORMED_RE.fullmatch(path.name)
    return (match[1] or "up") if match else None


def to_up(point: tuple[float, float, float], direction: str) -> tuple[float, float, float]:
    """按汽轮机旧生成器的预烘焙朝向逆变换回up标准坐标。"""
    x, y, z = point
    return {
        "up": (x, y, z), "down": (x, 1-y, 1-z), "north": (x, 1-z, y),
        "south": (x, z, 1-y), "west": (y, 1-x, z), "east": (1-y, x, z),
    }[direction]


def normal_to_up(normal: tuple[float, float, float], direction: str) -> tuple[float, float, float]:
    x, y, z = normal
    result = {
        "up": (x, y, z), "down": (x, -y, -z), "north": (x, -z, y),
        "south": (x, z, -y), "west": (y, -x, z), "east": (-y, x, z),
    }[direction]
    length = math.sqrt(sum(value * value for value in result))
    return tuple(value / length for value in result)


def face_normal(doc: dict, face: dict, direction: str | None = None) -> tuple[float, float, float]:
    samples = [doc["normals"][index[2] - 1] for index in face["indices"] if index[2]]
    if not samples:
        raise ValueError(f"OBJ面缺少法线：{doc['path']}")
    normal = tuple(sum(sample[i] for sample in samples) / len(samples) for i in range(3))
    if direction:
        return normal_to_up(normal, direction)
    length = math.sqrt(sum(value * value for value in normal))
    return tuple(value / length for value in normal)


def target_models() -> list[Path]:
    formed = sorted(path for path in MESH.glob("casing_d*_*.obj") if formed_details(path))
    loose = sorted(path for path in MESH.glob("turbine_casing_unformed*.obj") if loose_direction(path))
    if len(formed) != 206 or len(loose) != 6:
        raise ValueError(f"212个目标集合不符：成型{len(formed)}，未成型{len(loose)}")
    return formed + loose


def shared_tangent_ranges(paths: list[Path]) -> dict[tuple[int, int, int], tuple[float, float]]:
    """以完整截面同法线的全局切向范围计算V，使邻格相位连续。"""
    values: dict[tuple[int, int, int], list[float]] = defaultdict(list)
    for path in paths:
        details = formed_details(path)
        if not details:
            continue
        diameter, _, ox, oy = details
        doc = parse_obj(path)
        for face in doc["faces"]:
            if face["material"] != "casing":
                continue
            nx, ny, nz = face_normal(doc, face)
            if abs(nz) > 0.01:
                raise ValueError(f"成型casing面不属于侧壳：{path.name}")
            key = (diameter, round(nx * 1000), round(ny * 1000))
            tx, ty = -ny, nx
            for index in face["indices"]:
                x, y, _ = doc["vertices"][index[0] - 1]
                values[key].append((x + ox) * tx + (y + oy) * ty)
    return {key: (min(samples), max(samples)) for key, samples in values.items()}


def assign_uv(updates: dict[int, tuple[float, float]], materials: dict[int, str], index: int, material: str,
              u: float, v: float, path: Path) -> None:
    if index < 1 or not -1e-7 <= u <= 1.0000001 or not -1e-7 <= v <= 1.0000001:
        raise ValueError(f"候选UV越界或索引缺失：{path.name} vt={index} uv={(u,v)}")
    if index in materials and materials[index] != material:
        raise ValueError(f"OBJ vt跨材质共享，不能原位安全映射：{path.name} vt={index}")
    value = (max(0.0, min(1.0, u)), max(0.0, min(1.0, v)))
    if index in updates and any(abs(updates[index][i] - value[i]) > 1e-7 for i in range(2)):
        raise ValueError(f"同一vt需不同候选UV：{path.name} vt={index}")
    materials[index] = material
    updates[index] = value


def mapped_obj(path: Path, tangent_ranges: dict[tuple[int, int, int], tuple[float, float]]) -> tuple[str, dict]:
    doc = parse_obj(path)
    details = formed_details(path)
    direction = loose_direction(path)
    diameter, segment, ox, oy = details if details else (0, "unformed", 0, 0)
    updates: dict[int, tuple[float, float]] = {}
    materials: dict[int, str] = {}
    for face in doc["faces"]:
        material = face["material"]
        if material not in ("casing", "endcap"):
            continue
        normal = face_normal(doc, face, direction)
        points = [to_up(doc["vertices"][index[0] - 1], direction) if direction else doc["vertices"][index[0] - 1]
                  for index in face["indices"]]
        if material == "casing":
            if abs(normal[2]) > 0.01:
                raise ValueError(f"casing面法线沿轴向，材料应为端盖：{path.name}")
            if details:
                lo, hi = tangent_ranges[(diameter, round(normal[0] * 1000), round(normal[1] * 1000))]
            else:
                tangent = (-normal[1], normal[0])
                samples = [point[0] * tangent[0] + point[1] * tangent[1] for point in points]
                lo, hi = min(samples), max(samples)
            tx, ty = -normal[1], normal[0]
            for corner, point in zip(face["indices"], points):
                cross_x = point[0] + ox if details else point[0]
                cross_y = point[1] + oy if details else point[1]
                assign_uv(updates, materials, corner[1], material, point[2], (cross_x * tx + cross_y * ty - lo) / (hi - lo), path)
        elif details:
            radius = diameter // 2
            for corner, point in zip(face["indices"], points):
                assign_uv(updates, materials, corner[1], material,
                          (point[0] + ox + radius) / diameter, (point[1] + oy + radius) / diameter, path)
        else:
            for corner, point in zip(face["indices"], points):
                assign_uv(updates, materials, corner[1], material, point[0], point[1], path)
    if not updates:
        raise ValueError(f"机壳OBJ没有可替换casing/endcap UV：{path.name}")
    lines = doc["lines"]
    vt_lines = [index for index, line in enumerate(lines) if line.lstrip().startswith("vt ")]
    for index, (u, v) in updates.items():
        if index > len(vt_lines):
            raise ValueError(f"vt引用越界：{path.name} vt={index}")
        line_index = vt_lines[index - 1]
        ending = "\r\n" if lines[line_index].endswith("\r\n") else "\n" if lines[line_index].endswith("\n") else ""
        lines[line_index] = f"vt {u:.7f} {v:.7f}{ending}"
    lines = [line.replace("mtllib turbine.mtl", "mtllib turbine_casing_07.mtl") if line.lstrip().startswith("mtllib ") else line for line in lines]
    candidate = "".join(lines)
    record = {
        "source": path.relative_to(ROOT).as_posix(),
        "candidate": (Path("generated/models/block/turbine/mesh") / path.name).as_posix(),
        "diameter": diameter or None, "segment": segment, "changed_vt": len(updates),
        "sha256_source": sha256(path.read_bytes()), "sha256_candidate": sha256(candidate.encode("utf-8")),
    }
    return candidate, record


def generate_candidate() -> list[dict]:
    paths = target_models()
    tangent_ranges = shared_tangent_ranges(paths)
    out_dir = GENERATED / "models/block/turbine/mesh"
    out_dir.mkdir(parents=True, exist_ok=True)
    records = []
    for path in paths:
        content, record = mapped_obj(path, tangent_ranges)
        (out_dir / path.name).write_text(content, encoding="utf-8", newline="")
        records.append(record)
    mtl = [
        "newmtl casing", "Ka 0.25 0.25 0.25", "Kd 1 1 1", "map_Kd create_nuclear_industry:block/turbine/turbine_casing_07_panel", "",
        "newmtl inside", "Ka 0.25 0.25 0.25", "Kd 1 1 1", "map_Kd create_nuclear_industry:block/turbine/turbine_casing_panel", "",
        "newmtl edge", "Ka 0.25 0.25 0.25", "Kd 1 1 1", "map_Kd create_nuclear_industry:block/turbine/turbine_casing_panel", "",
        "newmtl endcap", "Ka 0.25 0.25 0.25", "Kd 1 1 1", "map_Kd create_nuclear_industry:block/turbine/turbine_casing_07_endcap", "",
    ]
    (out_dir / "turbine_casing_07.mtl").write_text("\n".join(mtl), encoding="utf-8", newline="\n")
    return records


def write_mapping(records: list[dict]) -> None:
    mapping = {
        "task": "ART-TURBINE-07B", "stage": "已安装试用版（阶段一候选映射基线）", "formal_targets": TARGETS,
        "materials": {"casing": "turbine_casing_07_panel.png", "endcap": "turbine_casing_07_endcap.png",
                      "inside": "保持原turbine_casing_panel.png", "edge": "保持原turbine_casing_panel.png"},
        "uv": {"formed_side_u": "标准局部Z原值；每个面按实际顶点取值，裁切不拉满",
               "formed_side_v": "按直径与法线分组，使用完整截面的共享切向范围；邻接裁切格边界UV连续",
               "formed_endcap": "局部XY加网格偏移后归一化至完整d3/d5/d7截面",
               "unformed": "按旧生成器的六向预烘焙变换逆回up坐标后映射",
               "invariants": "候选OBJ仅改mtllib与casing/endcap所用vt；v/vn/f/usemtl/o保持逐字一致"},
        "obj_targets": records,
    }
    (HERE / "mapping.json").write_text(json.dumps(mapping, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def model_instances(diameter: int) -> list[tuple[Path, tuple[float, float, float]]]:
    items = []
    for source in target_models():
        details = formed_details(source)
        if not details or details[0] != diameter:
            continue
        _, role, x, y = details
        z = {"front": 0.0, "middle": 1.0, "rear": 2.0}[role]
        items.append((GENERATED / "models/block/turbine/mesh" / source.name, (float(x), float(y), z)))
    return items


def solve_affine(screen: list[tuple[float, float]], uv: list[tuple[float, float]]) -> tuple[float, float, float, float, float, float]:
    source = [(u * 16.0, v * 16.0) for u, v in uv]
    for i in range(1, len(source)):
        for j in range(i + 1, len(source)):
            x0, y0 = source[0]
            x1, y1 = source[i]
            x2, y2 = source[j]
            det = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
            if abs(det) < 1e-9:
                continue
            sx0, sy0 = screen[0]
            sx1, sy1 = screen[i]
            sx2, sy2 = screen[j]
            a = ((sx1 - sx0) * (y2 - y0) - (sx2 - sx0) * (y1 - y0)) / det
            c = ((x1 - x0) * (sx2 - sx0) - (x2 - x0) * (sx1 - sx0)) / det
            b = ((sy1 - sy0) * (y2 - y0) - (sy2 - sy0) * (y1 - y0)) / det
            d = ((x1 - x0) * (sy2 - sy0) - (x2 - x0) * (sy1 - sy0)) / det
            e = sx0 - a * x0 - c * y0
            f = sy0 - b * x0 - d * y0
            return a, b, c, d, e, f
    raise ValueError("OBJ预览UV退化，不能映射纹理到实际面")


def collect_render_faces(instances: list[tuple[Path, tuple[float, float, float]]]) -> list[dict]:
    faces = []
    serial = 0
    for path, (ox, oy, oz) in instances:
        doc = parse_obj(path)
        for face in doc["faces"]:
            if face["material"] not in ("casing", "endcap", "edge", "inside"):
                continue
            points, uvs, depths = [], [], []
            for vi, ti, _ in face["indices"]:
                x, y, z = doc["vertices"][vi - 1]
                point = (x + ox, y + oy, z + oz)
                points.append(point)
                uvs.append(doc["uvs"][ti - 1])
                depths.append(sum(point))
            faces.append({"material": face["material"], "points": points, "uvs": uvs,
                          "depth": sum(depths) / len(depths), "serial": serial})
            serial += 1
    return faces


def render_model(title: str, instances: list[tuple[Path, tuple[float, float, float]]],
                 textures: dict[str, bytes], scale: float) -> str:
    faces = collect_render_faces(instances)
    if not faces:
        raise ValueError(f"OBJ预览没有可见面：{title}")
    project = lambda point: ((point[0] - point[2]) * scale,
                             ((point[0] + point[2]) * 0.5 - point[1]) * scale)
    all_points = [project(point) for face in faces for point in face["points"]]
    min_x, max_x = min(p[0] for p in all_points), max(p[0] for p in all_points)
    min_y, max_y = min(p[1] for p in all_points), max(p[1] for p in all_points)
    width = int(math.ceil(max_x - min_x + 72))
    height = int(math.ceil(max_y - min_y + 90))
    dx, dy = 36 - min_x, 48 - min_y
    data_urls = {key: "data:image/png;base64," + base64.b64encode(data).decode("ascii") for key, data in textures.items()}
    rows = [f'<svg xmlns="{NAMESPACE}" width="{width}" height="{height}" viewBox="0 0 {width} {height}" shape-rendering="crispEdges">',
            f"<title>{title}</title>", '<rect width="100%" height="100%" fill="#10171b"/>',
            f'<text x="18" y="27" fill="#e5e7e3" font-family="sans-serif" font-size="17">{title}</text>', "<defs>"]
    transformed = []
    for face in faces:
        screen = [(x + dx, y + dy) for x, y in (project(point) for point in face["points"])]
        point_text = " ".join(f"{x:.3f},{y:.3f}" for x, y in screen)
        serial = face["serial"]
        rows.append(f'<clipPath id="clip{serial}" clipPathUnits="userSpaceOnUse"><polygon points="{point_text}"/></clipPath>')
        transformed.append((face, screen, point_text))
    rows.append("</defs>")
    for face, screen, point_text in sorted(transformed, key=lambda item: (item[0]["depth"], item[0]["serial"])):
        material = face["material"]
        if material in data_urls:
            a, b, c, d, e, f = solve_affine(screen, face["uvs"])
            matrix = f"{a:.8f} {b:.8f} {c:.8f} {d:.8f} {e:.4f} {f:.4f}"
            serial = face["serial"]
            rows.append(f'<g clip-path="url(#clip{serial})"><image href="{data_urls[material]}" x="0" y="0" width="16" height="16" preserveAspectRatio="none" image-rendering="pixelated" transform="matrix({matrix})"/></g>')
        else:
            rows.append(f'<polygon points="{point_text}" fill="{OLD_MATERIAL_COLOR[material]}" stroke="#101519" stroke-width="0.35"/>')
    rows.append("</svg>")
    return "\n".join(rows) + "\n"


def make_previews() -> None:
    preview = GENERATED / "preview"
    preview.mkdir(parents=True, exist_ok=True)
    textures = {
        "casing": (GENERATED / "textures/block/turbine/turbine_casing_07_panel.png").read_bytes(),
        "endcap": (GENERATED / "textures/block/turbine/turbine_casing_07_endcap.png").read_bytes(),
    }
    loose = GENERATED / "models/block/turbine/mesh/turbine_casing_unformed.obj"
    (preview / "unformed.svg").write_text(render_model("未成型机壳｜实际up方向OBJ", [(loose, (0, 0, 0))], textures, 170), encoding="utf-8", newline="\n")
    for diameter in (3, 5, 7):
        title = f"d{diameter}成型｜前/中/后OBJ"
        (preview / f"formed-d{diameter}.svg").write_text(render_model(title, model_instances(diameter), textures, 35), encoding="utf-8", newline="\n")


def write_index() -> None:
    html = """<!doctype html>
<html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>汽轮机机壳B方案实际OBJ预览</title>
<style>body{margin:0;background:#0d1519;color:#e8e6e0;font:16px/1.5 system-ui,sans-serif}main{max-width:1500px;margin:auto;padding:22px}h1{margin:0}.lead{color:#b6c2c1}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(360px,1fr));gap:16px}.card{background:#192328;border:1px solid #38474d;border-radius:8px;padding:14px;overflow:auto}.tile{width:160px;height:160px;image-rendering:pixelated;background:#10171b;border:1px solid #647177}.models{display:grid;grid-template-columns:repeat(auto-fit,minmax(420px,1fr));gap:16px}.model{display:block;width:100%;height:420px}.row{display:flex;gap:20px;align-items:center}.meta{color:#c3cbc6;font-size:14px}code{color:#c9dcd5}</style>
<main><h1>汽轮机机壳｜B 分段轮舱试用候选</h1><p class="lead">以下几何直接来自现有汽轮机机壳 OBJ。浏览器预览按实际 MTL、顶点/法线/面组和 UV 将两张原创纹理投到真实面上；素材已安装，真实游戏视觉仍待复看。</p>
<section class="grid"><article class="card"><div class="row"><img class="tile" src="panel.svg"><div><h2>钢束带与窄铜缝</h2><p>U沿标准局部Z；V跨完整八棱同一平面，裁切邻格连续。</p></div></div></article><article class="card"><div class="row"><img class="tile" src="endcap.svg"><div><h2>钢法兰与铜内嵌端盖</h2><p>UV覆盖完整d3/d5/d7端截面，不在每个格子重复中心图案。</p></div></div></article></section>
<section class="models"><article class="card"><h2>未成型实际模型</h2><object class="model" type="image/svg+xml" data="generated/preview/unformed.svg"></object><p class="meta">六向OBJ按既有预烘焙旋转逆映射；此处展示up基准。</p></article>
<article class="card"><h2>d3成型模型</h2><object class="model" type="image/svg+xml" data="generated/preview/formed-d3.svg"></object></article>
<article class="card"><h2>d5成型模型</h2><object class="model" type="image/svg+xml" data="generated/preview/formed-d5.svg"></object></article>
<article class="card"><h2>d7成型模型</h2><object class="model" type="image/svg+xml" data="generated/preview/formed-d7.svg"></object></article></section>
<article class="card"><h2>生产映射摘要</h2><p><code>casing → turbine_casing_07_panel</code>；<code>endcap → turbine_casing_07_endcap</code>；<code>inside/edge</code>继续引用原贴图。机壳候选清单共212个OBJ，详见 <code>mapping.json</code>。</p></article></main></html>"""
    (HERE / "index.html").write_text(html, encoding="utf-8", newline="\n")


def write_readme() -> None:
    text = """# 汽轮机机壳B方案候选

本目录保存ART-TURBINE-07B的两张原创SVG、导出PNG、独立MTL、212份OBJ UV映射、映射清单与实际模型预览。两张PNG、独立MTL及212份OBJ已安装；客户端内的真实游戏视觉仍待复看。

- [真实OBJ/MTL/UV预览](index.html)：未成型up基准、d3/d5/d7成型组合。
- [面板SVG](panel.svg)：钢束带与窄铜缝；[端盖SVG](endcap.svg)：钢法兰与铜内嵌面。
- [212个机壳OBJ映射清单](mapping.json)。

外壳U取实际标准局部Z。V按完整截面同一外法线平面的全局切向边界计算，裁切邻格共享UV，不逐face归一化。端盖UV按完整d3/d5/d7截面XY归一化。未成型朝向按旧生成器六向预烘焙变换逆回up基准。独立MTL只替换casing/endcap；inside/edge继续用原贴图。候选OBJ只改mtllib与目标面引用的vt；v/vn/f/usemtl/o保持原样。

工具脚本只生成和检查候选副本；正式PNG、MTL与OBJ已安装，并已完成一次assemble。游戏未运行，客户端真实视觉仍待复看。低分辨率贴图源为原创16×16整数rect SVG，PNG由同源SVG确定性导出。
"""
    (HERE / "README.md").write_text(text, encoding="utf-8", newline="\n")


def build() -> None:
    GENERATED.mkdir(parents=True, exist_ok=True)
    grids = source_grids()
    for name, title in (("panel", "B方案钢束带与窄铜缝面板"), ("endcap", "B方案八棱钢法兰与铜内嵌端盖")):
        (HERE / f"{name}.svg").write_text(svg_text(grids[name], title), encoding="utf-8", newline="\n")
    texture_dir = GENERATED / "textures/block/turbine"
    texture_dir.mkdir(parents=True, exist_ok=True)
    pngs = {}
    for name, grid in grids.items():
        pngs[name] = export_png(grid)
        (texture_dir / f"turbine_casing_07_{name}.png").write_bytes(pngs[name])
    records = generate_candidate()
    mapping = {
        "task": "ART-TURBINE-07B", "stage": "已安装试用版（阶段一候选映射基线）", "formal_targets": TARGETS,
        "materials": {"casing": "turbine_casing_07_panel.png", "endcap": "turbine_casing_07_endcap.png",
                      "inside": "原turbine_casing_panel.png", "edge": "原turbine_casing_panel.png"},
        "uv": {"formed_side_u": "标准局部Z原值，逐顶点保留0..1真实位置与front/rear裁切",
               "formed_side_v": "按d与外法线分组的完整截面切向边界，邻接裁切格UV连续",
               "formed_endcap": "局部XY加格偏移，归一化到完整d3/d5/d7截面",
               "unformed": "六向按旧生成器预烘焙旋转逆映射回up基准后应用",
               "invariants": "仅替换mtllib及casing/endcap所用vt；保留v/vn/f/usemtl/o"},
        "obj_targets": records,
    }
    (HERE / "mapping.json").write_text(json.dumps(mapping, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    make_previews()
    write_index()
    write_readme()
    print(f"CANDIDATE_READY svg=2 png=2 obj_targets={len(records)} preview=4")


def check() -> None:
    grids = {name: svg_pixels(HERE / f"{name}.svg") for name in ("panel", "endcap")}
    for name, grid in grids.items():
        colors = {PALETTE[key] for row in grid for key in row}
        if len(colors) > 8:
            raise ValueError(f"源色板超限：{name} colors={len(colors)}")
        png = GENERATED / "textures/block/turbine" / f"turbine_casing_07_{name}.png"
        if not png.is_file() or png.read_bytes() != export_png(grid):
            raise ValueError(f"候选PNG与SVG不一致：{name}")
        print(f"SVG_PNG_OK {name} colors={len(colors)} size=16x16")
    sources = target_models()
    mapping = json.loads((HERE / "mapping.json").read_text(encoding="utf-8"))
    records = mapping.get("obj_targets", [])
    if len(sources) != 212 or len(records) != 212:
        raise ValueError(f"OBJ映射数量不是212：sources={len(sources)} mapping={len(records)}")
    output_dir = GENERATED / "models/block/turbine/mesh"
    for source in sources:
        candidate = output_dir / source.name
        before = source.read_text(encoding="utf-8").splitlines()
        after = candidate.read_text(encoding="utf-8").splitlines()
        for marker in ("v ", "vn ", "f ", "usemtl ", "o "):
            if [line for line in before if line.startswith(marker)] != [line for line in after if line.startswith(marker)]:
                raise ValueError(f"几何或材质组变化：{source.name} {marker}")
        if "mtllib turbine_casing_07.mtl" not in after:
            raise ValueError(f"独立MTL缺失：{source.name}")
    mtl = (output_dir / "turbine_casing_07.mtl").read_text(encoding="utf-8")
    for material in ("casing", "inside", "edge", "endcap"):
        if f"newmtl {material}" not in mtl:
            raise ValueError(f"候选MTL缺少材料：{material}")
    if "map_Kd create_nuclear_industry:block/turbine/turbine_casing_07_panel" not in mtl or \
       "map_Kd create_nuclear_industry:block/turbine/turbine_casing_07_endcap" not in mtl:
        raise ValueError("候选MTL的机壳贴图映射错误")
    index = (HERE / "index.html").read_text(encoding="utf-8")
    for target in ("unformed.svg", "formed-d3.svg", "formed-d5.svg", "formed-d7.svg"):
        if f'data="generated/preview/{target}"' not in index:
            raise ValueError(f"首页预览引用错误：{target}")
    for target in ("preview/unformed.svg", "preview/formed-d3.svg", "preview/formed-d5.svg", "preview/formed-d7.svg"):
        if not (GENERATED / target).is_file():
            raise ValueError(f"模型预览不存在：{target}")
    print("OBJ_MAPPING_OK total=212 formed=206 unformed=6")
    print("GEOMETRY_PRESERVED v/vn/f/usemtl/o unchanged for 212 OBJ")
    print("MTL_OK casing/endcap dedicated; inside/edge original mapping retained")
    print("MTL_TEXTURES_OK generated/models/block/turbine/mesh/turbine_casing_07.mtl → turbine_casing_07_panel + turbine_casing_07_endcap")
    print("PREVIEW_OK actual unformed OBJ + formed d3/d5/d7 OBJ sets")
    print("INDEX_REFERENCES_OK index.html → generated/preview/{unformed,formed-d3,formed-d5,formed-d7}.svg")
    print("FORMAL_INSTALL=0 gradle=0 game=0")


def main() -> None:
    parser = argparse.ArgumentParser(description="构建汽轮机机壳B方案候选与实际OBJ预览")
    parser.add_argument("--check", action="store_true", help="检查SVG导出、OBJ候选范围和几何保持")
    if parser.parse_args().check:
        check()
    else:
        build()


if __name__ == "__main__":
    main()
