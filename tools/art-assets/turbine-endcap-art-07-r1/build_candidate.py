"""生成汽轮机R1端盖atlas、限定OBJ UV候选和真实模型对照预览。"""

from __future__ import annotations

import base64
import hashlib
import importlib.util
import json
import math
import re
import struct
import sys
import zlib
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
HERE = Path(__file__).resolve().parent
GENERATED = HERE / "generated"
MESH = ROOT / "src/main/resources/assets/create_nuclear_industry/models/block/turbine/mesh"
BASELINE_MESH = ROOT / "tools/art-assets/turbine-casing-art-07/generated/models/block/turbine/mesh"
TEXTURES = ROOT / "src/main/resources/assets/create_nuclear_industry/textures/block/turbine"
BASELINE_TEXTURES = ROOT / "tools/art-assets/turbine-casing-art-07/generated/textures/block/turbine"
FORMED_RE = re.compile(r"casing_d([357])_(front|middle|rear)_x(-?\d+)_y(-?\d+)\.obj$")
UNFORMED_RE = re.compile(r"turbine_casing_unformed(?:_(up|down|north|south|west|east))?\.obj$")
SVG_NS = "http://www.w3.org/2000/svg"
ATLAS_W, ATLAS_H = 128, 256
REGIONS = {7: (0, 0), 5: (0, 112), 3: (80, 112), "unformed": (112, 0)}
PALETTE = {
    "D": "#28353A", "S": "#3E5058", "M": "#5B7078", "H": "#788E94",
    "L": "#A3B7B8", "W": "#D1DFDA", "C": "#A87942", "c": "#715035",
}
FLAT_COLOR = {"inside": "#28353A", "edge": "#3E5058"}
OLD_TOOL = ROOT / "tools/art-assets/turbine-casing-art-07/build_candidate.py"
spec = importlib.util.spec_from_file_location("turbine_07b_readonly", OLD_TOOL)
if spec is None or spec.loader is None:
    raise RuntimeError("无法只读加载既有OBJ解析器")
old = importlib.util.module_from_spec(spec)
spec.loader.exec_module(old)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def octagon_margin_px(x: float, y: float, diameter: int) -> float:
    """按原模型八棱角切量计算像素内沿距离，保持真实外轮廓。"""
    cut = diameter / (2 + math.sqrt(2))
    margins = (x, diameter - x, y, diameter - y,
               (x + y - cut) / math.sqrt(2),
               (diameter - x + y - cut) / math.sqrt(2),
               (x + diameter - y - cut) / math.sqrt(2),
               (2 * diameter - x - y - cut) / math.sqrt(2))
    return min(margins) * 16


def inside_octagon(x: float, y: float, diameter: int) -> bool:
    cut = diameter / (2 + math.sqrt(2))
    return min(x + y - cut, diameter - x + y - cut,
               x + diameter - y - cut, 2 * diameter - x - y - cut) >= -1e-9


def bolt_centres(diameter: int) -> list[tuple[float, float]]:
    """沿八棱直边按约一格间距放置固定像素大小的螺栓座。"""
    count = 3 if diameter == 7 else 1
    cut = diameter / (2 + math.sqrt(2))
    edge_length = diameter - 2 * cut
    inset = 3.25 / 16
    along = [cut + edge_length * (i + 1) / (count + 1) for i in range(count)]
    points = []
    for value in along:
        points.extend(((value, inset), (diameter - inset, value),
                       (value, diameter - inset), (inset, value)))
    return points


