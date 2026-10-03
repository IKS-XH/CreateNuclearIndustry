"""EXT-A-FUEL-02E：从可编辑SVG构造八格静态模型、活动件与真实几何预览。"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
ART = ROOT / "tools/art-assets"
sys.path.insert(0, str(ART))
import export as strict_exporter

ASSET = ROOT / "src/main/resources/assets/create_nuclear_industry"
SOURCE = ART / "sources/fuel-02e"
GENERATED = ART / "generated/block"
EVIDENCE = ROOT / "build/reports/extension/EXT-A-FUEL-02E-assets"
PREFIX = "shielded_assembly_multiblock"
MODEL_PREFIX = "create_nuclear_industry:block/shielded_assembly_station"
TEXTURE_PREFIX = f"create_nuclear_industry:block/{PREFIX}"

# 所有颜色由此处统一约束，SVG本身始终是可独立修改的权威稿。
COLORS = {
    "shadow": "#19272C", "lead": "#46565B", "lead_light": "#68787B",
    "steel": "#75888A", "steel_light": "#A4B3AC", "brass": "#B79A58",
    "brass_light": "#E0C477", "recess": "#101B21", "glass": "#263F46",
    "glass_light": "#5A8182", "orange": "#E7A83C", "orange_light": "#FFD477",
}


def svg_for(name: str) -> str:
    """建立逐像素、无滤镜的图样；窄杆采用纯金属图，不挤压整窗。"""
    c = COLORS
    rects: list[tuple[int, int, int, int, str]] = []
    def add(x: int, y: int, w: int, h: int, color: str) -> None:
        rects.append((x, y, w, h, c[color]))
    if name == "lead":
        add(0, 0, 16, 16, "lead")
        add(0, 0, 16, 2, "lead_light")
        add(0, 14, 16, 2, "shadow")
        add(2, 4, 12, 1, "steel")
        add(2, 11, 12, 1, "shadow")
        for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
            add(x, y, 1, 1, "brass")
    elif name == "steel":
        add(0, 0, 16, 16, "steel")
        add(0, 0, 16, 2, "steel_light")
        add(0, 14, 16, 2, "lead")
        add(2, 5, 12, 1, "lead_light")
        add(2, 10, 12, 1, "lead_light")
    elif name == "brass":
        add(0, 0, 16, 16, "brass")
        add(0, 0, 16, 2, "brass_light")
        add(0, 14, 16, 2, "lead")
        add(2, 5, 12, 1, "brass_light")
    elif name == "window":
        add(0, 0, 16, 16, "recess")
        add(1, 1, 14, 14, "glass")
        add(2, 2, 10, 2, "glass_light")
        add(3, 5, 9, 1, "lead_light")
        add(5, 8, 7, 1, "glass_light")
        add(1, 13, 14, 2, "shadow")
    elif name == "deck":
        add(0, 0, 16, 16, "lead")
        for z in (2, 6, 10, 14):
            add(1, z, 14, 1, "shadow")
        add(2, 2, 1, 12, "steel")
        add(13, 2, 1, 12, "steel")
    elif name == "top":
        add(0, 0, 16, 16, "lead")
        add(0, 0, 16, 2, "steel")
        add(0, 14, 16, 2, "lead_light")
        add(2, 2, 12, 12, "steel")
        add(3, 3, 10, 10, "lead_light")
        add(4, 4, 8, 8, "lead")
        add(2, 2, 2, 2, "brass")
        add(12, 2, 2, 2, "brass")
        add(2, 12, 2, 2, "brass")
        add(12, 12, 2, 2, "brass")
    elif name == "bottom":
        add(0, 0, 16, 16, "shadow")
        add(1, 1, 14, 14, "lead")
        add(3, 3, 10, 10, "steel")
        add(5, 5, 6, 6, "shadow")
        add(6, 6, 4, 4, "brass")
        add(7, 7, 2, 2, "shadow")
    elif name == "port":
        add(0, 0, 16, 16, "lead")
        add(1, 1, 14, 14, "steel")
        add(3, 3, 10, 10, "brass")
        add(4, 4, 8, 8, "recess")
        add(6, 6, 4, 4, "glass")
        add(7, 7, 2, 2, "steel_light")
    elif name in ("lamp_off", "lamp_on"):
        add(0, 0, 16, 16, "shadow")
        add(2, 2, 12, 12, "brass")
        add(4, 4, 8, 8, "orange" if name == "lamp_on" else "glass")
        add(5, 5, 6, 2, "orange_light" if name == "lamp_on" else "glass_light")
        add(4, 12, 8, 2, "lead")
    else:
        raise ValueError(name)
    body = "\n".join(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{color}"/>' for x, y, w, h, color in rects)
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" shape-rendering="crispEdges">\n{body}\n</svg>\n'


TEXTURES = ("lead", "steel", "brass", "window", "deck", "top", "bottom", "port", "lamp_off", "lamp_on")


def initialize_missing_sources() -> None:
    """初始化只补缺失稿，默认导出绝不重置现有SVG。"""
    SOURCE.mkdir(parents=True, exist_ok=True)
    for name in TEXTURES:
        path = SOURCE / f"{PREFIX}_{name}.svg"
        if not path.exists():
            path.write_text(svg_for(name), encoding="utf-8")


def dump(path: Path, value: dict) -> None:
    """把本任务范围内的模型和rig写成稳定JSON。"""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def textures() -> dict[str, str]:
    """给每个模型提供独立、完整的材质表。"""
    result = {name: f"{TEXTURE_PREFIX}_{name}" for name in TEXTURES}
    result["particle"] = result["lead"]
    return result


def element(a: tuple[float, float, float], b: tuple[float, float, float], tex: str,
            faces: tuple[str, ...] = ("north", "south", "east", "west", "up", "down")) -> dict:
    """按真实几何跨度采样UV，避免窄框条映射完整观察窗。"""
    x0, y0, z0 = a
    x1, y1, z1 = b
    def uv(face: str) -> list[float]:
        if face in ("north", "south"):
            return [x0 % 16, 16 - (y1 % 16 or 16), x1 % 16 or 16, 16 - y0 % 16]
        if face in ("east", "west"):
            return [z0 % 16, 16 - (y1 % 16 or 16), z1 % 16 or 16, 16 - y0 % 16]
        return [x0 % 16, z0 % 16, x1 % 16 or 16, z1 % 16 or 16]
    return {"from": list(a), "to": list(b), "faces": {f: {"texture": f"#{tex}", "uv": uv(f)} for f in faces}}


# 整机坐标以主控方块最小角为原点。每个静态盒在分块边界切开，活动件另行导出。
STATIC: list[tuple[tuple[float, float, float], tuple[float, float, float], str]] = []
def box(a: tuple[float, float, float], b: tuple[float, float, float], material: str) -> None:
    STATIC.append((a, b, material))


def design() -> None:
    """封闭底台、铅灰机罩及黄铜加强筋，中间为从前面可见的装配工位。"""
    if STATIC:
        return
    box((2, 0, 0), (30, 4, 32), "steel")
    box((0, 0, 2), (2, 4, 30), "steel")
    box((30, 0, 2), (32, 4, 30), "steel")
    box((1, 4, 1), (31, 7, 31), "lead")
    box((3, 7, 3), (29, 9, 29), "deck")
    box((2, 28, 0), (30, 32, 32), "top")
    box((0, 28, 2), (2, 32, 30), "top")
    box((30, 28, 2), (32, 32, 30), "top")
    box((2, 26, 2), (30, 28, 30), "steel")
    # 角柱和底、顶两道跨格围栏使四面的2×2外观连贯。
    # 四角用单独的旋转棱柱封口，避免在斜面后面残留可见方盒。
    for x in (2, 27):
        for low, high in ((8, 10), (25, 28)):
            box((x, low, 3), (x + 3, high, 29), "brass")
    for z in (2, 27):
        for low, high in ((8, 10), (25, 28)):
            box((3, low, z), (29, high, z + 3), "brass")
    # 前方中央窗口留空；后墙和左右侧的实心薄板可以接受漏斗。
    box((4, 8, 0), (28, 12, 2), "lead")
    box((4, 25, 0), (28, 28, 2), "lead")
    for x in (4, 27):
        box((x, 12, 0), (x + 1, 25, 2), "brass")
    box((4, 12, 29), (28, 27, 32), "lead")
    box((12, 14, 28), (20, 22, 29), "window")
    box((0, 8, 4), (3, 27, 28), "lead")
    box((29, 8, 4), (32, 27, 28), "lead")
    # 侧板上以凸起的接口板标记连续可接触面，不编码料槽身份。
    for z in (6, 22):
        for y in (10, 20):
            box((0, y, z), (1, y + 6, z + 6), "port")
            box((31, y, z), (32, y + 6, z + 6), "port")
    for x in (6, 22):
        for y in (10, 20):
            box((x, y, 31), (x + 6, y + 6, 32), "port")
    # 窗口后方的内机架与中央地台不占前玻璃口。
    box((5, 10, 25), (27, 11, 29), "steel")
    for x in (4, 26):
        box((x, 9, 6), (x + 2, 25, 8), "steel")
        box((x, 22, 7), (x + 2, 24, 25), "steel")
    box((12, 9, 11), (20, 11, 22), "brass")
    # 端面短条的纹理仅用金属/灯区，不把旧单格整张窗贴到横梁。
    box((14, 26, 0), (18, 28, 1), "lamp_off")


def chamfer_elements(part: int) -> list[dict]:
    """在被切除的四角放外露45度棱柱；三个高度段都不叠回原方角。"""
    x, z, y = part % 2, (part // 2) % 2, part // 4
    ox, oz = 16 * x, 16 * z
    angle = 45 if x == z else -45
    result = []
    for low, high, radius, half_width, distance, material in (
        (0, 4, 1.2, .55, 1.3, "steel"),
        (7, 16, 2.1, .85, 2.3, "lead"),
        (16, 28, 2.1, .85, 2.3, "lead"),
        (28, 32, 1.2, .55, 1.3, "top"),
    ):
        if not (16 * y <= low and high <= 16 * (y + 1)):
            continue
        cx = distance if x == 0 else 32 - distance
        cz = distance if z == 0 else 32 - distance
        a = (cx - radius - ox, low - 16 * y, cz - half_width - oz)
        b = (cx + radius - ox, high - 16 * y, cz + half_width - oz)
        edge = element(a, b, material)
        edge["rotation"] = {"origin": [cx - ox, low - 16 * y, cz - oz],
                            "axis": "y", "angle": angle, "rescale": False}
        result.append(edge)
    return result


def static_parts(working: bool) -> dict[int, dict]:
    """切到八个局部0..16模型，切面不渲染，避免内面闪烁。"""
    design()
    output: dict[int, dict] = {}
    for part in range(8):
        x, z, y = part % 2, (part // 2) % 2, part // 4
        offset = (16 * x, 16 * y, 16 * z)
        pieces: list[dict] = []
        for a, b, material in STATIC:
            lo = tuple(max(a[i], offset[i]) for i in range(3))
            hi = tuple(min(b[i], offset[i] + 16) for i in range(3))
            if any(lo[i] >= hi[i] for i in range(3)):
                continue
            local_a = tuple(lo[i] - offset[i] for i in range(3))
            local_b = tuple(hi[i] - offset[i] for i in range(3))
            shown = tuple(f for f, axis, edge in (("west", 0, 0), ("east", 0, 1), ("down", 1, 0), ("up", 1, 1), ("north", 2, 0), ("south", 2, 1))
                          if (lo if edge == 0 else hi)[axis] == (a if edge == 0 else b)[axis])
            if shown:
                mat = "lamp_on" if working and material == "lamp_off" else material
                piece = element(local_a, local_b, mat, shown)
                if mat == "port":
                    for face in ("west", "east", "south"):
                        if face in piece["faces"]:
                            piece["faces"][face]["uv"] = [0, 0, 16, 16]
                if mat == "window" and "north" in piece["faces"]:
                    piece["faces"]["north"]["uv"] = [
                        16 * (lo[0] - a[0]) / (b[0] - a[0]), 16 * (b[1] - hi[1]) / (b[1] - a[1]),
                        16 * (hi[0] - a[0]) / (b[0] - a[0]), 16 * (b[1] - lo[1]) / (b[1] - a[1]),
                    ]
                if part == 0 and a == (2, 0, 0) and b == (30, 4, 32) and "down" in piece["faces"]:
                    piece["faces"]["down"]["texture"] = "#bottom"
                pieces.append(piece)
        pieces.extend(chamfer_elements(part))
        output[part] = {"credit": "EXT-A-FUEL-02E", "ambientocclusion": True, "textures": textures(), "elements": pieces}
    return output


def partials() -> dict[str, dict]:
    """活动件使用主控原点的绝对模型坐标；静态分块中绝无这些盒。"""
    bodies = {
        "left_arm": [((7, 20, 10), (13, 23, 22), "steel"), ((8, 15, 12), (12, 20, 17), "brass"), ((9, 13, 13), (11, 15, 16), "steel")],
        "right_arm": [((19, 20, 10), (25, 23, 22), "steel"), ((20, 15, 12), (24, 20, 17), "brass"), ((21, 13, 13), (23, 15, 16), "steel")],
        "fixture": [((14, 11, 12), (18, 14, 20), "steel"), ((13, 14, 13), (19, 16, 19), "brass"), ((15, 16, 14), (17, 18, 18), "steel")],
    }
    return {name: {"credit": "EXT-A-FUEL-02E", "ambientocclusion": True, "textures": textures(),
                   "elements": [element(a, b, mat) for a, b, mat in geometry]} for name, geometry in bodies.items()}


def item_model(parts: dict[int, dict], moving: dict[str, dict]) -> dict:
    """把2格世界模型缩成0..16独立物品，不依赖BE的动态渲染。"""
    gathered: list[dict] = []
    for part, model in parts.items():
        x, z, y = part % 2, (part // 2) % 2, part // 4
        for el in model["elements"]:
            clone = json.loads(json.dumps(el))
            clone["from"] = [(clone["from"][i] + (16 * (x, y, z)[i])) / 2 for i in range(3)]
            clone["to"] = [(clone["to"][i] + (16 * (x, y, z)[i])) / 2 for i in range(3)]
            if "rotation" in clone:
                clone["rotation"]["origin"] = [(clone["rotation"]["origin"][i] + 16 * (x, y, z)[i]) / 2 for i in range(3)]
            gathered.append(clone)
    for model in moving.values():
        for el in model["elements"]:
            clone = json.loads(json.dumps(el))
            clone["from"] = [v / 2 for v in clone["from"]]
            clone["to"] = [v / 2 for v in clone["to"]]
            gathered.append(clone)
    return {"credit": "EXT-A-FUEL-02E", "ambientocclusion": True, "textures": textures(), "elements": gathered,
            "display": {
                "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [.78, .78, .78]},
                "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
                "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2, 0], "scale": [.5, .5, .5]},
                "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [.48, .48, .48]},
                "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [.48, .48, .48]},
                "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [.5, .5, .5]},
                "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [.8, .8, .8]},
            }}


def states() -> None:
    """保持旧机expanded=false旧模型，新机与代理只引用八格外壳。"""
    variants: dict[str, dict] = {}
    proxy: dict[str, dict] = {}
    for facing, turn in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for working, suffix in (("false", "off"), ("true", "on")):
            for expanded in ("false", "true"):
                key = f"expanded={expanded},facing={facing},working={working}"
                model = f"{MODEL_PREFIX}/parts/part_0_{suffix}" if expanded == "true" else f"create_nuclear_industry:block/shielded_assembly_station_{suffix}"
                variants[key] = {"model": model, "y": turn, "uvlock": True}
            for part in range(1, 8):
                proxy[f"facing={facing},part={part},working={working}"] = {"model": f"{MODEL_PREFIX}/parts/part_{part}_{suffix}", "y": turn, "uvlock": True}
    dump(ASSET / "blockstates/shielded_assembly_station.json", {"variants": variants})
    dump(ASSET / "blockstates/shielded_assembly_part.json", {"variants": proxy})


def rig() -> dict:
    """记录未来动画的可行自由度；本批不运行任何运动状态机。"""
    return {
        "task": "EXT-A-FUEL-02E", "units": "16 model units = 1 block", "origin": "north-facing controller minimum corner (0,0,0)",
        "axes": {"x": "east", "y": "up", "z": "south"}, "part_formula": "part=x+2*z+4*y, each of x/y/z in {0,1}",
        "machine_bounds": [[0, 0, 0], [32, 32, 32]], "world_rotation_pivot": [8, 8],
        "world_rotation_rule": "all blocks and partials rotate by facing around the controller block center x=8,z=8; preview may translate the result only to center the canvas",
        "shaft": {"center": [8, 0, 8], "axis": "Y", "source": "Create native shaft"},
        "parts": {
            "left_arm": {"pivot": [9, 21, 15], "axis": "+X", "travel": [0, 1], "idle_bounds": [[7, 13, 10], [13, 23, 22]], "swept_bounds": [[7, 13, 10], [14, 23, 22]]},
            "right_arm": {"pivot": [23, 21, 15], "axis": "-X", "travel": [0, 1], "idle_bounds": [[19, 13, 10], [25, 23, 22]], "swept_bounds": [[18, 13, 10], [25, 23, 22]]},
            "fixture": {"pivot": [16, 13, 16], "axis": "+Y", "travel": [0, 3], "idle_bounds": [[13, 11, 12], [19, 18, 20]], "swept_bounds": [[13, 11, 12], [19, 21, 20]]},
        },
        "static_clearance": "moving x=7..25,y=11..23,z=10..22; inner rails stop at x<=6 or x>=26, top frame y>=26, floor/plinth y<=11; contact at fixture base y=11 is intended",
        "render_contract": "controller renders all three partials once at idle in controller-origin coordinates; proxies render static shell only",
    }


FACE_VERTICES = {
    "north": lambda a, b: [(a[0], a[1], a[2]), (b[0], a[1], a[2]), (b[0], b[1], a[2]), (a[0], b[1], a[2])],
    "south": lambda a, b: [(b[0], a[1], b[2]), (a[0], a[1], b[2]), (a[0], b[1], b[2]), (b[0], b[1], b[2])],
    "east": lambda a, b: [(b[0], a[1], a[2]), (b[0], a[1], b[2]), (b[0], b[1], b[2]), (b[0], b[1], a[2])],
    "west": lambda a, b: [(a[0], a[1], b[2]), (a[0], a[1], a[2]), (a[0], b[1], a[2]), (a[0], b[1], b[2])],
    "up": lambda a, b: [(a[0], b[1], a[2]), (b[0], b[1], a[2]), (b[0], b[1], b[2]), (a[0], b[1], b[2])],
    "down": lambda a, b: [(a[0], a[1], b[2]), (b[0], a[1], b[2]), (b[0], a[1], a[2]), (a[0], a[1], a[2])],
}
NORMAL = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}


def rotated(point: tuple[float, float, float], turns: int, size: int) -> tuple[float, float, float]:
    """围绕整机中心转90度；世界分块坐标与blockstate模型同步旋转。"""
    x, y, z = point
    for _ in range(turns):
        x, z = size - z, x
    return x, y, z


def element_rotated(point: tuple[float, float, float], rotation: dict | None,
                    offset: tuple[int, int, int]) -> tuple[float, float, float]:
    """按Minecraft JSON的y轴element rotation在部件原点附近转斜角。"""
    if not rotation:
        return point
    ox = rotation["origin"][0] + offset[0]
    oz = rotation["origin"][2] + offset[2]
    theta = math.radians(rotation["angle"])
    dx, dz = point[0] - ox, point[2] - oz
    return (ox + dx * math.cos(theta) + dz * math.sin(theta), point[1],
            oz - dx * math.sin(theta) + dz * math.cos(theta))


def render(models: list[tuple[dict, tuple[int, int, int]]], pngs: dict[str, Image.Image],
           target: Path, camera: tuple[float, float, float], turns: int, label: str,
           model_size: int = 32) -> None:
    """从JSON元素和实际PNG逐面采样，逐像素深度测试，预览遮挡与模型一致。"""
    width = 500
    canvas = Image.new("RGB", (width, width), "#1c252b")
    zbuffer = [[-1e20] * width for _ in range(width)]
    cx, cy, cz = camera
    mag = math.sqrt(cx * cx + cy * cy + cz * cz)
    camera = (cx / mag, cy / mag, cz / mag)
    if abs(camera[1]) > .99:
        right = (1, 0, 0)
        up = (0, 0, 1 if camera[1] > 0 else -1)
    else:
        rm = math.sqrt(camera[0] ** 2 + camera[2] ** 2)
        right = (-camera[2] / rm, 0, camera[0] / rm)
        up = (right[1] * camera[2] - right[2] * camera[1],
              right[2] * camera[0] - right[0] * camera[2],
              right[0] * camera[1] - right[1] * camera[0])
    scale = 10.0 if model_size == 32 else 17.0
    center = model_size / 2
    def project(p: tuple[float, float, float]) -> tuple[float, float, float]:
        q = tuple(v - center for v in p)
        return (width / 2 + scale * sum(q[i] * right[i] for i in range(3)),
                width / 2 - scale * sum(q[i] * up[i] for i in range(3)),
                sum(q[i] * camera[i] for i in range(3)))
    pixels = canvas.load()
    def triangle(screen: list[tuple[float, float, float]], uv: list[tuple[float, float]],
                 image: Image.Image, shade: float) -> None:
        a, b, c = screen
        area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])
        if abs(area) < 1e-7:
            return
        lx = max(0, math.floor(min(p[0] for p in screen)))
        hx = min(width - 1, math.ceil(max(p[0] for p in screen)))
        ly = max(0, math.floor(min(p[1] for p in screen)))
        hy = min(width - 1, math.ceil(max(p[1] for p in screen)))
        texel = image.load()
        for sy in range(ly, hy + 1):
            for sx in range(lx, hx + 1):
                px, py = sx + .5, sy + .5
                w1 = ((px - a[0]) * (c[1] - a[1]) - (py - a[1]) * (c[0] - a[0])) / area
                w2 = ((b[0] - a[0]) * (py - a[1]) - (b[1] - a[1]) * (px - a[0])) / area
                w0 = 1 - w1 - w2
                if min(w0, w1, w2) < -1e-6:
                    continue
                depth = w0 * a[2] + w1 * b[2] + w2 * c[2]
                if depth <= zbuffer[sy][sx] + 1e-5:
                    continue
                u = sum(weight * p[0] for weight, p in zip((w0, w1, w2), uv))
                v = sum(weight * p[1] for weight, p in zip((w0, w1, w2), uv))
                color = texel[min(15, max(0, int(u))), min(15, max(0, int(v)))]
                pixels[sx, sy] = tuple(int(ch * shade) for ch in color[:3])
                zbuffer[sy][sx] = depth
    for model, offset in models:
        for el in model["elements"]:
            a = tuple(el["from"][i] + offset[i] for i in range(3))
            b = tuple(el["to"][i] + offset[i] for i in range(3))
            for face, data in el["faces"].items():
                normal = rotated(element_rotated(NORMAL[face],
                                                 {**el["rotation"], "origin": [0, 0, 0]} if "rotation" in el else None,
                                                 (0, 0, 0)), turns, 0)
                if sum(normal[i] * camera[i] for i in range(3)) <= 0:
                    continue
                points = [rotated(element_rotated(p, el.get("rotation"), offset), turns, model_size)
                          for p in FACE_VERTICES[face](a, b)]
                screen = [project(p) for p in points]
                u0, v0, u1, v1 = data["uv"]
                uv = [(u0, v1), (u1, v1), (u1, v0), (u0, v0)]
                texture = model["textures"][data["texture"][1:]].split("/")[-1]
                image = pngs[texture]
                shade = {"north": .88, "south": .75, "east": .68, "west": .82, "up": 1.0, "down": .48}[face]
                for ids in ((0, 1, 2), (0, 2, 3)):
                    triangle([screen[i] for i in ids], [uv[i] for i in ids], image, shade)
    ImageDraw.Draw(canvas).text((12, 10), f"EXT-A-FUEL-02E | {label} | JSON + PNG + depth", fill="#E9E8D9", font=ImageFont.load_default())
    target.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(target)


def composed(parts: dict[int, dict], moving: dict[str, dict]) -> list[tuple[dict, tuple[int, int, int]]]:
    """按冻结part公式把八格与一次活动件还原为北向整机。"""
    whole = [(model, (16 * (part % 2), 16 * (part // 4), 16 * ((part // 2) % 2))) for part, model in parts.items()]
    whole.extend((model, (0, 0, 0)) for model in moving.values())
    return whole


def evidence_previews(parts: dict[int, dict], moving: dict[str, dict], item: dict,
                      pngs: dict[str, Image.Image]) -> None:
    """产生整机、背顶底、物品与独立活动件图，所有图从最终模型重新读取。"""
    folder = EVIDENCE / "previews"
    full = composed(parts, moving)
    for name, camera, turns in (
        ("north-isometric", (1, .85, -1), 0), ("east-isometric", (1, .85, -1), 1),
        ("south-isometric", (1, .85, -1), 2), ("west-isometric", (1, .85, -1), 3),
        ("front", (0, 0, -1), 0), ("back", (0, 0, 1), 0),
        ("top", (0, 1, 0), 0), ("bottom", (0, -1, 0), 0),
    ):
        render(full, pngs, folder / f"{name}.png", camera, turns, name)
    render([(item, (0, 0, 0))], pngs, folder / "item-gui.png", (1, .85, -1), 0, "item-GUI 0..16", 16)
    for name, model in moving.items():
        render([(model, (0, 0, 0))], pngs, folder / f"partial-{name}.png", (1, .85, -1), 0, name)
    motion_diagram(folder)


def motion_diagram(folder: Path) -> None:
    """在真实模型正面与顶面预览上叠加预留行程，不把示意当成运行时动画。"""
    front = Image.open(folder / "front.png").convert("RGBA")
    top = Image.open(folder / "top.png").convert("RGBA")
    overlay = Image.new("RGBA", (1000, 500), (0, 0, 0, 0))
    overlay.paste(front, (0, 0))
    overlay.paste(top, (500, 0))
    sweep = Image.new("RGBA", overlay.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(sweep, "RGBA")
    specs = (("LEFT +X 0..1", (7, 13, 10), (14, 23, 22), (88, 211, 220, 90)),
             ("RIGHT -X 0..1", (18, 13, 10), (25, 23, 22), (231, 191, 98, 90)),
             ("FIXTURE +Y 0..3", (13, 11, 12), (19, 21, 20), (235, 111, 83, 90)))
    for name, lo, hi, color in specs:
        front_rect = (250 + 10 * (lo[0] - 16), 250 - 10 * (hi[1] - 16),
                      250 + 10 * (hi[0] - 16), 250 - 10 * (lo[1] - 16))
        top_rect = (750 + 10 * (lo[0] - 16), 250 - 10 * (hi[2] - 16),
                    750 + 10 * (hi[0] - 16), 250 - 10 * (lo[2] - 16))
        draw.rectangle(front_rect, fill=color, outline=color[:3] + (240,), width=2)
        draw.rectangle(top_rect, fill=color, outline=color[:3] + (240,), width=2)
    overlay = Image.alpha_composite(overlay, sweep)
    legend = ImageDraw.Draw(overlay)
    for index, (name, _, _, color) in enumerate(specs):
        legend.text((515, 420 + 18 * index), name, fill=color[:3] + (255,), font=ImageFont.load_default())
    overlay.convert("RGB").save(folder / "motion-envelope.png")


def check(parts: dict[int, dict], moving: dict[str, dict], item: dict) -> dict:
    """验证本任务引用、局部界与旧贴图字节，绝不修改HEAD内旧PNG。"""
    all_models = list(parts.values()) + list(moving.values()) + [item]
    count = 0
    for model in all_models:
        for el in model["elements"]:
            count += 1
            for i in range(3):
                assert el["from"][i] < el["to"][i]
            for data in el["faces"].values():
                assert data["texture"][1:] in model["textures"]
                assert all(0 <= coordinate <= 16 for coordinate in data["uv"])
                texture = model["textures"][data["texture"][1:]].split("/")[-1]
                assert (ASSET / "textures/block" / f"{texture}.png").is_file()
    for model in parts.values():
        for el in model["elements"]:
            assert all(0 <= v <= 16 for edge in ("from", "to") for v in el[edge])
            for vertex in FACE_VERTICES["up"](el["from"], el["to"]) + FACE_VERTICES["down"](el["from"], el["to"]):
                rotated_vertex = element_rotated(vertex, el.get("rotation"), (0, 0, 0))
                assert all(-1e-5 <= v <= 16 + 1e-5 for v in rotated_vertex)
    for model in moving.values():
        for el in model["elements"]:
            assert all(-16 <= v <= 32 for edge in ("from", "to") for v in el[edge])
    for el in item["elements"]:
        assert all(0 <= v <= 16 for edge in ("from", "to") for v in el[edge])
        for vertex in FACE_VERTICES["up"](el["from"], el["to"]) + FACE_VERTICES["down"](el["from"], el["to"]):
            rotated_vertex = element_rotated(vertex, el.get("rotation"), (0, 0, 0))
            assert all(-1e-5 <= v <= 16 + 1e-5 for v in rotated_vertex)
    states_main = json.loads((ASSET / "blockstates/shielded_assembly_station.json").read_text(encoding="utf-8"))["variants"]
    states_proxy = json.loads((ASSET / "blockstates/shielded_assembly_part.json").read_text(encoding="utf-8"))["variants"]
    assert len(states_main) == 16 and len(states_proxy) == 56
    for state in list(states_main.values()) + list(states_proxy.values()):
        namespace, relative = state["model"].split(":", 1)
        assert namespace == "create_nuclear_industry"
        assert (ASSET / "models" / f"{relative}.json").is_file(), relative
        assert state["y"] in (0, 90, 180, 270)
    assert all((ASSET / "models/block/shielded_assembly_station/parts" / f"part_{part}_{state}.json").is_file()
               for part in range(8) for state in ("off", "on"))
    assert all((ASSET / "models/block/shielded_assembly_station/partials" / f"{name}.json").is_file() for name in moving)
    # 端点行程与静态盒逐对求正体积交集；接触而无穿透允许。
    maximum = {"left_arm": (1, 0, 0), "right_arm": (-1, 0, 0), "fixture": (0, 3, 0)}
    static_collisions = []
    for name, model in moving.items():
        for movement in ((0, 0, 0), maximum[name]):
            for part_el in model["elements"]:
                a = tuple(part_el["from"][i] + movement[i] for i in range(3))
                b = tuple(part_el["to"][i] + movement[i] for i in range(3))
                for fixed_a, fixed_b, _ in STATIC:
                    if all(min(b[i], fixed_b[i]) - max(a[i], fixed_a[i]) > 1e-6 for i in range(3)):
                        static_collisions.append((name, movement, a, b, fixed_a, fixed_b))
    assert not static_collisions, static_collisions[:3]
    old = sorted((ASSET / "textures").rglob("*.png"))
    old = [p for p in old if not p.name.startswith(PREFIX)]
    digest = {p.relative_to(ASSET / "textures").as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in old}
    return {"static_models": 16, "partials": 3, "element_count": count, "main_state_variants": len(states_main),
            "proxy_state_variants": len(states_proxy), "static_collision_count": len(static_collisions),
            "existing_game_png_count": len(old), "existing_game_png_sha256": digest}


def build() -> dict:
    """只重绘新前缀纹理并写任务2允许的资源与证据。"""
    before = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in (ASSET / "textures").rglob("*.png") if not p.name.startswith(PREFIX)}
    pngs: dict[str, Image.Image] = {}
    for name in TEXTURES:
        source = SOURCE / f"{PREFIX}_{name}.svg"
        if not source.is_file():
            raise FileNotFoundError(f"缺SVG稿：{source}；先 --initialize-sources")
        image = strict_exporter.render_svg(source.read_text(encoding="utf-8"), set(COLORS.values()), (16, 16))
        if image.mode != "RGBA" or image.size != (16, 16) or image.getchannel("A").getextrema() != (255, 255):
            raise ValueError(f"{source.name}须产生16×16完全不透明RGBA")
        filename = f"{PREFIX}_{name}.png"
        GENERATED.mkdir(parents=True, exist_ok=True)
        image.save(GENERATED / filename)
        image.save(ASSET / "textures/block" / filename)
        pngs[f"{PREFIX}_{name}"] = image
    design()
    off, on, moving = static_parts(False), static_parts(True), partials()
    parts_dir = ASSET / "models/block/shielded_assembly_station/parts"
    for part in range(8):
        dump(parts_dir / f"part_{part}_off.json", off[part])
        dump(parts_dir / f"part_{part}_on.json", on[part])
    partial_dir = ASSET / "models/block/shielded_assembly_station/partials"
    for name, model in moving.items():
        dump(partial_dir / f"{name}.json", model)
    item = item_model(off, moving)
    dump(ASSET / "models/block/shielded_assembly_station_item.json", item)
    dump(ASSET / "models/item/shielded_assembly_station.json", {"parent": "create_nuclear_industry:block/shielded_assembly_station_item"})
    states()
    dump(SOURCE / "rig.json", rig())
    after = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in (ASSET / "textures").rglob("*.png") if not p.name.startswith(PREFIX)}
    if before != after:
        raise RuntimeError("旧游戏PNG被更改")
    # 预览与核对重新读取落盘JSON，保证看到的是客户端将加载的资源而非内存草稿。
    saved_off = {part: json.loads((parts_dir / f"part_{part}_off.json").read_text(encoding="utf-8")) for part in range(8)}
    saved_moving = {name: json.loads((partial_dir / f"{name}.json").read_text(encoding="utf-8")) for name in moving}
    saved_item = json.loads((ASSET / "models/block/shielded_assembly_station_item.json").read_text(encoding="utf-8"))
    result = check(saved_off, saved_moving, saved_item)
    dump(EVIDENCE / "resource-check.json", result)
    evidence_previews(saved_off, saved_moving, saved_item, pngs)
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--initialize-sources", action="store_true", help="只建立缺少的SVG稿")
    args = parser.parse_args()
    if args.initialize_sources:
        initialize_missing_sources()
    else:
        print(json.dumps({k: v for k, v in build().items() if k != "existing_game_png_sha256"}, ensure_ascii=False, indent=2))