def tile_color(diameter: int, px: int, py: int) -> str:
    """以每世界格16像素绘制钢板、窄法兰、拼缝与有限铜嵌片。"""
    size = diameter * 16
    x, y = (px + 0.5) / 16, (py + 0.5) / 16
    margin = octagon_margin_px(x, y, diameter)
    if not inside_octagon(x, y, diameter):
        return "D"

    # 螺栓座固定为3×3像素，沿八棱直边约一格间距分布。
    for bx, by in bolt_centres(diameter):
        cx, cy = round(bx * 16 - 0.5), round(by * 16 - 0.5)
        ox, oy = abs(px - cx), abs(py - cy)
        if ox <= 1 and oy <= 1:
            if ox == 0 and oy == 0:
                return "M"
            return "H" if px <= cx and py <= cy else "D"

    # 细八角法兰由稳定钢色带构成，外缘压暗，内缘保留一像素高光。
    if margin < 1.2:
        return "D"
    if margin < 2.2:
        return "S"
    if margin < 3.2:
        return "H"
    if margin < 5.0:
        return "M"

    half = size // 2
    hub_dx, hub_dy = abs(px + 0.5 - half), abs(py + 0.5 - half)
    hub_r = max(hub_dx, hub_dy)
    # 中心轴座用同心钢层收口，内凹轴孔保持纯暗色。
    if 7 <= hub_r < 8:
        return "L"
    if 6 <= hub_r < 7:
        return "H"
    if 4 <= hub_r < 6:
        return "D"
    if hub_r < 4:
        return "D"

    # 四块大面积钢板以暗拼缝和相邻一像素高光分界。
    seam_x = px == half
    seam_y = py == half
    if (seam_x or seam_y) and hub_r > 7:
        return "D"
    if (px == half + 1 or py == half + 1) and hub_r > 7:
        return "H"

    # 轴座向四方伸出短加强筋，避开中央十字缝并保持固定宽度。
    if 8 <= hub_dy <= 14 and 1.5 <= hub_dx <= 3:
        return "H" if px < half else "S"
    if 8 <= hub_dx <= 14 and 1.5 <= hub_dy <= 3:
        return "H" if py < half else "S"

    # 少量2×2铜嵌片置于轴座外侧四角，不组成环带也不覆盖钢板缝。
    copper_centres = ((half - 10, half - 10), (half + 8, half - 10),
                      (half - 10, half + 8), (half + 8, half + 8))
    for cx, cy in copper_centres:
        if cx <= px < cx + 2 and cy <= py < cy + 2:
            return "C" if (px + py) % 2 == 0 else "c"

    # 板面保持整片钢色，只用大块明暗区区分受光方向。
    if px < half and py < half:
        return "M"
    if px >= half and py >= half:
        return "S"
    return "M"


def build_atlas_grid() -> list[list[str]]:
    grid = [["D"] * ATLAS_W for _ in range(ATLAS_H)]
    for diameter in (3, 5, 7):
        ox, oy = REGIONS[diameter]
        for py in range(diameter * 16):
            for px in range(diameter * 16):
                grid[oy + py][ox + px] = tile_color(diameter, px, py)
    # 未成型端面实际高仅3/16格，映射成16×3像素细节，保持像素/世界格比例。
    ox, oy = REGIONS["unformed"]
    for py in range(3):
        for px in range(16):
            grid[oy + py][ox + px] = ("H" if py == 0 else "S" if py == 2 else "M")
            if px in (3, 12) and py == 1:
                grid[oy + py][ox + px] = "L"
            elif px in (7, 8) and py == 1:
                grid[oy + py][ox + px] = "D"
    return grid


def svg_for_grid(grid: list[list[str]]) -> str:
    rows = [f'<svg xmlns="{SVG_NS}" width="{ATLAS_W}" height="{ATLAS_H}" viewBox="0 0 {ATLAS_W} {ATLAS_H}" shape-rendering="crispEdges" data-source="ART-TURBINE-07B-R1">',
            "<title>汽轮机分块钢制端盖atlas</title>"]
    for y, row in enumerate(grid):
        x = 0
        while x < ATLAS_W:
            end = x + 1
            while end < ATLAS_W and row[end] == row[x]:
                end += 1
            rows.append(f'<rect x="{x}" y="{y}" width="{end-x}" height="1" fill="{PALETTE[row[x]]}"/>')
            x = end
    rows.append("</svg>")
    return "\n".join(rows) + "\n"


def png_chunk(kind: bytes, data: bytes) -> bytes:
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)


def export_png(grid: list[list[str]]) -> bytes:
    raw = b"".join(b"\x00" + b"".join(bytes.fromhex(PALETTE[key][1:]) + b"\xff" for key in row) for row in grid)
    return (b"\x89PNG\r\n\x1a\n" +
            png_chunk(b"IHDR", struct.pack(">IIBBBBB", ATLAS_W, ATLAS_H, 8, 6, 0, 0, 0)) +
            png_chunk(b"IDAT", zlib.compress(raw, 9)) + png_chunk(b"IEND", b""))


def source_targets() -> list[Path]:
    formed = sorted(p for p in BASELINE_MESH.glob("casing_d*_*.obj") if FORMED_RE.fullmatch(p.name))
    loose = sorted(p for p in BASELINE_MESH.glob("turbine_casing_unformed*.obj") if UNFORMED_RE.fullmatch(p.name))
    return formed + loose


def parse_for_updates(path: Path, atlas: bool) -> tuple[str, dict]:
    doc = old.parse_obj(path)
    details = old.formed_details(path)
    updates: dict[int, tuple[float, float]] = {}
    endcap_indices = set()
    non_endcap_indices = set()
    for face in doc["faces"]:
        target = endcap_indices if face["material"] == "endcap" else non_endcap_indices
        for _, ti, _ in face["indices"]:
            if ti:
                target.add(ti)
    if not endcap_indices:
        return "".join(doc["lines"]), {}
    overlap = endcap_indices & non_endcap_indices
    if overlap:
        raise ValueError(f"endcap vt也被其他材质使用，拒绝跨材质改UV：{path.name} {sorted(overlap)[:4]}")

    for face in doc["faces"]:
        if face["material"] != "endcap":
            continue
        for _, ti, _ in face["indices"]:
            u, v = doc["uvs"][ti - 1]
            if details:
                diameter = details[0]
                rx, ry = REGIONS[diameter]
                # 既有07B UV已表示完整截面0..1；按真实atlas宽高换算，跨格无缝。
                new_u = (rx + u * diameter * 16) / ATLAS_W
                new_v = (ry + v * diameter * 16) / ATLAS_H
            else:
                rx, ry = REGIONS["unformed"]
                # 未成型端面只占1格×3/16格，映射到16×3像素小区而不拉伸细节。
                new_u = (rx + u * 16) / ATLAS_W
                new_v = (ry + (v - 13 / 16) / (3 / 16) * 3) / ATLAS_H
            value = (max(0.0, min(1.0, new_u)), max(0.0, min(1.0, new_v)))
            if ti in updates and any(abs(updates[ti][i] - value[i]) > 1e-7 for i in range(2)):
                raise ValueError(f"同一endcap vt需不同atlas UV：{path.name} vt={ti}")
            updates[ti] = value

    lines = doc["lines"]
    vt_lines = [i for i, line in enumerate(lines) if line.lstrip().startswith("vt ")]
    for index, (u, v) in updates.items():
        line_index = vt_lines[index - 1]
        ending = "\r\n" if lines[line_index].endswith("\r\n") else "\n" if lines[line_index].endswith("\n") else ""
        lines[line_index] = f"vt {u:.7f} {v:.7f}{ending}"
    content = "".join(lines)
    record = {
        "source": path.relative_to(ROOT).as_posix(),
        "formal_target": (MESH / path.name).relative_to(ROOT).as_posix(),
        "candidate": (Path("generated/models/block/turbine/mesh") / path.name).as_posix(),
        "diameter": details[0] if details else None,
        "segment": details[1] if details else "unformed",
        "changed_endcap_vt": len(updates),
        "sha256_source": sha256(path.read_bytes()),
        "sha256_candidate": sha256(content.encode("utf-8")),
    }
    return content, record


def make_obj_candidates() -> list[dict]:
    out_dir = GENERATED / "models/block/turbine/mesh"
    out_dir.mkdir(parents=True, exist_ok=True)
    records = []
    for path in source_targets():
        content, record = parse_for_updates(path, atlas=True)
        if not record:
            continue
        (out_dir / path.name).write_text(content, encoding="utf-8", newline="")
        records.append(record)
    formed = [r for r in records if r["diameter"]]
    loose = [r for r in records if not r["diameter"]]
    if len(formed) != 158 or len(loose) != 6:
        raise ValueError(f"端盖消费者数量不符：成型{len(formed)}，未成型{len(loose)}")
    return records


def affine(screen: list[tuple[float, float]], uv: list[tuple[float, float]], tex_w: int, tex_h: int):
    source = [(u * tex_w, v * tex_h) for u, v in uv]
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
    raise ValueError("OBJ预览UV退化，无法把真实贴图采样到模型面")


def png_size(data: bytes) -> tuple[int, int]:
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError("预览输入不是PNG")
    return struct.unpack(">II", data[16:24])


def model_instances(diameter: int, variant: str) -> list[tuple[Path, tuple[float, float, float]]]:
    """按旧/新预览明确选择正式OBJ或候选OBJ，避免混用UV与贴图。"""
    if variant not in ("old", "new"):
        raise ValueError(f"未知预览版本：{variant}")
    items = []
    for source in source_targets():
        details = old.formed_details(source)
        if not details or details[0] != diameter:
            continue
        _, role, x, y = details
        z = {"front": 0.0, "middle": 1.0, "rear": 2.0}[role]
        candidate = GENERATED / "models/block/turbine/mesh" / source.name
        selected = candidate if variant == "new" and candidate.is_file() else source
        if variant == "new" and role != "middle" and not candidate.is_file():
            raise FileNotFoundError(f"缺少端盖UV候选OBJ：{candidate}")
        items.append((selected, (float(x), float(y), z)))
    return items


def render_model(title: str, instances: list[tuple[Path, tuple[float, float, float]]],
                 textures: dict[str, bytes], scale: float) -> str:
    faces = []
    serial = 0
    for path, (ox, oy, oz) in instances:
        doc = old.parse_obj(path)
        for face in doc["faces"]:
            material = face["material"]
            if material not in ("casing", "endcap", "edge", "inside"):
                continue
            points, uvs = [], []
            for vi, ti, _ in face["indices"]:
                x, y, z = doc["vertices"][vi - 1]
                points.append((x + ox, y + oy, z + oz))
                uvs.append(doc["uvs"][ti - 1])
            depth = sum(sum(point) for point in points) / len(points)
            faces.append({"material": material, "points": points, "uvs": uvs, "depth": depth, "serial": serial})
            serial += 1
    project = lambda point: ((point[0] - point[2]) * scale,
                             ((point[0] + point[2]) * 0.5 - point[1]) * scale)
    projected = [[project(point) for point in face["points"]] for face in faces]
    all_points = [p for polygon in projected for p in polygon]
    min_x, max_x = min(p[0] for p in all_points), max(p[0] for p in all_points)
    min_y, max_y = min(p[1] for p in all_points), max(p[1] for p in all_points)
    width, height = math.ceil(max_x - min_x + 72), math.ceil(max_y - min_y + 90)
    dx, dy = 36 - min_x, 48 - min_y
    urls = {key: "data:image/png;base64," + base64.b64encode(value).decode("ascii") for key, value in textures.items()}
    dims = {key: png_size(value) for key, value in textures.items()}
    rows = [f'<svg xmlns="{SVG_NS}" width="{width}" height="{height}" viewBox="0 0 {width} {height}" shape-rendering="crispEdges">',
            f"<title>{title}</title>", '<rect width="100%" height="100%" fill="#10171b"/>',
            f'<text x="18" y="27" fill="#e5e7e3" font-family="sans-serif" font-size="16">{title}</text>', "<defs>"]
    prepared = []
    for face, polygon in zip(faces, projected):
        screen = [(x + dx, y + dy) for x, y in polygon]
        serial = face["serial"]
        pts = " ".join(f"{x:.3f},{y:.3f}" for x, y in screen)
        rows.append(f'<clipPath id="cap{serial}" clipPathUnits="userSpaceOnUse"><polygon points="{pts}"/></clipPath>')
        prepared.append((face, screen, pts))
    rows.append("</defs>")
    for face, screen, pts in sorted(prepared, key=lambda item: (item[0]["depth"], item[0]["serial"])):
        material = face["material"]
        if material in urls:
            tw, th = dims[material]
            a, b, c, d, e, f = affine(screen, face["uvs"], tw, th)
            matrix = f"{a:.8f} {b:.8f} {c:.8f} {d:.8f} {e:.4f} {f:.4f}"
            rows.append(f'<g clip-path="url(#cap{face["serial"]})"><image href="{urls[material]}" x="0" y="0" width="{tw}" height="{th}" preserveAspectRatio="none" image-rendering="pixelated" transform="matrix({matrix})"/></g>')
        else:
            rows.append(f'<polygon points="{pts}" fill="{FLAT_COLOR[material]}" stroke="#101519" stroke-width="0.35"/>')
    rows.append("</svg>")
    return "\n".join(rows) + "\n"


def save_previews() -> None:
    preview = GENERATED / "preview"
    preview.mkdir(parents=True, exist_ok=True)
    old_endcap = (BASELINE_TEXTURES / "turbine_casing_07_endcap.png").read_bytes()
    panel = (BASELINE_TEXTURES / "turbine_casing_07_panel.png").read_bytes()
    new_endcap = (GENERATED / "textures/block/turbine/turbine_casing_07_endcap.png").read_bytes()
    up = BASELINE_MESH / "turbine_casing_unformed.obj"
    for variant, endcap in (("old", old_endcap), ("new", new_endcap)):
        texture_pair = {"casing": panel, "endcap": endcap}
        (preview / f"unformed-{variant}.svg").write_text(render_model(f"未成型｜{variant}端盖", [(GENERATED / "models/block/turbine/mesh" / up.name if variant == "new" else up, (0, 0, 0))], texture_pair, 170), encoding="utf-8", newline="\n")
        for diameter in (3, 5, 7):
            title = f"d{diameter}成型｜前/中/后OBJ｜{variant}端盖"
            out = preview / f"formed-d{diameter}-{variant}.svg"
            (out).write_text(render_model(title, model_instances(diameter, variant), texture_pair, 35), encoding="utf-8", newline="\n")


def svg_index() -> str:
    rows = ["""<!doctype html>
<html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>汽轮机端盖R1候选</title>
<style>body{margin:0;background:#0d1519;color:#e8e6e0;font:16px/1.5 system-ui,sans-serif}main{max-width:1500px;margin:auto;padding:20px}h1{margin:0}.lead{color:#b8c4c2}.grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.card{background:#192328;border:1px solid #38474d;border-radius:8px;padding:12px;overflow:auto}.model{display:block;width:100%;height:360px}.pair{display:grid;grid-template-columns:1fr 1fr;gap:10px}.pair object{height:300px}.meta{color:#c3cbc6;font-size:14px}.atlas{image-rendering:pixelated;width:128px;height:auto;max-width:100%}code{color:#c9dcd5}</style>
<main><h1>汽轮机B端盖｜R1分块钢板</h1><p class="lead">只更换端盖纹理并映射现有endcap面；B侧壳、专用MTL、几何和其他UV保持。候选128×256 atlas按每世界格16像素绘制，端盖纹理试用版已安装，真实游戏画面待用户复看。</p>
<section class="card"><h2>128×256端盖atlas</h2><img class="atlas" src="generated/textures/block/turbine/turbine_casing_07_endcap.png" alt="端盖atlas"></section>
"""]
    for diameter in (3, 5, 7):
        rows.append(f'<section class="card"><h2>d{diameter}格：旧/新端盖，实际前中后OBJ</h2><div class="pair"><article><h3>原端盖</h3><object class="model" type="image/svg+xml" data="generated/preview/formed-d{diameter}-old.svg"></object></article><article><h3>钢制端盖候选</h3><object class="model" type="image/svg+xml" data="generated/preview/formed-d{diameter}-new.svg"></object></article></div></section>')
    rows.append('<section class="card"><h2>未成型端盖对照</h2><div class="pair"><article><h3>原端盖</h3><object class="model" type="image/svg+xml" data="generated/preview/unformed-old.svg"></object></article><article><h3>钢制端盖候选</h3><object class="model" type="image/svg+xml" data="generated/preview/unformed-new.svg"></object></article></div></section>')
    rows.append('<section class="card"><p>新纹理源为本目录整数rect SVG；候选PNG由同源SVG导出。预览使用当前正式侧壳PNG、旧端盖PNG与真实OBJ几何/UV；候选端盖采样按128×256图像尺寸换算。完整受影响清单见 <code>mapping.json</code>。</p></section></main></html>')
    return "\n".join(rows)


def make_mapping(records: list[dict]) -> None:
    counts = {str(d): sum(1 for r in records if r["diameter"] == d) for d in (3, 5, 7)}
    mapping = {
        "task": "ART-TURBINE-07B-R1", "stage": "R1端盖试用版已安装，游戏内视觉仍待用户复看",
        "formal_target_png": "src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_casing_07_endcap.png",
        "source_texture": "src/main/resources/assets/create_nuclear_industry/textures/block/turbine/turbine_casing_07_endcap.png",
        "baseline_source_texture": (BASELINE_TEXTURES / "turbine_casing_07_endcap.png").relative_to(ROOT).as_posix(),
        "baseline_source_texture_sha256": sha256((BASELINE_TEXTURES / "turbine_casing_07_endcap.png").read_bytes()),
        "atlas": {"size": [ATLAS_W, ATLAS_H], "pixel_density": "16像素/世界格", "regions": {
            "d7": {"origin": REGIONS[7], "size": [112, 112]},
            "d5": {"origin": REGIONS[5], "size": [80, 80]},
            "d3": {"origin": REGIONS[3], "size": [48, 48]},
            "unformed": {"origin": REGIONS["unformed"], "size": [16, 3]},
        }},
        "consumer_counts": {"formed_d3_front_rear": 18, "formed_d5_front_rear": 50,
                             "formed_d7_front_rear": 90, "unformed": 6,
                             "total_obj": len(records), "middle_excluded": 48},
        "uv_contract": "只重映射endcap面使用的vt；成型沿现有完整d截面UV换算至对应atlas区，未成型16×3像素小区；所有其他vt和OBJ非vt记录逐字保持",
        "obj_targets": records,
    }
    (HERE / "mapping.json").write_text(json.dumps(mapping, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def check_candidate() -> None:
    """核本批atlas源、实际OBJ映射及唯一端盖面写集。"""
    mapping = json.loads((HERE / "mapping.json").read_text(encoding="utf-8"))
    svg_root = ET.parse(HERE / "endcap-atlas.svg").getroot()
    if svg_root.attrib.get("width") != str(ATLAS_W) or svg_root.attrib.get("height") != str(ATLAS_H):
        raise ValueError("SVG atlas尺寸不是128×256")
    symbols = {color.lower(): symbol for symbol, color in PALETTE.items()}
    svg_grid: list[list[str | None]] = [[None] * ATLAS_W for _ in range(ATLAS_H)]
    rect_count = 0
    for element in list(svg_root):
        if element.tag.endswith("title"):
            continue
        if not element.tag.endswith("rect"):
            raise ValueError(f"SVG含非rect绘图元素：{element.tag}")
        values = [element.attrib.get(name, "") for name in ("x", "y", "width", "height")]
        if any(not re.fullmatch(r"\d+", value) for value in values):
            raise ValueError(f"SVG rect含非整数坐标：{values}")
        x, y, width, height = (int(value) for value in values)
        color = element.attrib.get("fill", "").lower()
        if color not in symbols or width < 1 or height < 1 or x + width > ATLAS_W or y + height > ATLAS_H:
            raise ValueError(f"SVG rect超界或使用未批准色：{element.attrib}")
        for py in range(y, y + height):
            for px in range(x, x + width):
                if svg_grid[py][px] is not None:
                    raise ValueError(f"SVG rect重叠于{x},{y}")
                svg_grid[py][px] = symbols[color]
        rect_count += 1
    if any(cell is None for row in svg_grid for cell in row):
        raise ValueError("SVG atlas存在未绘制像素")
    concrete_grid = [[cell for cell in row if cell is not None] for row in svg_grid]
    png_path = GENERATED / "textures/block/turbine/turbine_casing_07_endcap.png"
    if export_png(concrete_grid) != png_path.read_bytes():
        raise ValueError("PNG与解析后的原创整数rect SVG不一致")

    copper_count = sum(row.count("C") + row.count("c") for row in concrete_grid)
    if copper_count < 32:
        raise ValueError(f"atlas未实际绘出少量铜嵌片：像素数={copper_count}")
    d3_ox, d3_oy = REGIONS[3]
    rib_samples = ((d3_ox + 22, d3_oy + 32), (d3_ox + 32, d3_oy + 22))
    if any(concrete_grid[y][x] != "H" for x, y in rib_samples):
        raise ValueError("d3短加强筋指定采样像素未绘出")

    records = mapping["obj_targets"]
    if len(records) != 164 or mapping["consumer_counts"]["total_obj"] != 164:
        raise ValueError(f"端盖消费者应为164，实际{len(records)}")
    changed_total = 0
    for record in records:
        baseline = ROOT / Path(record["source"])
        formal = ROOT / Path(record["formal_target"])
        candidate = HERE / Path(record["candidate"])
        if sha256(baseline.read_bytes()) != record["sha256_source"]:
            raise ValueError(f"07B UV基线漂移：{baseline}")
        if formal.read_bytes() != baseline.read_bytes():
            raise ValueError(f"正式OBJ源在安装前已漂移：{formal}")
        candidate_bytes = candidate.read_bytes()
        if sha256(candidate_bytes) != record["sha256_candidate"]:
            raise ValueError(f"候选OBJ SHA不符：{candidate}")
        source_lines = baseline.read_text(encoding="utf-8").splitlines(keepends=True)
        candidate_lines = candidate.read_text(encoding="utf-8").splitlines(keepends=True)
        if len(source_lines) != len(candidate_lines):
            raise ValueError(f"OBJ行数改变：{candidate}")
        source_doc = old.parse_obj(baseline)
        endcap_vt = {ti for face in source_doc["faces"] if face["material"] == "endcap"
                     for _, ti, _ in face["indices"] if ti}
        vt_index = 0
        changed = set()
        for source_line, candidate_line in zip(source_lines, candidate_lines):
            if source_line.lstrip().startswith("vt "):
                vt_index += 1
                if source_line != candidate_line:
                    changed.add(vt_index)
            elif source_line != candidate_line:
                raise ValueError(f"OBJ非vt记录被改动：{candidate}")
        if not changed <= endcap_vt:
            raise ValueError(f"endcap以外vt被改动：{candidate}")
        changed_total += len(changed)
        mtl_lines = [line for line in source_lines if line.lstrip().startswith("mtllib ")]
        if not mtl_lines or any(not (baseline.parent / line.split(None, 1)[1].strip()).is_file() for line in mtl_lines):
            raise ValueError(f"OBJ材质库引用缺失：{baseline}")
        target_uvs = old.parse_obj(candidate)["uvs"]
        for ti in endcap_vt:
            u, v = target_uvs[ti - 1]
            if not (0 <= u <= 1 and 0 <= v <= 1):
                raise ValueError(f"endcap atlas UV超界：{candidate} vt={ti}")

    png_target = ROOT / Path(mapping["formal_target_png"])
    baseline_png = ROOT / Path(mapping["baseline_source_texture"])
    if sha256(baseline_png.read_bytes()) != mapping["baseline_source_texture_sha256"]:
        raise ValueError("07B旧端盖PNG基线漂移")
    if png_target.read_bytes() != baseline_png.read_bytes():
        raise ValueError("正式端盖PNG在安装前已漂移")
    print(f"CHECK_OK svg_rects={rect_count} png_svg_identity=1 atlas=128x256 copper_pixels={copper_count} short_ribs=2 obj_targets=164 changed_endcap_vt={changed_total} non_endcap_vt_and_other_records=unchanged mtllib_refs=valid")


def check_installed() -> None:
    """确认精确安装的164 OBJ和1 PNG逐字匹配候选。"""
    mapping = json.loads((HERE / "mapping.json").read_text(encoding="utf-8"))
    checked = 0
    for record in mapping["obj_targets"]:
        formal = ROOT / Path(record["formal_target"])
        candidate = HERE / Path(record["candidate"])
        if formal.read_bytes() != candidate.read_bytes():
            raise ValueError(f"安装OBJ与候选字节不一致：{formal}")
        checked += 1
    png_target = ROOT / Path(mapping["formal_target_png"])
    png_candidate = GENERATED / "textures/block/turbine/turbine_casing_07_endcap.png"
    if png_target.read_bytes() != png_candidate.read_bytes():
        raise ValueError("安装PNG与候选字节不一致")
    print(f"INSTALL_CHECK_OK targets={checked + 1} obj={checked} png=1 byte_identity=1")


def build() -> None:
    grid = build_atlas_grid()
    (HERE / "endcap-atlas.svg").write_text(svg_for_grid(grid), encoding="utf-8", newline="\n")
    texture_dir = GENERATED / "textures/block/turbine"
    texture_dir.mkdir(parents=True, exist_ok=True)
    (texture_dir / "turbine_casing_07_endcap.png").write_bytes(export_png(grid))
    records = make_obj_candidates()
    make_mapping(records)
    (HERE / "index.html").write_text(svg_index(), encoding="utf-8", newline="\n")
    readme = """# 汽轮机B端盖R1候选

本批只更换现有B机壳endcap面使用的UV和端盖atlas；侧壳、专用MTL、OBJ几何与其他UV不变。源为128×256原创整数rect SVG，3/5/7完整八棱区域及未成型16×3区域维持每世界格16像素。

- [真实OBJ/UV旧新对照](index.html)
- [端盖atlas SVG](endcap-atlas.svg)
- [精确端盖消费者与SHA](mapping.json)

只含端盖面的164个OBJ进入候选；48个middle模型不含endcap面，未写。候选纹理和164个端盖UV已安装并完成一次assemble；实际游戏视觉仍待用户复看，工具只生成隔离候选副本。
"""
    (HERE / "README.md").write_text(readme, encoding="utf-8", newline="\n")
    save_previews()
    print(f"CANDIDATE_READY atlas=128x256 svg=1 png=1 obj_endcap_targets={len(records)} previews=8")


if __name__ == "__main__":
    if len(sys.argv) == 1:
        build()
    elif sys.argv[1:] == ["--check"]:
        check_candidate()
    elif sys.argv[1:] == ["--check-installed"]:
        check_installed()
    else:
        raise SystemExit("用法：build_candidate.py [--check|--check-installed]")

